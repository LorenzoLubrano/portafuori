package io.github.lorenzolubrano.portafuori.widget

import io.github.lorenzolubrano.portafuori.R
import io.github.lorenzolubrano.portafuori.data.StyleId
import io.github.lorenzolubrano.portafuori.ui.theme.Contrast

/** Layout, backgrounds and roundel edges of the widget for each style (spec «stili», section 5). */
object WidgetStyle {
    fun layout(style: StyleId, compact: Boolean): Int = when (style) {
        StyleId.LINEE -> if (compact) R.layout.widget_tonight_compact else R.layout.widget_tonight
        StyleId.ORIGINALE -> if (compact) R.layout.widget_tonight_originale_compact else R.layout.widget_tonight_originale
        StyleId.ANDROID -> if (compact) R.layout.widget_tonight_android_compact else R.layout.widget_tonight_android
    }

    /** Day and night backgrounds, as in the colour resources (Android: the fixed blue, close to the wallpaper ones). */
    fun backgrounds(style: StyleId): Pair<Long, Long> = when (style) {
        StyleId.LINEE -> 0xFFF3F5F9 to 0xFF0B1630
        StyleId.ORIGINALE -> 0xFFF6F3EC to 0xFF1B2420
        StyleId.ANDROID -> 0xFFF9F9FF to 0xFF1E1F25
    }

    /** Under 3:1 on either background: the edge never depends on the theme the widget was drawn in. */
    fun needsEdge(color: Long, style: StyleId): Boolean =
        backgrounds(style).let { (day, night) -> Contrast.needsEdge(color, day) || Contrast.needsEdge(color, night) }

    fun mutedColor(style: StyleId): Int = when (style) {
        StyleId.LINEE -> R.color.widget_text_muted
        StyleId.ORIGINALE -> R.color.widget_originale_text_muted
        StyleId.ANDROID -> R.color.widget_android_text_muted
    }
}
