package io.github.lorenzolubrano.portafuori.ui.screens

import androidx.compose.runtime.key
import io.github.lorenzolubrano.portafuori.data.Limits
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.lorenzolubrano.portafuori.data.BinEntity
import io.github.lorenzolubrano.portafuori.data.Presets
import io.github.lorenzolubrano.portafuori.rules.CalendarMode
import io.github.lorenzolubrano.portafuori.rules.ExposureMode
import io.github.lorenzolubrano.portafuori.rules.HolidayPolicy
import io.github.lorenzolubrano.portafuori.rules.It
import io.github.lorenzolubrano.portafuori.rules.ROME
import io.github.lorenzolubrano.portafuori.rules.Rule
import io.github.lorenzolubrano.portafuori.rules.RuleType
import io.github.lorenzolubrano.portafuori.rules.Schedule
import io.github.lorenzolubrano.portafuori.ui.Card
import io.github.lorenzolubrano.portafuori.ui.ChoiceChips
import io.github.lorenzolubrano.portafuori.ui.DateField
import io.github.lorenzolubrano.portafuori.ui.DatePickDialog
import io.github.lorenzolubrano.portafuori.ui.GroupDivider
import io.github.lorenzolubrano.portafuori.ui.GroupRow
import io.github.lorenzolubrano.portafuori.ui.MainViewModel
import io.github.lorenzolubrano.portafuori.ui.Page
import io.github.lorenzolubrano.portafuori.ui.ProfileSwitcher
import io.github.lorenzolubrano.portafuori.ui.Roundel
import io.github.lorenzolubrano.portafuori.ui.RowGroup
import io.github.lorenzolubrano.portafuori.ui.Screen
import io.github.lorenzolubrano.portafuori.ui.SectionTitle
import io.github.lorenzolubrano.portafuori.ui.ShellAction
import io.github.lorenzolubrano.portafuori.ui.SwitchRow
import io.github.lorenzolubrano.portafuori.ui.UiState
import io.github.lorenzolubrano.portafuori.ui.WeekdayChips
import io.github.lorenzolubrano.portafuori.ui.Wording
import io.github.lorenzolubrano.portafuori.ui.asColor
import io.github.lorenzolubrano.portafuori.ui.binIcon
import io.github.lorenzolubrano.portafuori.ui.theme.Contrast
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.TemporalAdjusters

