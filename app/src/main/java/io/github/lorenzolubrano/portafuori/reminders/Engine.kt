package io.github.lorenzolubrano.portafuori.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.lorenzolubrano.portafuori.App
import io.github.lorenzolubrano.portafuori.data.AppSettings
import io.github.lorenzolubrano.portafuori.data.DeliveryEntity
import io.github.lorenzolubrano.portafuori.data.ExceptionEntity
import io.github.lorenzolubrano.portafuori.data.ExceptionReason
import io.github.lorenzolubrano.portafuori.rules.ExceptionKind
import io.github.lorenzolubrano.portafuori.rules.ROME
import io.github.lorenzolubrano.portafuori.rules.Schedule
import io.github.lorenzolubrano.portafuori.widget.TonightWidget
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The single alarm chain: shows every due reminder that was not shown yet (late ones too, while
 * still useful), logs it, refreshes the widget and arms one alarm for the next event.
 */
object Engine {
    private const val TAG = "Engine"
    private val mutex = Mutex()
    private const val LATE_MS = 15 * 60_000L
    private const val LOOKBACK_MS = 3 * 24 * 3600_000L
    private const val RETRY_MS = 30 * 60_000L

    /** Like runCatching, but never swallows coroutine cancellation. */
    private inline fun <T> safely(what: String, block: () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        Log.e(TAG, what, t)
        null
    }

    suspend fun run(context: Context) = withContext(Dispatchers.Default) {
        mutex.withLock {
            val app = context.applicationContext as App
            val repo = app.repository
            val now = System.currentTimeMillis()
            val midnight = ZonedDateTime.of(LocalDate.now(ROME).plusDays(1), LocalTime.of(0, 5), ROME).toInstant().toEpochMilli()
            // Whatever fails below, the chain is re-armed: reminders must never stop for good
            var armAt = minOf(now + RETRY_MS, midnight)
            try {
                val bundles = repo.bundlesNow()
                val settings = safely("settings") { app.settings.now() } ?: AppSettings()
                val snoozes = safely("snoozes") { repo.snoozesNow() }.orEmpty()
                val slots = Planner.plan(bundles, snoozes, settings.testAt, now)
                val logged = safely("log keys") { repo.loggedKeysSince(now - LOOKBACK_MS) }.orEmpty()
                val createdAt = bundles.associate { it.profile.id to it.profile.createdAt }

                for (s in slots) {
                    if (s.at > now + 30_000) break
                    if (s.at < now - LOOKBACK_MS || s.key in logged) continue
                    // Never replay reminders from before the profile existed
                    if (s.kind != SlotKind.TEST && s.at < (createdAt[s.profileId] ?: Long.MAX_VALUE)) continue
                    val useful = now <= s.relevantUntil
                    // Blocked notifications are not logged, so they show up as soon as they are allowed again
                    if (useful && safely("post") { Notifications.post(context, s, late = now - s.at > LATE_MS) } != true) continue
                    safely("log") { repo.log(DeliveryEntity(s.key, s.kind.name, s.profileId, s.title, s.at, if (useful) now else null)) }
                    if (s.kind == SlotKind.TEST) safely("test reset") { app.settings.setTestAt(null) }
                }

                armAt = minOf(slots.firstOrNull { it.at > now + 30_000 }?.at ?: Long.MAX_VALUE, midnight)
                safely("widget") { TonightWidget.updateAll(context, bundles, settings.selectedProfileId) }
                safely("prune") { repo.prune(now) }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Log.e(TAG, "planning failed, retrying later", t)
            } finally {
                safely("arm") { AlarmChain.arm(context, armAt) }
            }
        }
    }

    /** Fire-and-forget from places that cannot suspend. */
    fun runAsync(context: Context, pending: BroadcastReceiver.PendingResult? = null) {
        scope.launch {
            try {
                run(context)
            } finally {
                pending?.finish()
            }
        }
    }

    val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, t -> Log.e(TAG, "uncaught", t) },
    )
}

object AlarmChain {
    private const val REQUEST = 1

    fun canExact(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    fun arm(context: Context, at: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, REQUEST, Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (canExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setWindow(AlarmManager.RTC_WAKEUP, at, 10 * 60_000L, pi)
        }
    }

    /** Next alarm time as the system sees it, for the reliability screen. */
    fun nextArmed(context: Context): Boolean = PendingIntent.getBroadcast(
        context, REQUEST, Intent(context, AlarmReceiver::class.java),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    ) != null
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Engine.runAsync(context, goAsync())
}

/** Boot, app update, clock/timezone change, exact-alarm permission change. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Exported only for these system broadcasts, which other apps are not allowed to send
        if (intent.action !in TRIGGERS) return
        Engine.runAsync(context, goAsync())
    }

    private companion object {
        val TRIGGERS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}

/** Buttons on the notifications. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as App
        val profileId = intent.getLongExtra(EXTRA_PROFILE, 0)
        val collection = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DATE, 0))
        NotificationManagerCompat.from(context).cancel(intent.getIntExtra(EXTRA_NOTIFICATION, 0))
        val pending = goAsync()
        Engine.scope.launch {
            try {
                val repo = app.repository
                when (intent.action) {
                    ACTION_DONE -> repo.markDone(profileId, collection)
                    ACTION_SNOOZE -> repo.snooze(profileId, collection, System.currentTimeMillis() + 30 * 60_000L)
                    ACTION_HOLIDAY_KEEP, ACTION_HOLIDAY_SKIP -> {
                        val bundle = repo.bundlesNow().firstOrNull { it.profile.id == profileId } ?: return@launch
                        val calendarDate = Schedule.calendarDate(bundle.rules, collection)
                        val evening = bundle.evenings(calendarDate, calendarDate).firstOrNull() ?: return@launch
                        val kind = if (intent.action == ACTION_HOLIDAY_KEEP) ExceptionKind.KEEP else ExceptionKind.SKIP
                        repo.addExceptions(
                            evening.pendingBins.map {
                                ExceptionEntity(
                                    profileId = profileId, binId = it.id, kind = kind,
                                    date = calendarDate, reason = ExceptionReason.HOLIDAY,
                                )
                            },
                        )
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "io.github.lorenzolubrano.portafuori.DONE"
        const val ACTION_SNOOZE = "io.github.lorenzolubrano.portafuori.SNOOZE"
        const val ACTION_HOLIDAY_KEEP = "io.github.lorenzolubrano.portafuori.HOLIDAY_KEEP"
        const val ACTION_HOLIDAY_SKIP = "io.github.lorenzolubrano.portafuori.HOLIDAY_SKIP"
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_DATE = "date"
        const val EXTRA_NOTIFICATION = "notification"
    }
}

/** Safety net against OEM battery managers: replays missed reminders and re-arms the chain. */
class WatchdogWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Engine.run(applicationContext)
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<WatchdogWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("watchdog", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ROME).toLocalDate()
