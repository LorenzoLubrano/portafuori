package io.github.lorenzolubrano.portafuori.data

import io.github.lorenzolubrano.portafuori.rules.BinRules
import io.github.lorenzolubrano.portafuori.rules.DateException
import io.github.lorenzolubrano.portafuori.rules.ExposureMode
import io.github.lorenzolubrano.portafuori.rules.ExposureWindow
import io.github.lorenzolubrano.portafuori.rules.HolidayCalendar
import io.github.lorenzolubrano.portafuori.rules.LocalHoliday
import io.github.lorenzolubrano.portafuori.rules.Occurrence
import io.github.lorenzolubrano.portafuori.rules.ProfileRules
import io.github.lorenzolubrano.portafuori.rules.Rule
import io.github.lorenzolubrano.portafuori.rules.Schedule
import io.github.lorenzolubrano.portafuori.rules.Status
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay

// --- Entity <-> rules-engine mapping ---

fun maskOf(days: Set<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
fun daysOf(mask: Int): Set<DayOfWeek> = DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()

private const val LAST_BIT = 6
fun ordinalsMaskOf(ords: Set<Int>): Int = ords.fold(0) { acc, o -> acc or (1 shl (if (o == Rule.LAST) LAST_BIT else o)) }
fun ordinalsOf(mask: Int): Set<Int> = buildSet {
    for (i in 1..5) if (mask and (1 shl i) != 0) add(i)
    if (mask and (1 shl LAST_BIT) != 0) add(Rule.LAST)
}

fun encodeMonthDay(md: MonthDay?): Int? = md?.let { it.monthValue * 100 + it.dayOfMonth }

/** Tolerant on purpose: a bad stored value must never crash the app on every launch. */
fun decodeMonthDay(v: Int?): MonthDay? = v?.let { runCatching { MonthDay.of(it / 100, it % 100) }.getOrNull() }

fun RuleEntity.toRule(): Rule {
    val start = decodeMonthDay(seasonStart)
    val end = decodeMonthDay(seasonEnd)
    val bothSeason = start != null && end != null
    return Rule(
        type = type,
        weekdays = daysOf(weekdaysMask),
        intervalWeeks = intervalWeeks.coerceIn(1, Limits.MAX_INTERVAL_WEEKS),
        anchor = anchor,
        ordinals = ordinalsOf(ordinalsMask),
        dates = dates.split(',').filter { it.isNotBlank() }.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet(),
        seasonStart = if (bothSeason) start else null,
        seasonEnd = if (bothSeason) end else null,
        validFrom = validFrom,
        validUntil = validUntil,
    )
}

fun Rule.toEntity(binId: Long = 0) = RuleEntity(
    binId = binId,
    type = type,
    weekdaysMask = maskOf(weekdays),
    intervalWeeks = intervalWeeks,
    anchor = anchor,
    ordinalsMask = ordinalsMaskOf(ordinals),
    dates = dates.sorted().joinToString(","),
    seasonStart = encodeMonthDay(seasonStart),
    seasonEnd = encodeMonthDay(seasonEnd),
    validFrom = validFrom,
    validUntil = validUntil,
)

fun ProfileEntity.toRules(holidays: List<HolidayEntity>) = ProfileRules(
    calendarMode = calendarMode,
    exposureMode = exposureMode,
    exposeStart = exposeStart,
    exposeEnd = exposeEnd,
    holidayPolicy = holidayPolicy,
    localHolidays = holidays.map { LocalHoliday(it.name, decodeMonthDay(it.monthDay), it.date) },
)

// --- Aggregates used by UI, reminders and widget ---

data class Bin(val entity: BinEntity, val rules: List<Rule>) {
    val id get() = entity.id
    val name get() = entity.name
}

/** All bins going out for one collection. */
data class Evening(
    val profileId: Long,
    val collectionDate: LocalDate,
    val calendarDate: LocalDate,
    val window: ExposureWindow,
    val bins: List<Bin>,
    val occurrences: List<Occurrence>,
    val done: Boolean,
    val paused: Boolean,
) {
    val holiday: String? get() = occurrences.firstNotNullOfOrNull { it.holiday }
    val pendingBins: List<Bin>
        get() {
            val ids = occurrences.mapNotNullTo(HashSet()) { o -> o.binId.takeIf { o.status == Status.HOLIDAY_PENDING } }
            return if (ids.isEmpty()) emptyList() else bins.filter { it.id in ids }
        }
    val binNames: String get() = bins.joinToString(" + ") { it.name }
}

data class ProfileBundle(
    val profile: ProfileEntity,
    val bins: List<Bin>,
    val exceptions: List<ExceptionEntity>,
    val holidays: List<HolidayEntity>,
    val done: Set<LocalDate>,
) {
    val rules: ProfileRules = profile.toRules(holidays)
    val holidayCalendar = HolidayCalendar(rules.localHolidays)
    private val binById = bins.associateBy { it.id }

    fun bin(id: Long) = binById[id]

    /** The evening (or morning) a collection's bins go out: the app's one clock. */
    fun eveningOf(collection: LocalDate): LocalDate = Schedule.exposureWindow(rules, collection).start.toLocalDate()

    /** Bins go out the same morning: the app's "evenings" are mornings. */
    val morning: Boolean get() = rules.exposureMode == ExposureMode.SAME_MORNING

    fun occurrences(from: LocalDate, to: LocalDate): List<Occurrence> = Schedule.compute(
        rules,
        bins.map { BinRules(it.id, it.rules) },
        exceptions.map { DateException(it.binId, it.kind, it.date, it.target) },
        from, to, holidayCalendar,
    ).sortedWith(compareBy({ it.calendarDate }, { binById[it.binId]?.entity?.sortOrder ?: 0 }, { it.binId }))

    fun isPaused(collection: LocalDate): Boolean {
        val from = profile.pausedFrom ?: return false
        val to = profile.pausedTo ?: return false
        return collection in from..to
    }

    /** Active collections grouped by collection date, whose calendar date is in [from, to]. */
    fun evenings(from: LocalDate, to: LocalDate): List<Evening> =
        occurrences(from, to).filter { it.isActive }.groupBy { it.collectionDate }.map { (collection, occ) ->
            Evening(
                profileId = profile.id,
                collectionDate = collection,
                calendarDate = occ.first().calendarDate,
                window = Schedule.exposureWindow(rules, collection),
                bins = occ.mapNotNull { binById[it.binId] }.distinctBy { it.id },
                occurrences = occ,
                done = collection in done,
                paused = isPaused(collection),
            )
        }.sortedBy { it.collectionDate }

    /** Like [evenings], minus those whose exposure window is already over at [nowMs]: what a preview should list. */
    fun upcomingEvenings(from: LocalDate, to: LocalDate, nowMs: Long = System.currentTimeMillis()): List<Evening> =
        evenings(from, to).filter { it.window.end.toInstant().toEpochMilli() > nowMs }
}
