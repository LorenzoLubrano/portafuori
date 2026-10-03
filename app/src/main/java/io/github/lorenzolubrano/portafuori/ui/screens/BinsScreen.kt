package io.github.lorenzolubrano.portafuori.ui.screens

import io.github.lorenzolubrano.portafuori.data.Limits
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
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
import io.github.lorenzolubrano.portafuori.ui.BinBadge
import io.github.lorenzolubrano.portafuori.ui.Card
import io.github.lorenzolubrano.portafuori.ui.ChoiceChips
import io.github.lorenzolubrano.portafuori.ui.DateField
import io.github.lorenzolubrano.portafuori.ui.DatePickDialog
import io.github.lorenzolubrano.portafuori.ui.MainViewModel
import io.github.lorenzolubrano.portafuori.ui.Page
import io.github.lorenzolubrano.portafuori.ui.Screen
import io.github.lorenzolubrano.portafuori.ui.SectionTitle
import io.github.lorenzolubrano.portafuori.ui.UiState
import io.github.lorenzolubrano.portafuori.ui.WeekdayChips
import io.github.lorenzolubrano.portafuori.ui.asColor
import io.github.lorenzolubrano.portafuori.ui.binIcon
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
        items(b.bins, key = { it.id }) { bin ->
            val next = remember(bin, b.exceptions) {
                b.occurrences(today, today.plusDays(60)).filter { it.binId == bin.id && it.isActive }.take(3).map { It.weekdayDay(it.calendarDate) }
            }
            Card(onClick = { vm.open(Screen.BinEditor(bin.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BinBadge(bin.entity.colorArgb, bin.entity.iconKey, 40.dp)
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
        item { SectionTitle("Regole del calendario") }
        item {
            val p = b.profile
            Card(onClick = { vm.open(Screen.ProfileSettings) }) {
                Text(
                    if (p.calendarMode == CalendarMode.COLLECTION_DAY) "Il calendario indica il giorno del ritiro" else "Il calendario indica la sera in cui esporre",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    (if (p.exposureMode == ExposureMode.EVENING_BEFORE) "Esposizione la sera prima" else "Esposizione la mattina stessa") +
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BinEditorScreen(vm: MainViewModel, state: UiState, binId: Long?) {
    val b = state.selected ?: return
    val existing = binId?.let { b.bin(it) }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var color by remember { mutableStateOf(existing?.entity?.colorArgb ?: Presets.colors[8]) }
    var icon by remember { mutableStateOf(existing?.entity?.iconKey ?: "bag") }
    val rules = remember { mutableStateListOf<Rule>().apply { addAll(existing?.rules.orEmpty()) } }
    var editing by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun save() {
        val entity = (existing?.entity ?: BinEntity(profileId = b.profile.id, name = "", colorArgb = color, iconKey = icon))
            .copy(name = name.trim().ifBlank { "Bidone" }, colorArgb = color, iconKey = icon)
        vm.saveBin(entity, rules.toList())
        vm.back()
    }

    Page(
        title = if (existing == null) "Nuovo bidone" else existing.name,
        onBack = { vm.back() },
        actions = {
            if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Elimina bidone") }
            TextButton(onClick = ::save) { Text("Salva") }
        },
    ) {
        item {
            OutlinedTextField(name, { name = it.take(Limits.BIN_NAME) }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item { SectionTitle("Colore") }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Presets.colors.forEach { c ->
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(c.asColor())
                            .border(if (c == color) 3.dp else 1.dp, if (c == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable { color = c },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (c == color) Icon(Icons.Filled.Check, contentDescription = "Colore scelto", tint = Color.White)
                    }
                }
            }
        }
        item { SectionTitle("Icona") }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Presets.icons.forEach { key ->
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .background(if (key == icon) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { icon = key }
                            .semantics { contentDescription = "Icona $key" },
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
                    IconButton(onClick = { rules.removeAt(i) }) { Icon(Icons.Filled.Close, "Elimina regola") }
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
            val preview = previewDates(rules.toList(), 8)
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
            onSave = { r -> if (idx >= 0) rules[idx] = r else rules.add(r); editing = null },
        )
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminare «${existing.name}»?") },
            text = { Text("Spariscono anche le sue regole e le modifiche ai singoli giorni.") },
            confirmButton = { TextButton(onClick = { vm.deleteBin(existing.id); confirmDelete = false; vm.back() }) { Text("Elimina") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } },
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
    var type by remember { mutableStateOf(initial?.type ?: RuleType.WEEKLY) }
    var days by remember { mutableStateOf(initial?.weekdays ?: emptySet()) }
    var interval by remember { mutableStateOf(initial?.intervalWeeks?.coerceAtLeast(2) ?: 2) }
    var anchor by remember { mutableStateOf(initial?.anchor) }
    var ordinals by remember { mutableStateOf(initial?.ordinals ?: setOf(1)) }
    var dates by remember { mutableStateOf(initial?.dates ?: emptySet()) }
    var seasonOn by remember { mutableStateOf(initial?.seasonStart != null) }
    var seasonStart by remember { mutableStateOf(initial?.seasonStart ?: MonthDay.of(5, 1)) }
    var seasonEnd by remember { mutableStateOf(initial?.seasonEnd ?: MonthDay.of(10, 31)) }
    var validOn by remember { mutableStateOf(initial?.validFrom != null || initial?.validUntil != null) }
    var validFrom by remember { mutableStateOf(initial?.validFrom) }
    var validUntil by remember { mutableStateOf(initial?.validUntil) }
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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Page(
                title = if (initial == null) "Nuova regola" else "Modifica regola",
                onBack = onDismiss,
                actions = { TextButton(onClick = { build()?.let(onSave) }, enabled = build() != null) { Text("OK") } },
            ) {
                item {
                    val types = listOf(RuleType.WEEKLY to "Ogni settimana", RuleType.EVERY_N_WEEKS to "Ogni N settimane", RuleType.MONTHLY_NTH to "N-esimo del mese", RuleType.FIXED_DATES to "Date fisse")
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
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Solo in un periodo dell'anno", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(seasonOn, { seasonOn = it })
                    }
                }
                if (seasonOn) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { pickSeason = 0 }) { Text("Dal ${It.monthDay(seasonStart)}") }
                            OutlinedButton(onClick = { pickSeason = 1 }) { Text("Al ${It.monthDay(seasonEnd)}") }
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Valida solo tra due date", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(validOn, { validOn = it })
                    }
                }
                if (validOn) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
