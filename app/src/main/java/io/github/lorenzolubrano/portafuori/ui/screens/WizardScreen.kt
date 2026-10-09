package io.github.lorenzolubrano.portafuori.ui.screens

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import io.github.lorenzolubrano.portafuori.data.Limits
import io.github.lorenzolubrano.portafuori.Brand
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.lorenzolubrano.portafuori.data.Bin
import io.github.lorenzolubrano.portafuori.data.BinEntity
import io.github.lorenzolubrano.portafuori.data.Presets
import io.github.lorenzolubrano.portafuori.reminders.Planner
import io.github.lorenzolubrano.portafuori.rules.CalendarMode
import io.github.lorenzolubrano.portafuori.rules.ExposureMode
import io.github.lorenzolubrano.portafuori.rules.It
import io.github.lorenzolubrano.portafuori.rules.ROME
import io.github.lorenzolubrano.portafuori.ui.BinBadge
import io.github.lorenzolubrano.portafuori.ui.BinPills
import io.github.lorenzolubrano.portafuori.ui.Card
import io.github.lorenzolubrano.portafuori.ui.DraftBin
import io.github.lorenzolubrano.portafuori.ui.LinesIn
import io.github.lorenzolubrano.portafuori.ui.MainViewModel
import io.github.lorenzolubrano.portafuori.ui.Page
import io.github.lorenzolubrano.portafuori.ui.Roundel
import io.github.lorenzolubrano.portafuori.ui.Screen
import io.github.lorenzolubrano.portafuori.ui.SectionTitle
import io.github.lorenzolubrano.portafuori.ui.Station
import io.github.lorenzolubrano.portafuori.ui.SwitchRow
import io.github.lorenzolubrano.portafuori.ui.TimeField
import io.github.lorenzolubrano.portafuori.ui.WeekdayChips
import io.github.lorenzolubrano.portafuori.ui.theme.extra
import java.time.LocalDate

