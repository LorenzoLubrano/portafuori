package io.github.lorenzolubrano.portafuori.ui.calendar

import io.github.lorenzolubrano.portafuori.data.ProfileBundle
import io.github.lorenzolubrano.portafuori.rules.Occurrence
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

    fun weekStart(d: LocalDate): LocalDate = d.minusDays(d.dayOfWeek.value - 1L)

    /** How many bins a month cell hides behind "+N": with more than [MAX_ROUNDELS] it shows MAX_ROUNDELS - 1 roundels. */
    fun overflow(count: Int): Int = if (count > MAX_ROUNDELS) count - (MAX_ROUNDELS - 1) else 0

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
