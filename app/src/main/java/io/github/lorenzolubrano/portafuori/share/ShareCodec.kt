package io.github.lorenzolubrano.portafuori.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.github.lorenzolubrano.portafuori.Brand
import io.github.lorenzolubrano.portafuori.data.Bin
import io.github.lorenzolubrano.portafuori.data.BinEntity
import io.github.lorenzolubrano.portafuori.data.ExceptionEntity
import io.github.lorenzolubrano.portafuori.data.ExceptionReason
import io.github.lorenzolubrano.portafuori.data.HolidayEntity
import io.github.lorenzolubrano.portafuori.data.Limits
import io.github.lorenzolubrano.portafuori.data.ProfileBundle
import io.github.lorenzolubrano.portafuori.data.ProfileEntity
import io.github.lorenzolubrano.portafuori.data.RuleEntity
import io.github.lorenzolubrano.portafuori.data.clamp
import io.github.lorenzolubrano.portafuori.data.encodeMonthDay
import io.github.lorenzolubrano.portafuori.data.sanitized
import io.github.lorenzolubrano.portafuori.data.toEntity
import io.github.lorenzolubrano.portafuori.data.toRule
import io.github.lorenzolubrano.portafuori.rules.CalendarMode
import io.github.lorenzolubrano.portafuori.rules.ExceptionKind
import io.github.lorenzolubrano.portafuori.rules.ExposureMode
import io.github.lorenzolubrano.portafuori.rules.HolidayPolicy
import io.github.lorenzolubrano.portafuori.rules.Rule
import io.github.lorenzolubrano.portafuori.rules.RuleType
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.MonthDay
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Short field names keep the QR code small. Unknown fields are ignored so newer files still import.

@Serializable
data class Envelope(
    val v: Int = 1,
    val app: String = "portafuori",
    val kind: String,
    val exportedAt: Long,
    val profiles: List<ProfileDto>,
)

@Serializable
data class ProfileDto(
    val name: String,
    val area: String = "",
    val cal: String = CalendarMode.COLLECTION_DAY.name,
    val exp: String = ExposureMode.EVENING_BEFORE.name,
    val from: String = "20:00",
    val to: String = "06:00",
    val hol: String = HolidayPolicy.ASK.name,
    val notes: String = "",
    val bins: List<BinDto> = emptyList(),
    val ex: List<ExDto> = emptyList(),
    val lh: List<HolDto> = emptyList(),
    /** Personal reminder settings: only in backups, never in neighbour sharing. */
    val rem: ReminderDto? = null,
)

@Serializable
data class BinDto(val n: String, val c: Long, val i: String, val p: String? = null, val r: List<RuleDto> = emptyList())

@Serializable
data class RuleDto(
    val t: String,
    val w: Int = 0,
    val n: Int = 1,
    val a: String? = null,
    val o: Int = 0,
    val d: String = "",
    val ss: Int? = null,
    val se: Int? = null,
    val vf: String? = null,
    val vu: String? = null,
)

/** [b] is the index of the bin inside [ProfileDto.bins]. */
@Serializable
data class ExDto(val b: Int, val k: String, val d: String, val t: String? = null)

@Serializable
data class HolDto(val n: String, val md: Int? = null, val d: String? = null)

@Serializable
data class ReminderDto(
    val eOn: Boolean = true,
    val eAt: String = "20:30",
    val pOn: Boolean = false,
    val pAt: String = "18:00",
    val rOn: Boolean = false,
    val rAt: String = "13:00",
    val ins: Boolean = false,
    val pf: String? = null,
    val pt: String? = null,
)

/** Everything needed to recreate a profile through the repository. Already validated and clamped. */
class ImportedProfile(
    val profile: ProfileEntity,
    val bins: List<Pair<BinEntity, List<Rule>>>,
    val exceptions: List<ExceptionEntity>,
    val exceptionBinIndex: List<Int>,
    val holidays: List<HolidayEntity>,
) {
    /** An unsaved bundle with fake ids, for the import preview. */
    fun previewBundle() = ProfileBundle(
        profile = profile,
        bins = bins.mapIndexed { i, (b, r) -> Bin(b.copy(id = i + 1L), r) },
        exceptions = exceptions.mapIndexed { i, e -> e.copy(binId = exceptionBinIndex[i] + 1L) },
        holidays = holidays,
        done = emptySet(),
    )
}