@Composable
fun BinsScreen(vm: MainViewModel, state: UiState) {
    val b = state.selected ?: return
    val today = LocalDate.now(ROME)
    Page(
        title = "Bidoni",
        actions = { ProfileSwitcher(vm, state) },
        floating = {
            ExtendedFloatingActionButton(
                onClick = { vm.open(Screen.BinEditor(null)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Bidone") },
            )
        },
    ) {
        // the bins form one group of rows (spec: «elenco in un gruppo»)
        if (b.bins.isNotEmpty()) item {
            RowGroup {
                b.bins.forEachIndexed { i, bin ->
                    key(bin.id) {
                        if (i > 0) GroupDivider()
                        val next = remember(bin, b.exceptions) {
                            b.occurrences(today, today.plusDays(60)).filter { it.binId == bin.id && it.isActive }.take(3).map { It.weekdayDay(it.calendarDate) }
                        }
                        GroupRow(onClick = { vm.open(Screen.BinEditor(bin.id)) }) {
                            Roundel(bin)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(bin.name, style = MaterialTheme.typography.titleMedium)
                                Text(It.describeAll(bin.rules), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (next.isNotEmpty()) {
                                    Text("Prossimi: " + next.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Icon(Icons.Filled.Edit, contentDescription = "Modifica", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
        item { SectionTitle("Regole del calendario") }
        item {
            val p = b.profile
            Card(onClick = { vm.open(Screen.ProfileSettings) }) {
                Text(
                    if (p.calendarMode == CalendarMode.COLLECTION_DAY) "Il calendario indica il giorno del ritiro" else "Il calendario indica la sera in cui portarli fuori",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    (if (p.exposureMode == ExposureMode.EVENING_BEFORE) "Fuori la sera prima" else "Fuori la mattina stessa") +
                        ", dalle ${It.time(p.exposeStart)} entro le ${It.time(p.exposeEnd)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Festivi: " + when (p.holidayPolicy) {
                        HolidayPolicy.ASK -> "chiedimi ogni volta"
                        HolidayPolicy.KEEP -> "il ritiro si fa comunque"
                        HolidayPolicy.SKIP -> "il ritiro salta"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text("Modifica", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        // room for the floating button, so it never covers the last row
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BinEditorScreen(vm: MainViewModel, state: UiState, binId: Long?) {
    val b = state.selected ?: return
    val existing = binId?.let { b.bin(it) }
    val original = existing?.let { MainViewModel.BinDraft(binId, it.name, it.entity.colorArgb, it.entity.iconKey, it.rules) }
        ?: MainViewModel.BinDraft(null, "", Presets.colors[8], "bag", emptyList())
    // the draft in the ViewModel wins while it belongs to this bin; every edit writes it back
    val draft = vm.binDraft?.takeIf { it.binId == binId } ?: original
    fun edit(d: MainViewModel.BinDraft) { vm.binDraft = d }
    val rules = draft.rules
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    val dirty = draft != original

    fun leave() { if (dirty) confirmExit = true else { vm.binDraft = null; vm.back() } }
    BackHandler(onBack = ::leave)

    fun save() {
        val entity = (existing?.entity ?: BinEntity(profileId = b.profile.id, name = "", colorArgb = draft.color, iconKey = draft.icon))
            .copy(name = draft.name.trim().ifBlank { "Bidone" }, colorArgb = draft.color, iconKey = draft.icon)
        vm.saveBin(entity, rules)
        vm.message = "Bidone salvato."
        vm.binDraft = null
        vm.back()
    }

    Page(
        title = if (existing == null) "Nuovo bidone" else existing.name,
        onBack = ::leave,
        actions = {
            if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Elimina bidone") }
            ShellAction("Salva", onClick = ::save)
        },
    ) {
        item {
            OutlinedTextField(draft.name, { edit(draft.copy(name = it.take(Limits.BIN_NAME))) }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item { SectionTitle("Colore") }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val bg = MaterialTheme.colorScheme.background.toArgb().toLong() and 0xFFFFFFFFL
                Presets.colors.forEach { c ->
                    val chosen = c == draft.color
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .then(
                                when {
                                    chosen -> Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape).padding(5.dp).clip(CircleShape)
                                    Contrast.needsEdge(c, bg) -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                                    else -> Modifier
                                },
                            )
                            .background(c.asColor())
                            .clickable { edit(draft.copy(color = c)) }
                            .semantics { contentDescription = Wording.colorName(c); selected = chosen },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (chosen) Icon(Icons.Filled.Check, contentDescription = null, tint = Contrast.onColor(c).asColor())
                    }
                }
            }
        }
        item { SectionTitle("Icona") }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Presets.icons.forEach { key ->
                    val chosen = key == draft.icon
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .background(if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .then(if (chosen) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                            .clickable { edit(draft.copy(icon = key)) }
                            .semantics { contentDescription = "Icona ${Wording.iconName(key)}"; selected = chosen },
                        contentAlignment = Alignment.Center,
                    ) { Icon(binIcon(key), contentDescription = null) }
                }
            }
        }
        item { SectionTitle("Giorni di raccolta") }
        if (rules.isEmpty()) {
            item { Text("Nessuna regola: questo bidone non comparirà nel calendario.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(rules.size) { i ->
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(It.describe(rules[i]), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = { editing = i }) { Icon(Icons.Filled.Edit, "Modifica regola") }
                    IconButton(onClick = { edit(draft.copy(rules = rules.filterIndexed { j, _ -> j != i })) }) { Icon(Icons.Filled.Close, "Elimina regola") }
                }
            }
        }
        item {
            OutlinedButton(onClick = { editing = -1 }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Aggiungi regola")
            }
        }
        item { SectionTitle("Anteprima") }
        item {
            val preview = previewDates(rules, 8)
            Text(
                if (preview.isEmpty()) "Nessuna data nei prossimi 12 mesi." else preview.joinToString(" · ") { It.weekdayDay(it) },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item { Spacer(Modifier.height(8.dp)); Button(onClick = ::save, modifier = Modifier.fillMaxWidth()) { Text("Salva") } }
    }

    editing?.let { idx ->
        RuleEditorDialog(
            initial = rules.getOrNull(idx),
            onDismiss = { editing = null },
            onSave = { r ->
                edit(draft.copy(rules = if (idx >= 0) rules.mapIndexed { j, old -> if (j == idx) r else old } else rules + r))
                editing = null
            },
        )
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminare «${existing.name}»?") },
            text = { Text("Spariscono anche le sue regole e le modifiche ai singoli giorni.") },
            confirmButton = { TextButton(onClick = { vm.deleteBin(existing.id); confirmDelete = false; vm.binDraft = null; vm.back() }) { Text("Elimina") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } },
        )
    }
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Uscire senza salvare?") },
            text = { Text("Le modifiche andranno perse.") },
            confirmButton = { TextButton(onClick = { confirmExit = false; vm.binDraft = null; vm.back() }) { Text("Esci") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Resta") } },
        )
    }
}

fun previewDates(rules: List<Rule>, count: Int): List<LocalDate> {
    val out = mutableListOf<LocalDate>()
    var d = LocalDate.now(ROME)
    val end = d.plusDays(366)
    while (d <= end && out.size < count) {
        if (rules.any { Schedule.matches(it, d) }) out += d
        d = d.plusDays(1)
    }
    return out
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorDialog(initial: Rule?, onDismiss: () -> Unit, onSave: (Rule) -> Unit) {
    val today = LocalDate.now(ROME)
    var type by rememberSaveable { mutableStateOf(initial?.type ?: RuleType.WEEKLY) }
    var days by rememberSaveable(stateSaver = listSaver({ it.map(DayOfWeek::getValue) }, { it.map(DayOfWeek::of).toSet() })) {
        mutableStateOf(initial?.weekdays ?: emptySet())
    }
    var interval by rememberSaveable { mutableIntStateOf(initial?.intervalWeeks?.coerceAtLeast(2) ?: 2) }
    var anchor by rememberSaveable { mutableStateOf(initial?.anchor) }
    var ordinals by rememberSaveable(stateSaver = listSaver({ it.toList() }, { it.toSet() })) {
        mutableStateOf(initial?.ordinals ?: setOf(1))
    }
    var dates by rememberSaveable(stateSaver = listSaver({ it.map(LocalDate::toEpochDay) }, { it.map(LocalDate::ofEpochDay).toSet() })) {
        mutableStateOf(initial?.dates ?: emptySet())
    }
    var seasonOn by rememberSaveable { mutableStateOf(initial?.seasonStart != null) }
    var seasonStart by rememberSaveable { mutableStateOf(initial?.seasonStart ?: MonthDay.of(5, 1)) }
    var seasonEnd by rememberSaveable { mutableStateOf(initial?.seasonEnd ?: MonthDay.of(10, 31)) }
    var validOn by rememberSaveable { mutableStateOf(initial?.validFrom != null || initial?.validUntil != null) }
    var validFrom by rememberSaveable { mutableStateOf(initial?.validFrom) }
    var validUntil by rememberSaveable { mutableStateOf(initial?.validUntil) }
    var pickDate by remember { mutableStateOf(false) }
    var pickSeason by remember { mutableStateOf<Int?>(null) }

    fun build(): Rule? {
        val needsDays = type != RuleType.FIXED_DATES
        if (needsDays && days.isEmpty()) return null
        if (type == RuleType.FIXED_DATES && dates.isEmpty()) return null
        if (type == RuleType.MONTHLY_NTH && ordinals.isEmpty()) return null
        val a = if (type == RuleType.EVERY_N_WEEKS) {
            anchor ?: today.with(TemporalAdjusters.nextOrSame(days.minOrNull() ?: today.dayOfWeek))
        } else {
            null
        }
        return Rule(
            type = type,
            weekdays = if (needsDays) days else emptySet(),
            intervalWeeks = if (type == RuleType.EVERY_N_WEEKS) interval else 1,
            anchor = a,
            ordinals = if (type == RuleType.MONTHLY_NTH) ordinals else emptySet(),
            dates = if (type == RuleType.FIXED_DATES) dates else emptySet(),
            seasonStart = if (seasonOn) seasonStart else null,
            seasonEnd = if (seasonOn) seasonEnd else null,
            validFrom = if (validOn) validFrom else null,
            validUntil = if (validOn) validUntil else null,
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Page(
                title = if (initial == null) "Nuova regola" else "Modifica regola",
                onBack = onDismiss,
                actions = { ShellAction("OK", enabled = build() != null) { build()?.let(onSave) } },
            ) {
                item {
                    val types = listOf(RuleType.WEEKLY to "Ogni settimana", RuleType.EVERY_N_WEEKS to "Ogni N settimane", RuleType.MONTHLY_NTH to "1°, 2°… del mese", RuleType.FIXED_DATES to "Date fisse")
                    ChoiceChips(types.map { it.second to (it.first == type) }) { type = types[it].first }
                }
                if (type != RuleType.FIXED_DATES) {
                    item { SectionTitle("Giorno") }
                    item { WeekdayChips(days) { days = it } }
                }
                when (type) {
                    RuleType.EVERY_N_WEEKS -> {
                        item { SectionTitle("Ogni quante settimane") }
                        item { ChoiceChips((2..6).map { "$it" to (it == interval) }) { interval = it + 2 } }
                        item { SectionTitle("Una data in cui passano") }
                        item {
                            DateField("Scegli", anchor) { anchor = it }
                            Text(
                                "Serve per sapere quali settimane sono quelle giuste.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    RuleType.MONTHLY_NTH -> {
                        item { SectionTitle("Quale del mese") }
                        item {
                            val opts = listOf(1, 2, 3, 4, 5, Rule.LAST)
                            ChoiceChips(opts.map { (if (it == Rule.LAST) "Ultimo" else "$it°") to (it in ordinals) }) { i ->
                                val o = opts[i]
                                ordinals = if (o in ordinals) ordinals - o else ordinals + o
                            }
                        }
                    }
                    RuleType.FIXED_DATES -> {
                        item { SectionTitle("Date") }
                        item {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                dates.sorted().forEach { d ->
                                    InputChip(
                                        selected = false, onClick = { dates = dates - d }, label = { Text(It.full(d)) },
                                        trailingIcon = { Icon(Icons.Filled.Close, "Togli", Modifier.size(16.dp)) },
                                    )
                                }
                            }
                        }
                        item { OutlinedButton(onClick = { pickDate = true }) { Text("Aggiungi data") } }
                    }
                    else -> Unit
                }
                item { SwitchRow("Solo in un periodo dell'anno", seasonOn, { seasonOn = it }) }
                if (seasonOn) {
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { pickSeason = 0 }) { Text("Dal ${It.monthDay(seasonStart)}") }
                            OutlinedButton(onClick = { pickSeason = 1 }) { Text("Al ${It.monthDay(seasonEnd)}") }
                        }
                    }
                }
                item { SwitchRow("Valida solo tra due date", validOn, { validOn = it }) }
                if (validOn) {
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DateField("Dal", validFrom) { validFrom = it }
                            DateField("Al", validUntil) { validUntil = it }
                        }
                    }
                }
                item { SectionTitle("Prossime date") }
                item {
                    val r = build()
                    Text(
                        if (r == null) "Completa la regola per vedere le date." else previewDates(listOf(r), 8).joinToString(" · ") { It.weekdayDay(it) }.ifEmpty { "Nessuna data nei prossimi 12 mesi." },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    r?.let { Text(It.describe(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
        }
    }
    if (pickDate) DatePickDialog(today, { pickDate = false }) { dates = dates + it }
    pickSeason?.let { which ->
        val md = if (which == 0) seasonStart else seasonEnd
        DatePickDialog(md.atYear(today.year), { pickSeason = null }) {
            val v = MonthDay.from(it)
            if (which == 0) seasonStart = v else seasonEnd = v
        }
    }
}
