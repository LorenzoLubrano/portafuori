package io.github.lorenzolubrano.portafuori.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.lorenzolubrano.portafuori.MainActivity
import io.github.lorenzolubrano.portafuori.R
import io.github.lorenzolubrano.portafuori.data.StylePrefs

object Notifications {
    const val CH_EXPOSE = "esponi"
    const val CH_RETRIEVE = "ritira"
    const val CH_HOLIDAY = "festivi"
    const val EXTRA_OPEN = "open"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_EXPOSE, "Esponi i bidoni", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "L'avviso della sera: quali bidoni esporre" },
                NotificationChannel(CH_RETRIEVE, "Ritira i bidoni", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Promemoria per riportare dentro i bidoni" },
                NotificationChannel(CH_HOLIDAY, "Festività e calendario", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Domande sui ritiri nei giorni festivi" },
            ),
        )
    }

    fun channelFor(kind: SlotKind) = when (kind) {
        SlotKind.RETRIEVE -> CH_RETRIEVE
        SlotKind.HOLIDAY_ASK -> CH_HOLIDAY
        else -> CH_EXPOSE
    }

    /** False when the system would silently drop the notification (app or channel blocked). */
    fun canPost(context: Context, channel: String? = null): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (channel != null) {
            val ch = context.getSystemService(NotificationManager::class.java).getNotificationChannel(channel)
            if (ch != null && ch.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }

    /** @return true only if the notification was actually handed to the system. */
    @SuppressLint("MissingPermission")
    fun post(context: Context, s: Slot, late: Boolean): Boolean {
        val channel = channelFor(s.kind)
        if (!canPost(context, channel)) return false
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN, if (s.kind == SlotKind.HOLIDAY_ASK) "holidays" else "today")
        }
        val text = if (late && s.kind != SlotKind.TEST) "In ritardo. ${s.text}" else s.text
        val b = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accentFor(StylePrefs.cached(context), if (Build.VERSION.SDK_INT >= 31) context.getColor(android.R.color.system_accent1_600) else null))
            .setContentTitle(s.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (channel == CH_EXPOSE) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(PendingIntent.getActivity(context, s.notificationId, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            // Gone by itself once the bins may no longer stand outside
            .setTimeoutAfter((s.relevantUntil - System.currentTimeMillis()).coerceAtLeast(60_000))
        val date = s.collectionDate?.toEpochDay() ?: 0
        when (s.kind) {
            SlotKind.EXPOSE, SlotKind.EXPOSE_REPEAT, SlotKind.SNOOZE, SlotKind.PREPARE -> {
                b.addAction(0, "Fatto", action(context, ActionReceiver.ACTION_DONE, s, date))
                b.addAction(0, "Tra 30 min", action(context, ActionReceiver.ACTION_SNOOZE, s, date))
            }
            SlotKind.HOLIDAY_ASK -> {
                b.addAction(0, "Si fa", action(context, ActionReceiver.ACTION_HOLIDAY_KEEP, s, date))
                b.addAction(0, "Salta", action(context, ActionReceiver.ACTION_HOLIDAY_SKIP, s, date))
            }
            else -> Unit
        }
        NotificationManagerCompat.from(context).notify(s.notificationId, b.build())
        return true
    }

    private fun action(context: Context, action: String, s: Slot, date: Long): PendingIntent {
        val i = Intent(context, ActionReceiver::class.java).apply {
            this.action = action
            putExtra(ActionReceiver.EXTRA_PROFILE, s.profileId)
            putExtra(ActionReceiver.EXTRA_DATE, date)
            putExtra(ActionReceiver.EXTRA_NOTIFICATION, s.notificationId)
        }
        return PendingIntent.getBroadcast(
            context, (action + s.key).hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/** Everything that can stop a reminder from arriving on time, checked live. */
data class ReliabilityStatus(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val battery: Boolean,
    /** "Sospendi attività se inutilizzata" turned off (null = not applicable on this Android). */
    val hibernationExempt: Boolean?,
) {
    val allGood get() = notifications && exactAlarms && battery && hibernationExempt != false
}

object Reliability {
    fun status(context: Context): ReliabilityStatus {
        val pm = context.getSystemService(PowerManager::class.java)
        val hibernation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { context.packageManager.isAutoRevokeWhitelisted }.getOrNull()
        } else {
            null
        }
        return ReliabilityStatus(
            notifications = Notifications.canPost(context, Notifications.CH_EXPOSE),
            exactAlarms = AlarmChain.canExact(context),
            battery = pm.isIgnoringBatteryOptimizations(context.packageName),
            hibernationExempt = hibernation,
        )
    }

    fun notificationSettings(context: Context) =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun exactAlarmSettings(context: Context): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        } else {
            null
        }

    /** Allowed because the app is not distributed through Google Play. */
    @SuppressLint("BatteryLife")
    fun batteryExemption(context: Context) =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))

    fun hibernationSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Intent.ACTION_AUTO_REVOKE_PERMISSIONS, Uri.parse("package:${context.packageName}"))
        } else {
            appDetails(context)
        }

    fun appDetails(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
}
