package io.github.lorenzolubrano.portafuori.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lorenzolubrano.portafuori.data.Bin
import io.github.lorenzolubrano.portafuori.ui.theme.extra

// The «Linee» world: bins are metro lines, evenings are stops.

/** Bin roundel: line colour + icon, ringed like a metro interchange. The name always sits next to it. */
@Composable
fun Roundel(bin: Bin, size: Dp = 44.dp, ring: Boolean = true) {
    if (!ring) {
        BinBadge(bin.entity.colorArgb, bin.entity.iconKey, size)
        return
    }
    val c = bin.entity.colorArgb.asColor()
    val gap = MaterialTheme.colorScheme.surfaceContainerLowest
    // outer ring in the bin colour, a gap of surface, then the badge (which adds its own dark edge on pale colours)
    Box(
        Modifier.size(size + 10.dp).border(2.dp, c, CircleShape).padding(2.dp).border(3.dp, gap, CircleShape).padding(3.dp),
        contentAlignment = Alignment.Center,
    ) { BinBadge(bin.entity.colorArgb, bin.entity.iconKey, size) }
}

/** Tonight's container: white enamel with a thick ink frame. */
@Composable
fun Station(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(4.dp, extra().stationFrame),
        modifier = Modifier.fillMaxWidth(),
    ) { Column(Modifier.padding(18.dp), content = content) }
}

/** Tonight's bins arriving at the station as coloured lines. */
@Composable
fun LinesIn(bins: List<Bin>) {
    Row(Modifier.padding(start = 30.dp).height(22.dp).clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        bins.take(8).forEach { Box(Modifier.width(8.dp).fillMaxHeight().background(it.entity.colorArgb.asColor())) }
    }
}

/** A vertical line with stops, for anything ordered in time. */
@Composable
fun Route(content: @Composable ColumnScope.() -> Unit) {
    val line = extra().line
    Column(
        Modifier.drawBehind {
            val x = 21.dp.toPx()
            drawRoundRect(
                line,
                Offset(x - 3.dp.toPx(), 20.dp.toPx()),
                Size(6.dp.toPx(), (size.height - 40.dp.toPx()).coerceAtLeast(0f)),
                CornerRadius(3.dp.toPx()),
            )
        },
        content = content,
    )
}

@Composable
fun RouteStop(content: @Composable ColumnScope.() -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val fill = MaterialTheme.colorScheme.surfaceContainerLowest
    Row(Modifier.padding(vertical = 10.dp)) {
        Box(Modifier.width(42.dp).padding(top = 2.dp), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(fill).border(5.dp, ink, CircleShape))
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), content = content)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StopBins(bins: List<Bin>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        bins.forEach { bin ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 32.dp)) {
                Roundel(bin, 24.dp, ring = false)
                Spacer(Modifier.width(6.dp))
                Text(bin.name, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun QuietRow(icon: ImageVector, text: String, action: String, onAction: () -> Unit, secondary: String? = null, onSecondary: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        if (secondary != null) TextButton(onClick = onSecondary, modifier = Modifier.heightIn(min = 48.dp)) { Text(secondary) }
        TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
    }
}

/** «Casa ▾» on the shell: which house the screen shows, and the way to switch or add one. */
@Composable
fun ProfileSwitcher(vm: MainViewModel, state: UiState) {
    var open by remember { mutableStateOf(false) }
    val sel = state.selected ?: return
    val on = extra().onShell
    Box {
        Surface(
            onClick = { open = true },
            shape = RoundedCornerShape(20.dp),
            color = on.copy(alpha = 0.14f),
            contentColor = on,
            modifier = Modifier.padding(end = 8.dp).heightIn(min = 48.dp)
                .semantics { contentDescription = "Casa scelta: ${sel.profile.name}. Tocca per cambiare" },
        ) {
            Row(Modifier.padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    sel.profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge, modifier = Modifier.widthIn(max = 140.dp),
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            state.bundles.forEach { b ->
                DropdownMenuItem(
                    text = { Text(b.profile.name) },
                    leadingIcon = { if (b.profile.id == sel.profile.id) Icon(Icons.Filled.Check, contentDescription = null) },
                    onClick = { vm.selectProfile(b.profile.id); open = false },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Aggiungi una casa") },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = { open = false; vm.startWizard(first = false) },
            )
        }
    }
}
