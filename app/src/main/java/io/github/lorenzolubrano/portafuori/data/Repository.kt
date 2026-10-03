package io.github.lorenzolubrano.portafuori.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.withTransaction
import io.github.lorenzolubrano.portafuori.share.ImportedProfile
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.catch
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.lorenzolubrano.portafuori.rules.ExceptionKind
import io.github.lorenzolubrano.portafuori.rules.Rule
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class Repository(private val db: PortafuoriDb, private val onChange: suspend () -> Unit) {
    private val dao = db.dao()

    val bundles: Flow<List<ProfileBundle>> = combine(
        combine(dao.profiles(), dao.bins(), dao.rules()) { p, b, r -> Triple(p, b, r) },
        dao.exceptions(),
        dao.holidays(),
        dao.done(),
    ) { (p, b, r), e, h, d -> assemble(p, b, r, e, h, d) }
        // An unreadable database must show an empty app, not crash-loop at the splash screen
        .catch { t -> Log.e(TAG, "cannot read the calendar", t); emit(emptyList()) }

    val deliveries: Flow<List<DeliveryEntity>> = dao.deliveries()

    suspend fun bundlesNow(): List<ProfileBundle> =
        assemble(dao.profilesNow(), dao.binsNow(), dao.rulesNow(), dao.exceptionsNow(), dao.holidaysNow(), dao.doneNow())

    private fun assemble(
        profiles: List<ProfileEntity>,
        bins: List<BinEntity>,
        rules: List<RuleEntity>,
        exceptions: List<ExceptionEntity>,
        holidays: List<HolidayEntity>,
        done: List<DoneEntity>,
    ): List<ProfileBundle> {
        val rulesByBin = rules.groupBy { it.binId }
        return profiles.map { p ->
            ProfileBundle(
                profile = p,
                bins = bins.filter { it.profileId == p.id }.map { b -> Bin(b, rulesByBin[b.id].orEmpty().map { it.toRule() }) },
                exceptions = exceptions.filter { it.profileId == p.id },
                holidays = holidays.filter { it.profileId == p.id },
                done = done.filter { it.profileId == p.id }.map { it.collectionDate }.toSet(),
            )
        }
    }

    /** The write is committed first; a failure while re-planning must never surface as a crash. */
    private suspend fun <T> change(block: suspend () -> T): T = block().also {
        try {
            onChange()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "re-planning failed", t)
        }
    }

    // --- profiles ---

    suspend fun createProfile(profile: ProfileEntity, bins: List<Pair<BinEntity, List<Rule>>>): Long =
        change { db.withTransaction { insertProfile(profile, bins, emptyList(), emptyList(), emptyList()) } }

    /**
     * Imports validated profiles atomically: either everything is written or nothing changes,
     * so «Sostituisci tutto» can never leave the user with no houses. Returns the last id.
     */
    suspend fun importProfiles(profiles: List<ImportedProfile>, replaceAll: Boolean, replaceId: Long?): Long? = change {
        db.withTransaction {
            if (replaceAll) dao.profilesNow().forEach { dao.deleteProfile(it.id) }
            var last: Long? = null
            for (p in profiles) {
                last = if (replaceId != null && profiles.size == 1) {
                    replaceCalendar(replaceId, p)
                    replaceId
                } else {
                    insertProfile(p.profile, p.bins, p.exceptions, p.exceptionBinIndex, p.holidays)
                }
            }
            last
        }
    }

    private suspend fun insertProfile(
        profile: ProfileEntity,
        bins: List<Pair<BinEntity, List<Rule>>>,
        exceptions: List<ExceptionEntity>,
        binIndexOfException: List<Int>,
        holidays: List<HolidayEntity>,
    ): Long {
        val order = dao.profilesNow().size
        val id = dao.insertProfile(profile.sanitized().copy(id = 0, sortOrder = order, createdAt = System.currentTimeMillis()))
        val binIds = bins.mapIndexed { i, (b, r) ->
            dao.saveBin(b.sanitized().copy(id = 0, profileId = id, sortOrder = i), r.map { it.toEntity() })
        }
        dao.insertExceptions(exceptions.mapIndexed { i, e -> e.copy(id = 0, profileId = id, binId = binIds[binIndexOfException[i]]) })
        dao.insertHolidays(holidays.map { it.sanitized().copy(id = 0, profileId = id) })
        return id
    }

    /**
     * A neighbour's calendar over an existing house: replaces bins, rules, edits and local holidays,
     * but keeps the recipient's own name, notes and reminder settings.
     */
    private suspend fun replaceCalendar(targetId: Long, p: ImportedProfile) {
        val old = dao.profile(targetId) ?: return
        val new = p.profile
        dao.updateProfile(
            old.copy(
                areaNote = old.areaNote.ifBlank { new.areaNote },
                calendarMode = new.calendarMode,
                exposureMode = new.exposureMode, exposeStart = new.exposeStart, exposeEnd = new.exposeEnd,
                holidayPolicy = new.holidayPolicy,
            ).sanitized(),
        )
        dao.binsOf(targetId).forEach { dao.deleteBin(it.id) }
        dao.holidaysNow().filter { it.profileId == targetId }.forEach { dao.deleteHoliday(it.id) }
        val binIds = p.bins.mapIndexed { i, (b, r) ->
            dao.saveBin(b.sanitized().copy(id = 0, profileId = targetId, sortOrder = i), r.map { it.toEntity() })
        }
        dao.insertExceptions(p.exceptions.mapIndexed { i, e -> e.copy(id = 0, profileId = targetId, binId = binIds[p.exceptionBinIndex[i]]) })
        dao.insertHolidays(p.holidays.map { it.sanitized().copy(id = 0, profileId = targetId) })
    }

    suspend fun updateProfile(p: ProfileEntity) = change { dao.updateProfile(p.sanitized()) }
    suspend fun deleteProfile(id: Long) = change { dao.deleteProfile(id) }

    // --- bins ---

    suspend fun saveBin(bin: BinEntity, rules: List<Rule>): Long = change {
        val order = if (bin.id == 0L) dao.binsOf(bin.profileId).size else bin.sortOrder
        dao.saveBin(bin.sanitized().copy(sortOrder = order), rules.take(Limits.RULES_PER_BIN).map { it.toEntity() })
    }

    suspend fun deleteBin(id: Long) = change { dao.deleteBin(id) }

    // --- single-date edits ---

    /** Removes every edit touching [date] for this bin, restoring what the rules say. */
    suspend fun restore(binId: Long, date: LocalDate) = change { dao.deleteExceptionsOn(binId, date) }

    suspend fun deleteException(id: Long) = change { dao.deleteException(id) }

    suspend fun addException(e: ExceptionEntity) = change {
        if (e.kind != ExceptionKind.ADD) dao.deleteExceptionsOn(e.binId, e.date)
        dao.insertException(e)
    }

    suspend fun addExceptions(list: List<ExceptionEntity>) = change {
        list.forEach {
            if (it.kind != ExceptionKind.ADD) dao.deleteExceptionsOn(it.binId, it.date)
            dao.insertException(it)
        }
    }

    // --- holidays ---

    suspend fun addHoliday(h: HolidayEntity) = change { dao.insertHoliday(h.sanitized()) }
    suspend fun deleteHoliday(id: Long) = change { dao.deleteHoliday(id) }

    // --- "Fatto" and snooze ---

    suspend fun markDone(profileId: Long, collection: LocalDate) = change {
        dao.insertDone(DoneEntity(profileId, collection))
        dao.deleteSnoozes(profileId, collection)
    }

    suspend fun undoDone(profileId: Long, collection: LocalDate) = change { dao.deleteDone(profileId, collection) }

    suspend fun snooze(profileId: Long, collection: LocalDate, at: Long) = change {
        dao.insertSnooze(SnoozeEntity(profileId = profileId, collectionDate = collection, at = at))
    }

    suspend fun snoozesNow() = dao.snoozesNow()

    // --- delivery log (no onChange: written by the reminder engine itself) ---

    suspend fun loggedKeysSince(since: Long) = dao.loggedKeysSince(since).toSet()
    suspend fun log(entry: DeliveryEntity) = dao.insertDelivery(entry)

    suspend fun prune(now: Long) {
        val day = 24 * 3600 * 1000L
        dao.pruneDeliveries(now - 60 * day)
        dao.pruneSnoozes(now - 2 * day)
        dao.pruneDone(LocalDate.now().minusDays(60))
    }
}

