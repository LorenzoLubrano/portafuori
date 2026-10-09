package io.github.lorenzolubrano.portafuori.ui

import io.github.lorenzolubrano.portafuori.data.Evening
import io.github.lorenzolubrano.portafuori.rules.It
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle

/** Texts the screens compute (spec section 6). Plain Kotlin so they are unit-tested. */
object Wording {
    fun hm(t: LocalTime): String = It.time(t)

    fun from(e: Evening): String = from(e.window.start.toLocalTime())
    fun from(t: LocalTime): String = if (t.hour == 1) "Dall'${hm(t)}" else "Dalle ${hm(t)}"

    fun until(e: Evening): String = until(e.window.start.toLocalDate(), e.window.end.toLocalDate(), e.window.end.toLocalTime())
    fun until(startDay: LocalDate, endDay: LocalDate, end: LocalTime): String {
        val at = if (end.hour == 1) "l'${hm(end)}" else "le ${hm(end)}"
        val day = when (endDay) {
            startDay -> ""
            startDay.plusDays(1) -> " di ${It.dayName(endDay.dayOfWeek)}"
            else -> " di ${It.longDay(endDay)}"
        }
        return "al più tardi entro $at$day"
    }

    fun headline(label: String) = "$label porta fuori"
    fun doneButton(bins: Int) = if (bins == 1) "Fatto, l'ho portato fuori" else "Fatto, li ho portati fuori"
    fun doneState(bins: Int) = if (bins == 1) "Fatto: è fuori" else "Fatto: sono fuori"

    fun ritiro(d: LocalDate) = "ritiro ${It.dayName(d.dayOfWeek).take(3)} ${d.dayOfMonth}"
    fun ritiroLong(d: LocalDate) = "ritiro ${It.dayName(d.dayOfWeek)} ${d.dayOfMonth}"
    fun eveningTitle(d: LocalDate, morning: Boolean = false) = It.dayName(d.dayOfWeek).replaceFirstChar { it.uppercase() } + " ${d.dayOfMonth} " + if (morning) "mattina" else "sera"

    private fun elided(day: Int) = day == 1 || day == 8 || day == 11
    private fun monthName(d: LocalDate) = d.month.getDisplayName(TextStyle.FULL, It.locale)

    fun weekTitle(start: LocalDate): String {
        val end = start.plusDays(6)
        val dal = if (elided(start.dayOfMonth)) "dall'" else "dal "
        val al = if (elided(end.dayOfMonth)) "all'" else "al "
        val startPart = when {
            start.year != end.year -> "${start.dayOfMonth} ${monthName(start)} ${start.year}"
            start.month != end.month -> "${start.dayOfMonth} ${monthName(start)}"
            else -> "${start.dayOfMonth}"
        }
        val endPart = "${end.dayOfMonth} ${monthName(end)}" + if (start.year != end.year) " ${end.year}" else ""
        return "Sere $dal$startPart $al$endPart"
    }

    fun monthTitle(m: YearMonth) = "Sere di ${m.month.getDisplayName(TextStyle.FULL, It.locale)} ${m.year}"

    fun iconName(key: String): String = when (key) {
        "compost" -> "Organico"
        "bottle" -> "Bottiglia"
        "paper" -> "Carta"
        "glass" -> "Vetro"
        "recycle" -> "Riciclo"
        "grass" -> "Erba"
        "baby" -> "Pannolino"
        "bag" -> "Sacchetto"
        "box" -> "Scatola"
        "battery" -> "Pila"
        "oil" -> "Olio"
        else -> "Cestino"
    }

    private val colourNames = listOf(
        0xFF8D5B3A to "Marrone", 0xFFF2C230 to "Giallo", 0xFF2F6FD6 to "Blu", 0xFF2E9E5B to "Verde",
        0xFF6B7280 to "Grigio", 0xFF1FA2C7 to "Azzurro", 0xFF6E8B3D to "Verde oliva", 0xFF8E6CC1 to "Viola",
        0xFFD64545 to "Rosso", 0xFFE67E22 to "Arancione", 0xFF222222 to "Nero", 0xFFB0B7C3 to "Grigio chiaro",
    )

    /** Name of the nearest palette colour: imported bins may carry any colour, TalkBack still needs a word. */
    fun colorName(argb: Long): String {
        fun ch(c: Long, s: Int) = ((c shr s) and 0xFF).toInt()
        return colourNames.minBy { (c, _) ->
            val dr = ch(c, 16) - ch(argb, 16); val dg = ch(c, 8) - ch(argb, 8); val db = ch(c, 0) - ch(argb, 0)
            dr * dr + dg * dg + db * db
        }.second
    }
}
