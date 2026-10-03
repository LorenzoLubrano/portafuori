package io.github.lorenzolubrano.portafuori.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lorenzolubrano.portafuori.data.Bin
import io.github.lorenzolubrano.portafuori.rules.It
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

/** Colour + icon, never colour alone. */
@Composable
fun BinBadge(color: Long, icon: String, size: Dp = 36.dp) {
    val c = color.asColor()
    val onC = if (c.luminance() > 0.55f) Color(0xFF1B1B1B) else Color.White
    Box(
        Modifier.size(size).clip(CircleShape).background(c)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(binIcon(icon), contentDescription = null, tint = onC, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun BinPill(bin: Bin, large: Boolean = false, muted: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (muted) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(start = 4.dp, end = if (large) 16.dp else 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BinBadge(bin.entity.colorArgb, bin.entity.iconKey, if (large) 36.dp else 26.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                bin.name,
                style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BinPills(bins: List<Bin>, large: Boolean = false, muted: Boolean = false) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (secondary != null) TextButton(onClick = onSecondary) { Text(secondary, color = fg) }
            if (action != null) TextButton(onClick = onAction) { Text(action, color = fg) }
        }
    }
}

enum class Tone { Error, Warn, Info }

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = Kicker,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun WeekdayChips(selected: Set<DayOfWeek>, onChange: (Set<DayOfWeek>) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { d ->
            val on = d in selected
            val name = It.dayName(d)
            Box(
                Modifier.weight(1f).size(40.dp).clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onChange(if (on) selected - d else selected + d) }
                    .semantics { contentDescription = name + if (on) ", selezionato" else "" },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(label: String, time: LocalTime, enabled: Boolean = true, onChange: (LocalTime) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, enabled = enabled) {
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
    OutlinedButton(onClick = { open = true }) { Text(if (date == null) label else "$label ${It.full(date)}") }
    if (open) DatePickDialog(date ?: LocalDate.now(), { open = false }, onChange)
}

@Composable
fun ChoiceChips(options: List<Pair<String, Boolean>>, onClick: (Int) -> Unit) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, (label, selected) ->
            FilterChip(selected = selected, onClick = { onClick(i) }, label = { Text(label) })
        }
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick) else it },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}