/** Thrown for anything we refuse to import; the message is shown to the user. */
class InvalidImport(message: String) : IllegalArgumentException(message)

object ShareCodec {
    private const val QR_PREFIX = "PORTAFUORI1:"
    const val QR_MAX_CHARS = 1800

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    private fun check(ok: Boolean, what: String) {
        if (!ok) throw InvalidImport(what)
    }

    fun toDto(b: ProfileBundle, withReminders: Boolean, today: LocalDate = LocalDate.now()): ProfileDto {
        val p = b.profile
        val binIndex = b.bins.mapIndexed { i, bin -> bin.id to i }.toMap()
        // Neighbours only need current and future edits; backups keep everything
        val relevant = b.exceptions.filter { withReminders || maxOf(it.date, it.target ?: it.date) >= today.minusDays(1) }
        return ProfileDto(
            name = p.name,
            area = p.areaNote,
            cal = p.calendarMode.name,
            exp = p.exposureMode.name,
            from = p.exposeStart.toString(),
            to = p.exposeEnd.toString(),
            hol = p.holidayPolicy.name,
            notes = p.notes,
            bins = b.bins.map { bin ->
                BinDto(bin.name, bin.entity.colorArgb, bin.entity.iconKey, bin.entity.presetKey, bin.rules.map { it.toEntity().toDto() })
            },
            ex = relevant.mapNotNull { e ->
                binIndex[e.binId]?.let { ExDto(it, e.kind.name, e.date.toString(), e.target?.toString()) }
            },
            lh = b.holidays.map { HolDto(it.name, it.monthDay, it.date?.toString()) },
            rem = if (withReminders) {
                ReminderDto(
                    p.exposeReminderOn, p.exposeReminderAt.toString(), p.prepareReminderOn, p.prepareReminderAt.toString(),
                    p.retrieveReminderOn, p.retrieveReminderAt.toString(), p.insistent,
                    p.pausedFrom?.toString(), p.pausedTo?.toString(),
                )
            } else {
                null
            },
        )
    }

    private fun RuleEntity.toDto() = RuleDto(
        type.name, weekdaysMask, intervalWeeks, anchor?.toString(), ordinalsMask, dates,
        seasonStart, seasonEnd, validFrom?.toString(), validUntil?.toString(),
    )

    // --- strict parsing of untrusted values ---

    private fun date(s: String): LocalDate {
        val d = runCatching { LocalDate.parse(s) }.getOrNull() ?: throw InvalidImport("Data non valida")
        check(d in Limits.MIN_DATE..Limits.MAX_DATE, "Data fuori dal calendario")
        return d
    }

    private fun time(s: String): LocalTime =
        runCatching { LocalTime.parse(s) }.getOrNull() ?: throw InvalidImport("Orario non valido")

