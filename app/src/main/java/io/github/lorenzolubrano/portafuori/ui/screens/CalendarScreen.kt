package io.github.lorenzolubrano.portafuori.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lorenzolubrano.portafuori.data.Bin
import io.github.lorenzolubrano.portafuori.data.ProfileBundle
import io.github.lorenzolubrano.portafuori.rules.It
import io.github.lorenzolubrano.portafuori.rules.Occurrence
import io.github.lorenzolubrano.portafuori.rules.Origin
import io.github.lorenzolubrano.portafuori.rules.ROME
import io.github.lorenzolubrano.portafuori.rules.Schedule
import io.github.lorenzolubrano.portafuori.rules.Status
import io.github.lorenzolubrano.portafuori.ui.BinBadge
import io.github.lorenzolubrano.portafuori.ui.Card
import io.github.lorenzolubrano.portafuori.ui.DatePickDialog
import io.github.lorenzolubrano.portafuori.ui.MainViewModel
import io.github.lorenzolubrano.portafuori.ui.Page
import io.github.lorenzolubrano.portafuori.ui.ProfileSwitcher
import io.github.lorenzolubrano.portafuori.ui.Roundel
import io.github.lorenzolubrano.portafuori.ui.SegmentedChoice
import io.github.lorenzolubrano.portafuori.ui.UiState
import io.github.lorenzolubrano.portafuori.ui.Wording
import io.github.lorenzolubrano.portafuori.ui.asColor
import io.github.lorenzolubrano.portafuori.ui.calendar.CalendarModel
import io.github.lorenzolubrano.portafuori.ui.calendar.Night
import io.github.lorenzolubrano.portafuori.ui.calendar.Stop
import io.github.lorenzolubrano.portafuori.ui.theme.Contrast
import io.github.lorenzolubrano.portafuori.ui.theme.Styles
import io.github.lorenzolubrano.portafuori.ui.theme.extra
import io.github.lorenzolubrano.portafuori.ui.theme.lightBarIcons
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(vm: MainViewModel, state: UiState) {
    val b = state.selected ?: return
    val today = LocalDate.now(ROME)
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 = settimana, 1 = mese
    var weekStart by rememberSaveable { mutableStateOf(CalendarModel.weekStart(today)) }
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today)) }
    // the evening whose day sheet is open; every evening opens, even an empty one (extra collections)
    var open by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val big = LocalDensity.current.fontScale > 1.3f
    val nights = remember(b, mode, weekStart, month) {
        if (mode == 0) {
            CalendarModel.nights(b, weekStart, weekStart.plusDays(6))
        } else {
            val gridStart = CalendarModel.weekStart(month.atDay(1))
            CalendarModel.nights(b, gridStart, gridStart.plusDays(41))
        }
    }

    Page(title = "Calendario", actions = { ProfileSwitcher(vm, state) }) {
        item { SegmentedChoice(listOf("Settimana", "Mese"), mode) { mode = it } }
        item {
            Crossfade(mode, animationSpec = tween(180), label = "vista") { m ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (m == 0) {
                        Pager(
                            Wording.weekTitle(weekStart), "Settimana precedente", "Settimana successiva",
                            { weekStart = weekStart.minusWeeks(1) }, { weekStart = weekStart.plusWeeks(1) },
                        )
                        AnimatedContent(weekStart, transitionSpec = { slideByTime() }, label = "settimana") { ws ->
                            val n = remember(b, ws) { CalendarModel.nights(b, ws, ws.plusDays(6)) }
                            WeekLines(b, ws, today, n, big) { open = it }
                        }
                    } else {
                        Pager(
                            Wording.monthTitle(month), "Mese precedente", "Mese successivo",
                            { month = month.minusMonths(1) }, { month = month.plusMonths(1) },
                        )
                        AnimatedContent(month, transitionSpec = { slideByTime() }, label = "mese") { ym ->
                            val gridStart = CalendarModel.weekStart(ym.atDay(1))
                            val n = remember(b, ym) { CalendarModel.nights(b, gridStart, gridStart.plusDays(41)) }
                            MonthGrid(b, ym, gridStart, today, n) { open = it }
                        }
                    }
                }
            }
        }
    }

    open?.let { evening ->
        val date = nights[evening]?.calendarDate ?: CalendarModel.calendarDateOf(b, evening)
        DaySheet(vm, b, date, b.holidayCalendar.nameOf(date)) { open = null }
    }
}

/** Later weeks and months come in from the right, earlier ones from the left. */
private fun <T : Comparable<T>> AnimatedContentTransitionScope<T>.slideByTime(): ContentTransform {
    val dir = if (targetState > initialState) 1 else -1
    return (slideInHorizontally(tween(250)) { it * dir / 4 } + fadeIn(tween(250))) togetherWith
        (slideOutHorizontally(tween(200)) { -it * dir / 4 } + fadeOut(tween(150)))
}

