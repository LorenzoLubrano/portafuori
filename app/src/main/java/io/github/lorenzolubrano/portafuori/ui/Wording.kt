package io.github.lorenzolubrano.portafuori.ui

import io.github.lorenzolubrano.portafuori.data.Evening
import io.github.lorenzolubrano.portafuori.data.Presets
import io.github.lorenzolubrano.portafuori.rules.It
import io.github.lorenzolubrano.portafuori.ui.calendar.Stop
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

    fun weekTitle(start: LocalDate, morning: Boolean = false): String {
        val end = start.plusDays(6)
        val dal = if (elided(start.dayOfMonth)) "dall'" else "dal "
        val al = if (elided(end.dayOfMonth)) "all'" else "al "
        val startPart = when {
            start.year != end.year -> "${start.dayOfMonth} ${monthName(start)} ${start.year}"
            start.month != end.month -> "${start.dayOfMonth} ${monthName(start)}"
            else -> "${start.dayOfMonth}"
        }
        val endPart = "${end.dayOfMonth} ${monthName(end)}" + if (start.year != end.year) " ${end.year}" else ""
        return (if (morning) "Mattine" else "Sere") + " $dal$startPart $al$endPart"
    }

    fun monthTitle(m: YearMonth, morning: Boolean = false) = (if (morning) "Mattine" else "Sere") + " di ${m.month.getDisplayName(TextStyle.FULL, It.locale)} ${m.year}"

    fun upcomingTitle(morning: Boolean) = if (morning) "Prossime mattine" else "Prossime sere"

    /** "Carta: mercoledì, sabato festivo da confermare; spostato lunedì": a bin's week line, as TalkBack reads it. */
    fun binWeek(name: String, days: List<Pair<Stop, String>>, morning: Boolean): String {
        val out = days.mapNotNull { (stop, day) ->
            when (stop) {
                Stop.OUT -> day
                Stop.PENDING -> "$day festivo da confermare"
                else -> null
            }
        }.joinToString().ifEmpty { if (morning) "nessuna mattina" else "nessuna sera" }
        fun gone(stop: Stop, one: String, many: String): String {
            val list = days.filter { it.first == stop }.map { it.second }
            return if (list.isEmpty()) "" else "; " + (if (list.size == 1) one else many) + " " + list.joinToString()
        }
        return "$name: $out" + gone(Stop.MOVED, "spostato", "spostati") + gone(Stop.SKIPPED, "saltato", "saltati")
    }

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

    // the editor's palette, in its order: one list of colours, here only their names
    private val colourNames = Presets.colors.zip(
        listOf("Marrone", "Giallo", "Blu", "Verde", "Grigio", "Azzurro", "Verde oliva", "Viola", "Rosso", "Arancione", "Nero", "Grigio chiaro"),
    ).also { check(it.size == Presets.colors.size) }

    /** Name of the nearest palette colour: imported bins may carry any colour, TalkBack still needs a word. */
    fun colorName(argb: Long): String {
        fun ch(c: Long, s: Int) = ((c shr s) and 0xFF).toInt()
        return colourNames.minBy { (c, _) ->
            val dr = ch(c, 16) - ch(argb, 16); val dg = ch(c, 8) - ch(argb, 8); val db = ch(c, 0) - ch(argb, 0)
            dr * dr + dg * dg + db * db
        }.second
    }
}
