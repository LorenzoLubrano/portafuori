package io.github.lorenzolubrano.portafuori.reminders

import io.github.lorenzolubrano.portafuori.data.Evening
import io.github.lorenzolubrano.portafuori.rules.ExposureWindow
import io.github.lorenzolubrano.portafuori.rules.ROME
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class PlannerLabelTest {

    private fun at(d: String, t: String) = ZonedDateTime.of(LocalDate.parse(d), LocalTime.parse(t), ROME).toInstant().toEpochMilli()

    // Put out Wednesday 23/09 from 20:00, collected Thursday by 06:00
    private val evening = Evening(
        profileId = 1,
        collectionDate = LocalDate.parse("2026-09-24"),
        calendarDate = LocalDate.parse("2026-09-24"),
        window = ExposureWindow(
            ZonedDateTime.of(LocalDate.parse("2026-09-23"), LocalTime.of(20, 0), ROME),
            ZonedDateTime.of(LocalDate.parse("2026-09-24"), LocalTime.of(6, 0), ROME),
        ),
        bins = emptyList(),
        occurrences = emptyList(),
        done = false,
        paused = false,
    )

    @Test fun labels() {
        assertEquals("Domani sera", Planner.label(evening, at("2026-09-22", "21:00")))
        assertEquals("Stasera", Planner.label(evening, at("2026-09-23", "18:00")))
        assertEquals("Stasera", Planner.label(evening, at("2026-09-23", "23:30")))
        // After midnight the window is still open: not "Mer 23/09 sera"
        assertEquals("Stanotte", Planner.label(evening, at("2026-09-24", "00:03")))
        assertEquals("Stanotte", Planner.label(evening, at("2026-09-24", "05:59")))
    }

    @Test fun windowTextSaysWhenToPutThemOut() {
        assertEquals("Da portare fuori dalle 20:00, ritiro entro le 6:00 di giovedì 24 settembre.", Planner.windowText(evening))
        val sameDay = evening.copy(
            window = ExposureWindow(
                ZonedDateTime.of(LocalDate.parse("2026-09-23"), LocalTime.of(20, 0), ROME),
                ZonedDateTime.of(LocalDate.parse("2026-09-23"), LocalTime.of(23, 0), ROME),
            ),
        )
        assertEquals("Da portare fuori dalle 20:00 alle 23:00.", Planner.windowText(sameDay))
    }
}
