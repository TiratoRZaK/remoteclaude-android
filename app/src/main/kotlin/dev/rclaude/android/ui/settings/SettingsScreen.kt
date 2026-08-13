package dev.rclaude.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rclaude.android.ui.theme.AppStyle
import dev.rclaude.android.ui.theme.spec
import kotlinx.coroutines.flow.StateFlow

/** Экран настроек: подключение и галерея оформлений с живым предпросмотром. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: StateFlow<SettingsUiState>,
    onPickStyle: (AppStyle) -> Unit,
    onEditConnection: () -> Unit,
    onBack: () -> Unit,
) {
    val ui by state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Подключение", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = ui.server ?: "не настроено",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = onEditConnection) { Text("Изменить подключение") }
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                    Text(
                        text = "Оформление",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = "Шрифты, палитра, скругления, фон и раскраска терминала — тапни, чтобы примерить.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(count = AppStyle.entries.size) { index ->
                val style = AppStyle.entries[index]
                StyleCard(
                    style = style,
                    selected = style == ui.style,
                    onPick = { onPickStyle(style) },
                )
            }
        }
    }
}

/** Карточка оформления, нарисованная его же палитрой и шрифтами. */
@Composable
private fun StyleCard(style: AppStyle, selected: Boolean, onPick: () -> Unit) {
    val spec = remember(style) { style.spec() }
    MaterialTheme(
        colorScheme = spec.colorScheme,
        typography = spec.typography,
        shapes = spec.shapes,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(spec.shapes.large)
                .background(spec.background)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) spec.colorScheme.primary else spec.colorScheme.outline,
                    shape = spec.shapes.large,
                )
                .clickable(onClick = onPick)
                .padding(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = style.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = spec.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.titleLarge,
                            color = spec.colorScheme.primary,
                        )
                    }
                }
                Text(
                    text = style.tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = spec.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "привет",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .background(spec.colorScheme.primaryContainer, spec.shapes.small)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                    Text(
                        text = "Bash · npm test",
                        style = MaterialTheme.typography.labelSmall,
                        color = spec.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "❯ 76 tests passed",
                    fontFamily = spec.terminal.fontFamily,
                    fontSize = 12.sp,
                    color = spec.terminal.foreground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(spec.terminal.background, spec.shapes.extraSmall)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
