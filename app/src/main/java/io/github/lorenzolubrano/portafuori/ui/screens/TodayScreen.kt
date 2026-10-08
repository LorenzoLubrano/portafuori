package io.github.lorenzolubrano.portafuori.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.lorenzolubrano.portafuori.data.Evening
import io.github.lorenzolubrano.portafuori.reminders.Planner
import io.github.lorenzolubrano.portafuori.reminders.Reliability
import io.github.lorenzolubrano.portafuori.rules.It
import io.github.lorenzolubrano.portafuori.rules.ROME
import io.github.lorenzolubrano.portafuori.ui.Banner
import io.github.lorenzolubrano.portafuori.ui.Card
import io.github.lorenzolubrano.portafuori.ui.LinesIn
import io.github.lorenzolubrano.portafuori.ui.MainViewModel
import io.github.lorenzolubrano.portafuori.ui.Page
import io.github.lorenzolubrano.portafuori.ui.ProfileSwitcher
import io.github.lorenzolubrano.portafuori.ui.QuietRow
import io.github.lorenzolubrano.portafuori.ui.Roundel
import io.github.lorenzolubrano.portafuori.ui.Route
import io.github.lorenzolubrano.portafuori.ui.RouteStop
import io.github.lorenzolubrano.portafuori.ui.Screen
import io.github.lorenzolubrano.portafuori.ui.SectionTitle
import io.github.lorenzolubrano.portafuori.ui.Station
import io.github.lorenzolubrano.portafuori.ui.StopBins
import io.github.lorenzolubrano.portafuori.ui.Tone
import io.github.lorenzolubrano.portafuori.ui.UiState
import io.github.lorenzolubrano.portafuori.ui.Wording
import io.github.lorenzolubrano.portafuori.ui.theme.extra
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
    val noRules = b.bins.isEmpty() || b.bins.all { it.rules.isEmpty() }
    val tonight = next != null && TonightWidget.isTonight(next, now)
    val p = b.profile

    Page(
        title = It.longDay(today).replaceFirstChar { it.uppercase() },
        actions = { ProfileSwitcher(vm, state) },
    ) {
        // Only a missing permission may sit above tonight: without it nothing else matters
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
        }

        item {
            when {
                noRules -> Station {
                    Text("Per iniziare", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(8.dp))
                    Text("Apri «Bidoni» e scegli i giorni di ogni bidone.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { vm.switchTab(Screen.Bins) }, modifier = Modifier.heightIn(min = 56.dp)) {
                        Text("Vai ai bidoni", style = MaterialTheme.typography.titleMedium)
                    }
                }
                tonight -> Column {
                    LinesIn(next!!.bins)
                    Station { TonightStop(vm, next, now) }
                }
                else -> Station {
                    Text("Stasera niente da portare fuori", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(10.dp))
                    if (next == null) {
                        Text("Nessun ritiro nelle prossime 3 settimane.", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        Text(
                            "La prossima volta: ${Planner.label(next, now).replaceFirstChar { it.lowercase() }}",
                            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        StopBins(next.bins)
                    }
                }
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
        if (rel.notifications && !rel.allGood) {
            item {
                Banner("Gli avvisi potrebbero arrivare in ritardo.", Tone.Warn, Icons.Filled.Warning, "Controlla", onAction = { vm.open(Screen.Reliability) })
            }
        }
        if (p.pausedFrom != null && p.pausedTo != null && !today.isAfter(p.pausedTo)) {
            item {
                Banner(
                    "Pausa vacanza dal ${It.dayMonth(p.pausedFrom)} al ${It.dayMonth(p.pausedTo)}: niente avvisi.", Tone.Info, Icons.Filled.PauseCircle,
                    "Riprendi", onAction = { vm.updateProfile(p.copy(pausedFrom = null, pausedTo = null)) },
                )
            }
        }

        if (upcoming.isNotEmpty()) {
            item { SectionTitle("Prossime sere") }
            item {
                Route { upcoming.forEach { e -> RouteStop { UpcomingStop(e, now) } } }
            }
        }
        if (p.notes.isNotBlank()) {
            item { SectionTitle("Note") }
            item { Card { Text(p.notes, style = MaterialTheme.typography.bodyLarge) } }
        }

        if (today.monthValue == 12 && state.settings.annualCheckDismissedYear < today.year + 1) {
            item {
                QuietRow(
                    Icons.Filled.Update, "È uscito il calendario ${today.year + 1}? Controlla che i giorni siano ancora giusti.",
                    "Controlla", { vm.switchTab(Screen.Bins) }, secondary = "Ok", onSecondary = { vm.dismissAnnualCheck(today.year + 1) },
                )
            }
        }
        if (state.settings.lastBackupAt == null && now - p.createdAt > 3 * 24 * 3600_000L) {
            item {
                QuietRow(Icons.Filled.Backup, "Fai un backup: se cambi telefono, il calendario non va perso.", "Backup", { vm.open(Screen.Share) })
            }
        }
    }
}

@Composable
private fun TonightStop(vm: MainViewModel, e: Evening, now: Long) {
    Text(Wording.headline(Planner.label(e, now)), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        e.bins.forEach { bin ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Roundel(bin)
                Spacer(Modifier.width(12.dp))
                Text(bin.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    HorizontalDivider(thickness = 2.dp, color = extra().line)
    Spacer(Modifier.height(12.dp))
    Text(Wording.from(e), style = MaterialTheme.typography.headlineSmall)
    Text(Wording.until(e), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (e.pendingBins.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text("È festivo (${e.holiday}): verifica che il ritiro si faccia.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.tertiary)
    }
    Spacer(Modifier.height(16.dp))
    if (e.done) {
        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Text(Wording.doneState(e.bins.size), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { vm.undoDone(e.profileId, e.collectionDate) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Annulla", style = MaterialTheme.typography.titleMedium)
            }
        }
    } else {
        Button(onClick = { vm.markDone(e.profileId, e.collectionDate) }, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(Wording.doneButton(e.bins.size), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun UpcomingStop(e: Evening, now: Long) {
    Text(Planner.label(e, now), style = MaterialTheme.typography.titleMedium)
    Text(Wording.ritiro(e.collectionDate), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(6.dp))
    StopBins(e.bins)
    if (e.pendingBins.isNotEmpty()) {
        Text("Festivo: ${e.holiday}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
    }
    if (e.done) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = okColor(), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(Wording.doneState(e.bins.size), style = MaterialTheme.typography.bodyMedium, color = okColor())
        }
    }
}
