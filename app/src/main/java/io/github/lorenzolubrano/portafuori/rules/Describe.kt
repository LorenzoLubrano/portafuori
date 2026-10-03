package io.github.lorenzolubrano.portafuori.rules

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Italian wording for dates and rules. */
object It {
    val locale: Locale = Locale.ITALIAN

    private val dayMonth = DateTimeFormatter.ofPattern("d/MM", locale)
    private val full = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
    private val weekdayDay = DateTimeFormatter.ofPattern("EEE d/MM", locale)
    private val longDay = DateTimeFormatter.ofPattern("EEEE d MMMM", locale)
    private val time = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val month = DateTimeFormatter.ofPattern("LLLL yyyy", locale)

    fun dayName(d: DayOfWeek): String = when (d) {
        DayOfWeek.MONDAY -> "lunedì"
        DayOfWeek.TUESDAY -> "martedì"
        DayOfWeek.WEDNESDAY -> "mercoledì"
        DayOfWeek.THURSDAY -> "giovedì"
        DayOfWeek.FRIDAY -> "venerdì"
        DayOfWeek.SATURDAY -> "sabato"
        DayOfWeek.SUNDAY -> "domenica"
    }

    fun shortDay(d: DayOfWeek): String = dayName(d).take(3).replaceFirstChar { it.uppercase() }

    fun dayMonth(d: LocalDate): String = d.format(dayMonth)
    fun full(d: LocalDate): String = d.format(full)
    /** "lun 29/09" */
    fun weekdayDay(d: LocalDate): String = d.format(weekdayDay)
    /** "mercoledì 24 settembre" */
    fun longDay(d: LocalDate): String = d.format(longDay)
    fun time(t: LocalTime): String = t.format(time)
    fun month(d: LocalDate): String = d.format(month).replaceFirstChar { it.uppercase() }
    fun monthDay(md: MonthDay): String = "${md.dayOfMonth}/${md.monthValue}"

    /** "a, b e c" */
    fun join(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        else -> items.dropLast(1).joinToString(", ") + " e " + items.last()
    }

    private fun ordinal(n: Int) = if (n == Rule.LAST) "l'ultimo" else "$n°"

    fun describe(rule: Rule): String {
        val days = rule.weekdays.sorted().map(::dayName)
        val base = when (rule.type) {
            RuleType.WEEKLY -> when {
                rule.weekdays.size == 7 -> "Tutti i giorni"
                days.isEmpty() -> "Nessun giorno scelto"
                else -> "Ogni " + join(days)
            }
            RuleType.EVERY_N_WEEKS -> {
                val n = rule.intervalWeeks.coerceAtLeast(1)
                val from = rule.anchor?.let { " (a partire dalla settimana del ${dayMonth(it)})" }.orEmpty()
                "Ogni $n settimane: " + join(days).ifEmpty { "nessun giorno" } + from
            }
            RuleType.MONTHLY_NTH -> {
                val ords = rule.ordinals.sortedWith(compareBy { if (it == Rule.LAST) 99 else it }).map(::ordinal)
                val onlyLast = rule.ordinals == setOf(Rule.LAST)
                val article = if (onlyLast) "" else "Il "
                val text = article + join(ords) + " " + join(days) + " del mese"
                text.replaceFirstChar { it.uppercase() }
            }
            RuleType.FIXED_DATES -> {
                val sorted = rule.dates.sorted()
                val shown = sorted.take(3).joinToString(", ") { dayMonth(it) }
                val more = if (sorted.size > 3) "…" else ""
                when (sorted.size) {
                    0 -> "Nessuna data"
                    1 -> "Solo il ${full(sorted[0])}"
                    else -> "${sorted.size} date: $shown$more"
                }
            }
        }
        val season = if (rule.seasonStart != null && rule.seasonEnd != null) {
            " · dal ${monthDay(rule.seasonStart)} al ${monthDay(rule.seasonEnd)}"
        } else {
            ""
        }
        val validity = when {
            rule.validFrom != null && rule.validUntil != null -> " · valida dal ${full(rule.validFrom)} al ${full(rule.validUntil)}"
            rule.validFrom != null -> " · dal ${full(rule.validFrom)}"
            rule.validUntil != null -> " · fino al ${full(rule.validUntil)}"
            else -> ""
        }
        return base + season + validity
    }

    fun describeAll(rules: List<Rule>): String =
        if (rules.isEmpty()) "Nessuna regola: aggiungi i giorni di raccolta" else rules.joinToString("; ") { describe(it) }
}
