package com.example.id

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt

// ── Palette ───────────────────────────────────────────────────────────────────

private val Paper     = Color(0xFFF6EFDF)
private val Ink       = Color(0xFF141414)
private val PopYellow = Color(0xFFFFC700)
private val PopPink   = Color(0xFFFF90E8)
private val PopRed    = Color(0xFFFF4D2E)
private val PopOrange = Color(0xFFFF9F1C)
private val PopGreen  = Color(0xFF2EC27E)
private val PopBlue   = Color(0xFF4D9DE0)
private val PopPurple = Color(0xFF8A4FFF)

private val presetColors = listOf(
    Ink, PopRed, PopOrange, PopYellow, PopGreen, PopBlue, PopPurple, PopPink
)

// ── Model ─────────────────────────────────────────────────────────────────────

private class BrushStroke(val color: Color, val widthDp: Float) {
    val points: SnapshotStateList<Offset> = mutableStateListOf()
}

private enum class PaintScreen { CANVAS, COLOR_PICKER }

// ── Neo-brutalist building blocks ─────────────────────────────────────────────

private fun Modifier.hardShadow(corner: Dp, offset: Dp = 5.dp) = drawBehind {
    drawRoundRect(
        color = Ink,
        topLeft = Offset(offset.toPx(), offset.toPx()),
        size = size,
        cornerRadius = CornerRadius(corner.toPx())
    )
}

@Composable
private fun Sticker(text: String, fill: Color, textColor: Color, tilt: Float) {
    Box(
        modifier = Modifier
            .rotate(tilt)
            .hardShadow(corner = 8.dp, offset = 4.dp)
            .background(fill, RoundedCornerShape(8.dp))
            .border(3.dp, Ink, RoundedCornerShape(8.dp))
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun NeoButton(
    label: String,
    fill: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Ink,
    swatch: Color? = null
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .hardShadow(corner = 12.dp)
            .background(fill, RoundedCornerShape(12.dp))
            .border(3.dp, Ink, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        if (swatch != null) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .border(2.dp, Ink, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
        )
    }
}

// ── Root ──────────────────────────────────────────────────────────────────────

@Composable
fun PaintingApp() {
    var screen by remember { mutableStateOf(PaintScreen.CANVAS) }
    var brushColor by remember { mutableStateOf(Ink) }
    var brushWidth by remember { mutableFloatStateOf(6f) }
    val strokes = remember { mutableStateListOf<BrushStroke>() }

    when (screen) {
        PaintScreen.CANVAS -> CanvasScreen(
            strokes = strokes,
            brushColor = brushColor,
            brushWidth = brushWidth,
            onErase = { strokes.clear() },
            onSetColor = { screen = PaintScreen.COLOR_PICKER }
        )
        PaintScreen.COLOR_PICKER -> ColorPickerScreen(
            color = brushColor,
            onColorChange = { brushColor = it },
            width = brushWidth,
            onWidthChange = { brushWidth = it },
            onDone = { screen = PaintScreen.CANVAS }
        )
    }
}

// ── Screen 1: Canvas ──────────────────────────────────────────────────────────

@Composable
private fun CanvasScreen(
    strokes: SnapshotStateList<BrushStroke>,
    brushColor: Color,
    brushWidth: Float,
    onErase: () -> Unit,
    onSetColor: () -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Paper) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Sticker(text = "PAINT!", fill = Ink, textColor = Paper, tilt = -3f)
                BrushChip(color = brushColor, width = brushWidth)
            }

            Spacer(Modifier.height(20.dp))

            PaintCanvas(
                strokes = strokes,
                brushColor = brushColor,
                brushWidth = brushWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NeoButton(
                    label = "ERASE",
                    fill = PopPink,
                    onClick = onErase,
                    modifier = Modifier.weight(1f)
                )
                NeoButton(
                    label = "SET COLOR",
                    fill = PopYellow,
                    onClick = onSetColor,
                    modifier = Modifier.weight(1f),
                    swatch = brushColor
                )
            }
        }
    }
}

