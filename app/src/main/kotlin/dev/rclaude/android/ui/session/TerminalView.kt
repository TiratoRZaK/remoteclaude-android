package dev.rclaude.android.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
import dev.rclaude.android.ui.theme.LocalTerminalSkin
import dev.rclaude.android.ui.theme.TerminalSkin
import dev.rclaude.protocol.term.TerminalLine
import dev.rclaude.protocol.term.TerminalSnapshot

/** Кегль и ширина полотна терминала при текущем увеличении. */
private data class TerminalMetrics(
    val fontSize: TextUnit,
    val lineHeight: TextUnit,
    val contentWidth: Dp,
)

/**
 * Экран терминала: строки снимка эмулятора моноширинным шрифтом.
 *
 * При увеличении [zoom] равном единице вся ширина сессии вписана в экран, дальше
 * строки становятся крупнее и полотно листается вбок. Размер PTY при этом не
 * меняется — им владеет вкладка терминала на компьютере.
 */
@Composable
fun TerminalView(
    snapshot: TerminalSnapshot,
    zoom: Float,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val skin = LocalTerminalSkin.current

    BoxWithConstraints(modifier = modifier.background(skin.background)) {
        val viewportWidth = maxWidth
        val metrics = remember(viewportWidth, snapshot.cols, density, skin.fontFamily, zoom) {
            measureMetrics(measurer, density, viewportWidth, snapshot.cols, skin.fontFamily, zoom)
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
            fontFamily = skin.fontFamily,
            fontSize = metrics.fontSize,
            lineHeight = metrics.lineHeight,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .width(maxOf(metrics.contentWidth, viewportWidth)),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        ) {
            items(count = snapshot.lines.size) { index ->
                Text(
                    text = snapshot.lines[index].toAnnotated(skin),
                    style = style,
                    color = skin.foreground,
                    softWrap = false,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun TerminalLine.toAnnotated(skin: TerminalSkin): AnnotatedString = buildAnnotatedString {
    for (run in runs) {
        val (foreground, background) = skin.resolve(run.style)
        withStyle(
            SpanStyle(
                color = foreground,
                background = if (background == skin.background) Color.Unspecified else background,
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
    viewportWidth: Dp,
    cols: Int,
    fontFamily: FontFamily,
    zoom: Float,
): TerminalMetrics {
    val probe = measurer.measure(
        text = AnnotatedString(PROBE_TEXT),
        style = TextStyle(fontFamily = fontFamily, fontSize = PROBE_SIZE.sp),
    )
    val probeCharWidthPx = probe.size.width.toFloat() / PROBE_TEXT.length
    val widthPx = with(density) { viewportWidth.toPx() }

    val fitted = if (probeCharWidthPx <= 0f || cols <= 0) {
        PROBE_SIZE
    } else {
        PROBE_SIZE * widthPx / (probeCharWidthPx * cols)
    }
    val fontSize = (fitted * zoom).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
    val charWidthPx = probeCharWidthPx * fontSize / PROBE_SIZE
    val contentWidth = with(density) { (charWidthPx * cols).toDp() }
    return TerminalMetrics(
        fontSize = fontSize.sp,
        lineHeight = (fontSize * LINE_HEIGHT_FACTOR).sp,
        contentWidth = contentWidth,
    )
}

private const val PROBE_TEXT = "WWWWWWWWWWWWWWWWWWWW"
private const val PROBE_SIZE = 14f
private const val MIN_FONT_SIZE = 4f
private const val MAX_FONT_SIZE = 36f
private const val LINE_HEIGHT_FACTOR = 1.25f
