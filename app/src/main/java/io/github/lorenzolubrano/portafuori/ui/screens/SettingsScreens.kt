package io.github.lorenzolubrano.portafuori.ui.screens

import io.github.lorenzolubrano.portafuori.data.Limits
import io.github.lorenzolubrano.portafuori.Brand
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.lorenzolubrano.portafuori.BuildConfig
import io.github.lorenzolubrano.portafuori.data.ProfileEntity
import io.github.lorenzolubrano.portafuori.data.StyleId
import io.github.lorenzolubrano.portafuori.data.ThemeMode
import io.github.lorenzolubrano.portafuori.rules.CalendarMode
import io.github.lorenzolubrano.portafuori.rules.ExposureMode
import io.github.lorenzolubrano.portafuori.rules.HolidayPolicy
import io.github.lorenzolubrano.portafuori.rules.It
import io.github.lorenzolubrano.portafuori.ui.Card
import io.github.lorenzolubrano.portafuori.ui.ChoiceChips
import io.github.lorenzolubrano.portafuori.ui.DateField
import io.github.lorenzolubrano.portafuori.ui.GroupDivider
import io.github.lorenzolubrano.portafuori.ui.GroupRow
import io.github.lorenzolubrano.portafuori.ui.MainViewModel
import io.github.lorenzolubrano.portafuori.ui.Page
import io.github.lorenzolubrano.portafuori.ui.ProfileSwitcher
import io.github.lorenzolubrano.portafuori.ui.RowGroup
import io.github.lorenzolubrano.portafuori.ui.Screen
import io.github.lorenzolubrano.portafuori.ui.SectionTitle
import io.github.lorenzolubrano.portafuori.ui.SegmentedChoice
import io.github.lorenzolubrano.portafuori.ui.ShellAction
import io.github.lorenzolubrano.portafuori.ui.SwitchRow
import io.github.lorenzolubrano.portafuori.ui.TimeField
import io.github.lorenzolubrano.portafuori.ui.UiState
import io.github.lorenzolubrano.portafuori.ui.theme.okColor
import io.github.lorenzolubrano.portafuori.ui.dial
import io.github.lorenzolubrano.portafuori.ui.openLink

@Composable
fun MoreScreen(vm: MainViewModel, state: UiState) {
    val rel = vm.reliability
    Page(title = "Altro", actions = { ProfileSwitcher(vm, state) }) {
        item {
            RowGroup {
                MenuRow(
                    Icons.Filled.NotificationsActive, "Affidabilità dei promemoria",
                    if (rel.allGood) "Tutto a posto" else "Da controllare", if (rel.allGood) okColor() else MaterialTheme.colorScheme.tertiary,
                ) { vm.open(Screen.Reliability) }
                GroupDivider()
                MenuRow(Icons.Filled.EventBusy, "Festività ed eccezioni", "Festivi, patrono, ritiri spostati") { vm.open(Screen.Holidays) }
                GroupDivider()
                MenuRow(Icons.Filled.QrCode2, "Condividi e backup", "QR per i vicini, file di backup") { vm.open(Screen.Share) }
                GroupDivider()
                MenuRow(Icons.Filled.Tune, "Impostazioni di «${state.selected?.profile?.name}»", "Orari, avvisi, pausa vacanza, note") { vm.open(Screen.ProfileSettings) }
                GroupDivider()
                MenuRow(Icons.Filled.AddHome, "Aggiungi una casa", "Seconda casa, casa dei genitori…") { vm.startWizard(first = false) }
            }
        }
        item { SectionTitle("Stile") }
        item {
            val styles = listOf(StyleId.LINEE to "Linee", StyleId.ORIGINALE to "Originale", StyleId.ANDROID to "Android")
            SegmentedChoice(styles.map { it.second }, selected = styles.indexOfFirst { it.first == state.settings.style }) { vm.setStyle(styles[it].first) }
        }
        item { SectionTitle("Tema") }
        item {
            val modes = listOf(ThemeMode.SYSTEM to "Sistema", ThemeMode.LIGHT to "Chiaro", ThemeMode.DARK to "Scuro")
            SegmentedChoice(modes.map { it.second }, selected = modes.indexOfFirst { it.first == state.settings.theme }) { vm.setTheme(modes[it].first) }
        }
        item { SectionTitle("Informazioni") }
        item { RowGroup { MenuRow(Icons.Filled.Info, "Info e privacy", "Versione ${BuildConfig.VERSION_NAME} · nessun dato lascia il telefono") { vm.open(Screen.Info) } } }
    }
}