    /** month * 100 + day, accepting 29 February. */
    private fun monthDay(v: Int): MonthDay {
        val month = v / 100
        val day = v % 100
        check(month in 1..12 && day in 1..Month.of(month).maxLength(), "Periodo dell'anno non valido")
        return MonthDay.of(month, day)
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E =
        runCatching { enumValueOf<E>(name) }.getOrDefault(fallback)

    private fun RuleDto.toRule(): Rule {
        val type = runCatching { RuleType.valueOf(t) }.getOrNull() ?: throw InvalidImport("Tipo di regola sconosciuto")
        val dateList = d.split(',').filter { it.isNotBlank() }
        check(dateList.size <= Limits.DATES_PER_RULE, "Troppe date in una regola")
        check((ss == null) == (se == null), "Periodo dell'anno incompleto")
        val entity = RuleEntity(
            binId = 0,
            type = type,
            weekdaysMask = w and 0x7F,
            intervalWeeks = n.coerceIn(1, Limits.MAX_INTERVAL_WEEKS),
            anchor = a?.let(::date),
            ordinalsMask = o and 0x7E,
            dates = dateList.map(::date).distinct().sorted().joinToString(","),
            seasonStart = ss?.let { encodeMonthDay(monthDay(it)) },
            seasonEnd = se?.let { encodeMonthDay(monthDay(it)) },
            validFrom = vf?.let(::date),
            validUntil = vu?.let(::date),
        )
        check(type != RuleType.EVERY_N_WEEKS || entity.anchor != null, "Regola «ogni N settimane» senza data di partenza")
        return entity.toRule()
    }

    /**
     * Validates and converts one profile. Throws [InvalidImport] for anything malformed.
     * [trustReminders] is true only for a backup the user chose to restore: a neighbour's code
     * must never change the recipient's reminder times or pause them.
     */
    fun fromDto(dto: ProfileDto, trustReminders: Boolean): ImportedProfile {
        check(dto.bins.size <= Limits.BINS, "Troppi bidoni")
        check(dto.ex.size <= Limits.EXCEPTIONS, "Troppe modifiche ai singoli giorni")
        check(dto.lh.size <= Limits.HOLIDAYS, "Troppe festività")
        check(dto.bins.all { it.r.size <= Limits.RULES_PER_BIN }, "Troppe regole in un bidone")

        val base = ProfileEntity(
            name = dto.name,
            areaNote = dto.area,
            calendarMode = enumOr(dto.cal, CalendarMode.COLLECTION_DAY),
            exposureMode = enumOr(dto.exp, ExposureMode.EVENING_BEFORE),
            exposeStart = time(dto.from),
            exposeEnd = time(dto.to),
            holidayPolicy = enumOr(dto.hol, HolidayPolicy.ASK),
            notes = dto.notes,
        ).sanitized()
        val rem = dto.rem?.takeIf { trustReminders }
        val profile = if (rem == null) {
            base
        } else {
            val pf = rem.pf?.let { runCatching { date(it) }.getOrNull() }
            val pt = rem.pt?.let { runCatching { date(it) }.getOrNull() }
            base.copy(
                exposeReminderOn = rem.eOn,
                exposeReminderAt = runCatching { time(rem.eAt) }.getOrDefault(base.exposeReminderAt),
                prepareReminderOn = rem.pOn,
                prepareReminderAt = runCatching { time(rem.pAt) }.getOrDefault(base.prepareReminderAt),
                retrieveReminderOn = rem.rOn,
                retrieveReminderAt = runCatching { time(rem.rAt) }.getOrDefault(base.retrieveReminderAt),
                insistent = rem.ins,
                pausedFrom = if (pf != null && pt != null && pf <= pt) pf else null,
                pausedTo = if (pf != null && pt != null && pf <= pt) pt else null,
            )
        }
        val bins = dto.bins.mapIndexed { i, b ->
            BinEntity(profileId = 0, presetKey = b.p?.clamp(32), name = b.n, colorArgb = b.c, iconKey = b.i.clamp(32), sortOrder = i)
                .sanitized() to b.r.map { it.toRule() }
        }
        val exceptions = dto.ex.map { e ->
            check(e.b in bins.indices, "Modifica riferita a un bidone che non esiste")
            val kind = runCatching { ExceptionKind.valueOf(e.k) }.getOrNull() ?: throw InvalidImport("Modifica sconosciuta")
            val target = e.t?.let(::date)
            check(kind != ExceptionKind.MOVE || target != null, "Spostamento senza data")
            e.b to ExceptionEntity(profileId = 0, binId = 0, kind = kind, date = date(e.d), target = target, reason = ExceptionReason.IMPORT)
        }.distinctBy { (b, ex) -> listOf(b, ex.kind, ex.date, ex.target) }
        val holidays = dto.lh.map { h ->
            val md = h.md?.let { encodeMonthDay(monthDay(it)) }
            val d = h.d?.let(::date)
            check(md != null || d != null, "Festività senza data")
            HolidayEntity(profileId = 0, name = h.n, monthDay = if (d == null) md else null, date = d).sanitized()
        }
        return ImportedProfile(
            profile = profile,
            bins = bins,
            exceptions = exceptions.map { it.second },
            exceptionBinIndex = exceptions.map { it.first },
            holidays = holidays,
        )
    }

    fun encode(env: Envelope): String = json.encodeToString(Envelope.serializer(), env)

    /**
     * Accepts a raw JSON file, a QR payload, or a chat message containing the payload.
     * Everything is size-capped and fully validated here, so callers only need one try/catch.
     */
    fun decode(text: String): Envelope {
        check(text.length <= Limits.IMPORT_BYTES, "Il file è troppo grande")
        val t = text.trim()
        val idx = t.indexOf(QR_PREFIX)
        val jsonText = if (idx >= 0) {
            val payload = t.substring(idx + QR_PREFIX.length).takeWhile { !it.isWhitespace() }
            val bytes = runCatching { Base64.getUrlDecoder().decode(payload) }.getOrNull() ?: throw InvalidImport("Codice incompleto")
            inflate(bytes).decodeToString()
        } else {
            t
        }
        val env = runCatching { json.decodeFromString(Envelope.serializer(), jsonText) }.getOrNull()
            ?: throw InvalidImport("Non è un calendario di ${Brand.NAME}")
        check(env.app == "portafuori", "Non è un calendario di ${Brand.NAME}")
        check(env.profiles.size in 1..Limits.PROFILES, "Numero di case non valido")
        env.profiles.forEach { fromDto(it, trustReminders = true) }
        return env
    }

    fun qrPayload(env: Envelope): String =
        QR_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(deflate(encode(env).encodeToByteArray()))

    /** Reads at most [Limits.IMPORT_BYTES]; a longer or endless stream is rejected, not buffered. */
    fun readLimited(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8 * 1024)
        while (true) {
            val n = input.read(buf)
            if (n < 0) return out.toByteArray()
            out.write(buf, 0, n)
            check(out.size() <= Limits.IMPORT_BYTES, "Il file è troppo grande")
        }
    }

