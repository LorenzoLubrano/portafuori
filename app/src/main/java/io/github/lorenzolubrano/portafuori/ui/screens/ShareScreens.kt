package io.github.lorenzolubrano.portafuori.ui.screens

import io.github.lorenzolubrano.portafuori.data.Limits
import io.github.lorenzolubrano.portafuori.Brand
import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.zxing.PlanarYUVLuminanceSource
import io.github.lorenzolubrano.portafuori.data.Bin
import io.github.lorenzolubrano.portafuori.data.ProfileBundle
import io.github.lorenzolubrano.portafuori.rules.It
import io.github.lorenzolubrano.portafuori.rules.ROME
import io.github.lorenzolubrano.portafuori.share.Qr
import io.github.lorenzolubrano.portafuori.share.ShareCodec
import io.github.lorenzolubrano.portafuori.ui.BinBadge
import io.github.lorenzolubrano.portafuori.ui.Card
import io.github.lorenzolubrano.portafuori.ui.MainViewModel
import io.github.lorenzolubrano.portafuori.ui.Page
import io.github.lorenzolubrano.portafuori.ui.Route
import io.github.lorenzolubrano.portafuori.ui.RouteStop
import io.github.lorenzolubrano.portafuori.ui.SectionTitle
import io.github.lorenzolubrano.portafuori.ui.StopBins
import io.github.lorenzolubrano.portafuori.ui.UiState
import io.github.lorenzolubrano.portafuori.ui.Wording
import io.github.lorenzolubrano.portafuori.ui.theme.extra
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareScreen(vm: MainViewModel, state: UiState) {
    val context = LocalContext.current
    val b = state.selected
    val payload = remember(b) { b?.let { ShareCodec.qrPayload(vm.shareEnvelope(it)) } }
    val qr = remember(payload) { payload?.takeIf { it.length <= ShareCodec.QR_MAX_CHARS }?.let { Qr.encode(it) } }
    var fullscreen by remember { mutableStateOf(false) }
    var pasting by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.importImage(context, it) }
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.importUri(context, it) } }
    // Only this button restores personal reminder settings: the file itself cannot ask for it
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importUri(context, it, restore = true) }
    }
    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { vm.writeBackup(context, it) } }

    Page(title = if (b == null) "Importa" else "Condividi e backup", onBack = { vm.back() }) {
        if (b != null && payload != null) {
            item { SectionTitle("Condividi con i vicini") }
            item {
                Text(
                    "Il vicino apre ${Brand.NAME}, sceglie «Importa» e inquadra il QR. Passano nome e zona della casa, bidoni, giorni, festività e note; i tuoi orari degli avvisi restano tuoi.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (b.profile.notes.isNotBlank()) {
                item {
                    Card {
                        Text("Con il calendario passano anche queste note:", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(b.profile.notes, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Se non vuoi condividerle, toglile da Impostazioni della casa prima di inviare.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Surface(
                    shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    contentColor = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth(),
                ) { Column(Modifier.padding(18.dp)) {
                    if (qr != null) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Image(
                                qr.asImageBitmap(), contentDescription = "QR del calendario di ${b.profile.name}",
                                modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f).clip(MaterialTheme.shapes.medium).background(Color.White)
                                    .clickable { fullscreen = true },
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tocca il QR per ingrandirlo.", style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text("Il calendario è troppo grande per un QR: usa «Invia file».", style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = extra().line)
                    if (qr != null) ActionRow(Icons.Filled.Share, "Invia come immagine") { shareQrImage(context, qr, b, payload) }
                    ActionRow(Icons.Filled.Description, "Invia file") { shareJsonFile(context, b, ShareCodec.encode(vm.shareEnvelope(b))) }
                    ActionRow(Icons.Filled.ContentCopy, "Copia codice") {
                        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("${Brand.NAME}", payload))
                        vm.message = "Codice copiato: incollalo in una chat."
                    }
                } }
            }
        }
        item { SectionTitle("Importa un calendario") }
        item {
            Button(onClick = { vm.open(io.github.lorenzolubrano.portafuori.ui.Screen.Scan) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Scansiona QR", style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            Card {
                ActionRow(Icons.Filled.Image, "Da un'immagine") { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                ActionRow(Icons.Filled.Description, "Da un file") { pickFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
                ActionRow(Icons.Filled.ContentPaste, "Incolla codice") { pasting = true }
            }
        }
        item { SectionTitle("Backup") }
        item {
            Text(
                state.settings.lastBackupAt?.let { "Ultimo backup: " + It.full(Instant.ofEpochMilli(it).atZone(ROME).toLocalDate()) }
                    ?: "Non hai ancora fatto un backup. Salvalo su Drive, in una chiavetta o mandalo a te stesso.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item {
            Card {
                if (state.bundles.isNotEmpty()) ActionRow(Icons.Filled.Save, "Salva backup") { saveBackup.launch("portafuori-backup-${LocalDate.now(ROME)}.json") }
                ActionRow(Icons.Filled.Restore, "Ripristina backup") { pickBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            }
        }
    }

    if (fullscreen && qr != null) {
        Dialog(onDismissRequest = { fullscreen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            MaxBrightness()
            Box(Modifier.fillMaxSize().background(Color.White).clickable { fullscreen = false }, contentAlignment = Alignment.Center) {
                Image(qr.asImageBitmap(), contentDescription = "QR a tutto schermo", modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(16.dp))
            }
        }
    }
    if (pasting) {
        val clip = remember {
            context.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        }
        var text by remember { mutableStateOf(if (clip.contains("PORTAFUORI1:") || clip.trimStart().startsWith("{")) clip else "") }
        AlertDialog(
            onDismissRequest = { pasting = false },
            title = { Text("Incolla il codice") },
            text = { OutlinedTextField(text, { text = it.take(Limits.IMPORT_BYTES) }, minLines = 4, maxLines = 8, placeholder = { Text("PORTAFUORI1:…") }) },
            confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { pasting = false; vm.importText(text) }) { Text("Importa") } },
            dismissButton = { TextButton(onClick = { pasting = false }) { Text("Annulla") } },
        )
    }
}

/** A quiet action: icon and words on one tappable row. */
@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun MaxBrightness() {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(Unit) {
        val window = activity.window
        val old = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }
        onDispose { window.attributes = window.attributes.apply { screenBrightness = old } }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}


private fun shareFile(context: Context, file: File, mime: String, text: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Condividi il calendario"))
}

private fun shareDir(context: Context) = File(context.cacheDir, "share").apply { mkdirs() }

private fun safeName(b: ProfileBundle) = b.profile.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "casa" }

private fun shareQrImage(context: Context, qr: Bitmap, b: ProfileBundle, payload: String) {
    val file = File(shareDir(context), "portafuori-${safeName(b)}.png")
    file.outputStream().use { qr.compress(Bitmap.CompressFormat.PNG, 100, it) }
    shareFile(context, file, "image/png", "Calendario della raccolta «${b.profile.name}». Aprilo con ${Brand.NAME}: Importa › Da un'immagine.\n\n$payload")
}

private fun shareJsonFile(context: Context, b: ProfileBundle, json: String) {
    val file = File(shareDir(context), "portafuori-${safeName(b)}.json")
    file.writeText(json)
    shareFile(context, file, "application/json", "Calendario della raccolta «${b.profile.name}» per ${Brand.NAME}.")
}

@Composable
fun ScanScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val found = remember { AtomicBoolean(false) }
    // the frames are read off the main thread; the thread ends with the screen
    val analyzer = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(analyzer) { onDispose { analyzer.shutdown() } }

    Page(title = "Scansiona il QR", onBack = { vm.back() }) {
        item { Text("Inquadra il QR mostrato dal vicino.", style = MaterialTheme.typography.bodyLarge) }
        item {
            if (!granted) {
                Card {
                    Text("Serve la fotocamera solo per leggere il QR. Nessuna foto viene salvata.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { ask.launch(Manifest.permission.CAMERA) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Consenti fotocamera") }
                }
            } else {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.large).background(Color.Black),
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val view = PreviewView(ctx)
                            val providerFuture = ProcessCameraProvider.getInstance(ctx)
                            providerFuture.addListener({
                                val provider = providerFuture.get()
                                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                                analysis.setAnalyzer(analyzer) { image ->
                                    try {
                                        if (!found.get()) {
                                            val plane = image.planes[0]
                                            val bytes = ByteArray(plane.buffer.remaining()).also { plane.buffer.get(it) }
                                            val source = PlanarYUVLuminanceSource(bytes, plane.rowStride, image.height, 0, 0, image.width, image.height, false)
                                            val text = Qr.decode(source)
                                            if (text != null && found.compareAndSet(false, true)) {
                                                ContextCompat.getMainExecutor(ctx).execute {
                                                    provider.unbindAll()
                                                    vm.back()
                                                    vm.importText(text)
                                                }
                                            }
                                        }
                                    } finally {
                                        image.close()
                                    }
                                }
                                provider.unbindAll()
                                provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                            }, ContextCompat.getMainExecutor(ctx))
                            view
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ImportPreviewScreen(vm: MainViewModel, state: UiState) {
    val env = vm.pendingImport ?: run { vm.back(); return }
    val today = LocalDate.now(ROME)
    // Decided by which button the user pressed, never by the file
    val isBackup = vm.pendingRestore
    val previews = remember(env, isBackup) {
        runCatching { env.profiles.map { ShareCodec.fromDto(it, trustReminders = isBackup).previewBundle() } }.getOrNull()
    }
    if (previews == null) {
        LaunchedEffect(Unit) { vm.message = "Questo calendario non è valido."; vm.cancelImport() }
        return
    }
    // replacing drops data that cannot come back: one more question first
    var replaceId by rememberSaveable { mutableStateOf<Long?>(null) }
    var replaceAll by rememberSaveable { mutableStateOf(false) }
    Page(title = if (isBackup) "Ripristina backup" else "Importa calendario", onBack = { vm.cancelImport() }) {
        item {
            Text(
                "Esportato il " + It.full(Instant.ofEpochMilli(env.exportedAt.coerceIn(0, 4102444800000)).atZone(ROME).toLocalDate()) +
                    if (env.profiles.size > 1) " · ${env.profiles.size} case" else "",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (env.kind == "backup" && !isBackup) {
            item {
                Text(
                    "È un backup: da qui importi solo i calendari. Per ripristinare anche gli orari degli avvisi usa «Condividi e backup › Ripristina backup».",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        items(previews) { pb ->
            Card {
                Text(pb.profile.name, style = MaterialTheme.typography.titleLarge)
                if (pb.profile.areaNote.isNotBlank()) Text(pb.profile.areaNote, style = MaterialTheme.typography.bodySmall)
                if (pb.profile.notes.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Note incluse", style = MaterialTheme.typography.labelMedium)
                    Text(pb.profile.notes, style = MaterialTheme.typography.bodySmall)
                }
                if (isBackup) {
                    val p = pb.profile
                    Text(
                        "Avvisi: porta fuori " + (if (p.exposeReminderOn) It.time(p.exposeReminderAt) else "spento") +
                            " · prepara " + (if (p.prepareReminderOn) It.time(p.prepareReminderAt) else "spento") +
                            " · ritira " + (if (p.retrieveReminderOn) It.time(p.retrieveReminderAt) else "spento"),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                pb.bins.forEach { bin ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        BinBadge(bin.entity.colorArgb, bin.entity.iconKey, 26.dp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(bin.name, style = MaterialTheme.typography.titleSmall)
                            Text(It.describeAll(bin.rules), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (pb.exceptions.isNotEmpty()) Text("${pb.exceptions.size} modifiche ai singoli giorni", style = MaterialTheme.typography.bodySmall)
            }
            if (!isBackup) {
                SectionTitle("Prossime 4 settimane")
                Route {
                    pb.upcomingEvenings(today, today.plusDays(27)).forEach { e ->
                        RouteStop {
                            Text(
                                It.weekdayDay(e.window.start.toLocalDate()) + if (e.window.start.hour >= 12) " sera" else " mattina",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(Wording.ritiro(e.collectionDate), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            StopBins(e.bins)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
        val enabled = !vm.importing
        if (isBackup) {
            if (state.bundles.isNotEmpty()) {
                item { Button(enabled = enabled, onClick = { replaceAll = true }, modifier = Modifier.fillMaxWidth()) { Text("Sostituisci tutto con il backup") } }
                item { OutlinedButton(enabled = enabled, onClick = { vm.confirmImport(null, replaceAll = false) }, modifier = Modifier.fillMaxWidth()) { Text("Aggiungi alle case attuali") } }
            } else {
                item { Button(enabled = enabled, onClick = { vm.confirmImport(null, replaceAll = false) }, modifier = Modifier.fillMaxWidth()) { Text("Ripristina") } }
            }
        } else {
            item {
                Button(enabled = enabled, onClick = { vm.confirmImport(null, replaceAll = false) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (previews.size > 1) "Crea ${previews.size} nuove case" else "Crea una nuova casa")
                }
            }
            // Replacing needs a house to replace: with none, neither the buttons nor their note make sense
            if (previews.size == 1 && state.bundles.isNotEmpty()) {
                items(state.bundles, key = { it.profile.id }) { b ->
                    OutlinedButton(enabled = enabled, onClick = { replaceId = b.profile.id }, modifier = Modifier.fillMaxWidth()) {
                        Text("Sostituisci il calendario di «${b.profile.name}»")
                    }
                }
                item {
                    Text(
                        "Sostituendo, la tua casa tiene nome, note e orari degli avvisi: cambiano solo bidoni e giorni.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Text(
                "Controlla sempre che i giorni coincidano con il calendario ufficiale del tuo comune.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    state.bundles.firstOrNull { it.profile.id == replaceId }?.let { b ->
        AlertDialog(
            onDismissRequest = { replaceId = null },
            title = { Text("Sostituire il calendario di «${b.profile.name}»?") },
            text = { Text("Bidoni e giorni di adesso saranno sostituiti da quelli del file. Non si può annullare.") },
            confirmButton = { TextButton(onClick = { replaceId = null; vm.confirmImport(b.profile.id, replaceAll = false) }) { Text("Sostituisci") } },
            dismissButton = { TextButton(onClick = { replaceId = null }) { Text("Annulla") } },
        )
    }
    if (replaceAll) {
        AlertDialog(
            onDismissRequest = { replaceAll = false },
            title = { Text("Sostituire tutto con il backup?") },
            text = { Text("Le case di adesso saranno sostituite da quelle del backup. Non si può annullare.") },
            confirmButton = { TextButton(onClick = { replaceAll = false; vm.confirmImport(null, replaceAll = true) }) { Text("Sostituisci") } },
            dismissButton = { TextButton(onClick = { replaceAll = false }) { Text("Annulla") } },
        )
    }
}
