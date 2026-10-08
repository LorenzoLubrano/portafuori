package io.github.lorenzolubrano.portafuori.widget

import io.github.lorenzolubrano.portafuori.Brand
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import io.github.lorenzolubrano.portafuori.MainActivity
import io.github.lorenzolubrano.portafuori.R
import io.github.lorenzolubrano.portafuori.data.Bin
import io.github.lorenzolubrano.portafuori.data.Evening
import io.github.lorenzolubrano.portafuori.data.ProfileBundle
import io.github.lorenzolubrano.portafuori.reminders.ActionReceiver
import io.github.lorenzolubrano.portafuori.reminders.Engine
import io.github.lorenzolubrano.portafuori.reminders.Planner
import io.github.lorenzolubrano.portafuori.reminders.eveningNotificationId
import io.github.lorenzolubrano.portafuori.rules.ROME
import io.github.lorenzolubrano.portafuori.ui.theme.Contrast
import java.time.LocalDate

class TonightWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Engine.runAsync(context, goAsync())
    }

    // a resize can switch between the compact and the full layout
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
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
            val bundle = bundles.firstOrNull { it.profile.id == selectedId } ?: bundles.firstOrNull()
            ids.forEach { id -> manager.updateAppWidget(id, build(context, bundle, compact(manager, id))) }
        }

        /** One row high on a phone in portrait (the height there is the options' maximum): roundels beside the title. */
        private fun compact(manager: AppWidgetManager, id: Int): Boolean =
            manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) in 1 until 150

        private fun build(context: Context, bundle: ProfileBundle?, compact: Boolean): RemoteViews {
            val v = RemoteViews(context.packageName, if (compact) R.layout.widget_tonight_compact else R.layout.widget_tonight)
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            v.setOnClickPendingIntent(R.id.widget_root, open)
            v.setViewVisibility(R.id.widget_done, View.GONE)
            v.setViewVisibility(R.id.widget_done_state, View.GONE)
            v.setViewVisibility(R.id.widget_roundels, View.GONE)
            // without roundels the secondary line has room in the one-row layout too
            v.setViewVisibility(R.id.widget_sub, View.VISIBLE)
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
                    v.setImageViewBitmap(R.id.widget_roundels, roundels(context, e.bins))
                    v.setContentDescription(R.id.widget_roundels, e.binNames)
                    v.setViewVisibility(R.id.widget_roundels, View.VISIBLE)
                    v.setTextViewText(R.id.widget_bins, names(e))
                    v.setTextViewText(R.id.widget_sub, Planner.windowText(e))
                    if (compact) v.setViewVisibility(R.id.widget_sub, View.GONE)
                    // done: a check and «Esposti» take the button's place, in both layouts
                    if (e.done) v.setViewVisibility(R.id.widget_done_state, View.VISIBLE)
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

        /** Bin names on as few lines as possible: a line breaks only between one bin and the next. */
        private fun names(e: Evening): String = e.bins.joinToString(" · ") { it.name.replace(' ', '\u00A0') }

        private const val MAX_ROUNDELS = 6

        /** Tonight's bins as Linee roundels: colour + icon, a dark edge on pale colours, at most [MAX_ROUNDELS]. */
        private fun roundels(context: Context, bins: List<Bin>): Bitmap {
            val d = context.resources.displayMetrics.density
            val shown = bins.take(MAX_ROUNDELS)
            val size = (28 * d).toInt()
            val gap = (6 * d).toInt()
            val height = (32 * d).toInt()
            val bmp = Bitmap.createBitmap(maxOf(1, shown.size * size + (shown.size - 1).coerceAtLeast(0) * gap), height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val bg = ContextCompat.getColor(context, R.color.widget_bg).toLong() and 0xFFFFFFFFL
            val edge = ContextCompat.getColor(context, R.color.widget_text_muted)
            val top = (height - size) / 2f
            shown.forEachIndexed { i, bin ->
                val color = bin.entity.colorArgb
                val left = i * (size + gap).toFloat()
                val r = size / 2f
                paint.style = Paint.Style.FILL
                paint.color = color.toInt()
                canvas.drawCircle(left + r, top + r, r, paint)
                if (Contrast.needsEdge(color, bg)) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1.5f * d
                    paint.color = edge
                    canvas.drawCircle(left + r, top + r, r - paint.strokeWidth / 2, paint)
                }
                val inset = (6 * d).toInt()
                ContextCompat.getDrawable(context, iconRes(bin.entity.iconKey))!!.mutate().apply {
                    setTint(Contrast.onColor(color).toInt())
                    setBounds(left.toInt() + inset, top.toInt() + inset, left.toInt() + size - inset, top.toInt() + size - inset)
                }.draw(canvas)
            }
            return bmp
        }

        private fun iconRes(key: String): Int = when (key) {
            "compost" -> R.drawable.ic_bin_compost
            "bottle" -> R.drawable.ic_bin_bottle
            "paper" -> R.drawable.ic_bin_paper
            "glass" -> R.drawable.ic_bin_glass
            "trash" -> R.drawable.ic_bin_trash
            "recycle" -> R.drawable.ic_bin_recycle
            "grass" -> R.drawable.ic_bin_grass
            "baby" -> R.drawable.ic_bin_baby
            "bag" -> R.drawable.ic_bin_bag
            "box" -> R.drawable.ic_bin_box
            "battery" -> R.drawable.ic_bin_battery
            "oil" -> R.drawable.ic_bin_oil
            else -> R.drawable.ic_bin_trash
        }
    }
}