    private fun deflate(data: ByteArray): ByteArray {
        val d = Deflater(Deflater.BEST_COMPRESSION, true)
        try {
            d.setInput(data)
            d.finish()
            val out = ByteArrayOutputStream()
            val buf = ByteArray(1024)
            while (!d.finished()) out.write(buf, 0, d.deflate(buf))
            return out.toByteArray()
        } finally {
            d.end()
        }
    }

    /** Stops at [Limits.INFLATED_BYTES]: a tiny code cannot expand into hundreds of MB. */
    private fun inflate(data: ByteArray): ByteArray {
        val inf = Inflater(true)
        try {
            inf.setInput(data)
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8 * 1024)
            while (!inf.finished()) {
                val n = runCatching { inf.inflate(buf) }.getOrNull() ?: throw InvalidImport("Codice danneggiato")
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break
                out.write(buf, 0, n)
                check(out.size() <= Limits.INFLATED_BYTES, "Il codice è troppo grande")
            }
            return out.toByteArray()
        } finally {
            inf.end()
        }
    }
}

object Qr {
    private const val MAX_SIDE = 2048

    fun encode(text: String, size: Int = 900): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(m.width * m.height) { i -> if (m.get(i % m.width, i / m.width)) Color.BLACK else Color.WHITE }
        return Bitmap.createBitmap(pixels, m.width, m.height, Bitmap.Config.ARGB_8888)
    }

    /** Loads a picked image downsampled to at most [MAX_SIDE] px, so a 200 MP photo cannot exhaust memory. */
    fun loadImage(context: Context, uri: Uri): Bitmap? = runCatching {
        val cr = context.contentResolver
        if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(cr, uri)) { d, info, _ ->
                d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > MAX_SIDE) d.setTargetSampleSize((longest + MAX_SIDE - 1) / MAX_SIDE)
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        }
    }.getOrNull()

    fun decode(bitmap: Bitmap): String? {
        val source = runCatching {
            val w = bitmap.width
            val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            RGBLuminanceSource(w, h, pixels)
        }.getOrNull() ?: return null
        return decode(source)
    }

    fun decode(source: LuminanceSource): String? {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true,
        )
        return runCatching { MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)), hints).text }.getOrNull()
    }
}