// --- small settings store ---

private const val TAG = "Repository"

// A corrupted settings file is reset instead of crashing every read
private val Context.dataStore by preferencesDataStore(
    "settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val selectedProfileId: Long? = null,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val lastBackupAt: Long? = null,
    val autoBackupUri: String? = null,
    val testAt: Long? = null,
    val annualCheckDismissedYear: Int = 0,
)

class Settings(private val context: Context) {
    private object K {
        val selected = longPreferencesKey("selected_profile")
        val theme = stringPreferencesKey("theme")
        val lastBackup = longPreferencesKey("last_backup")
        val autoBackup = stringPreferencesKey("auto_backup_uri")
        val testAt = longPreferencesKey("test_at")
        val annual = intPreferencesKey("annual_check_dismissed")
    }

    val data: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            selectedProfileId = p[K.selected],
            theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            lastBackupAt = p[K.lastBackup],
            autoBackupUri = p[K.autoBackup],
            testAt = p[K.testAt],
            annualCheckDismissedYear = p[K.annual] ?: 0,
        )
    }

    suspend fun now(): AppSettings = data.first()

    suspend fun setSelectedProfile(id: Long) = context.dataStore.edit { it[K.selected] = id }
    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[K.theme] = mode.name }
    suspend fun setLastBackup(at: Long) = context.dataStore.edit { it[K.lastBackup] = at }
    suspend fun setAutoBackupUri(uri: String?) = context.dataStore.edit {
        if (uri == null) it.remove(K.autoBackup) else it[K.autoBackup] = uri
    }
    suspend fun setTestAt(at: Long?) = context.dataStore.edit {
        if (at == null) it.remove(K.testAt) else it[K.testAt] = at
    }
    suspend fun dismissAnnualCheck(year: Int) = context.dataStore.edit { it[K.annual] = year }
}
