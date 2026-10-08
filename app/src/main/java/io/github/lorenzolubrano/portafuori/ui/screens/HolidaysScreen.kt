package io.github.lorenzolubrano.portafuori.ui.screens

import io.github.lorenzolubrano.portafuori.data.Limits
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import io.github.lorenzolubrano.portafuori.data.HolidayEntity
import io.github.lorenzolubrano.portafuori.data.decodeMonthDay
import io.github.lorenzolubrano.portafuori.data.encodeMonthDay
import io.github.lorenzolubrano.portafuori.rules.ExceptionKind
import io.github.lorenzolubrano.portafuori.rules.HolidayPolicy
import io.github.lorenzolubrano.portafuori.rules.It
import io.github.lorenzolubrano.portafuori.rules.ROME
import io.github.lorenzolubrano.portafuori.rules.Status
import io.github.lorenzolubrano.portafuori.ui.BinBadge
import io.github.lorenzolubrano.portafuori.ui.Card
import io.github.lorenzolubrano.portafuori.ui.ChoiceChips
import io.github.lorenzolubrano.portafuori.ui.DateField
import io.github.lorenzolubrano.portafuori.ui.MainViewModel
import io.github.lorenzolubrano.portafuori.ui.Page
import io.github.lorenzolubrano.portafuori.ui.SectionTitle
import io.github.lorenzolubrano.portafuori.ui.SwitchRow
import io.github.lorenzolubrano.portafuori.ui.UiState
import java.time.LocalDate
import java.time.MonthDay

@Composable
fun HolidaysScreen(vm: MainViewModel, state: UiState) {
    val b = state.selected ?: return
    val today = LocalDate.now(ROME)
    val end = today.plusDays(365)
    val holidays = remember(b) { b.holidayCalendar.between(today, end) }
    val byCollection = remember(b) { b.occurrences(today.minusDays(1), end.plusDays(1)).groupBy { it.collectionDate } }
    var adding by remember { mutableStateOf(false) }
    val pid = b.profile.id

    Page(title = "Festività ed eccezioni", onBack = { vm.back() }) {
        item { SectionTitle("Se un ritiro cade in un festivo") }
        item {
            val opts = listOf(HolidayPolicy.ASK to "Chiedimi", HolidayPolicy.KEEP to "Si ritira comunque", HolidayPolicy.SKIP to "Salta")
            ChoiceChips(opts.map { it.second to (it.first == b.profile.holidayPolicy) }) { vm.updateProfile(b.profile.copy(holidayPolicy = opts[it].first)) }
            Text(
                "Con «Chiedimi» ricevi una domanda 3 giorni prima. Nel dubbio controlla il sito del tuo comune.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { SectionTitle("Festivi dei prossimi 12 mesi") }
        items(holidays, key = { it.first.toString() }) { (date, name) ->
            val occ = byCollection[date].orEmpty()
            val pending = occ.filter { it.status == Status.HOLIDAY_PENDING }
            Card {
                Text("${It.weekdayDay(date).replaceFirstChar { it.uppercase() }} · $name", style = MaterialTheme.typography.titleSmall)
                if (occ.isEmpty()) {
                    Text("Nessun ritiro", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                occ.forEach { o ->
                    val bin = b.bin(o.binId) ?: return@forEach
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        BinBadge(bin.entity.colorArgb, bin.entity.iconKey, 24.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            bin.name + ": " + when (o.status) {
                                Status.HOLIDAY_PENDING -> "da decidere"
                                Status.ACTIVE -> "si fa"
                                Status.SKIPPED, Status.HOLIDAY_SKIPPED -> "salta"
                                Status.MOVED_OUT -> "spostato al ${o.movedTo?.let { It.dayMonth(it) }}"
                            },
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (pending.isNotEmpty()) {
                    Row {
                        TextButton(onClick = { vm.holidayDecision(pid, pending, keep = true) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Si fa") }
                        TextButton(onClick = { vm.holidayDecision(pid, pending, keep = false) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Salta") }
                    }
                }
            }
        }
        item { SectionTitle("Festività locali") }
        items(b.holidays, key = { it.id }) { h ->
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(h.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            decodeMonthDay(h.monthDay)?.let { "Ogni anno il ${It.monthDay(it)}" } ?: h.date?.let { "Solo il ${It.full(it)}" }.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = { vm.deleteHoliday(h.id) }) { Icon(Icons.Filled.Close, "Elimina festività") }
                }
            }
        }
        item {
            OutlinedButton(onClick = { adding = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Aggiungi festività (es. santo patrono)")
            }
        }
        val edits = b.exceptions.filter { maxOf(it.date, it.target ?: it.date) >= today }
        item { SectionTitle("Modifiche ai singoli giorni") }
        if (edits.isEmpty()) item { Text("Nessuna modifica.", style = MaterialTheme.typography.bodyMedium) }
        items(edits, key = { it.id }) { e ->
            val bin = b.bin(e.binId)
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    bin?.let { BinBadge(it.entity.colorArgb, it.entity.iconKey, 26.dp) }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        (bin?.name ?: "") + ": " + when (e.kind) {
                            ExceptionKind.SKIP -> "salta il ${It.weekdayDay(e.date)}"
                            ExceptionKind.MOVE -> "dal ${It.weekdayDay(e.date)} al ${e.target?.let { It.weekdayDay(it) }}"
                            ExceptionKind.ADD -> "in più il ${It.weekdayDay(e.date)}"
                            ExceptionKind.KEEP -> "si fa il ${It.weekdayDay(e.date)} (festivo)"
                        },
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { vm.restoreException(e) }) { Icon(Icons.Filled.Close, "Annulla modifica") }
                }
            }
        }
    }

    if (adding) {
        var name by remember { mutableStateOf("") }
        var date by remember { mutableStateOf<LocalDate?>(null) }
        var yearly by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Nuova festività") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it.take(Limits.HOLIDAY_NAME) }, label = { Text("Nome") }, singleLine = true)
                    DateField("Data", date) { date = it }
                    SwitchRow("Ogni anno", yearly, { yearly = it })
                    Spacer(Modifier.height(4.dp))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank() && date != null,
                    onClick = {
                        val d = date ?: return@TextButton
                        vm.addHoliday(
                            HolidayEntity(
                                profileId = pid, name = name.trim(),
                                monthDay = if (yearly) encodeMonthDay(MonthDay.from(d)) else null,
                                date = if (yearly) null else d,
                            ),
                        )
                        adding = false
                    },
                ) { Text("Aggiungi") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Annulla") } },
        )
    }
}
