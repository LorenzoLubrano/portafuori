package io.github.lorenzolubrano.portafuori.rules

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

val ROME: ZoneId = ZoneId.of("Europe/Rome")

data class ExposureWindow(val start: ZonedDateTime, val end: ZonedDateTime)

object Schedule {

    fun matches(rule: Rule, date: LocalDate): Boolean {
        if (rule.validFrom != null && date < rule.validFrom) return false
        if (rule.validUntil != null && date > rule.validUntil) return false
        if (!inSeason(rule.seasonStart, rule.seasonEnd, date)) return false
        return when (rule.type) {
            RuleType.WEEKLY -> date.dayOfWeek in rule.weekdays
            RuleType.EVERY_N_WEEKS -> {
                val anchor = rule.anchor ?: return false
                date.dayOfWeek in rule.weekdays &&
                    Math.floorMod(weeksBetween(anchor, date), rule.intervalWeeks.coerceAtLeast(1).toLong()) == 0L
            }
            RuleType.MONTHLY_NTH -> date.dayOfWeek in rule.weekdays && (
                ordinalInMonth(date) in rule.ordinals ||
                    (Rule.LAST in rule.ordinals && date.plusWeeks(1).month != date.month)
                )
            RuleType.FIXED_DATES -> date in rule.dates
        }
    }

    /** 1 for days 1-7, 2 for days 8-14, ... 5 for days 29-31. */
    fun ordinalInMonth(date: LocalDate) = (date.dayOfMonth - 1) / 7 + 1

    fun inSeason(start: MonthDay?, end: MonthDay?, date: LocalDate): Boolean {
        if (start == null || end == null) return true
        val md = MonthDay.from(date)
        return if (start <= end) md in start..end else md >= start || md <= end
    }

    private fun weeksBetween(a: LocalDate, b: LocalDate): Long {
        val monday = TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        return ChronoUnit.WEEKS.between(a.with(monday), b.with(monday))
    }

    fun collectionDate(p: ProfileRules, calendarDate: LocalDate): LocalDate =
        if (p.calendarMode == CalendarMode.EXPOSE_DAY && p.exposureMode == ExposureMode.EVENING_BEFORE) {
            calendarDate.plusDays(1)
        } else {
            calendarDate
        }

    /** Inverse of [collectionDate]. */
    fun calendarDate(p: ProfileRules, collectionDate: LocalDate): LocalDate =
        if (p.calendarMode == CalendarMode.EXPOSE_DAY && p.exposureMode == ExposureMode.EVENING_BEFORE) {
            collectionDate.minusDays(1)
        } else {
            collectionDate
        }

    /**
     * When the bin may stand on the street. Evening mode starts the day before the collection;
     * morning mode ends on the collection day. Times that do not exist (DST gap) move forward.
     */
    fun exposureWindow(p: ProfileRules, collection: LocalDate, zone: ZoneId = ROME): ExposureWindow {
        val crossesMidnight = !p.exposeEnd.isAfter(p.exposeStart)
        return when (p.exposureMode) {
            ExposureMode.EVENING_BEFORE -> {
                val startDay = collection.minusDays(1)
                val endDay = if (crossesMidnight) collection else startDay
                ExposureWindow(ZonedDateTime.of(startDay, p.exposeStart, zone), ZonedDateTime.of(endDay, p.exposeEnd, zone))
            }
            ExposureMode.SAME_MORNING -> {
                val startDay = if (crossesMidnight) collection.minusDays(1) else collection
                ExposureWindow(ZonedDateTime.of(startDay, p.exposeStart, zone), ZonedDateTime.of(collection, p.exposeEnd, zone))
            }
        }
    }

    /** Every occurrence (active or not) of every bin whose calendar date is in [from, to]. */
    fun compute(
        profile: ProfileRules,
        bins: List<BinRules>,
        exceptions: List<DateException>,
        from: LocalDate,
        to: LocalDate,
        holidays: HolidayCalendar = HolidayCalendar(profile.localHolidays),
    ): List<Occurrence> {
        val out = ArrayList<Occurrence>()
        val byBin = exceptions.groupBy { it.binId }
        for (bin in bins) {
            val exs = byBin[bin.binId].orEmpty()
            val skipped = exs.filter { it.kind == ExceptionKind.SKIP }.map { it.date }.toSet()
            val movedOut = exs.filter { it.kind == ExceptionKind.MOVE && it.target != null }.associateBy { it.date }
            val kept = exs.filter { it.kind == ExceptionKind.KEEP }.map { it.date }.toSet()

            fun byRule(date: LocalDate): Occurrence {
                val collection = collectionDate(profile, date)
                val holiday = holidays.nameOf(collection)
                val status = when {
                    holiday == null || date in kept -> Status.ACTIVE
                    profile.holidayPolicy == HolidayPolicy.ASK -> Status.HOLIDAY_PENDING
                    profile.holidayPolicy == HolidayPolicy.SKIP -> Status.HOLIDAY_SKIPPED
                    else -> Status.ACTIVE
                }
                return Occurrence(bin.binId, date, collection, Origin.RULE, status, holiday = holiday)
            }

            // Active dates of this bin, so explicit additions stay linear even with many exceptions
            val activeDates = HashSet<LocalDate>()
            var d = from
            while (d <= to) {
                if (bin.rules.any { matches(it, d) }) {
                    val occ = when {
                        d in skipped -> byRule(d).copy(status = Status.SKIPPED)
                        d in movedOut -> byRule(d).copy(status = Status.MOVED_OUT, movedTo = movedOut.getValue(d).target)
                        else -> byRule(d)
                    }
                    out += occ
                    if (occ.isActive) activeDates += d
                }
                d = d.plusDays(1)
            }
            // Explicit user choices: never questioned again, even on a holiday
            for (ex in exs) {
                val date = when (ex.kind) {
                    ExceptionKind.MOVE -> ex.target
                    ExceptionKind.ADD -> ex.date
                    else -> null
                } ?: continue
                if (date < from || date > to) continue
                if (!activeDates.add(date)) continue
                val collection = collectionDate(profile, date)
                out += Occurrence(
                    binId = bin.binId,
                    calendarDate = date,
                    collectionDate = collection,
                    origin = if (ex.kind == ExceptionKind.MOVE) Origin.MOVED_IN else Origin.ADDED,
                    status = Status.ACTIVE,
                    movedFrom = if (ex.kind == ExceptionKind.MOVE) ex.date else null,
                    holiday = holidays.nameOf(collection),
                )
            }
        }
        out.sortBy { it.calendarDate }
        return out
    }
}
