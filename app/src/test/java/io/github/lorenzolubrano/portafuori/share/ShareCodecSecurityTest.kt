package io.github.lorenzolubrano.portafuori.share

import io.github.lorenzolubrano.portafuori.Brand
import io.github.lorenzolubrano.portafuori.data.Bin
import io.github.lorenzolubrano.portafuori.data.BinEntity
import io.github.lorenzolubrano.portafuori.data.Limits
import io.github.lorenzolubrano.portafuori.data.ProfileBundle
import io.github.lorenzolubrano.portafuori.data.ProfileEntity
import io.github.lorenzolubrano.portafuori.rules.BinRules
import io.github.lorenzolubrano.portafuori.rules.CalendarMode
import io.github.lorenzolubrano.portafuori.rules.DateException
import io.github.lorenzolubrano.portafuori.rules.ExceptionKind
import io.github.lorenzolubrano.portafuori.rules.ExposureMode
import io.github.lorenzolubrano.portafuori.rules.HolidayPolicy
import io.github.lorenzolubrano.portafuori.rules.ProfileRules
import io.github.lorenzolubrano.portafuori.rules.Rule
import io.github.lorenzolubrano.portafuori.rules.RuleType
import io.github.lorenzolubrano.portafuori.rules.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Base64
import java.util.zip.Deflater

/** Regression tests for the security audit of 24/09/2026 (findings S1, S2, S5, S6, S8, S10). */
class ShareCodecSecurityTest {

    private fun code(json: String): String {
        val d = Deflater(9, true)
        d.setInput(json.encodeToByteArray())
        d.finish()
        val buf = ByteArray(json.length + 1024)
        val n = d.deflate(buf)
        d.end()
        return "PORTAFUORI1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(buf.copyOf(n))
    }

    private fun env(profile: String) = """{"app":"portafuori","kind":"profile","exportedAt":0,"profiles":[$profile]}"""
    private val bin = """{"n":"Carta","c":4281298902,"i":"paper","r":[{"t":"WEEKLY","w":1}]}"""

    private fun rejected(text: String) {
        try {
            ShareCodec.decode(text)
            fail("should have been rejected")
        } catch (e: InvalidImport) {
            assertTrue(e.message!!.isNotBlank())
        }
    }

    @Test fun roundTripStillWorks() {
        val profile = ProfileEntity(id = 1, name = "Casa", notes = "Isola ecologica: sab 9-12")
        val b = ProfileBundle(
            profile,
            listOf(Bin(BinEntity(id = 1, profileId = 1, name = "Vetro", colorArgb = 0xFF2E9E5B, iconKey = "glass"), listOf(Rule(RuleType.WEEKLY, weekdays = setOf(DayOfWeek.MONDAY))))),
            emptyList(), emptyList(), emptySet(),
        )
        val env = ShareCodec.decode(ShareCodec.qrPayload(Envelope(kind = "profile", exportedAt = 0, profiles = listOf(ShareCodec.toDto(b, withReminders = false)))))
        val p = ShareCodec.fromDto(env.profiles.single(), trustReminders = false)
        assertEquals("Casa", p.profile.name)
        assertEquals("Vetro", p.bins.single().first.name)
        assertEquals(setOf(DayOfWeek.MONDAY), p.bins.single().second.single().weekdays)
    }

    @Test fun decompressionBombIsRejected() {
        rejected(code(env("""{"name":"x","notes":"${" ".repeat(5_000_000)}"}""")))
    }

    @Test fun hugeInputIsRejectedBeforeParsing() {
        rejected(env("""{"name":"x","notes":"${"A".repeat(Limits.IMPORT_BYTES)}"}"""))
        try {
            ShareCodec.readLimited(ByteArrayInputStream(ByteArray(Limits.IMPORT_BYTES + 1)))
            fail("should stop reading")
        } catch (_: InvalidImport) {
        }
    }

    @Test fun longTextIsClampedNotStored() {
        val env = ShareCodec.decode(env("""{"name":"${"N".repeat(5000)}","notes":"${"A".repeat(100_000)}","bins":[{"n":"${"B".repeat(900)}","c":1,"i":"evil","p":"nope","r":[]}]}"""))
        val p = ShareCodec.fromDto(env.profiles.single(), trustReminders = false)
        assertEquals(Limits.PROFILE_NAME, p.profile.name.length)
        assertEquals(Limits.NOTES, p.profile.notes.length)
        val b = p.bins.single().first
        assertEquals(Limits.BIN_NAME, b.name.length)
        assertEquals("bag", b.iconKey)
        assertEquals(null, b.presetKey)
        assertEquals(0xFF000001, b.colorArgb)
    }

