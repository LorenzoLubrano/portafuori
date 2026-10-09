package io.github.lorenzolubrano.portafuori.ui

import io.github.lorenzolubrano.portafuori.data.Presets
import io.github.lorenzolubrano.portafuori.rules.It
import io.github.lorenzolubrano.portafuori.ui.calendar.Stop
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.Locale

class WordingTest {
    private val wed = LocalDate.of(2026, 10, 7)

    @Test fun fromTime() {
        assertEquals("Dalle 20:00", Wording.from(LocalTime.of(20, 0)))
        assertEquals("Dall'1:30", Wording.from(LocalTime.of(1, 30)))
        assertEquals("Dalle 5:05", Wording.from(LocalTime.of(5, 5)))
    }

    @Test fun timesHaveNoLeadingZeroAndLatinDigits() {
        assertEquals("6:00", It.time(LocalTime.of(6, 0)))
        assertEquals("20:30", It.time(LocalTime.of(20, 30)))
        // a phone set to a language with other digits still reads the app's Italian times
        val before = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("6:05", Wording.hm(LocalTime.of(6, 5)))
            assertEquals("6:05", It.time(LocalTime.of(6, 5)))
        } finally {
            Locale.setDefault(before)
        }
    }

    @Test fun untilTime() {
        assertEquals("al più tardi entro le 6:00 di giovedì", Wording.until(wed, wed.plusDays(1), LocalTime.of(6, 0)))
        assertEquals("al più tardi entro le 23:00", Wording.until(wed, wed, LocalTime.of(23, 0)))
        assertEquals("al più tardi entro l'1:00 di giovedì", Wording.until(wed, wed.plusDays(1), LocalTime.of(1, 0)))
        assertEquals("al più tardi entro le 8:00 di venerdì 9 ottobre", Wording.until(wed, wed.plusDays(2), LocalTime.of(8, 0)))
    }

    @Test fun doneLabels() {
        assertEquals("Fatto, l'ho portato fuori", Wording.doneButton(1))
        assertEquals("Fatto, li ho portati fuori", Wording.doneButton(3))
        assertEquals("Fatto: è fuori", Wording.doneState(1))
        assertEquals("Fatto: sono fuori", Wording.doneState(2))
        assertEquals("Stasera porta fuori", Wording.headline("Stasera"))
    }

    @Test fun ritiroAndEvening() {
        assertEquals("ritiro gio 8", Wording.ritiro(LocalDate.of(2026, 10, 8)))
        assertEquals("ritiro giovedì 8", Wording.ritiroLong(LocalDate.of(2026, 10, 8)))
        assertEquals("Mercoledì 7 sera", Wording.eveningTitle(wed))
        // bins out the same morning: the day is a morning, not an evening
        assertEquals("Giovedì 8 mattina", Wording.eveningTitle(LocalDate.of(2026, 10, 8), morning = true))
    }

    @Test fun weekTitles() {
        assertEquals("Sere dal 5 all'11 ottobre", Wording.weekTitle(LocalDate.of(2026, 10, 5)))
        assertEquals("Sere dall'1 al 7 giugno", Wording.weekTitle(LocalDate.of(2026, 6, 1)))
        assertEquals("Sere dall'8 al 14 giugno", Wording.weekTitle(LocalDate.of(2026, 6, 8)))
        assertEquals("Sere dal 28 settembre al 4 ottobre", Wording.weekTitle(LocalDate.of(2026, 9, 28)))
        assertEquals("Sere dal 28 dicembre 2026 al 3 gennaio 2027", Wording.weekTitle(LocalDate.of(2026, 12, 28)))
    }

    @Test fun monthTitle() = assertEquals("Sere di ottobre 2026", Wording.monthTitle(YearMonth.of(2026, 10)))

    /** Bins out in the morning: the calendar and Oggi speak of mornings. */
    @Test fun morningTitles() {
        assertEquals("Mattine dal 5 all'11 ottobre", Wording.weekTitle(LocalDate.of(2026, 10, 5), morning = true))
        assertEquals("Mattine di ottobre 2026", Wording.monthTitle(YearMonth.of(2026, 10), morning = true))
        assertEquals("Prossime sere", Wording.upcomingTitle(morning = false))
        assertEquals("Prossime mattine", Wording.upcomingTitle(morning = true))
    }

    /** TalkBack reads a bin's week as it is drawn: out, waiting for a holiday answer, skipped. */
    @Test fun binWeekForTalkBack() {
        assertEquals("Carta: mercoledì, sabato", Wording.binWeek("Carta", listOf(Stop.NONE to "lunedì", Stop.OUT to "mercoledì", Stop.OUT to "sabato"), morning = false))
        assertEquals(
            "Carta: mercoledì, sabato festivo da confermare; saltato lunedì",
            Wording.binWeek("Carta", listOf(Stop.SKIPPED to "lunedì", Stop.OUT to "mercoledì", Stop.PENDING to "sabato"), morning = false),
        )
        assertEquals("Carta: nessuna sera", Wording.binWeek("Carta", emptyList(), morning = false))
        assertEquals("Carta: nessuna mattina", Wording.binWeek("Carta", emptyList(), morning = true))
        assertEquals("Carta: nessuna sera; saltati lunedì, giovedì", Wording.binWeek("Carta", listOf(Stop.SKIPPED to "lunedì", Stop.SKIPPED to "giovedì"), morning = false))
        assertEquals("Carta: giovedì; spostato mercoledì", Wording.binWeek("Carta", listOf(Stop.MOVED to "mercoledì", Stop.OUT to "giovedì"), morning = false))
    }

    @Test fun iconNames() {
        val keys = listOf("compost", "bottle", "paper", "glass", "trash", "recycle", "grass", "baby", "bag", "box", "battery", "oil")
        assertEquals(
            listOf("Organico", "Bottiglia", "Carta", "Vetro", "Cestino", "Riciclo", "Erba", "Pannolino", "Sacchetto", "Scatola", "Pila", "Olio"),
            keys.map(Wording::iconName),
        )
        assertEquals("Cestino", Wording.iconName("sconosciuta")) // same fallback as binIcon()
    }

    /** Every colour of the editor's palette has its own name, whatever the palette becomes. */
    @Test fun everyPaletteColourHasItsOwnName() {
        assertEquals(Presets.colors.size, Presets.colors.map { Wording.colorName(it) }.toSet().size)
    }

    @Test fun colourNames() {
        assertEquals("Giallo", Wording.colorName(0xFFF2C230))
        assertEquals("Grigio chiaro", Wording.colorName(0xFFB0B7C3))
        // an imported colour outside the palette gets the nearest name, never an empty label
        assertEquals("Rosso", Wording.colorName(0xFFE04040))
    }
}
