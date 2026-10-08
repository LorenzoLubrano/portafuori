package io.github.lorenzolubrano.portafuori.ui.screens

import androidx.compose.runtime.key
import io.github.lorenzolubrano.portafuori.Brand
import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lorenzolubrano.portafuori.reminders.Reliability
import io.github.lorenzolubrano.portafuori.rules.ROME
import io.github.lorenzolubrano.portafuori.ui.Card
import io.github.lorenzolubrano.portafuori.ui.GroupDivider
import io.github.lorenzolubrano.portafuori.ui.GroupItem
import io.github.lorenzolubrano.portafuori.ui.GroupRow
import io.github.lorenzolubrano.portafuori.ui.MainViewModel
import io.github.lorenzolubrano.portafuori.ui.Page
import io.github.lorenzolubrano.portafuori.ui.Route
import io.github.lorenzolubrano.portafuori.ui.RouteStop
import io.github.lorenzolubrano.portafuori.ui.RowGroup
import io.github.lorenzolubrano.portafuori.ui.SectionTitle
import io.github.lorenzolubrano.portafuori.ui.theme.okColor
import java.time.Instant
import java.time.format.DateTimeFormatter

private data class Guide(val brand: String, val match: List<String>, val steps: List<String>)

private val GUIDES = listOf(
    Guide(
        "Samsung", listOf("samsung"),
        listOf(
            "Impostazioni › Batteria › Limiti di utilizzo in background.",
            "Apri «App mai in sospensione» e aggiungi ${Brand.NAME}.",
            "Controlla che ${Brand.NAME} non sia in «App in sospensione» o «App in sospensione profonda».",
            "In Impostazioni › App › ${Brand.NAME} › Batteria scegli «Senza restrizioni».",
        ),
    ),
    Guide(
        "Xiaomi, Redmi, POCO", listOf("xiaomi", "redmi", "poco"),
        listOf(
            "Impostazioni › App › Gestisci app › ${Brand.NAME}.",
            "Attiva «Avvio automatico».",
            "Risparmio batteria › «Nessuna restrizione».",
            "Nel menu delle app recenti, tieni premuto ${Brand.NAME} e tocca il lucchetto.",
        ),
    ),
    Guide(
        "Huawei, Honor", listOf("huawei", "honor"),
        listOf(
            "Impostazioni › Batteria › Avvio app.",
            "Trova ${Brand.NAME}, disattiva «Gestisci automaticamente» e lascia attivi avvio automatico, avvio secondario ed esecuzione in background.",
        ),
    ),
    Guide(
        "OPPO, realme, OnePlus", listOf("oppo", "realme", "oneplus"),
        listOf(
            "Impostazioni › Batteria › Altre impostazioni › Ottimizza utilizzo batteria: ${Brand.NAME} su «Non ottimizzare».",
            "Impostazioni › App › ${Brand.NAME} › Utilizzo batteria: consenti attività in background e avvio automatico.",
        ),
    ),
    Guide(
        "Google Pixel, Motorola, altri", emptyList(),
        listOf(
            "Impostazioni › App › ${Brand.NAME} › Batteria: «Senza restrizioni».",
            "Impostazioni › App › ${Brand.NAME}: disattiva «Sospendi attività app se inutilizzata».",
        ),
    ),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReliabilityScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val rel = vm.reliability
    val deliveries by vm.deliveries.collectAsStateWithLifecycle()
    val brand = Build.MANUFACTURER.lowercase()
    val mine = GUIDES.firstOrNull { g -> g.match.any { brand.contains(it) } } ?: GUIDES.last()
    var expanded by remember { mutableStateOf(mine.brand) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refresh() }
    val fmt = remember { DateTimeFormatter.ofPattern("EEE d/MM HH:mm", io.github.lorenzolubrano.portafuori.rules.It.locale) }

    fun go(intent: Intent?) {
        runCatching { context.startActivity(intent ?: Reliability.appDetails(context)) }
            .onFailure { context.startActivity(Reliability.appDetails(context)) }
    }

    Page(title = "Affidabilità", onBack = { vm.back() }) {
        item {
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (rel.allGood) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline, contentDescription = null,
                        tint = if (rel.allGood) okColor() else MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (rel.allGood) "Tutto a posto: gli avvisi dovrebbero arrivare puntuali." else "Sistema le voci rosse, altrimenti il telefono potrebbe ritardare o bloccare gli avvisi.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        item { SectionTitle("Controlli") }
        // the checks are one group of rows, like the other lists
        item {
            RowGroup {
                CheckRow("Notifiche consentite", rel.notifications) {
                    if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else go(Reliability.notificationSettings(context))
                }
                GroupDivider(52.dp)
                CheckRow("Avvisi all'ora esatta", rel.exactAlarms) { go(Reliability.exactAlarmSettings(context)) }
                GroupDivider(52.dp)
                CheckRow("Batteria senza restrizioni", rel.battery) { go(Reliability.batteryExemption(context)) }
                rel.hibernationExempt?.let { ok ->
                    GroupDivider(52.dp)
                    CheckRow("«Sospendi attività se inutilizzata» disattivato", ok) { go(Reliability.hibernationSettings(context)) }
                }
            }
        }
        item { SectionTitle("Prova") }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.testNow() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Notifica di prova") }
                OutlinedButton(onClick = { vm.testInOneMinute() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Prova tra 1 minuto") }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Per la prova tra 1 minuto, blocca lo schermo e aspetta: se la notifica arriva, anche gli avvisi della sera arriveranno.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { SectionTitle("Guida per il tuo telefono") }
        item {
            RowGroup {
                GUIDES.forEachIndexed { n, g ->
                    key(g.brand) {
                        if (n > 0) GroupDivider(16.dp)
                        val open = expanded == g.brand
                        GroupRow(onClick = { expanded = if (open) "" else g.brand }) {
                            Text(g.brand + if (g == mine) " (il tuo)" else "", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                        }
                        if (open) {
                            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                                g.steps.forEachIndexed { i, s -> Text("${i + 1}. $s", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp)) }
                                TextButton(onClick = { go(Reliability.appDetails(context)) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Apri le impostazioni di ${Brand.NAME}") }
                            }
                        }
                    }
                }
            }
        }
        item { SectionTitle("Registro degli ultimi avvisi") }
        val recent = deliveries.take(30)
        if (recent.isEmpty()) {
            item { Text("Ancora nessun avviso.", style = MaterialTheme.typography.bodyMedium) }
        }
        // one stop per notice, newest first
        if (recent.isNotEmpty()) {
            item {
                Route {
                    recent.forEach { d ->
                        val planned = Instant.ofEpochMilli(d.scheduledAt).atZone(ROME)
                        val late = d.postedAt?.let { (it - d.scheduledAt) / 60_000 }
                        RouteStop {
                            Text(d.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Previsto ${planned.format(fmt)} · " + when {
                                    d.postedAt == null -> "mancato"
                                    late != null && late >= 5 -> "arrivato con $late min di ritardo"
                                    else -> "arrivato"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    d.postedAt == null -> MaterialTheme.colorScheme.error
                                    late != null && late >= 5 -> MaterialTheme.colorScheme.tertiary
                                    else -> okColor()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(label: String, ok: Boolean, onFix: () -> Unit) {
    GroupItem {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ok) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline, contentDescription = if (ok) "OK" else "Da sistemare",
                tint = if (ok) okColor() else MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
        // under the label, so a long label and a large font never squeeze it
        if (!ok) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onFix, modifier = Modifier.padding(start = 36.dp).heightIn(min = 48.dp)) { Text("Apri impostazioni") }
        }
    }
}
