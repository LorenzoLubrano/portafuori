package io.github.lorenzolubrano.portafuori.ui.calendar

import io.github.lorenzolubrano.portafuori.data.Bin
import io.github.lorenzolubrano.portafuori.data.BinEntity
import io.github.lorenzolubrano.portafuori.data.ExceptionEntity
import io.github.lorenzolubrano.portafuori.data.ProfileBundle
import io.github.lorenzolubrano.portafuori.data.ProfileEntity
import io.github.lorenzolubrano.portafuori.rules.ExceptionKind
import io.github.lorenzolubrano.portafuori.rules.ExposureMode
import io.github.lorenzolubrano.portafuori.rules.Rule
import io.github.lorenzolubrano.portafuori.rules.RuleType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.LocalDate
import java.time.LocalTime

class CalendarModelTest {
    private fun bin(id: Long, name: String, vararg days: java.time.DayOfWeek) =
        Bin(BinEntity(id = id, profileId = 1, name = name, colorArgb = 0xFF6B7280, iconKey = "trash"), listOf(Rule(RuleType.WEEKLY, weekdays = days.toSet())))

    private val organico = bin(1, "Organico", MONDAY, THURSDAY)
    private val plastica = bin(2, "Plastica", THURSDAY)

    // Default profile: bins out the evening before, 20:00 to 06:00
    private fun bundle(profile: ProfileEntity = ProfileEntity(id = 1, name = "Casa"), exceptions: List<ExceptionEntity> = emptyList(), bins: List<Bin> = listOf(organico, plastica)) =
        ProfileBundle(profile, bins, exceptions, emptyList(), emptySet())

    private val mon5 = LocalDate.of(2026, 10, 5)

    @Test fun weekStartsOnMonday() = assertEquals(mon5, CalendarModel.weekStart(LocalDate.of(2026, 10, 7)))

    @Test fun collectionsSitOnTheEveningBefore() {
        val nights = CalendarModel.nights(bundle(), mon5, mon5.plusDays(6))
        // Thursday 8 collection -> Wednesday 7 evening; Monday 12 collection -> Sunday 11 evening
        assertEquals(setOf(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 11)), nights.keys)
        val wed = nights.getValue(LocalDate.of(2026, 10, 7))
        assertEquals(Stop.OUT, wed.stopOf(1))
        assertEquals(Stop.OUT, wed.stopOf(2))
        assertEquals(LocalDate.of(2026, 10, 8), wed.collectionDate)
        assertEquals(Stop.NONE, nights.getValue(LocalDate.of(2026, 10, 11)).stopOf(2))
    }

    /** Review Focus 5: a skipped collection still shows on its evening, as skipped. */
    @Test fun skippedCollectionKeepsItsEvening() {
        val skip = ExceptionEntity(profileId = 1, binId = 2, kind = ExceptionKind.SKIP, date = LocalDate.of(2026, 10, 8))
        val wed = CalendarModel.nights(bundle(exceptions = listOf(skip)), mon5, mon5.plusDays(6)).getValue(LocalDate.of(2026, 10, 7))
        assertEquals(Stop.SKIPPED, wed.stopOf(2))
        assertEquals(Stop.OUT, wed.stopOf(1))
        assertEquals(listOf(1L), wed.activeBinIds)
    }

    /** Review Focus 5: in morning mode the "evening" is the collection morning itself. */
    @Test fun morningModeUsesTheCollectionDay() {
        val morning = ProfileEntity(id = 1, name = "Casa", exposureMode = ExposureMode.SAME_MORNING, exposeStart = LocalTime.of(5, 0), exposeEnd = LocalTime.of(8, 0))
        val nights = CalendarModel.nights(bundle(profile = morning), mon5, mon5.plusDays(6))
        assertEquals(setOf(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 8)), nights.keys)
    }

    /** Review Focus 4: seven bins on one evening show three roundels and "+4". */
    @Test fun manyBinsSameEvening() {
        val seven = (1L..7L).map { bin(it, "Bidone $it", THURSDAY) }
        val wed = CalendarModel.nights(bundle(bins = seven), mon5, mon5.plusDays(6)).getValue(LocalDate.of(2026, 10, 7))
        assertEquals(7, wed.activeBinIds.size)
        // a month cell has 4 places: 3 roundels + "+4"
        assertEquals(4, CalendarModel.overflow(wed.activeBinIds.size))
        assertEquals(0, CalendarModel.overflow(4))
    }
}
