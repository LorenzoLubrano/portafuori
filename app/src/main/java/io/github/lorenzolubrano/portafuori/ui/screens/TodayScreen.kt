package io.github.lorenzolubrano.portafuori.ui.screens

import io.github.lorenzolubrano.portafuori.Brand
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.lorenzolubrano.portafuori.data.Evening
import io.github.lorenzolubrano.portafuori.reminders.Planner
import io.github.lorenzolubrano.portafuori.reminders.Reliability
import io.github.lorenzolubrano.portafuori.rules.It
import io.github.lorenzolubrano.portafuori.rules.ROME
import io.github.lorenzolubrano.portafuori.ui.Banner
import io.github.lorenzolubrano.portafuori.ui.BinPills
import io.github.lorenzolubrano.portafuori.ui.Card
import io.github.lorenzolubrano.portafuori.ui.MainViewModel
import io.github.lorenzolubrano.portafuori.ui.Page
import io.github.lorenzolubrano.portafuori.ui.Screen
import io.github.lorenzolubrano.portafuori.ui.SectionTitle
import io.github.lorenzolubrano.portafuori.ui.Tone
import io.github.lorenzolubrano.portafuori.ui.UiState
import io.github.lorenzolubrano.portafuori.ui.theme.okColor
import io.github.lorenzolubrano.portafuori.widget.TonightWidget
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun TodayScreen(vm: MainViewModel, state: UiState) {
    val b = state.selected ?: return
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }
    val today = LocalDate.now(ROME)
    val next = remember(b, now) { TonightWidget.nextEvening(b, now) }
    val upcoming = remember(b, now) {
        b.upcomingEvenings(today, today.plusDays(10), now).filter { it != next }.take(7)
    }
    val pendingHoliday = remember(b) { b.evenings(today, today.plusDays(21)).firstOrNull { it.pendingBins.isNotEmpty() } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refresh() }
    val rel = vm.reliability

    Page(
        title = "${Brand.NAME}",
        actions = { ProfileSwitcher(vm, state) },
    ) {
        if (!rel.notifications) {
            item {
                Banner(
                    "Le notifiche sono spente: non riceverai gli avvisi.", Tone.Error, Icons.Filled.NotificationsOff, "Attiva",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else context.startActivity(Reliability.notificationSettings(context))
                    },
                )
            }
        } else if (!rel.allGood) {
            item {
                Banner("Gli avvisi potrebbero arrivare in ritardo.", Tone.Warn, Icons.Filled.Warning, "Sistema", onAction = { vm.open(Screen.Reliability) })
            }
        }
        if (pendingHoliday != null) {
            item {
                Banner(
                    "${It.weekdayDay(pendingHoliday.collectionDate).replaceFirstChar { it.uppercase() }} è festivo (${pendingHoliday.holiday}): il ritiro si fa?",
                    Tone.Warn, Icons.Filled.EventBusy, "Decidi", onAction = { vm.open(Screen.Holidays) },
                )
            }
        }
        val p = b.profile
        if (p.pausedFrom != null && p.pausedTo != null && !today.isAfter(p.pausedTo)) {
            item {
                Banner(
                    "Pausa vacanza dal ${It.dayMonth(p.pausedFrom)} al ${It.dayMonth(p.pausedTo)}: niente avvisi.", Tone.Info, Icons.Filled.PauseCircle,
                    "Riprendi", onAction = { vm.updateProfile(p.copy(pausedFrom = null, pausedTo = null)) },
                )
            }
        }
        if (today.monthValue == 12 && state.settings.annualCheckDismissedYear < today.year + 1) {
            item {
                Banner(
                    "È uscito il calendario ${today.year + 1}? Controlla che i giorni siano ancora giusti.", Tone.Info, Icons.Filled.Update,
                    "Controlla", onAction = { vm.switchTab(Screen.Bins) }, secondary = "Ok", onSecondary = { vm.dismissAnnualCheck(today.year + 1) },
                )
            }
        }
        if (state.settings.lastBackupAt == null && now - p.createdAt > 3 * 24 * 3600_000L) {
            item {
                Banner(
                    "Fai un backup: se cambi telefono, il calendario non va perso.", Tone.Info, Icons.Filled.Backup,
                    "Backup", onAction = { vm.open(Screen.Share) },
                )
            }
        }

        item { TonightCard(vm, next, now, b.bins.isEmpty() || b.bins.all { it.rules.isEmpty() }) }

        if (upcoming.isNotEmpty()) {
            item { SectionTitle("Prossimi giorni") }
            items(upcoming, key = { it.collectionDate.toString() }) { e -> UpcomingRow(e, now) }
        }
        if (b.profile.notes.isNotBlank()) {
            item { SectionTitle("Note") }
            item { Card { Text(b.profile.notes, style = MaterialTheme.typography.bodyMedium) } }
        }
    }
}

@Composable
private fun TonightCard(vm: MainViewModel, e: Evening?, now: Long, noRules: Boolean) {
    val tonight = e != null && TonightWidget.isTonight(e, now)
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (tonight && e?.done != true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            when {
                noRules -> {
                    Text("PER INIZIARE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Aggiungi i giorni di raccolta", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Apri «Bidoni» e scegli i giorni di ogni bidone.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { vm.switchTab(Screen.Bins) }) { Text("Vai ai bidoni") }
                }
                e == null -> {
                    Text("STASERA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Niente da esporre", style = MaterialTheme.typography.displaySmall)
                    Text("Nessun ritiro nelle prossime 3 settimane.", style = MaterialTheme.typography.bodyMedium)
                }
                tonight -> {
                    Text(Planner.label(e, now).uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    BinPills(e.bins, large = true)
                    Spacer(Modifier.height(12.dp))
                    Text(Planner.windowText(e), style = MaterialTheme.typography.bodyLarge)
                    if (e.pendingBins.isNotEmpty()) {
                        Text(
                            "È festivo (${e.holiday}): verifica che il ritiro si faccia.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    if (e.done) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = okColor())
                            Spacer(Modifier.width(8.dp))
                            Text("Esposti", style = MaterialTheme.typography.titleMedium, color = okColor(), modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.undoDone(e.profileId, e.collectionDate) }) { Text("Annulla") }
                        }
                    } else {
                        Button(
                            onClick = { vm.markDone(e.profileId, e.collectionDate) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(),
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Fatto, li ho esposti")
                        }
                    }
                }
                else -> {
                    Text("STASERA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Niente da esporre", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))
                    Text(Planner.label(e, now), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BinPills(e.bins)
                    Spacer(Modifier.height(8.dp))
                    Text(Planner.windowText(e), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun UpcomingRow(e: Evening, now: Long) {
    Card {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.width(112.dp)) {
                Text(Planner.label(e, now), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "ritiro ${It.dayMonth(e.collectionDate)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.weight(1f)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BinPills(e.bins)
                    if (e.pendingBins.isNotEmpty()) {
                        Text("Festivo: ${e.holiday}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    if (e.done) Text("✓ Esposti", style = MaterialTheme.typography.bodySmall, color = okColor())
                }
            }
        }
    }
}

@Composable
fun ProfileSwitcher(vm: MainViewModel, state: UiState) {
    var open by remember { mutableStateOf(false) }
    val sel = state.selected ?: return
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.padding(end = 8.dp)) {
            Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(sel.profile.name, maxLines = 1)
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