@Composable
private fun Pager(title: String, prevLabel: String, nextLabel: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onPrev, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, prevLabel) }
        Text(
            title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() },
        )
        FilledTonalIconButton(onClick = onNext, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, nextLabel) }
    }
}

/** Pale bins on white enamel get a dark edge, as in [BinBadge]. */
@Composable
private fun edgeFor(color: Long): Color? {
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest.toArgb().toLong() and 0xFFFFFFFFL
    return if (Contrast.needsEdge(color, surface)) MaterialTheme.colorScheme.onSurfaceVariant else null
}

@Composable
private fun WeekLines(b: ProfileBundle, start: LocalDate, today: LocalDate, nights: Map<LocalDate, Night>, big: Boolean, onOpen: (LocalDate) -> Unit) {
    val days = (0..6).map { start.plusDays(it.toLong()) }
    val colW = 30.dp
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLowest, contentColor = MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.padding(10.dp)) {
            // header: one column per evening, tonight highlighted, each column opens its evening
            Row(verticalAlignment = Alignment.Bottom) {
                if (!big) Spacer(Modifier.weight(1f))
                days.forEach { d ->
                    val tonight = d == today
                    Column(
                        Modifier.then(if (big) Modifier.weight(1f) else Modifier.width(colW + 4.dp)).heightIn(min = 48.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(if (tonight) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onOpen(d) }
                            .semantics { contentDescription = Wording.eveningTitle(d) },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        val ink = if (tonight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        Text(It.dayName(d.dayOfWeek).take(1).uppercase(), style = MaterialTheme.typography.labelMedium, color = ink)
                        Text("${d.dayOfMonth}", style = MaterialTheme.typography.labelLarge, color = ink)
                    }
                }
            }
            b.bins.forEach { bin ->
                val stops = days.map { nights[it]?.stopOf(bin.id) ?: Stop.NONE }
                BinLine(bin, stops, big, colW)
            }
        }
    }
}

