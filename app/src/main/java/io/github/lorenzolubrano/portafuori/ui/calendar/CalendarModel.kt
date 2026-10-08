package io.github.lorenzolubrano.portafuori.ui.calendar

import io.github.lorenzolubrano.portafuori.data.ProfileBundle
import io.github.lorenzolubrano.portafuori.rules.Occurrence
import io.github.lorenzolubrano.portafuori.rules.Schedule
import io.github.lorenzolubrano.portafuori.rules.Status
import java.time.LocalDate

/** How a bin stands on one evening of the calendar. */
enum class Stop { NONE, OUT, PENDING, SKIPPED }

/** Everything put out on one evening, active or not, so skipped and moved collections stay visible. */
data class Night(val evening: LocalDate, val occurrences: List<Occurrence>) {
    /** The calendar date the day sheet works on (its actions take calendar dates). */
    val calendarDate: LocalDate get() = occurrences.first().calendarDate
    val collectionDate: LocalDate get() = occurrences.first().collectionDate
    val activeBinIds: List<Long> get() = occurrences.filter { it.isActive }.map { it.binId }.distinct()

    fun stopOf(binId: Long): Stop {
        val mine = occurrences.filter { it.binId == binId }
        return when {
            mine.isEmpty() -> Stop.NONE
            mine.any { it.status == Status.HOLIDAY_PENDING } -> Stop.PENDING
            mine.any { it.isActive } -> Stop.OUT
            else -> Stop.SKIPPED
        }
    }
}

object CalendarModel {
    /** Roundels a month cell can hold before it shows "+N". */
    const val MAX_ROUNDELS = 4
    /** The same at large text, where each roundel is drawn bigger. */
    const val MAX_ROUNDELS_BIG = 2

    fun weekStart(d: LocalDate): LocalDate = d.minusDays(d.dayOfWeek.value - 1L)

    /** How many bins a month cell hides behind "+N": with more than [max] places it shows max - 1 roundels. */
    fun overflow(count: Int, max: Int = MAX_ROUNDELS): Int = if (count > max) count - (max - 1) else 0

    /** The collection date whose bins go out on [evening] (the next day, or the same morning). */
    private fun collectionOf(b: ProfileBundle, evening: LocalDate): LocalDate {
        val next = evening.plusDays(1)
        return if (b.eveningOf(next) == evening) next else evening
    }

    /** The calendar date whose bins go out on [evening]: what the day sheet opens, even on an evening with nothing on it. */
    fun calendarDateOf(b: ProfileBundle, evening: LocalDate): LocalDate = Schedule.calendarDate(b.rules, collectionOf(b, evening))

    /** The public holiday on the collection that goes out on [evening], if any. */
    fun holidayOf(b: ProfileBundle, evening: LocalDate): String? = b.holidayCalendar.nameOf(collectionOf(b, evening))

    /**
     * Nights whose evening falls in [from, to]. A collection goes out at most the evening before,
     * and a calendar date can sit one day before its collection, hence the wider query.
     */
    fun nights(b: ProfileBundle, from: LocalDate, to: LocalDate): Map<LocalDate, Night> =
        b.occurrences(from.minusDays(1), to.plusDays(2))
            .groupBy { b.eveningOf(it.collectionDate) }
            .filterKeys { !it.isBefore(from) && !it.isAfter(to) }
            .mapValues { (evening, occ) -> Night(evening, occ) }
}
