package io.github.lorenzolubrano.portafuori.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay
import java.time.ZonedDateTime

class ScheduleTest {

    private fun d(s: String) = LocalDate.parse(s)
    private fun t(s: String) = LocalTime.parse(s)

    private val evening = ProfileRules(
        calendarMode = CalendarMode.COLLECTION_DAY,
        exposureMode = ExposureMode.EVENING_BEFORE,
        exposeStart = t("22:00"),
        exposeEnd = t("05:00"),
        holidayPolicy = HolidayPolicy.ASK,
    )

    private fun dates(rule: Rule, from: String, to: String): List<LocalDate> {
        val out = mutableListOf<LocalDate>()
        var x = d(from)
        while (x <= d(to)) {
            if (Schedule.matches(rule, x)) out += x
            x = x.plusDays(1)
        }
        return out
    }

    @Test fun easterDates() {
        assertEquals(d("2026-04-05"), ItalianHolidays.easter(2026))
        assertEquals(d("2027-03-28"), ItalianHolidays.easter(2027))
        assertEquals(d("2028-04-16"), ItalianHolidays.easter(2028))
        assertEquals(d("2025-04-20"), ItalianHolidays.easter(2025))
    }

    @Test fun nationalHolidays() {
        val h = HolidayCalendar()
        assertEquals("Lunedì dell'Angelo", h.nameOf(d("2026-04-06")))
        assertEquals("San Francesco d'Assisi", h.nameOf(d("2026-10-04")))
        assertNull(h.nameOf(d("2025-10-04")))
        assertEquals("Santo Stefano", h.nameOf(d("2027-12-26")))
        val local = HolidayCalendar(listOf(LocalHoliday("San Gennaro", monthDay = MonthDay.of(9, 19))))
        assertEquals("San Gennaro", local.nameOf(d("2026-09-19")))
    }

    @Test fun weekly() {
        val r = Rule(RuleType.WEEKLY, weekdays = setOf(MONDAY, THURSDAY))
        assertEquals(
            listOf(d("2026-09-28"), d("2026-10-01"), d("2026-10-05")),
            dates(r, "2026-09-28", "2026-10-06"),
        )
    }

    @Test fun everyTwoWeeksAnchorInPastAndFuture() {
        val past = Rule(RuleType.EVERY_N_WEEKS, weekdays = setOf(TUESDAY), intervalWeeks = 2, anchor = d("2026-01-06"))
        val future = past.copy(anchor = d("2027-01-05"))
        val expected = listOf(d("2026-09-29"), d("2026-10-13"), d("2026-10-27"))
        assertEquals(expected, dates(past, "2026-09-28", "2026-11-01"))
        // 2027-01-05 is 66 weeks after 2026-09-29: same parity
        assertEquals(expected, dates(future, "2026-09-28", "2026-11-01"))
        // Anchor mid-week still selects the whole week
        assertEquals(expected, dates(past.copy(anchor = d("2026-01-09")), "2026-09-28", "2026-11-01"))
    }

    @Test fun fifthTuesdayAndLastFriday() {
        val fifth = Rule(RuleType.MONTHLY_NTH, weekdays = setOf(TUESDAY), ordinals = setOf(5))
        assertEquals(listOf(d("2026-09-29"), d("2026-12-29")), dates(fifth, "2026-09-01", "2026-12-31"))
        val lastFri = Rule(RuleType.MONTHLY_NTH, weekdays = setOf(FRIDAY), ordinals = setOf(Rule.LAST))
        assertEquals(listOf(d("2026-09-25"), d("2026-10-30")), dates(lastFri, "2026-09-01", "2026-10-31"))
        val firstThird = Rule(RuleType.MONTHLY_NTH, weekdays = setOf(TUESDAY), ordinals = setOf(1, 3))
        assertEquals(listOf(d("2026-10-06"), d("2026-10-20")), dates(firstThird, "2026-10-01", "2026-10-31"))
    }

