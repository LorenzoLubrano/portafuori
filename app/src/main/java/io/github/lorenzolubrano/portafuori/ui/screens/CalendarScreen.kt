package io.github.lorenzolubrano.portafuori.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lorenzolubrano.portafuori.data.ProfileBundle
import io.github.lorenzolubrano.portafuori.reminders.Planner
import io.github.lorenzolubrano.portafuori.rules.It
import io.github.lorenzolubrano.portafuori.rules.Occurrence
import io.github.lorenzolubrano.portafuori.rules.Origin
import io.github.lorenzolubrano.portafuori.rules.ROME
import io.github.lorenzolubrano.portafuori.rules.Status
import io.github.lorenzolubrano.portafuori.ui.BinBadge
import io.github.lorenzolubrano.portafuori.ui.BinPills
import io.github.lorenzolubrano.portafuori.ui.Card
import io.github.lorenzolubrano.portafuori.ui.DatePickDialog
import io.github.lorenzolubrano.portafuori.ui.MainViewModel
import io.github.lorenzolubrano.portafuori.ui.Page
import io.github.lorenzolubrano.portafuori.ui.ProfileSwitcher
import io.github.lorenzolubrano.portafuori.ui.UiState
import io.github.lorenzolubrano.portafuori.ui.asColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(vm: MainViewModel, state: UiState) {
    val b = state.selected ?: return
    val today = LocalDate.now(ROME)
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    val first = month.atDay(1)
    val gridStart = first.minusDays((first.dayOfWeek.value - 1).toLong())
    val gridEnd = gridStart.plusDays(41)
    val occ = remember(b, month) { b.occurrences(gridStart, gridEnd).groupBy { it.calendarDate } }
    val holidays = remember(b, month) { b.holidayCalendar.between(gridStart, gridEnd.plusDays(1)).toMap() }

    Page(title = "Calendario", actions = { ProfileSwitcher(vm, state) }) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mese precedente") }
                Text(It.month(first), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mese successivo") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth()) {
                DayOfWeek.entries.forEach {
                    Text(
                        It.shortDay(it), modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (week in 0 until 6) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (dow in 0 until 7) {
                            val d = gridStart.plusDays((week * 7 + dow).toLong())
                            DayCell(
                                date = d,
                                inMonth = d.month == month.month,
                                today = d == today,
                                holiday = holidays[d],
                                occurrences = occ[d].orEmpty(),
                                bundle = b,
                                modifier = Modifier.weight(1f),
                                onClick = { selected = d },
                            )
                        }
                    }
                }
            }
        }
        item {
            Text(
                "I giorni sono quelli del calendario del comune. Rosso = festivo, barrato = saltato.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { BinPills(b.bins) }
    }

    selected?.let { d -> DaySheet(vm, b, d, holidays[d] ?: b.holidayCalendar.nameOf(d)) { selected = null } }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    today: Boolean,
    holiday: String?,
    occurrences: List<Occurrence>,
    bundle: ProfileBundle,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val active = occurrences.filter { it.isActive }
    val skipped = occurrences.any { !it.isActive }
    val desc = buildString {
        append(It.longDay(date))
        holiday?.let { append(", festivo: $it") }
        if (active.isNotEmpty()) append(", ritiro: " + active.mapNotNull { bundle.bin(it.binId)?.name }.joinToString())
    }
    Column(
        modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active.isNotEmpty() && inMonth) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.background)
            .then(if (today) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = desc }
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (today) FontWeight.Bold else FontWeight.Normal,
            color = when {
                !inMonth -> MaterialTheme.colorScheme.outline
                holiday != null || date.dayOfWeek == DayOfWeek.SUNDAY -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        Spacer(Modifier.height(4.dp))
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            active.take(6).forEach { o ->
                val bin = bundle.bin(o.binId) ?: return@forEach
                Box(
                    Modifier.size(9.dp).clip(CircleShape).background(bin.entity.colorArgb.asColor().copy(alpha = if (inMonth) 1f else 0.4f))
                        .then(if (o.status == Status.HOLIDAY_PENDING) Modifier.border(1.5.dp, MaterialTheme.colorScheme.tertiary, CircleShape) else Modifier),
                )
            }
        }
        if (skipped && active.isEmpty()) {
            Text("✕", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, textDecoration = TextDecoration.None)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DaySheet(vm: MainViewModel, b: ProfileBundle, date: LocalDate, holiday: String?, onClose: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val occ = b.occurrences(date, date)
    val evening = b.evenings(date, date).firstOrNull()
    var moving by remember { mutableStateOf<Occurrence?>(null) }
    val pid = b.profile.id
    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheet) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp).verticalScroll(rememberScrollState())) {
            Text(It.longDay(date).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineSmall)
            holiday?.let { Text("Festivo: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            if (evening != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    Planner.label(evening, System.currentTimeMillis()) + ". " + Planner.windowText(evening),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (occ.isEmpty()) Text("Nessun ritiro in questo giorno.", style = MaterialTheme.typography.bodyLarge)
            occ.forEach { o ->
                val bin = b.bin(o.binId) ?: return@forEach
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BinBadge(bin.entity.colorArgb, bin.entity.iconKey, 32.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                bin.name, style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (o.isActive) TextDecoration.None else TextDecoration.LineThrough,
                            )
                            Text(statusText(o, bin.rules), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        when (o.status) {
                            Status.HOLIDAY_PENDING -> {
                                TextButton(onClick = { vm.holidayDecision(pid, listOf(o), keep = true) }) { Text("Si fa") }
                                TextButton(onClick = { vm.holidayDecision(pid, listOf(o), keep = false) }) { Text("Salta") }
                                TextButton(onClick = { moving = o }) { Text("Sposta") }
                            }
                            Status.ACTIVE -> {
                                if (o.origin == Origin.RULE) {
                                    TextButton(onClick = { vm.skip(o, pid) }) { Text("Salta") }
                                } else {
                                    TextButton(onClick = { vm.restore(o) }) { Text(if (o.origin == Origin.ADDED) "Rimuovi" else "Annulla spostamento") }
                                }
                                TextButton(onClick = { moving = o }) { Text("Sposta") }
                            }
                            Status.SKIPPED, Status.MOVED_OUT -> TextButton(onClick = { vm.restore(o) }) { Text("Ripristina") }
                            Status.HOLIDAY_SKIPPED -> TextButton(onClick = { vm.holidayDecision(pid, listOf(o), keep = true) }) { Text("Si fa lo stesso") }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            val activeIds = occ.filter { it.isActive }.map { it.binId }.toSet()
            val others = b.bins.filter { it.id !in activeIds }
            if (others.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Raccolta straordinaria in questo giorno", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    others.forEach { bin ->
                        AssistChip(
                            onClick = { vm.addExtra(pid, bin.id, date) },
                            label = { Text("+ ${bin.name}") },
                            leadingIcon = { BinBadge(bin.entity.colorArgb, bin.entity.iconKey, 18.dp) },
                        )
                    }
                }
            }
        }
    }
    moving?.let { o ->
        DatePickDialog(o.calendarDate, { moving = null }) { to -> if (to != o.calendarDate) vm.move(o, pid, to) }
    }
}

private fun statusText(o: Occurrence, rules: List<io.github.lorenzolubrano.portafuori.rules.Rule>): String = when (o.status) {
    Status.SKIPPED -> "Saltato"
    Status.MOVED_OUT -> "Spostato al ${o.movedTo?.let { It.weekdayDay(it) }}"
    Status.HOLIDAY_PENDING -> "Festivo (${o.holiday}): si fa?"
    Status.HOLIDAY_SKIPPED -> "Saltato perché festivo"
    Status.ACTIVE -> when (o.origin) {
        Origin.MOVED_IN -> "Spostato dal ${o.movedFrom?.let { It.weekdayDay(it) }}"
        Origin.ADDED -> "Raccolta straordinaria"
        Origin.RULE -> It.describeAll(rules)
    }
}
