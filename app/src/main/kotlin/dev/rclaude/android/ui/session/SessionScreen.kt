package dev.rclaude.android.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rclaude.protocol.TerminalKeys
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

private const val TAB_CHAT = 0
private const val TAB_TERMINAL = 1
private const val ZOOM_STEP = 1.25f
private const val MIN_ZOOM = 0.5f
private const val MAX_ZOOM = 5f

/** Экран сессии: чат и терминал поверх одного соединения, общий композер. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    state: StateFlow<SessionUiState>,
    onPrompt: (String) -> Unit,
    onKey: (String) -> Unit,
    onBack: () -> Unit,
) {
    val ui by state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(TAB_CHAT) }
    var tabChosen by remember { mutableStateOf(false) }
    var prompt by remember { mutableStateOf("") }
    // Лупа увеличивает текст только на телефоне: размер PTY остаётся за вкладкой
    // терминала на компьютере, трогать его из приложения нельзя.
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }

    LaunchedEffect(ui.loaded, ui.chatAvailable) {
        if (ui.loaded && !tabChosen) {
            tab = if (ui.chatAvailable) TAB_CHAT else TAB_TERMINAL
            tabChosen = true
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text(
                            text = ui.name.ifEmpty { "Сессия" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${ui.cols}×${ui.rows}${if (ui.connected) "" else " · нет связи"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } },
                actions = {
                    if (tab == TAB_TERMINAL) {
                        ZoomControls(
                            zoom = zoom,
                            onZoom = { value -> zoom = value.coerceIn(MIN_ZOOM, MAX_ZOOM) },
                        )
                    }
                },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
            ) {
                HorizontalDivider()
                if (ui.menuWaiting) {
                    MenuPlaque(onOpenTerminal = { tab = TAB_TERMINAL })
                }
                val status = ui.status
                if (status != null) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
                if (tab == TAB_TERMINAL) {
                    KeyBar(onKey = onKey, enabled = ui.connected)
                }
                Composer(
                    value = prompt,
                    onValueChange = { prompt = it },
                    enabled = ui.connected,
                    onSend = {
                        onPrompt(prompt)
                        prompt = ""
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == TAB_CHAT,
                    onClick = { tab = TAB_CHAT },
                    text = { Text("Чат") },
                )
                Tab(
                    selected = tab == TAB_TERMINAL,
                    onClick = { tab = TAB_TERMINAL },
                    text = { Text("Терминал") },
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (tab == TAB_CHAT) {
                    ChatFeed(
                        events = ui.events,
                        note = ui.chatNote,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    TerminalView(
                        snapshot = ui.snapshot,
                        zoom = zoom,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/** Лупа терминала: шаг увеличения кнопками, тап по проценту возвращает «вписано». */
@Composable
private fun ZoomControls(zoom: Float, onZoom: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = { onZoom(zoom / ZOOM_STEP) },
            enabled = zoom > MIN_ZOOM,
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        TextButton(
            onClick = { onZoom(1f) },
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            Text("${(zoom * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        TextButton(
            onClick = { onZoom(zoom * ZOOM_STEP) },
            enabled = zoom < MAX_ZOOM,
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun MenuPlaque(onOpenTerminal: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Claude ждёт ответа в терминале",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        TextButton(onClick = onOpenTerminal) { Text("Открыть") }
    }
}

@Composable
private fun KeyBar(onKey: (String) -> Unit, enabled: Boolean) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(count = TerminalKeys.QUICK_KEYS.size) { index ->
            val key = TerminalKeys.QUICK_KEYS[index]
            FilledTonalButton(
                onClick = { onKey(key.sequence) },
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(text = key.label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Промпт для Claude…") },
            minLines = 1,
            maxLines = 5,
        )
        FilledTonalButton(
            onClick = onSend,
            enabled = enabled && value.isNotBlank(),
        ) {
            Text("Отправить")
        }
    }
}
