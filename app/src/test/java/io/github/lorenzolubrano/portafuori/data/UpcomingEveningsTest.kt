package io.github.lorenzolubrano.portafuori.data

import io.github.lorenzolubrano.portafuori.rules.Rule
import io.github.lorenzolubrano.portafuori.rules.RuleType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Previews (wizard, import, Oggi) must never list an evening whose exposure window is over (1.0.5). */
class UpcomingEveningsTest {

    private val rome = ZoneId.of("Europe/Rome")

    // Pick-up every Tuesday, bins out the evening before from 20:00 to 06:00
    private val bundle = ProfileBundle(
        ProfileEntity(id = 1, name = "Casa"),
        listOf(Bin(BinEntity(id = 1, profileId = 1, name = "Indifferenziato", colorArgb = 0xFF6B7280, iconKey = "trash"), listOf(Rule(RuleType.WEEKLY, weekdays = setOf(DayOfWeek.TUESDAY))))),
        emptyList(), emptyList(), emptySet(),
    )

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) = ZonedDateTime.of(y, m, d, h, min, 0, 0, rome).toInstant().toEpochMilli()

    @Test fun eveningAlreadyOverIsLeftOut() {
        // Tuesday 13/10/2026 at 19:55: Monday evening (for today's pick-up) ended at 06:00
        val today = LocalDate.of(2026, 10, 13)
        val upcoming = bundle.upcomingEvenings(today, today.plusDays(27), at(2026, 10, 13, 19, 55))
        // Tuesdays up to 9/11: 13/10 (over), 20/10, 27/10, 3/11
        assertEquals(
            listOf(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 27), LocalDate.of(2026, 11, 3)),
            upcoming.map { it.collectionDate },
        )
        assertEquals(4, bundle.evenings(today, today.plusDays(27)).size)
    }

    @Test fun eveningStillOpenAfterMidnightIsKept() {
        // Tuesday 13/10/2026 at 01:30: the window for today's pick-up is still open until 06:00
        val today = LocalDate.of(2026, 10, 13)
        val upcoming = bundle.upcomingEvenings(today, today.plusDays(27), at(2026, 10, 13, 1, 30))
        assertEquals(LocalDate.of(2026, 10, 13), upcoming.first().collectionDate)
    }
}