@Composable
fun MenuRow(icon: ImageVector, title: String, subtitle: String, subtitleColor: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit) {
    GroupRow(onClick = onClick) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp).semantics { heading() })
}

private val PHONE = Regex("""(?:\+39\s?)?(?:\d[\s.]?){6,11}\d""")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsScreen(vm: MainViewModel, state: UiState) {
    val b = state.selected ?: return
    val context = LocalContext.current
    // the draft lives in the ViewModel so a rotation or a theme change keeps the edits
    val p = vm.profileDraft?.takeIf { it.id == b.profile.id } ?: b.profile
    fun set(x: ProfileEntity) { vm.profileDraft = x }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    val dirty = p != b.profile
    fun leave(save: Boolean) {
        if (save && dirty) vm.updateProfile(p)
        vm.profileDraft = null
        vm.back()
    }
    // the arrow and system back give the same result: they ask before dropping unsaved edits, like the bin editor;
    // «Salva» saves
    fun exit() { if (dirty) confirmExit = true else leave(save = false) }
    BackHandler(onBack = ::exit)

    Page(
        title = "Impostazioni",
        onBack = ::exit,
        actions = { ShellAction("Salva", enabled = dirty) { leave(save = true) } },
    ) {
        item {
            OutlinedTextField(p.name, { set(p.copy(name = it.take(Limits.PROFILE_NAME))) }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(p.areaNote, { set(p.copy(areaNote = it.take(Limits.AREA))) }, label = { Text("Zona o via") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item { SectionTitle("Calendario") }
        // two separate single choices: each gets the wizard's own title, so two ticked circles never look like one list
        item {
            Column(Modifier.selectableGroup()) {
                GroupLabel("Il tuo calendario indica…")
                RadioLine("Indica il giorno del ritiro", p.calendarMode == CalendarMode.COLLECTION_DAY) { set(p.copy(calendarMode = CalendarMode.COLLECTION_DAY)) }
                RadioLine("Indica la sera in cui esporre", p.calendarMode == CalendarMode.EXPOSE_DAY) { set(p.copy(calendarMode = CalendarMode.EXPOSE_DAY)) }
            }
        }
        item {
            Column(Modifier.selectableGroup()) {
                GroupLabel("Quando si espone il bidone?")
                RadioLine("Si espone la sera prima", p.exposureMode == ExposureMode.EVENING_BEFORE) { set(p.copy(exposureMode = ExposureMode.EVENING_BEFORE)) }
                RadioLine("Si espone la mattina stessa", p.exposureMode == ExposureMode.SAME_MORNING) { set(p.copy(exposureMode = ExposureMode.SAME_MORNING)) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField("Dalle", p.exposeStart) { set(p.copy(exposeStart = it)) }
                TimeField("Entro le", p.exposeEnd) { set(p.copy(exposeEnd = it)) }
            }
        }
        item { SectionTitle("Festivi") }
        item {
            val opts = listOf(HolidayPolicy.ASK to "Chiedimi", HolidayPolicy.KEEP to "Si ritira comunque", HolidayPolicy.SKIP to "Salta")
            ChoiceChips(opts.map { it.second to (it.first == p.holidayPolicy) }) { set(p.copy(holidayPolicy = opts[it].first)) }
        }
        item { SectionTitle("Avvisi") }
        item {
            ReminderLine("Esponi", "L'avviso principale", p.exposeReminderOn, p.exposeReminderAt, { set(p.copy(exposeReminderOn = it)) }, { set(p.copy(exposeReminderAt = it)) })
        }
        item {
            ReminderLine("Prepara", "Un avviso prima, per svuotare i cestini", p.prepareReminderOn, p.prepareReminderAt, { set(p.copy(prepareReminderOn = it)) }, { set(p.copy(prepareReminderAt = it)) })
        }
        item {
            ReminderLine("Ritira", "Il giorno del ritiro", p.retrieveReminderOn, p.retrieveReminderAt, { set(p.copy(retrieveReminderOn = it)) }, { set(p.copy(retrieveReminderAt = it)) })
        }
        item {
            Card {
                SwitchRow("Insistente", p.insistent, { set(p.copy(insistent = it)) }, subtitle = "Ripete l'avviso dopo 45 minuti se non premi «Fatto»")
            }
        }
        item { SectionTitle("Pausa vacanza") }
        item {
            Text("Niente avvisi in questi giorni: il calendario resta com'è.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            // wraps instead of squeezing: two full dates and «Togli» do not fit one line at large text
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Dal", p.pausedFrom) { set(p.copy(pausedFrom = it, pausedTo = p.pausedTo ?: it.plusDays(7))) }
                DateField("Al", p.pausedTo) { set(p.copy(pausedTo = it)) }
                if (p.pausedFrom != null) TextButton(onClick = { set(p.copy(pausedFrom = null, pausedTo = null)) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Togli") }
            }
        }
        item { SectionTitle("Note") }
        item {
            OutlinedTextField(
                p.notes, { set(p.copy(notes = it.take(Limits.NOTES))) }, minLines = 3, modifier = Modifier.fillMaxWidth(),
                label = { Text("Isola ecologica, numero ingombranti, gestore…") },
            )
        }
        val phones = PHONE.findAll(p.notes).map { it.value.trim() }.distinct().toList()
        if (phones.isNotEmpty()) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    phones.take(3).forEach { n ->
                        OutlinedButton(onClick = {
                            if (!dial(context, n.filter { it.isDigit() || it == '+' })) vm.message = "Nessuna app per telefonare: il numero è $n"
                        }) {
                            Icon(Icons.Filled.Call, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(n)
                        }
                    }
                }
            }
        }
        item { SectionTitle("Elimina") }
        item {
            OutlinedButton(onClick = { confirmDelete = true }) { Text("Elimina «${b.profile.name}»", color = MaterialTheme.colorScheme.error) }
        }
    }
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Uscire senza salvare?") },
            text = { Text("Le modifiche andranno perse.") },
            confirmButton = { TextButton(onClick = { confirmExit = false; leave(save = false) }) { Text("Esci") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Resta") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminare «${b.profile.name}»?") },
            text = { Text("Spariscono bidoni, regole e avvisi di questa casa. Non si può annullare, salvo da un backup.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.profileDraft = null; vm.deleteProfile(b.profile.id) }) { Text("Elimina") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } },
        )
    }
}

@Composable
fun InfoScreen(vm: MainViewModel) {
    val context = LocalContext.current
    Page(title = "Info e privacy", onBack = { vm.back() }) {
        item {
            Column {
                Text("${Brand.NAME} ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text("Stasera cosa esce? Te lo dice ${Brand.NAME}, la sera prima.", style = MaterialTheme.typography.bodyLarge)
            }
        }
        item { SectionTitle("Privacy") }
        item {
            Text(
                "${Brand.NAME} non ha il permesso di usare Internet: i tuoi dati restano sul telefono. " +
                    "Niente account, niente pubblicità, niente statistiche. Condividi il calendario solo quando lo decidi tu, con un QR o un file.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        item { SectionTitle("Sostieni ${Brand.NAME}") }
        item {
            Card {
                Text(
                    "${Brand.NAME} è gratis, senza pubblicità e senza abbonamenti. Se ti è utile, puoi offrirmi un caffè.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    if (!openLink(context, Brand.KOFI_URL)) vm.message = "Nessun browser per aprire ko-fi.com/portafuori"
                }) { Text("Offrimi un caffè") }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Si apre il browser: l'app non invia nessun dato.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { SectionTitle("Attenzione") }
        item {
            Text(
                "${Brand.NAME} ricorda quello che hai inserito tu. Verifica sempre il calendario ufficiale del tuo comune, " +
                    "soprattutto a inizio anno, nei festivi e in caso di scioperi.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        item { SectionTitle("Festività nazionali") }
        item {
            Text(
                "Calcolate sul telefono, Pasqua compresa. Dal 2026 c'è anche il 4 ottobre, San Francesco d'Assisi (Legge 151/2025). " +
                    "Il santo patrono si aggiunge da «Festività ed eccezioni».",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        item { SectionTitle("Licenze") }
        item {
            Card {
                Text("${Brand.NAME} è software libero, licenza GPL-3.0", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    if (!openLink(context, Brand.SOURCE_URL)) vm.message = "Nessun browser per aprire github.com/LorenzoLubrano/portafuori"
                }) { Text("Codice sorgente") }
                Spacer(Modifier.height(8.dp))
                Text(
                    "AndroidX e Jetpack Compose, Kotlin, kotlinx.serialization, ZXing e le icone Material Symbols: Apache License 2.0. " +
                        "Il carattere Atkinson Hyperlegible Next: SIL Open Font License 1.1.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            Text(
                "Ultimo aggiornamento del calendario festività: ${It.full(java.time.LocalDate.of(2026, 9, 23))}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
