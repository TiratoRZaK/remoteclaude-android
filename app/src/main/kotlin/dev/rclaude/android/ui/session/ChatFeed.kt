package dev.rclaude.android.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.rclaude.protocol.ChatEvent
import dev.rclaude.protocol.ChatKind
import kotlinx.coroutines.launch

/**
 * Чат-лента: реплики пользователя справа, ответы Claude слева, вызовы инструментов —
 * узкой строкой. Прокрутка идёт за новыми сообщениями, пока лента прижата к низу.
 */
@Composable
fun ChatFeed(
    events: List<ChatEvent>,
    note: String?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var pinned by remember { mutableStateOf(true) }

    // Лента прижата к низу, пока пользователь сам не отлистал вверх: признак
    // снимается по завершении жеста, а не по приходу новых сообщений.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) pinned = !listState.canScrollForward
        }
    }
    val itemCount = events.size + if (note != null) 1 else 0
    LaunchedEffect(itemCount) {
        if (pinned && itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (note != null) {
                item {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(count = events.size) { index -> ChatRow(events[index]) }
        }
        if (!pinned && itemCount > 0) {
            FilledTonalButton(
                onClick = { scope.launch { listState.animateScrollToItem(itemCount - 1) } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            ) {
                Text("↓ вниз")
            }
        }
    }
}

@Composable
private fun ChatRow(event: ChatEvent) {
    when (event.kind) {
        ChatKind.TOOL -> Text(
            text = event.display,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )

        ChatKind.USER -> Bubble(
            text = event.display,
            background = MaterialTheme.colorScheme.primaryContainer,
            foreground = MaterialTheme.colorScheme.onPrimaryContainer,
            alignment = Alignment.End,
        )

        ChatKind.ASSISTANT -> Bubble(
            text = event.display,
            background = MaterialTheme.colorScheme.surfaceVariant,
            foreground = MaterialTheme.colorScheme.onSurfaceVariant,
            alignment = Alignment.Start,
        )
    }
}

@Composable
private fun Bubble(
    text: String,
    background: Color,
    foreground: Color,
    alignment: Alignment.Horizontal,
) {
    val fromUser = alignment == Alignment.End
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (fromUser) 40.dp else 0.dp, end = if (fromUser) 0.dp else 40.dp),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = foreground,
            modifier = Modifier
                .background(color = background, shape = RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