    @Test fun impossibleValuesAreRejected() {
        rejected(env("""{"name":"x","bins":[{"n":"C","c":1,"i":"paper","r":[{"t":"WEEKLY","w":1,"ss":1399,"se":1231}]}]}"""))
        rejected(env("""{"name":"x","from":"25:99","bins":[$bin]}"""))
        rejected(env("""{"name":"x","bins":[{"n":"C","c":1,"i":"paper","r":[{"t":"DAILY_HACK","w":1}]}]}"""))
        rejected(env("""{"name":"x","bins":[{"n":"C","c":1,"i":"paper","r":[{"t":"WEEKLY","w":1,"vu":"+999999999-12-31"}]}]}"""))
        rejected(env("""{"name":"x","bins":[$bin],"ex":[{"b":7,"k":"SKIP","d":"2026-10-01"}]}"""))
        rejected(env("""{"name":"x","bins":[$bin],"ex":[{"b":0,"k":"MOVE","d":"2026-10-01"}]}"""))
        rejected("""{"app":"altro","kind":"profile","exportedAt":0,"profiles":[{"name":"x"}]}""")
        rejected("PORTAFUORI1:%%%")
        // A normal date is fine
        ShareCodec.decode(env("""{"name":"x","bins":[{"n":"C","c":1,"i":"paper","r":[{"t":"WEEKLY","w":1,"vu":"2026-12-31"}]}]}"""))
    }

    @Test fun tooManyItemsAreRejected() {
        val many = (0..Limits.EXCEPTIONS).joinToString(",") { """{"b":0,"k":"ADD","d":"2026-10-01"}""" }
        rejected(env("""{"name":"x","bins":[$bin],"ex":[$many]}"""))
        val bins = (0..Limits.BINS).joinToString(",") { bin }
        rejected(env("""{"name":"x","bins":[$bins]}"""))
    }

    @Test fun neighbourCannotChangeMyReminders() {
        val e = ShareCodec.decode(env("""{"name":"x","bins":[$bin],"rem":{"eOn":false,"eAt":"04:00","pt":"2027-01-01","pf":"2026-01-01"}}"""))
        val fromNeighbour = ShareCodec.fromDto(e.profiles.single(), trustReminders = false).profile
        assertTrue(fromNeighbour.exposeReminderOn)
        assertEquals(LocalTime.of(20, 30), fromNeighbour.exposeReminderAt)
        assertEquals(null, fromNeighbour.pausedFrom)
        val restored = ShareCodec.fromDto(e.profiles.single(), trustReminders = true).profile
        assertFalse(restored.exposeReminderOn)
    }

    @Test fun manyExceptionsStayFast() {
        val p = ProfileRules(CalendarMode.COLLECTION_DAY, ExposureMode.EVENING_BEFORE, LocalTime.of(20, 0), LocalTime.of(6, 0), HolidayPolicy.ASK)
        val start = LocalDate.of(2026, 10, 1)
        val bins = (1L..Limits.BINS).map { BinRules(it, listOf(Rule(RuleType.WEEKLY, weekdays = DayOfWeek.entries.toSet()))) }
        val ex = bins.flatMap { b -> (0 until Limits.EXCEPTIONS / bins.size).map { DateException(b.binId, ExceptionKind.ADD, start.plusDays((it % 45).toLong())) } }
        val t0 = System.nanoTime()
        repeat(3) { Schedule.compute(p, bins, ex, start, start.plusDays(45)) }
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertTrue("compute took $ms ms", ms < 1500)
    }

    @Test fun oldMastelloCodesAreRejectedPolitely() {
        // A real 1.0.5 QR: old prefix around a compressed old envelope. It must fail with the friendly message, never crash.
        val oldEnvelope = """{"app":"mastello","kind":"profile","exportedAt":0,"profiles":[{"name":"x","bins":[$bin]}]}"""
        val oldQr = "MASTELLO1:" + code(oldEnvelope).removePrefix("PORTAFUORI1:")
        val e = runCatching { ShareCodec.decode(oldQr) }.exceptionOrNull()
        assertTrue("atteso InvalidImport, arrivato $e", e is InvalidImport)
        assertEquals("Non è un calendario di ${Brand.NAME}", e?.message)
        // A 1.0.5 backup file: the same envelope as plain JSON.
        val e2 = runCatching { ShareCodec.decode(oldEnvelope) }.exceptionOrNull()
        assertEquals("Non è un calendario di ${Brand.NAME}", e2?.message)
    }

    @Test fun newPrefixRoundTrips() {
        val env = ShareCodec.decode(env("""{"name":"x","bins":[$bin]}"""))
        val qr = ShareCodec.qrPayload(env)
        assertTrue(qr.startsWith("PORTAFUORI1:"))
        assertEquals(env, ShareCodec.decode(qr))
    }
}
