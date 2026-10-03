package io.github.lorenzolubrano.portafuori.widget

import io.github.lorenzolubrano.portafuori.Brand
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.RemoteViews
import io.github.lorenzolubrano.portafuori.MainActivity
import io.github.lorenzolubrano.portafuori.R
import io.github.lorenzolubrano.portafuori.data.Evening
import io.github.lorenzolubrano.portafuori.data.ProfileBundle
import io.github.lorenzolubrano.portafuori.reminders.ActionReceiver
import io.github.lorenzolubrano.portafuori.reminders.Engine
import io.github.lorenzolubrano.portafuori.reminders.Planner
import io.github.lorenzolubrano.portafuori.reminders.eveningNotificationId
import io.github.lorenzolubrano.portafuori.rules.ROME
import java.time.LocalDate

class TonightWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Engine.runAsync(context, goAsync())
    }

    companion object {
        /** What the widget and the "Stasera" card show: the first collection whose window has not closed yet. */
        fun nextEvening(bundle: ProfileBundle, now: Long): Evening? {
            val today = LocalDate.now(ROME)
            return bundle.evenings(today.minusDays(2), today.plusDays(21))
                .firstOrNull { it.window.end.toInstant().toEpochMilli() > now && !it.paused }
        }

        fun isTonight(e: Evening, now: Long): Boolean =
            e.window.start.toLocalDate() <= LocalDate.now(ROME) || e.window.start.toInstant().toEpochMilli() <= now

        fun updateAll(context: Context, bundles: List<ProfileBundle>, selectedId: Long?) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TonightWidget::class.java))
            if (ids.isEmpty()) return
            val views = build(context, bundles.firstOrNull { it.profile.id == selectedId } ?: bundles.firstOrNull())
            manager.updateAppWidget(ids, views)
        }

        private fun build(context: Context, bundle: ProfileBundle?): RemoteViews {
            val v = RemoteViews(context.packageName, R.layout.widget_tonight)
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            v.setOnClickPendingIntent(R.id.widget_root, open)
            v.setViewVisibility(R.id.widget_done, View.GONE)
            if (bundle == null || bundle.bins.isEmpty()) {
                v.setTextViewText(R.id.widget_title, "${Brand.NAME}")
                v.setTextViewText(R.id.widget_bins, "Configura il calendario")
                v.setTextViewText(R.id.widget_sub, "Tocca per iniziare")
                return v
            }
            val now = System.currentTimeMillis()
            val e = nextEvening(bundle, now)
            when {
                e == null -> {
                    v.setTextViewText(R.id.widget_title, "Stasera")
                    v.setTextViewText(R.id.widget_bins, "Niente")
                    v.setTextViewText(R.id.widget_sub, "Nessun ritiro nelle prossime 3 settimane")
                }
                isTonight(e, now) -> {
                    v.setTextViewText(R.id.widget_title, Planner.label(e, now))
                    v.setTextViewText(R.id.widget_bins, dots(e))
                    v.setTextViewText(R.id.widget_sub, if (e.done) "✓ Esposti" else Planner.windowText(e))
                    if (!e.done) {
                        v.setViewVisibility(R.id.widget_done, View.VISIBLE)
                        val i = Intent(context, ActionReceiver::class.java).apply {
                            action = ActionReceiver.ACTION_DONE
                            putExtra(ActionReceiver.EXTRA_PROFILE, e.profileId)
                            putExtra(ActionReceiver.EXTRA_DATE, e.collectionDate.toEpochDay())
                            putExtra(ActionReceiver.EXTRA_NOTIFICATION, eveningNotificationId(e.profileId, e.collectionDate))
                        }
                        v.setOnClickPendingIntent(
                            R.id.widget_done,
                            PendingIntent.getBroadcast(context, 7, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
                        )
                    }
                }
                else -> {
                    v.setTextViewText(R.id.widget_title, "Stasera")
                    v.setTextViewText(R.id.widget_bins, "Niente")
                    v.setTextViewText(R.id.widget_sub, Planner.label(e, now) + ": " + e.binNames)
                }
            }
            return v
        }

        private fun dots(e: Evening): CharSequence {
            val sb = SpannableStringBuilder()
            e.bins.forEachIndexed { i, b ->
                if (i > 0) sb.append("  ")
                val start = sb.length
                sb.append("●")
                sb.setSpan(ForegroundColorSpan(b.entity.colorArgb.toInt()), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.append(" ").append(b.name)
            }
            return sb
        }
    }
}
