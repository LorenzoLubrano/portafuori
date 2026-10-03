package io.github.lorenzolubrano.portafuori.reminders

import io.github.lorenzolubrano.portafuori.Brand
import io.github.lorenzolubrano.portafuori.data.Evening
import io.github.lorenzolubrano.portafuori.data.ProfileBundle
import io.github.lorenzolubrano.portafuori.data.SnoozeEntity
import io.github.lorenzolubrano.portafuori.rules.ExposureMode
import io.github.lorenzolubrano.portafuori.rules.It
import io.github.lorenzolubrano.portafuori.rules.ROME
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

enum class SlotKind { PREPARE, EXPOSE, EXPOSE_REPEAT, SNOOZE, RETRIEVE, HOLIDAY_ASK, TEST }

/** A notification the app should show at [at] (epoch ms), still useful until [relevantUntil]. */
data class Slot(
    val key: String,
    val kind: SlotKind,
    val profileId: Long,
    val collectionDate: LocalDate?,
    val at: Long,
    val relevantUntil: Long,
    val title: String,
    val text: String,
) {
    /** EXPOSE, repeats, snoozes and PREPARE of one evening share a single notification. */
    val notificationId: Int
        get() = when (kind) {
            SlotKind.RETRIEVE, SlotKind.HOLIDAY_ASK, SlotKind.TEST -> key.hashCode()
            else -> eveningNotificationId(profileId, collectionDate)
        }
}

fun eveningNotificationId(profileId: Long, collection: LocalDate?) = "$profileId|$collection".hashCode()

object Planner {
    const val HORIZON_DAYS = 45L
    private const val REPEAT_MINUTES = 45L
    private val HOLIDAY_ASK_TIME: LocalTime = LocalTime.of(19, 0)

