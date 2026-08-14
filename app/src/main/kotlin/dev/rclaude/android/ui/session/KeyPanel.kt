package dev.rclaude.android.ui.session

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.rclaude.android.R
import dev.rclaude.protocol.TerminalKeys

/** Что показывает экран сессии. */
enum class SessionView { CHAT, TERMINAL }

private val KEY_MIN_HEIGHT = 34.dp
private val KEY_MIN_WIDTH = 36.dp

/**
 * Постоянная панель клавиш: управляющие последовательности уходят прямо в PTY.
 *
 * Первая кнопка переключает вид. Из чата это значок терминала — он загорается и
 * мигает, когда Claude ждёт ответа в меню; из терминала это значок сообщения с
 * точкой, если в ленте появились новые реплики. Клавиши намеренно плотнее обычных
 * кнопок: это клавиатура, а не набор отдельных действий.
 */
@Composable
fun KeyPanel(
    view: SessionView,
    menuWaiting: Boolean,
    unreadChat: Boolean,
    enabled: Boolean,
    onKey: (String) -> Unit,
    onPaste: () -> Unit,
    onSwitchView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides KEY_MIN_HEIGHT) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            KeyRow {
                ViewSwitchKey(
                    view = view,
                    menuWaiting = menuWaiting,
                    unreadChat = unreadChat,
                    onClick = onSwitchView,
                )
                TextKey("Esc", enabled) { onKey(TerminalKeys.ESC) }
                TextKey("⇧Tab", enabled) { onKey(TerminalKeys.SHIFT_TAB) }
                TextKey("↑", enabled) { onKey(TerminalKeys.UP) }
                TextKey("↓", enabled) { onKey(TerminalKeys.DOWN) }
                TextKey("←", enabled) { onKey(TerminalKeys.LEFT) }
                TextKey("→", enabled) { onKey(TerminalKeys.RIGHT) }
                IconKey(R.drawable.ic_key_backspace, "Забой", enabled) {
                    onKey(TerminalKeys.BACKSPACE)
                }
            }
            KeyRow {
                TextKey("Enter", enabled) { onKey(TerminalKeys.ENTER) }
                TextKey("/", enabled) { onKey("/") }
                TextKey("@", enabled) { onKey("@") }
                for (digit in 1..5) {
                    TextKey(digit.toString(), enabled) { onKey(digit.toString()) }
                }
            }
            KeyRow {
                IconKey(R.drawable.ic_key_stop, "Ctrl+C — прервать", enabled) {
                    onKey(TerminalKeys.CTRL_C)
                }
                IconKey(R.drawable.ic_key_paste, "Вставить из буфера", enabled, onClick = onPaste)
                IconKey(R.drawable.ic_key_repeat, "Ctrl+R — подробности", enabled) {
                    onKey(TerminalKeys.CTRL_R)
                }
                IconKey(R.drawable.ic_key_tasks, "Ctrl+T — задачи", enabled) {
                    onKey(TerminalKeys.CTRL_T)
                }
                IconKey(R.drawable.ic_key_background, "Ctrl+B — в фон", enabled) {
                    onKey(TerminalKeys.CTRL_B)
                }
                IconKey(R.drawable.ic_key_clear, "Очистить ввод", enabled) {
                    onKey(TerminalKeys.CLEAR)
                }
            }
        }
    }
}

/** Группа клавиш: не влезающие по ширине переносятся на следующую строку. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyRow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        content = content,
    )
}

@Composable
private fun TextKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    KeyShell(enabled = enabled, onClick = onClick) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun IconKey(
    icon: Int,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    KeyShell(enabled = enabled, onClick = onClick) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun KeyShell(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Box(
            modifier = Modifier
                .sizeIn(minWidth = KEY_MIN_WIDTH, minHeight = KEY_MIN_HEIGHT)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun ViewSwitchKey(
    view: SessionView,
    menuWaiting: Boolean,
    unreadChat: Boolean,
    onClick: () -> Unit,
) {
    val waiting = view == SessionView.CHAT && menuWaiting
    val blink by rememberInfiniteTransition(label = "мигание").animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 650), RepeatMode.Reverse),
        label = "прозрачность",
    )
    val icon = if (view == SessionView.CHAT) R.drawable.ic_view_terminal else R.drawable.ic_view_chat
    val description = if (view == SessionView.CHAT) "Открыть терминал" else "Открыть чат"

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (waiting) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = if (waiting) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        modifier = Modifier.alpha(if (waiting) blink else 1f),
    ) {
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 42.dp, minHeight = KEY_MIN_HEIGHT)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = description,
                modifier = Modifier.size(19.dp),
            )
            if (view == SessionView.TERMINAL && unreadChat) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape),
                )
            }
        }
    }
}
