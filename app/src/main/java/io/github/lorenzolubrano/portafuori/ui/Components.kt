package io.github.lorenzolubrano.portafuori.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lorenzolubrano.portafuori.data.Bin
import io.github.lorenzolubrano.portafuori.rules.It
import io.github.lorenzolubrano.portafuori.ui.theme.Contrast
import io.github.lorenzolubrano.portafuori.ui.theme.extra
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

fun binIcon(key: String): ImageVector = when (key) {
    "compost" -> Icons.Filled.Eco
    "bottle" -> Icons.Filled.LocalDrink
    "paper" -> Icons.Filled.Description
    "glass" -> Icons.Filled.WineBar
    "trash" -> Icons.Filled.Delete
    "recycle" -> Icons.Filled.Recycling
    "grass" -> Icons.Filled.Grass
    "baby" -> Icons.Filled.ChildFriendly
    "bag" -> Icons.Filled.ShoppingBag
    "box" -> Icons.Filled.Inventory2
    "battery" -> Icons.Filled.BatteryFull
    "oil" -> Icons.Filled.WaterDrop
    else -> Icons.Filled.Delete
}

fun Long.asColor() = Color(this.toInt())

/** Colour + icon, never colour alone. Ink picked for contrast on the user's colour; pale colours get a dark edge. */
@Composable
fun BinBadge(color: Long, icon: String, size: Dp = 36.dp) {
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest.toArgb().toLong() and 0xFFFFFFFFL
    val edge = if (Contrast.needsEdge(color, surface)) Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape) else Modifier
    Box(Modifier.size(size).clip(CircleShape).background(color.asColor()).then(edge), contentAlignment = Alignment.Center) {
        Icon(binIcon(icon), contentDescription = null, tint = Contrast.onColor(color).asColor(), modifier = Modifier.size(size * 0.56f))
    }
}

@Composable
fun BinPill(bin: Bin, large: Boolean = false, muted: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = if (large) 48.dp else 32.dp)) {
        BinBadge(bin.entity.colorArgb, bin.entity.iconKey, if (large) 36.dp else 24.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            bin.name,
            style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BinPills(bins: List<Bin>, large: Boolean = false, muted: Boolean = false) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        bins.forEach { BinPill(it, large, muted) }
    }
}

@Composable
fun Banner(
    text: String,
    tone: Tone,
    icon: ImageVector,
    action: String? = null,
    onAction: () -> Unit = {},
    secondary: String? = null,
    onSecondary: () -> Unit = {},
) {
    val (bg, fg) = when (tone) {
        Tone.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        Tone.Warn -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        Tone.Info -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (secondary != null) TextButton(onClick = onSecondary, modifier = Modifier.heightIn(min = 48.dp)) { Text(secondary, color = fg) }
            if (action != null) TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(action, color = fg, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

enum class Tone { Error, Warn, Info }

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(top = 20.dp, bottom = 4.dp).semantics { heading() },
    )
}

@Composable
fun WeekdayChips(selected: Set<DayOfWeek>, onChange: (Set<DayOfWeek>) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { d ->
            val on = d in selected
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .toggleable(value = on, role = Role.Checkbox, onValueChange = { onChange(if (on) selected - d else selected + d) })
                    .semantics { contentDescription = It.dayName(d) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    It.shortDay(d).take(2),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A switch whose label is part of the control: the whole row toggles and TalkBack reads them together. */
@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedChoice(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val style = MaterialTheme.typography.labelLarge
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        // a segment must hold each label's longest word beside the check mark (padding 24 + icon 18 + gap 8 + border 2),
        // or the word breaks mid-word; with large text the choices stack as radio rows instead
        val segment = maxWidth / options.size
        val fits = options.all { label ->
            val word = label.split(' ').maxOf { measurer.measure(it, style).size.width }
            with(density) { word.toDp() } + 52.dp <= segment
        }
        if (fits) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = i == selected,
                        onClick = { onSelect(i) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(label, style = style) }
                }
            }
        } else {
            Column(Modifier.selectableGroup()) {
                options.forEachIndexed { i, label ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(MaterialTheme.shapes.medium)
                            .selectable(selected = i == selected, role = Role.RadioButton, onClick = { onSelect(i) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = i == selected, onClick = null, modifier = Modifier.padding(horizontal = 12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(label: String, time: LocalTime, enabled: Boolean = true, onChange: (LocalTime) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
        Text("$label ${It.time(time)}")
    }
    if (open) {
        val state = rememberTimePickerState(time.hour, time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { onChange(LocalTime.of(state.hour, state.minute)); open = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Annulla") } },
            text = { TimePicker(state) },
        )
    }
}

private fun LocalDate.utcMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.utcDate() = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

// Same range as the Material date picker; also keeps absurd stored dates from overflowing
private val PICKER_MIN = LocalDate.of(1900, 1, 1)
private val PICKER_MAX = LocalDate.of(2100, 12, 31)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.coerceIn(PICKER_MIN, PICKER_MAX).utcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onPick(it.utcDate()) }; onDismiss() }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    ) { DatePicker(state) }
}

@Composable
fun DateField(label: String, date: LocalDate?, onChange: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (date == null) label else "$label ${It.full(date)}") }
    if (open) DatePickDialog(date ?: LocalDate.now(), { open = false }, onChange)
}

@Composable
fun ChoiceChips(options: List<Pair<String, Boolean>>, onClick: (Int) -> Unit) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, (label, selected) ->
            // the chosen chip is filled and ticked like the weekday circles, so the choice is not a faint tint alone
            FilterChip(
                selected = selected,
                onClick = { onClick(i) },
                label = { Text(label) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = MaterialTheme.colorScheme.outline),
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

/** Rows that belong together share one enamel surface, split by thin lines, instead of a stack of separate cards. */
@Composable
fun RowGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(content = content)
    }
}

/** A tappable row inside a [RowGroup]; TalkBack reads its texts together. */
@Composable
fun GroupRow(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** The line between two rows of a group, starting where the row's text starts. */
@Composable
fun GroupDivider(inset: Dp = 72.dp) {
    HorizontalDivider(Modifier.padding(start = inset), color = extra().line)
}

@Composable
fun Card(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    Surface(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.clip(shape).clickable(onClick = onClick) else it },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}