    fun plan(
        bundles: List<ProfileBundle>,
        snoozes: List<SnoozeEntity>,
        testAt: Long?,
        now: Long,
    ): List<Slot> {
        val today = Instant.ofEpochMilli(now).atZone(ROME).toLocalDate()
        val prefixNames = bundles.size > 1
        val out = ArrayList<Slot>()
        for (b in bundles) {
            val p = b.profile
            val prefix = if (prefixNames) "${p.name} · " else ""
            val evenings = b.evenings(today.minusDays(2), today.plusDays(HORIZON_DAYS))
            for (e in evenings) {
                if (e.paused) continue
                val names = e.binNames
                val windowText = windowText(e)
                val holidayNote = e.holiday?.takeIf { e.pendingBins.isNotEmpty() }
                    ?.let { "\nÈ festivo ($it): verifica che il ritiro si faccia." }.orEmpty()
                val exposeDay = when {
                    p.exposureMode == ExposureMode.EVENING_BEFORE -> e.window.start.toLocalDate()
                    p.exposeReminderAt.isBefore(LocalTime.NOON) -> e.collectionDate
                    else -> e.collectionDate.minusDays(1)
                }
                val base = "${p.id}|${e.collectionDate}"
                if (!e.done && p.exposeReminderOn) {
                    val at = ms(exposeDay, p.exposeReminderAt)
                    val title = prefix + label(e, at) + ": " + names
                    out += Slot("$base|EXPOSE", SlotKind.EXPOSE, p.id, e.collectionDate, at, ms(e.window.end), title, windowText + holidayNote)
                    if (p.insistent) {
                        val again = at + REPEAT_MINUTES * 60_000
                        out += Slot(
                            "$base|REPEAT", SlotKind.EXPOSE_REPEAT, p.id, e.collectionDate, again, ms(e.window.end),
                            prefix + label(e, again) + ": " + names, "Non hai ancora premuto «Fatto». $windowText",
                        )
                    }
                }
                if (!e.done && p.prepareReminderOn) {
                    val at = ms(exposeDay, p.prepareReminderAt)
                    val until = maxOf(ms(e.window.start), at + 3600_000)
                    out += Slot(
                        "$base|PREPARE", SlotKind.PREPARE, p.id, e.collectionDate, at, until,
                        prefix + "Prepara per " + label(e, at).lowercase() + ": " + names,
                        "Esposizione dalle ${It.time(e.window.start.toLocalTime())}.",
                    )
                }
                if (p.retrieveReminderOn) {
                    val at = ms(e.collectionDate, p.retrieveReminderAt)
                    out += Slot(
                        "$base|RETRIEVE", SlotKind.RETRIEVE, p.id, e.collectionDate, at, ms(e.collectionDate, LocalTime.MAX),
                        prefix + "Ritira i bidoni: " + e.bins.joinToString(", ") { it.name },
                        "Se sono stati svuotati, riportali dentro.",
                    )
                }
                val pending = e.pendingBins
                if (pending.isNotEmpty()) {
                    val at = ms(e.collectionDate.minusDays(3), HOLIDAY_ASK_TIME)
                    out += Slot(
                        "$base|HOLIDAY", SlotKind.HOLIDAY_ASK, p.id, e.collectionDate, at, ms(e.window.start),
                        prefix + It.longDay(e.collectionDate).replaceFirstChar { it.uppercase() } + " è festivo (${e.holiday})",
                        "Il ritiro di " + It.join(pending.map { it.name.lowercase() }) + " si fa? Controlla il calendario del tuo comune.",
                    )
                }
            }
            for (s in snoozes.filter { it.profileId == p.id }) {
                val e = evenings.firstOrNull { it.collectionDate == s.collectionDate } ?: continue
                if (e.done || e.paused) continue
                out += Slot(
                    "snooze|${s.id}", SlotKind.SNOOZE, p.id, e.collectionDate, s.at, ms(e.window.end),
                    prefix + label(e, s.at) + ": " + e.binNames, windowText(e),
                )
            }
        }
        if (testAt != null) {
            out += Slot(
                "test|$testAt", SlotKind.TEST, 0, null, testAt, testAt + 3600_000,
                "Notifica di prova", "Se la vedi all'orario giusto, i promemoria di ${Brand.NAME} funzionano.",
            )
        }
        return out.sortedBy { it.at }
    }

    /** "Stasera", "Stanotte", "Domattina", "Domani sera" or "gio 25/09", seen from the moment [at]. */
    fun label(e: Evening, at: Long): String {
        val atDate = Instant.ofEpochMilli(at).atZone(ROME).toLocalDate()
        val start = e.window.start
        val evening = start.hour >= 12
        // Past midnight, a window that opened yesterday evening is still "tonight" for the user
        val open = start.toInstant().toEpochMilli() <= at && at < e.window.end.toInstant().toEpochMilli()
        if (open && start.toLocalDate().isBefore(atDate)) return "Stanotte"
        return when (start.toLocalDate()) {
            atDate -> if (evening) "Stasera" else "Stamattina"
            atDate.plusDays(1) -> if (evening) "Domani sera" else "Domattina"
            else -> It.weekdayDay(start.toLocalDate()).replaceFirstChar { it.uppercase() } + if (evening) " sera" else ""
        }
    }

    fun windowText(e: Evening): String {
        val s = e.window.start
        val end = e.window.end
        return if (s.toLocalDate() == end.toLocalDate()) {
            "Esponi dalle ${It.time(s.toLocalTime())} alle ${It.time(end.toLocalTime())}."
        } else {
            "Esponi dalle ${It.time(s.toLocalTime())}, ritiro entro le ${It.time(end.toLocalTime())} di ${It.longDay(end.toLocalDate())}."
        }
    }

    private fun ms(z: ZonedDateTime) = z.toInstant().toEpochMilli()
    private fun ms(d: LocalDate, t: LocalTime) = ZonedDateTime.of(d, t, ROME).toInstant().toEpochMilli()
}
