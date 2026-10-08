package io.github.lorenzolubrano.portafuori.ui

import io.github.lorenzolubrano.portafuori.Brand
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.lorenzolubrano.portafuori.App
import io.github.lorenzolubrano.portafuori.data.AppSettings
import io.github.lorenzolubrano.portafuori.data.Bin
import io.github.lorenzolubrano.portafuori.data.BinEntity
import io.github.lorenzolubrano.portafuori.data.ExceptionEntity
import io.github.lorenzolubrano.portafuori.data.ExceptionReason
import io.github.lorenzolubrano.portafuori.data.HolidayEntity
import io.github.lorenzolubrano.portafuori.data.Presets
import io.github.lorenzolubrano.portafuori.data.ProfileBundle
import io.github.lorenzolubrano.portafuori.data.ProfileEntity
import io.github.lorenzolubrano.portafuori.data.ThemeMode
import io.github.lorenzolubrano.portafuori.reminders.Engine
import io.github.lorenzolubrano.portafuori.reminders.Notifications
import io.github.lorenzolubrano.portafuori.reminders.Reliability
import io.github.lorenzolubrano.portafuori.reminders.Slot
import io.github.lorenzolubrano.portafuori.reminders.SlotKind
import io.github.lorenzolubrano.portafuori.rules.CalendarMode
import io.github.lorenzolubrano.portafuori.rules.ExceptionKind
import io.github.lorenzolubrano.portafuori.rules.ExposureMode
import io.github.lorenzolubrano.portafuori.rules.Occurrence
import io.github.lorenzolubrano.portafuori.rules.Origin
import io.github.lorenzolubrano.portafuori.rules.Rule
import io.github.lorenzolubrano.portafuori.rules.RuleType
import io.github.lorenzolubrano.portafuori.share.Envelope
import io.github.lorenzolubrano.portafuori.share.InvalidImport
import io.github.lorenzolubrano.portafuori.share.Qr
import io.github.lorenzolubrano.portafuori.share.ShareCodec
import android.content.ContentResolver
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Today : Screen
    data object Calendar : Screen
    data object Bins : Screen
    data object More : Screen
    data class BinEditor(val binId: Long?) : Screen
    data class Wizard(val first: Boolean) : Screen
    data object Reliability : Screen
    data object Holidays : Screen
    data object Share : Screen
    data object ProfileSettings : Screen
    data object Scan : Screen
    data object ImportPreview : Screen
    data object Info : Screen
}

data class UiState(
    val loaded: Boolean = false,
    val bundles: List<ProfileBundle> = emptyList(),
    val settings: AppSettings = AppSettings(),
) {
    val selected: ProfileBundle? = bundles.firstOrNull { it.profile.id == settings.selectedProfileId } ?: bundles.firstOrNull()
}

data class DraftBin(
    val key: String?,
    val name: String,
    val color: Long,
    val icon: String,
    val selected: Boolean,
    val days: Set<DayOfWeek> = emptySet(),
)

