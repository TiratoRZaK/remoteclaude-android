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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rclaude.protocol.TerminalKeys
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

private const val ZOOM_STEP = 1.25f
private const val MIN_ZOOM = 0.5f
private const val MAX_ZOOM = 5f

/**
 * Экран сессии: чат и терминал поверх одного соединения. Вид переключается кнопкой в
 * панели клавиш, панель и композер общие для обоих видов.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    state: StateFlow<SessionUiState>,
    onPrompt: (String) -> Unit,
    onKey: (String) -> Unit,
    onPaste: (String) -> Unit,
    onBack: () -> Unit,
) {
    val ui by state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    var view by rememberSaveable { mutableStateOf(SessionView.CHAT) }
    var viewChosen by remember { mutableStateOf(false) }
    var prompt by rememberSaveable { mutableStateOf("") }
    var liveInput by rememberSaveable { mutableStateOf(false) }
    // Лупа увеличивает текст только на телефоне: размер PTY остаётся за вкладкой
    // терминала на компьютере, трогать его из приложения нельзя.
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var seenEvents by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(ui.loaded, ui.chatAvailable) {
        if (ui.loaded && !viewChosen) {
            view = if (ui.chatAvailable) SessionView.CHAT else SessionView.TERMINAL
            viewChosen = true
        }
    }
    LaunchedEffect(view, ui.events.size) {
        if (view == SessionView.CHAT) seenEvents = ui.events.size
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
                            text = buildString {
                                append(if (view == SessionView.CHAT) "чат" else "терминал")
                                append(" · ${ui.cols}×${ui.rows}")
                                if (!ui.connected) append(" · нет связи")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } },
                actions = {
                    if (view == SessionView.TERMINAL) {
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
                val status = ui.status
                if (status != null) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
                KeyPanel(
                    view = view,
                    menuWaiting = ui.menuWaiting,
                    unreadChat = ui.events.size > seenEvents,
                    enabled = ui.connected,
                    onKey = { sequence ->
                        if (liveInput) prompt = prompt.afterKey(sequence)
                        onKey(sequence)
                    },
                    onPaste = {
                        val text = clipboard.getText()?.text
                        if (!text.isNullOrEmpty()) onPaste(text)
                    },
                    onSwitchView = {
                        view = if (view == SessionView.CHAT) SessionView.TERMINAL else SessionView.CHAT
                    },
                )
                Composer(
                    value = prompt,
                    liveInput = liveInput,
                    enabled = ui.connected,
                    onValueChange = { text ->
                        if (liveInput) onKey(inputDelta(prompt, text))
                        prompt = text
                    },
                    onToggleMode = {
                        liveInput = !liveInput
                        prompt = ""
                    },
                    onSend = {
                        if (liveInput) onKey(TerminalKeys.ENTER) else onPrompt(prompt)
                        prompt = ""
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (view == SessionView.CHAT) {
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

/**
 * Что отправить в PTY, чтобы он повторил правку поля ввода: лишние символы стираются
 * забоем, новый хвост уходит как есть. Нужно только в режиме живого ввода.
 */
internal fun inputDelta(before: String, after: String): String {
    var common = 0
    while (common < before.length && common < after.length && before[common] == after[common]) common++
    val erase = TerminalKeys.BACKSPACE.repeat(before.length - common)
    return erase + after.substring(common)
}

/**
 * Как меняется локальное поле, когда клавиша панели правит строку ввода в PTY:
 * забой стирает символ, а Esc, Ctrl+C, Clear и Enter опустошают строку и там, и тут.
 */
internal fun String.afterKey(sequence: String): String = when (sequence) {
    TerminalKeys.BACKSPACE -> dropLast(1)
    TerminalKeys.CLEAR, TerminalKeys.ENTER, TerminalKeys.ESC, TerminalKeys.CTRL_C -> ""
    else -> this
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
private fun Composer(
    value: String,
    liveInput: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            onClick = onToggleMode,
            shape = MaterialTheme.shapes.small,
            color = if (liveInput) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (liveInput) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (liveInput) "живой" else "пакет",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(if (liveInput) "Пишу сразу в терминал…" else "Промпт для Claude…")
            },
            minLines = 1,
            maxLines = 5,
        )
        FilledTonalButton(
            onClick = onSend,
            enabled = enabled && (liveInput || value.isNotBlank()),
            contentPadding = PaddingValues(horizontal = 14.dp),
        ) {
            Text("↵", style = MaterialTheme.typography.titleMedium)
        }
    }
}
