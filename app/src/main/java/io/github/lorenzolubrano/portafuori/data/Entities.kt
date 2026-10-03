package io.github.lorenzolubrano.portafuori.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.lorenzolubrano.portafuori.rules.CalendarMode
import io.github.lorenzolubrano.portafuori.rules.ExceptionKind
import io.github.lorenzolubrano.portafuori.rules.ExposureMode
import io.github.lorenzolubrano.portafuori.rules.HolidayPolicy
import io.github.lorenzolubrano.portafuori.rules.RuleType
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val areaNote: String = "",
    val calendarMode: CalendarMode = CalendarMode.COLLECTION_DAY,
    val exposureMode: ExposureMode = ExposureMode.EVENING_BEFORE,
    val exposeStart: LocalTime = LocalTime.of(20, 0),
    val exposeEnd: LocalTime = LocalTime.of(6, 0),
    val holidayPolicy: HolidayPolicy = HolidayPolicy.ASK,
    val pausedFrom: LocalDate? = null,
    val pausedTo: LocalDate? = null,
    val notes: String = "",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val exposeReminderOn: Boolean = true,
    val exposeReminderAt: LocalTime = LocalTime.of(20, 30),
    val prepareReminderOn: Boolean = false,
    val prepareReminderAt: LocalTime = LocalTime.of(18, 0),
    val retrieveReminderOn: Boolean = false,
    val retrieveReminderAt: LocalTime = LocalTime.of(13, 0),
    /** Repeat the "expose" reminder after 45 minutes until the user taps "Fatto". */
    val insistent: Boolean = false,
)

@Entity(
    tableName = "bin",
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("profileId")],
)
data class BinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val presetKey: String? = null,
    val name: String,
    val colorArgb: Long,
    val iconKey: String,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "rule",
    foreignKeys = [ForeignKey(BinEntity::class, ["id"], ["binId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("binId")],
)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val binId: Long,
    val type: RuleType,
    /** Bit 0 = Monday ... bit 6 = Sunday. */
    val weekdaysMask: Int = 0,
    val intervalWeeks: Int = 1,
    val anchor: LocalDate? = null,
    /** Bits 1..5 = first..fifth, bit 6 = last. */
    val ordinalsMask: Int = 0,
    /** ISO dates separated by commas. */
    val dates: String = "",
    /** Encoded as month * 100 + day. */
    val seasonStart: Int? = null,
    val seasonEnd: Int? = null,
    val validFrom: LocalDate? = null,
    val validUntil: LocalDate? = null,
)

enum class ExceptionReason { USER, HOLIDAY, IMPORT }

@Entity(
    tableName = "date_exception",
    foreignKeys = [
        ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(BinEntity::class, ["id"], ["binId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("profileId"), Index("binId")],
)
data class ExceptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val binId: Long,
    val kind: ExceptionKind,
    val date: LocalDate,
    val target: LocalDate? = null,
    val reason: ExceptionReason = ExceptionReason.USER,
)

@Entity(
    tableName = "local_holiday",
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("profileId")],
)
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,
    /** Yearly holiday: month * 100 + day. */
    val monthDay: Int? = null,
    /** One-off holiday. */
    val date: LocalDate? = null,
)

/** One row per reminder the app was supposed to show: delivered (postedAt) or missed (null). */
@Entity(tableName = "delivery_log", indices = [Index("scheduledAt")])
data class DeliveryEntity(
    @PrimaryKey val slotKey: String,
    val kind: String,
    val profileId: Long,
    val title: String,
    val scheduledAt: Long,
    val postedAt: Long?,
)

/** "Fatto": the bins for this collection are out. */
@Entity(tableName = "exposure_done", primaryKeys = ["profileId", "collectionDate"])
data class DoneEntity(
    val profileId: Long,
    val collectionDate: LocalDate,
    val doneAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "snooze")
data class SnoozeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val collectionDate: LocalDate,
    val at: Long,
)
