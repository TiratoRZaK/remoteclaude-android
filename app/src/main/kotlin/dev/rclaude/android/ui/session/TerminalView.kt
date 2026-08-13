package dev.rclaude.android.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import dev.rclaude.protocol.term.TerminalLine
import dev.rclaude.protocol.term.TerminalSnapshot

/** Размеры отрисовки терминала и геометрия, которую можно запросить у сервера. */
private data class TerminalMetrics(
    val fontSize: TextUnit,
    val lineHeight: TextUnit,
    val viewportCols: Int,
    val viewportRows: Int,
)

/**
 * Экран терминала: строки снимка эмулятора моноширинным шрифтом, кегль подобран так,
 * чтобы вся ширина сессии влезала в экран. Прокрутка следует за выводом, пока список
 * прижат к низу.
 */
@Composable
fun TerminalView(
    snapshot: TerminalSnapshot,
    onViewportMeasured: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.background(TerminalPalette.Background)) {
        val metrics = remember(maxWidth, maxHeight, snapshot.cols, density) {
            measureMetrics(measurer, density, maxWidth, maxHeight, snapshot.cols)
        }
        LaunchedEffect(metrics.viewportCols, metrics.viewportRows) {
            onViewportMeasured(metrics.viewportCols, metrics.viewportRows)
        }

        val listState = rememberLazyListState()
        // Прокрутка следует за выводом, пока пользователь сам не отлистал вверх:
        // признак снимается по завершении жеста, а не по приходу новых строк.
        var pinned by remember { mutableStateOf(true) }
        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
                if (!scrolling) pinned = !listState.canScrollForward
            }
        }
        LaunchedEffect(snapshot.revision) {
            if (pinned && snapshot.lines.isNotEmpty()) listState.scrollToItem(snapshot.lines.lastIndex)
        }

        val style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = metrics.fontSize,
            lineHeight = metrics.lineHeight,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        ) {
            items(count = snapshot.lines.size) { index ->
                Text(
                    text = snapshot.lines[index].toAnnotated(),
                    style = style,
                    color = TerminalPalette.Foreground,
                    softWrap = false,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun TerminalLine.toAnnotated(): AnnotatedString = buildAnnotatedString {
    for (run in runs) {
        val (foreground, background) = TerminalPalette.resolve(run.style)
        withStyle(
            SpanStyle(
                color = foreground,
                background = if (background == TerminalPalette.Background) Color.Unspecified else background,
                fontWeight = if (run.style.bold) FontWeight.Bold else null,
                fontStyle = if (run.style.italic) FontStyle.Italic else null,
                textDecoration = if (run.style.underline) TextDecoration.Underline else null,
            ),
        ) {
            append(run.text)
        }
    }
}

private fun measureMetrics(
    measurer: TextMeasurer,
    density: Density,
    maxWidth: Dp,
    maxHeight: Dp,
    cols: Int,
): TerminalMetrics {
    val probeSize = 14f
    val probe = measurer.measure(
        text = AnnotatedString(PROBE_TEXT),
        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = probeSize.sp),
    )
    val charWidthPx = probe.size.width.toFloat() / PROBE_TEXT.length
    val lineHeightPx = probe.size.height.toFloat()
    val widthPx = with(density) { maxWidth.toPx() }
    val heightPx = with(density) { maxHeight.toPx() }

    val fitted = if (charWidthPx <= 0f || cols <= 0) {
        probeSize
    } else {
        (probeSize * widthPx / (charWidthPx * cols)).coerceIn(MIN_FONT_SIZE, probeSize)
    }
    val comfortableCharWidth = charWidthPx * COMFORTABLE_FONT_SIZE / probeSize
    val comfortableLineHeight = lineHeightPx * COMFORTABLE_FONT_SIZE / probeSize
    val viewportCols = if (comfortableCharWidth > 0f) {
        (widthPx / comfortableCharWidth).toInt().coerceAtLeast(MIN_COLS)
    } else {
        MIN_COLS
    }
    val viewportRows = if (comfortableLineHeight > 0f) {
        (heightPx / comfortableLineHeight).toInt().coerceAtLeast(MIN_ROWS)
    } else {
        MIN_ROWS
    }
    return TerminalMetrics(
        fontSize = fitted.sp,
        lineHeight = (fitted * 1.25f).sp,
        viewportCols = viewportCols,
        viewportRows = viewportRows,
    )
}

private const val PROBE_TEXT = "WWWWWWWWWWWWWWWWWWWW"
private const val MIN_FONT_SIZE = 5.5f
private const val COMFORTABLE_FONT_SIZE = 13f
private const val MIN_COLS = 20
private const val MIN_ROWS = 8