    @Test fun seasonAcrossNewYearAndLeapDay() {
        val winter = Rule(
            RuleType.WEEKLY, weekdays = setOf(MONDAY),
            seasonStart = MonthDay.of(12, 1), seasonEnd = MonthDay.of(2, 29),
        )
        val got = dates(winter, "2027-11-20", "2028-03-10")
        assertEquals(d("2027-12-06"), got.first())
        assertEquals(d("2028-02-28"), got.last())
        assertTrue(got.contains(d("2028-01-03")))
        val leap = Rule(RuleType.FIXED_DATES, dates = setOf(d("2028-02-29")))
        assertEquals(listOf(d("2028-02-29")), dates(leap, "2028-02-01", "2028-03-31"))
    }

    @Test fun validityBounds() {
        val r = Rule(RuleType.WEEKLY, weekdays = setOf(MONDAY), validFrom = d("2026-12-28"), validUntil = d("2027-01-04"))
        assertEquals(listOf(d("2026-12-28"), d("2027-01-04")), dates(r, "2026-12-01", "2027-01-31"))
    }

    @Test fun windowCrossesMidnightAndYearEnd() {
        val w = Schedule.exposureWindow(evening, d("2027-01-01"))
        assertEquals(ZonedDateTime.of(d("2026-12-31"), t("22:00"), ROME), w.start)
        assertEquals(ZonedDateTime.of(d("2027-01-01"), t("05:00"), ROME), w.end)
    }

    @Test fun windowSameEvening() {
        val p = evening.copy(exposeStart = t("20:00"), exposeEnd = t("23:59"))
        val w = Schedule.exposureWindow(p, d("2026-10-01"))
        assertEquals(d("2026-09-30"), w.start.toLocalDate())
        assertEquals(d("2026-09-30"), w.end.toLocalDate())
    }

    @Test fun morningMode() {
        val p = evening.copy(exposureMode = ExposureMode.SAME_MORNING, exposeStart = t("04:00"), exposeEnd = t("07:00"))
        val w = Schedule.exposureWindow(p, d("2026-10-01"))
        assertEquals(ZonedDateTime.of(d("2026-10-01"), t("04:00"), ROME), w.start)
        val q = p.copy(exposeStart = t("21:00"), exposeEnd = t("06:00"))
        assertEquals(d("2026-09-30"), Schedule.exposureWindow(q, d("2026-10-01")).start.toLocalDate())
    }

    @Test fun dstNights() {
        // Spring forward 2027-03-28 02:00 -> 03:00: a 02:30 start moves forward, window stays valid
        val p = evening.copy(exposeStart = t("02:30"), exposeEnd = t("06:00"), exposureMode = ExposureMode.SAME_MORNING)
        val spring = Schedule.exposureWindow(p, d("2027-03-28"))
        assertEquals(3, spring.start.hour)
        assertTrue(spring.start.isBefore(spring.end))
        // Fall back 2026-10-25: 22:00 -> 05:00 lasts 8 hours instead of 7
        val fall = Schedule.exposureWindow(evening, d("2026-10-25"))
        assertEquals(8L, java.time.Duration.between(fall.start, fall.end).toHours())
    }

    @Test fun exposeDayModeShiftsCollection() {
        val p = evening.copy(calendarMode = CalendarMode.EXPOSE_DAY)
        assertEquals(d("2026-10-02"), Schedule.collectionDate(p, d("2026-10-01")))
        assertEquals(d("2026-10-01"), Schedule.calendarDate(p, d("2026-10-02")))
        val morning = p.copy(exposureMode = ExposureMode.SAME_MORNING)
        assertEquals(d("2026-10-01"), Schedule.collectionDate(morning, d("2026-10-01")))
    }

