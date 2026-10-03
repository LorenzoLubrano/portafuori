package io.github.lorenzolubrano.portafuori.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter fun dateToLong(d: LocalDate?): Long? = d?.toEpochDay()
    @TypeConverter fun longToDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)
    @TypeConverter fun timeToInt(t: LocalTime?): Int? = t?.toSecondOfDay()
    @TypeConverter fun intToTime(v: Int?): LocalTime? = v?.let { LocalTime.ofSecondOfDay(it.toLong()) }
}

@Dao
interface PortafuoriDao {
    @Query("SELECT * FROM profile ORDER BY sortOrder, id")
    fun profiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profile ORDER BY sortOrder, id")
    suspend fun profilesNow(): List<ProfileEntity>

    @Query("SELECT * FROM profile WHERE id = :id")
    suspend fun profile(id: Long): ProfileEntity?

    @Insert suspend fun insertProfile(p: ProfileEntity): Long
    @Update suspend fun updateProfile(p: ProfileEntity)
    @Query("DELETE FROM profile WHERE id = :id") suspend fun deleteProfile(id: Long)

    @Query("SELECT * FROM bin ORDER BY sortOrder, id") fun bins(): Flow<List<BinEntity>>
    @Query("SELECT * FROM bin ORDER BY sortOrder, id") suspend fun binsNow(): List<BinEntity>
    @Query("SELECT * FROM bin WHERE profileId = :profileId ORDER BY sortOrder, id") suspend fun binsOf(profileId: Long): List<BinEntity>
    @Insert suspend fun insertBin(b: BinEntity): Long
    @Update suspend fun updateBin(b: BinEntity)
    @Query("DELETE FROM bin WHERE id = :id") suspend fun deleteBin(id: Long)

    @Query("SELECT * FROM rule ORDER BY id") fun rules(): Flow<List<RuleEntity>>
    @Query("SELECT * FROM rule ORDER BY id") suspend fun rulesNow(): List<RuleEntity>
    @Insert suspend fun insertRules(r: List<RuleEntity>)
    @Query("DELETE FROM rule WHERE binId = :binId") suspend fun deleteRulesOf(binId: Long)

    @Query("SELECT * FROM date_exception ORDER BY date") fun exceptions(): Flow<List<ExceptionEntity>>
    @Query("SELECT * FROM date_exception ORDER BY date") suspend fun exceptionsNow(): List<ExceptionEntity>
    @Insert suspend fun insertException(e: ExceptionEntity): Long
    @Insert suspend fun insertExceptions(e: List<ExceptionEntity>)
    @Query("DELETE FROM date_exception WHERE id = :id") suspend fun deleteException(id: Long)
    @Query("DELETE FROM date_exception WHERE binId = :binId AND (date = :date OR target = :date)")
    suspend fun deleteExceptionsOn(binId: Long, date: LocalDate)

    @Query("SELECT * FROM local_holiday ORDER BY id") fun holidays(): Flow<List<HolidayEntity>>
    @Query("SELECT * FROM local_holiday ORDER BY id") suspend fun holidaysNow(): List<HolidayEntity>
    @Insert suspend fun insertHoliday(h: HolidayEntity): Long
    @Insert suspend fun insertHolidays(h: List<HolidayEntity>)
    @Query("DELETE FROM local_holiday WHERE id = :id") suspend fun deleteHoliday(id: Long)

    @Query("SELECT * FROM exposure_done") fun done(): Flow<List<DoneEntity>>
    @Query("SELECT * FROM exposure_done") suspend fun doneNow(): List<DoneEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDone(d: DoneEntity)
    @Query("DELETE FROM exposure_done WHERE profileId = :profileId AND collectionDate = :date")
    suspend fun deleteDone(profileId: Long, date: LocalDate)
    @Query("DELETE FROM exposure_done WHERE collectionDate < :before") suspend fun pruneDone(before: LocalDate)

    @Query("SELECT * FROM snooze") suspend fun snoozesNow(): List<SnoozeEntity>
    @Insert suspend fun insertSnooze(s: SnoozeEntity): Long
    @Query("DELETE FROM snooze WHERE profileId = :profileId AND collectionDate = :date")
    suspend fun deleteSnoozes(profileId: Long, date: LocalDate)
    @Query("DELETE FROM snooze WHERE at < :before") suspend fun pruneSnoozes(before: Long)

    @Query("SELECT slotKey FROM delivery_log WHERE scheduledAt >= :since") suspend fun loggedKeysSince(since: Long): List<String>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertDelivery(d: DeliveryEntity)
    @Query("SELECT * FROM delivery_log ORDER BY scheduledAt DESC LIMIT 200") fun deliveries(): Flow<List<DeliveryEntity>>
    @Query("DELETE FROM delivery_log WHERE scheduledAt < :before") suspend fun pruneDeliveries(before: Long)

    @Delete suspend fun deleteDoneRow(d: DoneEntity)

    /** Replaces a bin's rules in one go. */
    @Transaction
    suspend fun saveBin(bin: BinEntity, rules: List<RuleEntity>): Long {
        val id = if (bin.id == 0L) insertBin(bin) else bin.id.also { updateBin(bin) }
        deleteRulesOf(id)
        insertRules(rules.map { it.copy(id = 0, binId = id) })
        return id
    }
}

@Database(
    entities = [
        ProfileEntity::class, BinEntity::class, RuleEntity::class, ExceptionEntity::class,
        HolidayEntity::class, DeliveryEntity::class, DoneEntity::class, SnoozeEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PortafuoriDb : RoomDatabase() {
    abstract fun dao(): PortafuoriDao

    companion object {
        fun create(context: Context): PortafuoriDb =
            Room.databaseBuilder(context, PortafuoriDb::class.java, "portafuori.db")
                .addCallback(RepairOversizedRows)
                .build()
    }
}

/**
 * Shrinks any text field above the app limits before the first read. A row larger than the
 * 2 MB CursorWindow cannot be SELECTed but can still be UPDATEd, so this rescues a database
 * poisoned by an older version (or by a restored backup) instead of crashing on every launch.
 */
private object RepairOversizedRows : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE profile SET name = substr(name, 1, ${Limits.PROFILE_NAME}) WHERE length(name) > ${Limits.PROFILE_NAME}")
        db.execSQL("UPDATE profile SET areaNote = substr(areaNote, 1, ${Limits.AREA}) WHERE length(areaNote) > ${Limits.AREA}")
        db.execSQL("UPDATE profile SET notes = substr(notes, 1, ${Limits.NOTES}) WHERE length(notes) > ${Limits.NOTES}")
        db.execSQL("UPDATE bin SET name = substr(name, 1, ${Limits.BIN_NAME}) WHERE length(name) > ${Limits.BIN_NAME}")
        db.execSQL("UPDATE bin SET iconKey = 'bag' WHERE length(iconKey) > 32")
        db.execSQL("UPDATE bin SET presetKey = NULL WHERE length(presetKey) > 32")
        db.execSQL("UPDATE rule SET dates = '' WHERE length(dates) > ${Limits.DATES_PER_RULE * 11}")
        db.execSQL("UPDATE local_holiday SET name = substr(name, 1, ${Limits.HOLIDAY_NAME}) WHERE length(name) > ${Limits.HOLIDAY_NAME}")
        db.execSQL("UPDATE delivery_log SET title = substr(title, 1, 200) WHERE length(title) > 200")
    }
}
