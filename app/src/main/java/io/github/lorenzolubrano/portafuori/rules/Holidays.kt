package io.github.lorenzolubrano.portafuori.rules

import java.time.LocalDate

/** Italian national holidays, computed offline. */
object ItalianHolidays {

    /** Anonymous Gregorian algorithm (Meeus/Jones/Butcher). */
    fun easter(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    fun national(year: Int): Map<LocalDate, String> {
        val easter = easter(year)
        val map = linkedMapOf(
            LocalDate.of(year, 1, 1) to "Capodanno",
            LocalDate.of(year, 1, 6) to "Epifania",
            easter to "Pasqua",
            easter.plusDays(1) to "Lunedì dell'Angelo",
            LocalDate.of(year, 4, 25) to "Festa della Liberazione",
            LocalDate.of(year, 5, 1) to "Festa del Lavoro",
            LocalDate.of(year, 6, 2) to "Festa della Repubblica",
            LocalDate.of(year, 8, 15) to "Ferragosto",
            LocalDate.of(year, 11, 1) to "Ognissanti",
            LocalDate.of(year, 12, 8) to "Immacolata Concezione",
            LocalDate.of(year, 12, 25) to "Natale",
            LocalDate.of(year, 12, 26) to "Santo Stefano",
        )
        // Legge 151/2025: 4 October is a national holiday again from 2026
        if (year >= 2026) map[LocalDate.of(year, 10, 4)] = "San Francesco d'Assisi"
        return map
    }
}

/** National + local holidays, cached per year. */
class HolidayCalendar(private val local: List<LocalHoliday> = emptyList()) {
    private val cache = HashMap<Int, Map<LocalDate, String>>()

    fun nameOf(date: LocalDate): String? = forYear(date.year)[date]

    fun forYear(year: Int): Map<LocalDate, String> = cache.getOrPut(year) {
        val map = LinkedHashMap(ItalianHolidays.national(year))
        for (h in local) {
            val date = h.date?.takeIf { it.year == year }
                ?: h.monthDay?.takeIf { it.isValidYear(year) }?.atYear(year)
                ?: continue
            map.putIfAbsent(date, h.name)
        }
        map
    }

    fun between(from: LocalDate, to: LocalDate): List<Pair<LocalDate, String>> =
        (from.year..to.year).flatMap { forYear(it).entries }
            .filter { it.key in from..to }
            .sortedBy { it.key }
            .map { it.key to it.value }
}