    @Test fun exceptionsAndHolidays() {
        val bins = listOf(BinRules(1, listOf(Rule(RuleType.WEEKLY, weekdays = setOf(MONDAY)))))
        // 2026-12-07 normal, 2026-12-14 skipped, 2026-12-21 moved to 22, 2026-12-24 added
        val ex = listOf(
            DateException(1, ExceptionKind.SKIP, d("2026-12-14")),
            DateException(1, ExceptionKind.MOVE, d("2026-12-21"), d("2026-12-22")),
            DateException(1, ExceptionKind.ADD, d("2026-12-24")),
        )
        val occ = Schedule.compute(evening, bins, ex, d("2026-12-07"), d("2026-12-31"))
        val active = occ.filter { it.isActive }.map { it.calendarDate }
        assertEquals(listOf(d("2026-12-07"), d("2026-12-22"), d("2026-12-24"), d("2026-12-28")), active)
        assertEquals(Status.SKIPPED, occ.first { it.calendarDate == d("2026-12-14") }.status)
        assertEquals(d("2026-12-22"), occ.first { it.calendarDate == d("2026-12-21") }.movedTo)
        assertEquals(Origin.MOVED_IN, occ.first { it.calendarDate == d("2026-12-22") }.origin)
    }

    @Test fun holidayPolicies() {
        // Christmas 2028 is a Monday
        val bins = listOf(BinRules(1, listOf(Rule(RuleType.WEEKLY, weekdays = setOf(MONDAY)))))
        val xmas = d("2028-12-25")
        fun status(p: ProfileRules, ex: List<DateException> = emptyList()) =
            Schedule.compute(p, bins, ex, xmas, xmas).single().status
        assertEquals(Status.HOLIDAY_PENDING, status(evening))
        assertEquals(Status.ACTIVE, status(evening.copy(holidayPolicy = HolidayPolicy.KEEP)))
        assertEquals(Status.HOLIDAY_SKIPPED, status(evening.copy(holidayPolicy = HolidayPolicy.SKIP)))
        assertEquals(Status.ACTIVE, status(evening, listOf(DateException(1, ExceptionKind.KEEP, xmas))))
        val occ = Schedule.compute(evening, bins, emptyList(), xmas, xmas).single()
        assertTrue(occ.isActive)
        assertEquals("Natale", occ.holiday)
    }

    @Test fun holidayCheckedOnCollectionDayInExposeMode() {
        // Calendar says "expose Sunday 24/12/2028 evening" -> collection on Christmas Monday
        val p = evening.copy(calendarMode = CalendarMode.EXPOSE_DAY)
        val bins = listOf(BinRules(1, listOf(Rule(RuleType.WEEKLY, weekdays = setOf(java.time.DayOfWeek.SUNDAY)))))
        val occ = Schedule.compute(p, bins, emptyList(), d("2028-12-24"), d("2028-12-24")).single()
        assertEquals("Natale", occ.holiday)
        assertEquals(Status.HOLIDAY_PENDING, occ.status)
    }

    @Test fun describeInItalian() {
        assertEquals("Ogni lunedì e giovedì", It.describe(Rule(RuleType.WEEKLY, weekdays = setOf(THURSDAY, MONDAY))))
        assertEquals(
            "Il 1° e 3° martedì del mese",
            It.describe(Rule(RuleType.MONTHLY_NTH, weekdays = setOf(TUESDAY), ordinals = setOf(3, 1))),
        )
        assertEquals(
            "L'ultimo venerdì del mese",
            It.describe(Rule(RuleType.MONTHLY_NTH, weekdays = setOf(FRIDAY), ordinals = setOf(Rule.LAST))),
        )
        assertEquals(
            "Ogni sabato · dal 1/5 al 31/10",
            It.describe(
                Rule(
                    RuleType.WEEKLY, weekdays = setOf(java.time.DayOfWeek.SATURDAY),
                    seasonStart = MonthDay.of(5, 1), seasonEnd = MonthDay.of(10, 31),
                ),
            ),
        )
        assertFalse(It.describe(Rule(RuleType.FIXED_DATES)).isBlank())
    }
}
