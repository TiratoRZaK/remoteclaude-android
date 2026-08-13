package dev.rclaude.android.ui.connect

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.flow.StateFlow

/** Экран подключения: ссылка из `rclaude qr`, токен, проверка сервера. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    state: StateFlow<ConnectUiState>,
    onLinkChanged: (String) -> Unit,
    onTokenChanged: (String) -> Unit,
    onScanned: (String) -> Unit,
    onCheck: () -> Unit,
    onSave: () -> Unit,
    onSavedHandled: () -> Unit,
    onSaved: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val ui by state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let(onScanned)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) scanLauncher.launch(scanOptions())
    }
    val startScan: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) scanLauncher.launch(scanOptions()) else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(ui.saved) {
        if (ui.saved) {
            onSaved()
            onSavedHandled()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Подключение") },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text("←") }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Отсканируй QR из «rclaude qr» или вставь ссылку целиком — токен возьмётся из неё.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = ui.link,
                onValueChange = onLinkChanged,
                label = { Text("Ссылка сервера") },
                placeholder = { Text("http://192.168.1.40:7777/#t=токен") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ui.token,
                onValueChange = onTokenChanged,
                label = { Text("Токен") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = startScan) { Text("Сканировать QR") }
                OutlinedButton(onClick = onCheck, enabled = !ui.checking) {
                    Text(if (ui.checking) "Проверяю…" else "Проверить")
                }
            }
            val message = ui.message
            if (message != null) {
                Text(
                    text = message,
                    color = if (ui.messageIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            Button(
                onClick = onSave,
                enabled = ui.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить и продолжить")
            }
        }
    }
}

private fun scanOptions(): ScanOptions = ScanOptions().apply {
    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
    setPrompt("Наведи на QR из rclaude qr")
    setBeepEnabled(false)
    setOrientationLocked(false)
}