@Composable
private fun BinLine(bin: Bin, stops: List<Stop>, big: Boolean, colW: Dp) {
    val c = bin.entity.colorArgb.asColor()
    val edge = edgeFor(bin.entity.colorArgb)
    val fill = MaterialTheme.colorScheme.surfaceContainerLowest
    val none = stops.all { it == Stop.NONE }
    val label: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BinBadge(bin.entity.colorArgb, bin.entity.iconKey, 24.dp)
            Spacer(Modifier.width(8.dp))
            // the name column is narrow: break long words at Italian syllables, not mid-syllable
            Text(bin.name, style = MaterialTheme.typography.titleSmall.copy(hyphens = Hyphens.Auto, lineBreak = LineBreak.Paragraph, localeList = LocaleList("it")), color = if (none) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
        }
    }
    val stopsRow: @Composable RowScope.() -> Unit = {
        stops.forEach { s ->
            Box(Modifier.then(if (big) Modifier.weight(1f) else Modifier.width(colW + 4.dp)).height(32.dp), contentAlignment = Alignment.Center) {
                when (s) {
                    // a border draws over what follows it, so the dark edge goes first to stay on top of the ring
                    Stop.OUT -> Box(
                        Modifier.size(18.dp).clip(CircleShape)
                            .then(if (edge != null) Modifier.border(1.5.dp, edge, CircleShape) else Modifier)
                            .background(fill).border(4.dp, c, CircleShape),
                    )
                    Stop.PENDING -> Icon(Icons.Filled.EventBusy, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    Stop.SKIPPED -> Box(Modifier.size(14.dp).clip(CircleShape).border(2.dp, (edge ?: c).copy(alpha = 0.5f), CircleShape))
                    Stop.NONE -> {}
                }
            }
        }
    }
    val desc = bin.name + ": " + stops.zip(listOf("lunedì", "martedì", "mercoledì", "giovedì", "venerdì", "sabato", "domenica"))
        .filter { it.first == Stop.OUT || it.first == Stop.PENDING }.joinToString { it.second }.ifEmpty { "nessuna sera" }
    Column(Modifier.padding(vertical = 4.dp).semantics(mergeDescendants = true) { contentDescription = desc }) {
        if (big) {
            label(); Row(content = stopsRow)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { label() }
                Box {
                    // the line between the first and the last stop, drawn under the stops
                    val first = stops.indexOfFirst { it == Stop.OUT }
                    val last = stops.indexOfLast { it == Stop.OUT }
                    if (first >= 0 && last > first) {
                        Box(
                            Modifier.padding(start = (colW + 4.dp) * first + (colW + 4.dp) / 2).width((colW + 4.dp) * (last - first)).height(6.dp)
                                .align(Alignment.CenterStart).clip(CircleShape).background(c)
                                .then(if (edge != null) Modifier.border(1.dp, edge, CircleShape) else Modifier),
                        )
                    }
                    Row(content = stopsRow)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthGrid(b: ProfileBundle, month: YearMonth, gridStart: LocalDate, today: LocalDate, nights: Map<LocalDate, Night>, onOpen: (LocalDate) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach {
                Text(
                    It.shortDay(it), modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (week in 0 until 6) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (dow in 0 until 7) {
                    val d = gridStart.plusDays((week * 7 + dow).toLong())
                    val night = nights[d]
                    val inMonth = d.month == month.month
                    val active = night?.activeBinIds.orEmpty().mapNotNull { b.bin(it) }
                    val shown = if (active.size > CalendarModel.MAX_ROUNDELS) active.take(CalendarModel.MAX_ROUNDELS - 1) else active
                    Column(
                        Modifier.weight(1f).heightIn(min = 64.dp).clip(MaterialTheme.shapes.small)
                            .background(if (night != null && inMonth) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent)
                            .then(if (d == today) Modifier.border(3.dp, extra().stationFrame, MaterialTheme.shapes.small) else Modifier)
                            .clickable { onOpen(d) }
                            .semantics { contentDescription = Wording.eveningTitle(d) + if (active.isEmpty()) "" else ": " + active.joinToString { it.name } }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "${d.dayOfMonth}", style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (inMonth) FontWeight.Bold else FontWeight.Normal,
                            color = if (inMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            shown.forEach { BinBadge(it.entity.colorArgb, it.entity.iconKey, 16.dp) }
                            val more = CalendarModel.overflow(active.size)
                            if (more > 0) Text("+$more", style = MaterialTheme.typography.labelSmall)
                        }
                        if (night != null && active.isEmpty()) {
                            // only skipped collections this evening: a faint empty stop, as in the week
                            Box(Modifier.padding(top = 2.dp).size(12.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape))
                        }
                        if (night?.occurrences?.any { it.status == Status.HOLIDAY_PENDING } == true) {
                            Icon(Icons.Filled.EventBusy, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DaySheet(vm: MainViewModel, b: ProfileBundle, date: LocalDate, holiday: String?, onClose: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val occ = b.occurrences(date, date)
    val evening = b.evenings(date, date).firstOrNull()
    val collection = occ.firstOrNull()?.collectionDate ?: Schedule.collectionDate(b.rules, date)
    var moving by remember { mutableStateOf<Occurrence?>(null) }
    val pid = b.profile.id
    val appDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheet,
        // the sheet is a window of its own: its bar icons follow the app (shell above, sheet below), not the phone theme
        properties = ModalBottomSheetProperties(
            isAppearanceLightStatusBars = !lightBarIcons(Styles.current, appDark),
            isAppearanceLightNavigationBars = !appDark,
        ),
    ) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp).verticalScroll(rememberScrollState())) {
            Text(Wording.eveningTitle(b.eveningOf(collection)), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(Wording.ritiroLong(collection), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            holiday?.let { Text("Festivo: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            if (evening != null) {
                Spacer(Modifier.height(6.dp))
                Text(Wording.from(evening) + ", " + Wording.until(evening), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            if (occ.isEmpty()) Text("Nessun ritiro in questo giorno.", style = MaterialTheme.typography.bodyLarge)
            occ.forEach { o ->
                val bin = b.bin(o.binId) ?: return@forEach
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Roundel(bin, 32.dp, ring = false)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                bin.name, style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (o.isActive) TextDecoration.None else TextDecoration.LineThrough,
                            )
                            Text(statusText(o, bin.rules), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val tall = Modifier.heightIn(min = 48.dp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        when (o.status) {
                            Status.HOLIDAY_PENDING -> {
                                TextButton(onClick = { vm.holidayDecision(pid, listOf(o), keep = true) }, modifier = tall) { Text("Si fa") }
                                TextButton(onClick = { vm.holidayDecision(pid, listOf(o), keep = false) }, modifier = tall) { Text("Salta") }
                                TextButton(onClick = { moving = o }, modifier = tall) { Text("Sposta") }
                            }
                            Status.ACTIVE -> {
                                if (o.origin == Origin.RULE) {
                                    TextButton(onClick = { vm.skip(o, pid); vm.message = "Ritiro saltato." }, modifier = tall) { Text("Salta") }
                                } else {
                                    TextButton(onClick = { vm.restore(o) }, modifier = tall) { Text(if (o.origin == Origin.ADDED) "Rimuovi" else "Annulla spostamento") }
                                }
                                TextButton(onClick = { moving = o }, modifier = tall) { Text("Sposta") }
                            }
                            Status.SKIPPED, Status.MOVED_OUT -> TextButton(onClick = { vm.restore(o) }, modifier = tall) { Text("Ripristina") }
                            Status.HOLIDAY_SKIPPED -> TextButton(onClick = { vm.holidayDecision(pid, listOf(o), keep = true) }, modifier = tall) { Text("Si fa lo stesso") }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            val activeIds = occ.filter { it.isActive }.map { it.binId }.toSet()
            val others = b.bins.filter { it.id !in activeIds }
            if (others.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = extra().line)
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