data class WizardDraft(
    val step: Int = 0,
    val name: String = "Casa",
    val area: String = "",
    val bins: List<DraftBin> = Presets.bins.mapIndexed { i, p -> DraftBin(p.key, p.name, p.color, p.icon, selected = i < 5) },
    val calendarMode: CalendarMode = CalendarMode.COLLECTION_DAY,
    val exposureMode: ExposureMode = ExposureMode.EVENING_BEFORE,
    val exposeStart: LocalTime = LocalTime.of(20, 0),
    val exposeEnd: LocalTime = LocalTime.of(6, 0),
    val exposeOn: Boolean = true,
    val exposeAt: LocalTime = LocalTime.of(20, 30),
    val prepareOn: Boolean = false,
    val prepareAt: LocalTime = LocalTime.of(18, 0),
    val retrieveOn: Boolean = false,
    val retrieveAt: LocalTime = LocalTime.of(13, 0),
) {
    fun profile() = ProfileEntity(
        name = name.trim().ifBlank { "Casa" }, areaNote = area.trim(),
        calendarMode = calendarMode, exposureMode = exposureMode, exposeStart = exposeStart, exposeEnd = exposeEnd,
        exposeReminderOn = exposeOn, exposeReminderAt = exposeAt,
        prepareReminderOn = prepareOn, prepareReminderAt = prepareAt,
        retrieveReminderOn = retrieveOn, retrieveReminderAt = retrieveAt,
    )

    fun chosen() = bins.filter { it.selected }

    fun binsWithRules(): List<Pair<BinEntity, List<Rule>>> = chosen().map {
        BinEntity(profileId = 0, presetKey = it.key, name = it.name, colorArgb = it.color, iconKey = it.icon) to
            if (it.days.isEmpty()) emptyList() else listOf(Rule(RuleType.WEEKLY, weekdays = it.days))
    }

    /** An unsaved bundle, for the preview step. */
    fun previewBundle(): ProfileBundle = ProfileBundle(
        profile = profile(),
        bins = binsWithRules().mapIndexed { i, (b, r) -> Bin(b.copy(id = i + 1L, sortOrder = i), r) },
        exceptions = emptyList(), holidays = emptyList(), done = emptySet(),
    )
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as App
    private val repo = app.repository
    private val settingsStore = app.settings

    val state: StateFlow<UiState> = combine(repo.bundles, settingsStore.data) { b, s -> UiState(true, b, s) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState())
    val deliveries = repo.deliveries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- navigation: a tab plus a stack of pushed screens ---
    var tab by mutableStateOf<Screen>(Screen.Today)
        private set
    val stack = mutableStateListOf<Screen>()
    val current: Screen get() = stack.lastOrNull() ?: tab

    fun open(s: Screen) { stack.add(s) }
    fun switchTab(s: Screen) { clearStack(); tab = s }
    fun back(): Boolean = if (stack.isNotEmpty()) { stack.removeAt(stack.lastIndex); true } else if (tab != Screen.Today) { tab = Screen.Today; true } else false

    var message by mutableStateOf<String?>(null)
    var reliability by mutableStateOf(Reliability.status(application))
        private set

    fun refresh() {
        reliability = Reliability.status(app)
        Engine.runAsync(app)
    }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }

    // --- profiles ---
    fun selectProfile(id: Long) = launch { settingsStore.setSelectedProfile(id) }
    fun updateProfile(p: ProfileEntity) = launch { repo.updateProfile(p) }
    fun deleteProfile(id: Long) = launch {
        repo.deleteProfile(id)
        state.value.bundles.firstOrNull { it.profile.id != id }?.let { settingsStore.setSelectedProfile(it.profile.id) }
        clearStack()
    }
    fun setTheme(mode: ThemeMode) = launch { settingsStore.setTheme(mode) }

    /** Bin editor draft: lives in the ViewModel so a rotation or a system theme change keeps the edits. */
    data class BinDraft(val binId: Long?, val name: String, val color: Long, val icon: String, val rules: List<Rule>)
    var binDraft by mutableStateOf<BinDraft?>(null)
    /** House settings draft, for the same reason. */
    var profileDraft by mutableStateOf<ProfileEntity?>(null)

    /** Drops every open screen and, with them, their unsaved drafts: an abandoned edit never comes back on its own. */
    private fun clearStack() {
        stack.clear()
        binDraft = null
        profileDraft = null
    }

    // --- wizard ---
    var wizard by mutableStateOf(WizardDraft())
    fun startWizard(first: Boolean) {
        wizard = WizardDraft(name = if (state.value.bundles.isEmpty()) "Casa" else "Seconda casa")
        if (first) clearStack()
        open(Screen.Wizard(first))
    }
    fun saveWizard(onSaved: () -> Unit) = launch {
        val w = wizard
        val id = repo.createProfile(w.profile(), w.binsWithRules())
        settingsStore.setSelectedProfile(id)
        clearStack()
        tab = Screen.Today
        onSaved()
    }

    // --- bins ---
    fun saveBin(bin: BinEntity, rules: List<Rule>) = launch { repo.saveBin(bin, rules) }
    fun deleteBin(id: Long) = launch { repo.deleteBin(id) }

    // --- single dates ---
    fun skip(o: Occurrence, profileId: Long) = launch {
        when (o.origin) {
            Origin.RULE -> repo.addException(ExceptionEntity(profileId = profileId, binId = o.binId, kind = ExceptionKind.SKIP, date = o.calendarDate))
            Origin.ADDED -> repo.restore(o.binId, o.calendarDate)
            Origin.MOVED_IN -> {
                repo.restore(o.binId, o.calendarDate)
                o.movedFrom?.let { repo.addException(ExceptionEntity(profileId = profileId, binId = o.binId, kind = ExceptionKind.SKIP, date = it)) }
            }
        }
    }

    /** Drops every edit on this date (for a moved-in collection this undoes the move). */
    fun restore(o: Occurrence) = launch { repo.restore(o.binId, o.calendarDate) }
    fun move(o: Occurrence, profileId: Long, to: LocalDate) = launch {
        when (o.origin) {
            Origin.RULE -> repo.addException(ExceptionEntity(profileId = profileId, binId = o.binId, kind = ExceptionKind.MOVE, date = o.calendarDate, target = to))
            Origin.MOVED_IN -> {
                val from = o.movedFrom ?: return@launch
                repo.restore(o.binId, from)
                repo.addException(ExceptionEntity(profileId = profileId, binId = o.binId, kind = ExceptionKind.MOVE, date = from, target = to))
            }
            Origin.ADDED -> {
                repo.restore(o.binId, o.calendarDate)
                repo.addException(ExceptionEntity(profileId = profileId, binId = o.binId, kind = ExceptionKind.ADD, date = to))
            }
        }
    }
    fun restoreException(e: ExceptionEntity) = launch { repo.deleteException(e.id) }
    fun addExtra(profileId: Long, binId: Long, date: LocalDate) = launch {
        repo.addException(ExceptionEntity(profileId = profileId, binId = binId, kind = ExceptionKind.ADD, date = date))
    }
    fun holidayDecision(profileId: Long, occurrences: List<Occurrence>, keep: Boolean) = launch {
        repo.addExceptions(
            occurrences.map {
                ExceptionEntity(
                    profileId = profileId, binId = it.binId, kind = if (keep) ExceptionKind.KEEP else ExceptionKind.SKIP,
                    date = it.calendarDate, reason = ExceptionReason.HOLIDAY,
                )
            },
        )
    }
    fun addHoliday(h: HolidayEntity) = launch { repo.addHoliday(h) }
    fun deleteHoliday(id: Long) = launch { repo.deleteHoliday(id) }

    // --- "Fatto" ---
    fun markDone(profileId: Long, collection: LocalDate) = launch { repo.markDone(profileId, collection) }
    fun undoDone(profileId: Long, collection: LocalDate) = launch { repo.undoDone(profileId, collection) }

    // --- tests ---
    fun testNow() {
        val now = System.currentTimeMillis()
        val shown = Notifications.post(
            app,
            Slot("test|$now", SlotKind.TEST, 0, null, now, now + 60_000, "Notifica di prova", "Se la vedi, le notifiche di ${Brand.NAME} funzionano."),
            late = false,
        )
        if (!shown) message = "Le notifiche sono bloccate: attivale dalle impostazioni."
    }
    fun testInOneMinute() = launch {
        settingsStore.setTestAt(System.currentTimeMillis() + 60_000)
        Engine.run(app)
        message = "Prova programmata: blocca lo schermo e aspetta un minuto."
    }

    // --- share, import, backup ---
    var pendingImport by mutableStateOf<Envelope?>(null)
        private set

    /**
     * True only when the user chose «Ripristina backup»: then (and only then) personal reminder
     * settings are applied and «Sostituisci tutto» is offered. The sender cannot decide this.
     */
    var pendingRestore by mutableStateOf(false)
        private set

    /** Blocks double taps while an import is being written. */
    var importing by mutableStateOf(false)
        private set

    private fun importFailed(e: Throwable) {
        val reason = e.message?.takeIf { e is InvalidImport }?.replaceFirstChar { it.lowercase() } ?: "il calendario non è valido"
        message = "Non riesco a importare: $reason."
    }

    fun importText(text: String, restore: Boolean = false) = launch {
        withContext(Dispatchers.Default) { runCatching { ShareCodec.decode(text) } }
            .onSuccess { pendingImport = it; pendingRestore = restore && it.kind == "backup"; open(Screen.ImportPreview) }
            .onFailure(::importFailed)
    }

    fun importUri(context: Context, uri: Uri, restore: Boolean = false) {
        // Pickers and share sheets always give content:// URIs; file:// could point anywhere
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
            message = "Non riesco ad aprire il file."
            return
        }
        val resolver = context.applicationContext.contentResolver
        launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { resolver.openInputStream(uri)?.use { ShareCodec.readLimited(it).decodeToString() } }
            }
            text.onSuccess { if (it == null) message = "Non riesco ad aprire il file." else importText(it, restore) }
                .onFailure(::importFailed)
        }
    }

    /** QR from a gallery image: decoded off the main thread and downsampled. */
    fun importImage(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        launch {
            val text = withContext(Dispatchers.Default) { Qr.loadImage(appContext, uri)?.let { Qr.decode(it) } }
            if (text == null) message = "Nell'immagine non trovo un QR di ${Brand.NAME}." else importText(text)
        }
    }

    /** Neighbour share or backup: [replaceId] null creates new profiles. */
    fun confirmImport(replaceId: Long?, replaceAll: Boolean) {
        val env = pendingImport ?: return
        if (importing) return
        importing = true
        val restore = pendingRestore
        launch {
            try {
                val profiles = withContext(Dispatchers.Default) { env.profiles.map { ShareCodec.fromDto(it, trustReminders = restore) } }
                val lastId = repo.importProfiles(profiles, replaceAll = replaceAll && restore, replaceId = replaceId)
                lastId?.let { settingsStore.setSelectedProfile(it) }
                pendingImport = null
                pendingRestore = false
                clearStack()
                tab = Screen.Today
                message = if (restore) "Backup ripristinato." else "Calendario importato. Controlla i giorni e gli orari."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = "Importazione non riuscita: i tuoi dati non sono stati modificati."
            } finally {
                importing = false
            }
        }
    }

    fun cancelImport() {
        pendingImport = null
        pendingRestore = false
        back()
    }

    fun shareEnvelope(b: ProfileBundle) = Envelope(kind = "profile", exportedAt = System.currentTimeMillis(), profiles = listOf(ShareCodec.toDto(b, withReminders = false)))

    fun backupEnvelope() = Envelope(
        kind = "backup", exportedAt = System.currentTimeMillis(),
        profiles = state.value.bundles.map { ShareCodec.toDto(it, withReminders = true) },
    )

    fun writeBackup(context: Context, uri: Uri) = launch {
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(ShareCodec.encode(backupEnvelope()).encodeToByteArray()) }
        }.isSuccess
        if (ok) settingsStore.setLastBackup(System.currentTimeMillis())
        message = if (ok) "Backup salvato." else "Non sono riuscito a salvare il backup."
    }

    fun dismissAnnualCheck(year: Int) = launch { settingsStore.dismissAnnualCheck(year) }
}