@Composable
fun WelcomeScreen(vm: MainViewModel) {
    // a static example of tonight's station: three common bins arriving as lines
    val sample = remember {
        listOf("organico", "plastica", "vetro").mapNotNull { k -> Presets.bins.firstOrNull { it.key == k } }
            .map { Bin(BinEntity(profileId = 0, presetKey = it.key, name = it.name, colorArgb = it.color, iconKey = it.icon), emptyList()) }
    }
    Column(Modifier.fillMaxSize()) {
    // the status bar keeps Linee's shell behind it, so its light icons stay readable in the light theme
    Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(extra().shell))
    Column(
        Modifier.weight(1f).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(16.dp))
        Text(Brand.NAME, style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(20.dp))
        // an illustration, not content: TalkBack skips it, and large text drops it so the two buttons stay in view
        if (LocalDensity.current.fontScale <= 1.3f) Column(Modifier.clearAndSetSemantics {}) {
            LinesIn(sample)
            Station {
                sample.forEach { bin ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Roundel(bin, 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(bin.name, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("Stasera cosa esce?", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "${Brand.NAME} ti avvisa la sera prima quali bidoni portare fuori. Inserisci una volta il calendario del tuo comune, oppure importalo da un vicino.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = { vm.startWizard(first = true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("Configura il mio calendario", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { vm.open(Screen.Share) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("Importa da un vicino o da un backup", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Senza Internet · senza account · senza pubblicità",
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    // the shell runs behind the system navigation bar too, like the status bar above
    Box(Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars).background(extra().shell))
    }
}

private val STEP_TITLES = listOf("La tua casa", "I tuoi bidoni", "Come funziona il calendario", "I giorni", "Gli avvisi", "Controlla e salva")

@Composable
fun WizardScreen(vm: MainViewModel) {
    val w = vm.wizard
    fun set(block: (io.github.lorenzolubrano.portafuori.ui.WizardDraft) -> io.github.lorenzolubrano.portafuori.ui.WizardDraft) { vm.wizard = block(vm.wizard) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.saveWizard { vm.refresh() } }
    val last = STEP_TITLES.lastIndex

    Page(
        title = STEP_TITLES[w.step],
        onBack = { if (w.step > 0) set { it.copy(step = it.step - 1) } else vm.back() },
        bottom = {
            val stepText: @Composable (Modifier) -> Unit = { m ->
                Text("Passo ${w.step + 1} di ${last + 1}", style = MaterialTheme.typography.labelLarge, modifier = m)
            }
            val buttons: @Composable () -> Unit = {
                if (w.step > 0) TextButton(onClick = { set { it.copy(step = it.step - 1) } }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Indietro") }
                Spacer(Modifier.width(8.dp))
                Button(
                    modifier = Modifier.heightIn(min = 48.dp),
                    enabled = w.step != 1 || w.chosen().isNotEmpty(),
                    onClick = {
                        if (w.step < last) {
                            set { it.copy(step = it.step + 1) }
                        } else if (Build.VERSION.SDK_INT >= 33) {
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            vm.saveWizard { vm.refresh() }
                        }
                    },
                ) { Text(if (w.step < last) "Avanti" else "Salva e attiva") }
            }
            Column {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                val bar = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                // with large text the buttons take the whole row: the step count goes on its own line above them
                if (LocalDensity.current.fontScale > 1.3f) {
                    Column(bar) {
                        stepText(Modifier)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { buttons() }
                    }
                } else {
                    Row(bar, verticalAlignment = Alignment.CenterVertically) {
                        stepText(Modifier.weight(1f))
                        buttons()
                    }
                }
            }
            Box(Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars).background(extra().shell))
            }
        },
    ) {
        item { LinearProgressIndicator(progress = { (w.step + 1f) / (last + 1) }, modifier = Modifier.fillMaxWidth()) }
        when (w.step) {
            0 -> {
                item {
                    Text("Dai un nome a questo calendario. Se hai più case, potrai aggiungerle dopo.", style = MaterialTheme.typography.bodyLarge)
                }
                item {
                    OutlinedTextField(w.name, { v -> set { it.copy(name = v.take(Limits.PROFILE_NAME)) } }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(
                        w.area, { v -> set { it.copy(area = v.take(Limits.AREA)) } }, label = { Text("Zona o via (facoltativo)") },
                        supportingText = { Text("Utile se il tuo comune ha calendari diversi per zona.") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            1 -> {
                item { Text("Quali bidoni usi? I colori cambiano da comune a comune: potrai modificarli dopo.", style = MaterialTheme.typography.bodyLarge) }
                itemsIndexed(w.bins) { i, b ->
                    DraftBinRow(b) { set { d -> d.copy(bins = d.bins.toMutableList().also { it[i] = b.copy(selected = !b.selected) }) } }
                }
                item { AddCustomBin { name -> set { d -> d.copy(bins = d.bins + DraftBin(null, name, Presets.colors[8], "bag", selected = true)) } } }
            }
            2 -> {
                item { SectionTitle("Il tuo calendario indica…") }
                item {
                    RadioLine("Il giorno del ritiro", w.calendarMode == CalendarMode.COLLECTION_DAY) { set { it.copy(calendarMode = CalendarMode.COLLECTION_DAY) } }
                    RadioLine("La sera in cui portarli fuori", w.calendarMode == CalendarMode.EXPOSE_DAY) { set { it.copy(calendarMode = CalendarMode.EXPOSE_DAY) } }
                    Text(
                        "Guarda il volantino: se dice «lunedì: carta» e passano lunedì mattina, è il giorno del ritiro.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item { SectionTitle("Quando si porta fuori il bidone?") }
                item {
                    RadioLine("La sera prima del ritiro", w.exposureMode == ExposureMode.EVENING_BEFORE) {
                        set { it.copy(exposureMode = ExposureMode.EVENING_BEFORE, exposeStart = java.time.LocalTime.of(20, 0), exposeEnd = java.time.LocalTime.of(6, 0)) }
                    }
                    RadioLine("La mattina stessa del ritiro", w.exposureMode == ExposureMode.SAME_MORNING) {
                        set { it.copy(exposureMode = ExposureMode.SAME_MORNING, exposeStart = java.time.LocalTime.of(5, 0), exposeEnd = java.time.LocalTime.of(7, 0)) }
                    }
                }
                item { SectionTitle("Orari per portarli fuori") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeField("Dalle", w.exposeStart) { t -> set { it.copy(exposeStart = t) } }
                        TimeField("Entro le", w.exposeEnd) { t -> set { it.copy(exposeEnd = t) } }
                    }
                }
            }
            3 -> {
                item {
                    Text(
                        if (w.calendarMode == CalendarMode.COLLECTION_DAY) "Scegli i giorni del ritiro di ogni bidone." else "Scegli le sere in cui portare fuori ogni bidone.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                itemsIndexed(w.chosen()) { _, b ->
                    Card {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BinBadge(b.color, b.icon, 32.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(b.name, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(10.dp))
                        WeekdayChips(b.days) { days ->
                            set { d -> d.copy(bins = d.bins.map { if (it === b) it.copy(days = days) else it }) }
                        }
                    }
                }
                item {
                    Text(
                        "Ogni 2 settimane, «1° e 3° martedì», periodi dell'anno o date fisse: li imposti dopo, da «Bidoni».",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            4 -> {
                item { Text("Scegli quando ricevere gli avvisi. Arriva un solo avviso per sera, con tutti i bidoni.", style = MaterialTheme.typography.bodyLarge) }
                item {
                    ReminderLine(
                        "Porta fuori", if (w.exposureMode == ExposureMode.EVENING_BEFORE) "La sera in cui escono" else "La sera prima o la mattina",
                        w.exposeOn, w.exposeAt, { v -> set { it.copy(exposeOn = v) } }, { t -> set { it.copy(exposeAt = t) } },
                    )
                }
                item {
                    ReminderLine(
                        "Prepara", "Un avviso prima, per svuotare i cestini", w.prepareOn, w.prepareAt,
                        { v -> set { it.copy(prepareOn = v) } }, { t -> set { it.copy(prepareAt = t) } },
                    )
                }
                item {
                    ReminderLine(
                        "Ritira", "Il giorno del ritiro, per riportarli dentro", w.retrieveOn, w.retrieveAt,
                        { v -> set { it.copy(retrieveOn = v) } }, { t -> set { it.copy(retrieveAt = t) } },
                    )
                }
            }
            5 -> {
                val bundle = w.previewBundle()
                val today = LocalDate.now(ROME)
                val now = System.currentTimeMillis()
                val evenings = bundle.upcomingEvenings(today, today.plusDays(27), now)
                item { Text("Le prossime 4 settimane. Se qualcosa non torna, torna indietro e correggi.", style = MaterialTheme.typography.bodyLarge) }
                if (evenings.isEmpty()) {
                    item { Card { Text("Nessuna raccolta: non hai scelto nessun giorno. Puoi salvare e aggiungerli dopo.") } }
                }
                itemsIndexed(evenings) { _, e ->
                    Card {
                        Text(
                            It.weekdayDay(e.window.start.toLocalDate()).replaceFirstChar { it.uppercase() } +
                                (if (e.window.start.hour >= 12) " sera" else " mattina") + " · ritiro ${It.weekdayDay(e.collectionDate)}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        BinPills(e.bins)
                        if (e.pendingBins.isNotEmpty()) {
                            Text("Festivo: ${e.holiday}. Ti chiederò se il ritiro si fa.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
                item {
                    Text(
                        "Esempio di avviso: «${Planner.label(evenings.firstOrNull() ?: return@item, now)}: ${evenings.first().binNames}»",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftBinRow(b: DraftBin, onToggle: () -> Unit) {
    Card(onClick = onToggle) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BinBadge(b.color, b.icon, 36.dp)
            Spacer(Modifier.width(12.dp))
            Text(b.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                if (b.selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (b.selected) "Selezionato" else "Non selezionato",
                tint = if (b.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun AddCustomBin(onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(name, { name = it.take(Limits.BIN_NAME) }, label = { Text("Altro bidone") }, singleLine = true, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = { if (name.isNotBlank()) { onAdd(name.trim()); name = "" } }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("Aggiungi")
        }
    }
}

@Composable
fun RadioLine(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ReminderLine(
    title: String,
    subtitle: String,
    on: Boolean,
    at: java.time.LocalTime,
    onToggle: (Boolean) -> Unit,
    onTime: (java.time.LocalTime) -> Unit,
) {
    Card {
        SwitchRow(title, on, onToggle, subtitle = subtitle)
        if (on) {
            Spacer(Modifier.height(8.dp))
            TimeField("Alle", at, onChange = onTime)
        }
    }
}