@Composable
private fun BrushChip(color: Color, width: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .hardShadow(corner = 8.dp, offset = 4.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(3.dp, Ink, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(min(width, 24f).coerceAtLeast(8f).dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Ink, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${width.roundToInt()}dp",
            color = Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun PaintCanvas(
    strokes: SnapshotStateList<BrushStroke>,
    brushColor: Color,
    brushWidth: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .hardShadow(corner = 16.dp, offset = 7.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(3.dp, Ink, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(brushColor, brushWidth) {
                // A plain press never crosses the drag touch slop, so dots need their own gesture
                detectTapGestures { offset ->
                    strokes.add(BrushStroke(brushColor, brushWidth).apply { points.add(offset) })
                }
            }
            .pointerInput(brushColor, brushWidth) {
                detectDragGestures(
                    onDragStart = { offset ->
                        strokes.add(BrushStroke(brushColor, brushWidth).apply { points.add(offset) })
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        strokes.lastOrNull()?.points?.add(change.position)
                    }
                )
            }
    ) {
        strokes.forEach { stroke ->
            val points = stroke.points
            if (points.size == 1) {
                drawCircle(color = stroke.color, radius = stroke.widthDp.dp.toPx() / 2f, center = points.first())
            } else if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        // Quadratic through midpoints keeps the line smooth
                        quadraticTo(prev.x, prev.y, (prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
                    }
                    lineTo(points.last().x, points.last().y)
                }
                drawPath(
                    path = path,
                    color = stroke.color,
                    style = Stroke(width = stroke.widthDp.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

// ── Screen 2: Color picker ────────────────────────────────────────────────────

@Composable
private fun ColorPickerScreen(
    color: Color,
    onColorChange: (Color) -> Unit,
    width: Float,
    onWidthChange: (Float) -> Unit,
    onDone: () -> Unit
) {
    BackHandler(onBack = onDone)

    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Paper) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Sticker(text = "INK LAB", fill = PopYellow, textColor = Ink, tilt = 2f)

            Spacer(Modifier.height(28.dp))

            // Preview of the selected color
            Box(
                modifier = Modifier
                    .rotate(-2f)
                    .size(120.dp)
                    .hardShadow(corner = 60.dp, offset = 7.dp)
                    .background(color, CircleShape)
                    .border(4.dp, Ink, CircleShape)
            )

            Spacer(Modifier.height(28.dp))

            // Preset swatches, 2 rows of 4
            presetColors.chunked(4).forEach { rowColors ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    rowColors.forEach { preset ->
                        val isSelected = preset == color
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .then(
                                    if (isSelected) Modifier.hardShadow(corner = 10.dp, offset = 4.dp)
                                    else Modifier
                                )
                                .size(54.dp)
                                .background(preset, RoundedCornerShape(10.dp))
                                .border(3.dp, Ink, RoundedCornerShape(10.dp))
                                .clickable { onColorChange(preset) }
                        ) {
                            if (isSelected) {
                                Text(
                                    text = "✕",
                                    color = if (preset == Ink) Paper else Ink,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.height(14.dp))

            // RGB sliders
            ChannelSlider("R", color.red, PopRed) { onColorChange(color.copy(red = it)) }
            ChannelSlider("G", color.green, PopGreen) { onColorChange(color.copy(green = it)) }
            ChannelSlider("B", color.blue, PopBlue) { onColorChange(color.copy(blue = it)) }

            BrushWidthCard(color = color, width = width, onWidthChange = onWidthChange)

            Spacer(Modifier.height(8.dp))

            NeoButton(
                label = "BACK TO CANVAS →",
                fill = Ink,
                textColor = Paper,
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChannelSlider(label: String, value: Float, accent: Color, onValueChange: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .hardShadow(corner = 12.dp, offset = 4.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(3.dp, Ink, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(accent, RoundedCornerShape(6.dp))
                    .border(2.dp, Ink, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(text = label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            Text(
                text = (value * 255).roundToInt().toString(),
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = Ink,
                activeTrackColor = accent,
                inactiveTrackColor = Ink.copy(alpha = 0.12f)
            )
        )
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun BrushWidthCard(color: Color, width: Float, onWidthChange: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .hardShadow(corner = 12.dp, offset = 4.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(3.dp, Ink, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(PopOrange, RoundedCornerShape(6.dp))
                    .border(2.dp, Ink, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(text = "WIDTH", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            Text(
                text = "${width.roundToInt()} dp",
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(Modifier.height(10.dp))

        // Dot + line preview at the actual brush size
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Paper, RoundedCornerShape(8.dp))
                .border(2.dp, Ink, RoundedCornerShape(8.dp))
        ) {
            val widthPx = width.dp.toPx()
            val cy = size.height / 2f
            drawCircle(color = color, radius = widthPx / 2f, center = Offset(size.width * 0.15f, cy))
            drawLine(
                color = color,
                start = Offset(size.width * 0.3f, cy),
                end = Offset(size.width * 0.9f, cy),
                strokeWidth = widthPx,
                cap = StrokeCap.Round
            )
        }

        Slider(
            value = width,
            onValueChange = onWidthChange,
            valueRange = 2f..40f,
            colors = SliderDefaults.colors(
                thumbColor = Ink,
                activeTrackColor = PopOrange,
                inactiveTrackColor = Ink.copy(alpha = 0.12f)
            )
        )
    }
    Spacer(Modifier.height(20.dp))
}
