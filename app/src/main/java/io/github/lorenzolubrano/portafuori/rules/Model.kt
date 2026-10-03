package io.github.lorenzolubrano.portafuori.rules

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay

/** Does the official calendar list the collection day, or the evening you put the bin out? */
enum class CalendarMode { COLLECTION_DAY, EXPOSE_DAY }

/** When the bin goes out relative to the collection day. */
enum class ExposureMode { EVENING_BEFORE, SAME_MORNING }

/** What to do when a collection falls on a public holiday and the user has not decided yet. */
enum class HolidayPolicy { ASK, KEEP, SKIP }

enum class RuleType { WEEKLY, EVERY_N_WEEKS, MONTHLY_NTH, FIXED_DATES }

/** SKIP/MOVE/ADD edit a single calendar date; KEEP confirms a collection that falls on a holiday. */
enum class ExceptionKind { SKIP, MOVE, ADD, KEEP }

data class Rule(
    val type: RuleType,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val intervalWeeks: Int = 1,
    /** Any date in a week when EVERY_N_WEEKS collects. */
    val anchor: LocalDate? = null,
    /** 1..5 = first..fifth weekday of the month, [LAST] = last one. */
    val ordinals: Set<Int> = emptySet(),
    val dates: Set<LocalDate> = emptySet(),
    val seasonStart: MonthDay? = null,
    val seasonEnd: MonthDay? = null,
    val validFrom: LocalDate? = null,
    val validUntil: LocalDate? = null,
) {
    companion object {
        const val LAST = -1
    }
}

data class BinRules(val binId: Long, val rules: List<Rule>)

/** Dates are calendar dates, i.e. the day as printed on the official calendar. */
data class DateException(
    val binId: Long,
    val kind: ExceptionKind,
    val date: LocalDate,
    val target: LocalDate? = null,
)

/** A local holiday (e.g. the patron saint): yearly if [monthDay] is set, one-off if [date] is set. */
data class LocalHoliday(val name: String, val monthDay: MonthDay? = null, val date: LocalDate? = null)

data class ProfileRules(
    val calendarMode: CalendarMode,
    val exposureMode: ExposureMode,
    val exposeStart: LocalTime,
    val exposeEnd: LocalTime,
    val holidayPolicy: HolidayPolicy,
    val localHolidays: List<LocalHoliday> = emptyList(),
)

enum class Origin { RULE, MOVED_IN, ADDED }

enum class Status { ACTIVE, SKIPPED, MOVED_OUT, HOLIDAY_PENDING, HOLIDAY_SKIPPED }

data class Occurrence(
    val binId: Long,
    val calendarDate: LocalDate,
    val collectionDate: LocalDate,
    val origin: Origin,
    val status: Status,
    val movedFrom: LocalDate? = null,
    val movedTo: LocalDate? = null,
    /** Name of the holiday on the collection date, if any. */
    val holiday: String? = null,
) {
    /** Still happening: reminders fire for it (a pending holiday decision keeps it until the user says otherwise). */
    val isActive get() = status == Status.ACTIVE || status == Status.HOLIDAY_PENDING
}
