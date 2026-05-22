package com.example.id

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.id.ui.theme.IDTheme
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val BgBase    = Color(0xFF0D0910)
private val BgSurface = Color(0xFF181020)
private val BgCard    = Color(0xFF1C1428)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IDTheme {
                BmiScreen()
            }
        }
    }
}

private enum class MeasurementUnit { METRIC, IMPERIAL }

private data class BmiCategory(val label: String, val description: String, val color: Color)

private data class BmiRecord(val bmi: Float, val category: String, val timestamp: Long)

private fun bmiCategory(bmi: Float): BmiCategory = when {
    bmi < 18f -> BmiCategory("Underweight", "Have you tried eating? Highly recommended.", Color(0xFFCDB4DB))
    bmi < 25f -> BmiCategory("Normal",      "Peak mediocrity. Cherish it.",               Color(0xFF7FD8A0))
    bmi < 35f -> BmiCategory("Overweight",  "You're basically well-insulated.",           Color(0xFFFFB347))
    else      -> BmiCategory("Obese",       "Gravity has strong feelings about you.",     Color(0xFFFF6B6B))
}

private fun loadRecords(context: Context): List<BmiRecord> {
    val prefs = context.getSharedPreferences("bmi_records", Context.MODE_PRIVATE)
    val arr = JSONArray(prefs.getString("records", "[]") ?: "[]")
    return (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        BmiRecord(obj.getDouble("bmi").toFloat(), obj.getString("category"), obj.getLong("timestamp"))
    }
}

private fun persistRecords(context: Context, records: List<BmiRecord>) {
    val arr = JSONArray()
    records.forEach { r ->
        arr.put(JSONObject().apply {
            put("bmi", r.bmi.toDouble())
            put("category", r.category)
            put("timestamp", r.timestamp)
        })
    }
    context.getSharedPreferences("bmi_records", Context.MODE_PRIVATE)
        .edit { putString("records", arr.toString()) }
}

@Composable
fun BmiScreen() {
    val context = LocalContext.current
    var unit by remember { mutableStateOf(MeasurementUnit.METRIC) }

    var heightCm by remember { mutableFloatStateOf(170f) }
    var weightKg by remember { mutableFloatStateOf(70f) }
    var heightIn by remember { mutableFloatStateOf(67f) }
    var weightLb by remember { mutableFloatStateOf(154f) }

    val records = remember { mutableStateListOf<BmiRecord>().also { it.addAll(loadRecords(context)) } }

    val bmi: Float = when (unit) {
        MeasurementUnit.METRIC   -> weightKg / ((heightCm / 100f) * (heightCm / 100f))
        MeasurementUnit.IMPERIAL -> 703f * weightLb / (heightIn * heightIn)
    }
    val category = bmiCategory(bmi)

    val accentColor by animateColorAsState(category.color, tween(600), label = "accent")

    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgBase)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.18f),
                                accentColor.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(Float.POSITIVE_INFINITY / 2, 0f),
                            radius = 700f
                        )
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(32.dp))

                // ── Header ───────────────────────────────────────────────────
                Text(
                    text = "BMI",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 8.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "invented in 1832, still judging",
                    color = Color.White.copy(alpha = 0.18f),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(28.dp))

                // ── BMI Arc Gauge ─────────────────────────────────────────────
                BmiGaugeCard(bmi = bmi, category = category, accent = accentColor)

                Spacer(Modifier.height(20.dp))

                // ── Unit toggle ───────────────────────────────────────────────
                UnitToggle(selected = unit, onSelect = { unit = it }, accent = accentColor)

                Spacer(Modifier.height(16.dp))

                // ── Measurement cards ──────────────────────────────────────────
                when (unit) {
                    MeasurementUnit.METRIC -> MeasurementCard(
                        label = "HEIGHT", valueText = "${heightCm.roundToInt()} cm",
                        value = heightCm, onValueChange = { heightCm = it },
                        valueRange = 100f..220f, accent = accentColor, step = 1f
                    )
                    MeasurementUnit.IMPERIAL -> {
                        val feet = (heightIn / 12).toInt()
                        val inches = (heightIn % 12).roundToInt()
                        MeasurementCard(
                            label = "HEIGHT", valueText = "$feet' $inches\"",
                            value = heightIn, onValueChange = { heightIn = it },
                            valueRange = 48f..84f, accent = accentColor, step = 1f
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                when (unit) {
                    MeasurementUnit.METRIC -> MeasurementCard(
                        label = "WEIGHT", valueText = "${weightKg.roundToInt()} kg",
                        value = weightKg, onValueChange = { weightKg = it },
                        valueRange = 30f..200f, accent = accentColor, step = 1f
                    )
                    MeasurementUnit.IMPERIAL -> MeasurementCard(
                        label = "WEIGHT", valueText = "${weightLb.roundToInt()} lbs",
                        value = weightLb, onValueChange = { weightLb = it },
                        valueRange = 66f..440f, accent = accentColor, step = 1f
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── Save button ───────────────────────────────────────────────
                GlowButton(
                    label = "SAVE RESULT",
                    accent = accentColor,
                    onClick = {
                        records.add(0, BmiRecord(bmi, category.label, System.currentTimeMillis()))
                        persistRecords(context, records)
                    }
                )

                // ── Dashboard + History ───────────────────────────────────────
                if (records.isNotEmpty()) {
                    Spacer(Modifier.height(36.dp))

                    // Section header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DASHBOARD",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFFF6B6B).copy(alpha = 0.10f))
                                .border(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.22f), RoundedCornerShape(50))
                                .clickable { records.clear(); persistRecords(context, records) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "CLEAR ALL",
                                color = Color(0xFFFF6B6B).copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Stats: avg / min / max
                    DashboardStatsRow(records = records)

                    Spacer(Modifier.height(10.dp))

                    // Line chart with timestamps
                    BmiLineChart(records = records)

                    Spacer(Modifier.height(10.dp))

                    // Donut category chart
                    BmiPieChart(records = records)

                    Spacer(Modifier.height(28.dp))

                    // ── Section divider ───────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.05f))
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── History header ────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HISTORY",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp
                        )
                        Text(
                            text = "${records.size} ${if (records.size == 1) "entry" else "entries"}",
                            color = Color.White.copy(alpha = 0.18f),
                            fontSize = 10.sp
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    val categoryOrder = listOf("Underweight", "Normal", "Overweight", "Obese")
                    val grouped = records.groupBy { it.category }
                    categoryOrder.forEach { categoryName ->
                        val group = grouped[categoryName] ?: return@forEach
                        val catColor = bmiCategory(group.first().bmi).color

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(catColor)
                            )
                            Text(
                                text = categoryName.uppercase(),
                                color = catColor.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 3.sp
                            )
                            Text(
                                text = "${group.size} ${if (group.size == 1) "entry" else "entries"}",
                                color = Color.White.copy(alpha = 0.2f),
                                fontSize = 10.sp
                            )
                        }

                        group.forEachIndexed { index, record ->
                            val prevBmi = if (index < group.size - 1) group[index + 1].bmi else null
                            BmiHistoryItem(record, prevBmi)
                            Spacer(Modifier.height(8.dp))
                        }

                        Spacer(Modifier.height(12.dp))
                    }
                }

                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

// ── Dashboard composables ─────────────────────────────────────────────────────

@Composable
private fun DashboardStatsRow(records: List<BmiRecord>) {
    val bmis = records.map { it.bmi }
    val avg  = bmis.average().toFloat()
    val min  = bmis.min()
    val max  = bmis.max()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatChip(label = "MIN", bmi = min, modifier = Modifier.weight(1f))
        StatChip(label = "AVG", bmi = avg, modifier = Modifier.weight(1f))
        StatChip(label = "MAX", bmi = max, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatChip(label: String, bmi: Float, modifier: Modifier = Modifier) {
    val cat = bmiCategory(bmi)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cat.color.copy(alpha = 0.07f))
            .border(1.dp, cat.color.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.28f),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "%.1f".format(bmi),
            color = cat.color,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = cat.label.uppercase(),
            color = cat.color.copy(alpha = 0.5f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BmiPieChart(records: List<BmiRecord>) {
    val categoryOrder = listOf("Underweight", "Normal", "Overweight", "Obese")
    val grouped       = records.groupBy { it.category }
    val total         = records.size.toFloat()
    val textMeasurer  = rememberTextMeasurer()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgSurface)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "DISTRIBUTION",
            color = Color.White.copy(alpha = 0.2f),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp
        )

        Spacer(Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Donut with centre count
            Canvas(modifier = Modifier.size(110.dp)) {
                val strokeWidth = 20.dp.toPx()
                val radius      = (size.minDimension - strokeWidth) / 2f
                val center      = Offset(size.width / 2f, size.height / 2f)
                var startAngle  = -90f

                drawCircle(
                    color  = Color.White.copy(alpha = 0.04f),
                    radius = radius,
                    center = center,
                    style  = Stroke(strokeWidth)
                )

                categoryOrder.forEach { name ->
                    val count = grouped[name]?.size ?: 0
                    if (count == 0) return@forEach
                    val sweep = 360f * count / total
                    val color = bmiCategory(grouped[name]!!.first().bmi).color
                    drawArc(
                        color      = color,
                        startAngle = startAngle,
                        sweepAngle = sweep - 2f,
                        useCenter  = false,
                        topLeft    = Offset(center.x - radius, center.y - radius),
                        size       = Size(radius * 2f, radius * 2f),
                        style      = Stroke(strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }

                // Centre: record count
                val countStyle = TextStyle(
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black,
                    color      = Color.White.copy(alpha = 0.75f)
                )
                val subStyle = TextStyle(
                    fontSize   = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White.copy(alpha = 0.25f)
                )
                val countText = records.size.toString()
                val subText   = if (records.size == 1) "record" else "records"
                val cM = textMeasurer.measure(countText, countStyle)
                val sM = textMeasurer.measure(subText,   subStyle)
                val blockH = cM.size.height + 2.dp.toPx() + sM.size.height
                drawText(
                    textMeasurer, countText, style = countStyle,
                    topLeft = Offset(center.x - cM.size.width / 2f, center.y - blockH / 2f)
                )
                drawText(
                    textMeasurer, subText, style = subStyle,
                    topLeft = Offset(
                        center.x - sM.size.width / 2f,
                        center.y - blockH / 2f + cM.size.height + 2.dp.toPx()
                    )
                )
            }

            // Legend
            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.weight(1f)
            ) {
                categoryOrder.forEach { name ->
                    val count = grouped[name]?.size ?: 0
                    if (count == 0) return@forEach
                    val color = bmiCategory(grouped[name]!!.first().bmi).color
                    val pct   = (count * 100f / total).roundToInt()

                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            text     = name,
                            color    = color.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text       = "$pct%",
                            color      = color.copy(alpha = 0.45f),
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

// ── Line chart helpers ────────────────────────────────────────────────────────

// Returns timestamps for clean, round-interval tick marks on the x-axis.
// Steps are aligned to the UTC epoch (e.g. 5-min step → ticks always at :00/:05/:10…).
private fun niceTimeTicks(minTs: Long, maxTs: Long, maxCount: Int): List<Long> {
    val span = maxTs - minTs
    val niceSteps = longArrayOf(
        60_000L,           // 1 min
        2L   * 60_000,     // 2 min
        5L   * 60_000,     // 5 min
        10L  * 60_000,     // 10 min
        15L  * 60_000,     // 15 min
        30L  * 60_000,     // 30 min
        3_600_000L,        // 1 h
        2L   * 3_600_000,  // 2 h
        6L   * 3_600_000,  // 6 h
        12L  * 3_600_000,  // 12 h
        86_400_000L,       // 1 day
        7L   * 86_400_000  // 1 week
    )
    val targetStep = span / (maxCount - 1).coerceAtLeast(1)
    val step = niceSteps.firstOrNull { it >= targetStep } ?: niceSteps.last()

    val first = (minTs / step) * step   // floor to step boundary
    val ticks = mutableListOf<Long>()
    var t = first
    while (t <= maxTs) {
        if (t >= minTs) ticks.add(t)
        t += step
    }
    return ticks.ifEmpty { listOf(minTs, maxTs) }
}

// ── Line chart ────────────────────────────────────────────────────────────────

@Composable
private fun BmiLineChart(records: List<BmiRecord>) {
    if (records.size < 2) return

    val sorted = records.reversed()   // oldest → newest
    val textMeasurer = rememberTextMeasurer()

    val allBmis = sorted.map { it.bmi }
    val yMin = (allBmis.min() - 3f).coerceAtLeast(10f)
    val yMax = (allBmis.max() + 3f).coerceAtMost(50f)
    val thresholds = listOf(18f, 25f, 35f).filter { it in yMin..yMax }

    val minTs    = sorted.first().timestamp
    val maxTs    = sorted.last().timestamp
    val timeSpan = (maxTs - minTs).coerceAtLeast(1L).toFloat()
    val sameDay  = (maxTs - minTs) < 24L * 60 * 60 * 1000
    val dateFmt  = SimpleDateFormat(if (sameDay) "HH:mm" else "MMM d", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgSurface)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(top = 16.dp, bottom = 14.dp, start = 4.dp, end = 16.dp)
    ) {
        Text(
            text = "BMI TREND",
            color = Color.White.copy(alpha = 0.2f),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
        Spacer(Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val leftPad  = 38.dp.toPx()
            val rightPad = 8.dp.toPx()
            val topPad   = 8.dp.toPx()
            val botPad   = 24.dp.toPx()
            val chartW   = size.width - leftPad - rightPad
            val chartH   = size.height - topPad - botPad
            val n        = sorted.size

            fun bmiToY(b: Float) = topPad + chartH * (1f - (b - yMin) / (yMax - yMin))
            fun tsToX(ts: Long)  = leftPad + chartW * (ts - minTs).toFloat() / timeSpan

            // ── X-axis baseline ───────────────────────────────────────────
            drawLine(
                color       = Color.White.copy(alpha = 0.07f),
                start       = Offset(leftPad, topPad + chartH),
                end         = Offset(leftPad + chartW, topPad + chartH),
                strokeWidth = 1.dp.toPx()
            )

            // ── Threshold reference lines & Y labels ─────────────────────
            val labelStyle = TextStyle(
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.22f),
                fontWeight = FontWeight.Bold
            )
            thresholds.forEach { threshold ->
                val y = bmiToY(threshold)
                drawLine(
                    color = Color.White.copy(alpha = 0.09f),
                    start = Offset(leftPad, y),
                    end   = Offset(leftPad + chartW, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
                )
                val measured = textMeasurer.measure(threshold.toInt().toString(), labelStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = threshold.toInt().toString(),
                    style = labelStyle,
                    topLeft = Offset(
                        x = leftPad - measured.size.width - 6.dp.toPx(),
                        y = y - measured.size.height / 2f
                    )
                )
            }

            // ── Area fill under the line ──────────────────────────────────
            val areaPath = Path().apply {
                moveTo(tsToX(sorted.first().timestamp), topPad + chartH)
                lineTo(tsToX(sorted.first().timestamp), bmiToY(sorted.first().bmi))
                sorted.forEach { r -> lineTo(tsToX(r.timestamp), bmiToY(r.bmi)) }
                lineTo(tsToX(sorted.last().timestamp), topPad + chartH)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                    startY = topPad,
                    endY   = topPad + chartH
                )
            )

            // ── Line segments, coloured by source point's category ────────
            sorted.forEachIndexed { i, record ->
                if (i < n - 1) {
                    drawLine(
                        color = bmiCategory(record.bmi).color.copy(alpha = 0.9f),
                        start = Offset(tsToX(record.timestamp),        bmiToY(record.bmi)),
                        end   = Offset(tsToX(sorted[i + 1].timestamp), bmiToY(sorted[i + 1].bmi)),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // ── Data point dots ───────────────────────────────────────────
            sorted.forEach { record ->
                val cx = tsToX(record.timestamp)
                val cy = bmiToY(record.bmi)
                val dotColor = bmiCategory(record.bmi).color
                drawCircle(color = dotColor.copy(alpha = 0.25f), radius = 8.dp.toPx(),  center = Offset(cx, cy))
                drawCircle(color = dotColor,                     radius = 4.dp.toPx(),  center = Offset(cx, cy))
                drawCircle(color = BgSurface,                   radius = 2.dp.toPx(),  center = Offset(cx, cy))
            }

            // ── X-axis: round time-based tick labels ──────────────────────
            // Ticks are at epoch-aligned round intervals (e.g. :00/:15/:30/:45)
            // so labels are always clean regardless of when records were saved.
            val dateStyle = TextStyle(fontSize = 8.sp, color = Color.White.copy(alpha = 0.18f))
            val xAxisY    = topPad + chartH + 5.dp.toPx()
            val ticks     = niceTimeTicks(minTs, maxTs, maxCount = 4)

            var lastRightEdge = Float.NEGATIVE_INFINITY
            ticks.forEach { ts ->
                val x     = tsToX(ts)
                val label = dateFmt.format(Date(ts))
                val m     = textMeasurer.measure(label, dateStyle)
                val drawX = (x - m.size.width / 2f)
                    .coerceIn(leftPad, leftPad + chartW - m.size.width)
                if (drawX >= lastRightEdge + 6.dp.toPx()) {
                    drawText(textMeasurer, label, topLeft = Offset(drawX, xAxisY), style = dateStyle)
                    lastRightEdge = drawX + m.size.width
                }
            }
        }
    }
}

// ── Reusable UI components ────────────────────────────────────────────────────

@Composable
private fun BmiGaugeCard(bmi: Float, category: BmiCategory, accent: Color) {
    val animatedBmi by animateFloatAsState(
        targetValue = bmi.coerceIn(15f, 40f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "bmiAnim"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(BgCard)
            .border(1.dp, accent.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
            .padding(vertical = 32.dp, horizontal = 24.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val startAngle = 150f
                val sweepTotal = 240f
                val progress = ((animatedBmi - 15f) / 25f).coerceIn(0f, 1f)

                drawArc(
                    color = Color.White.copy(alpha = 0.07f),
                    startAngle = startAngle,
                    sweepAngle = sweepTotal,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFFCDB4DB),
                            0.35f to Color(0xFF7FD8A0),
                            0.60f to Color(0xFFFFB347),
                            1.0f  to Color(0xFFFF6B6B)
                        )
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.1f".format(bmi),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = accent,
                    lineHeight = 56.sp
                )
                Text(
                    text = "BMI",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.25f),
                    letterSpacing = 3.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.15f))
                .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(50))
                .padding(horizontal = 20.dp, vertical = 7.dp)
        ) {
            Text(
                text = category.label.uppercase(),
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = category.description,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.28f),
            letterSpacing = 0.3.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun MeasurementCard(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    accent: Color,
    step: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BgSurface)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.5.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StepperButton("-", accent) {
                    onValueChange((value - step).coerceIn(valueRange.start, valueRange.endInclusive))
                }
                Text(
                    text = valueText,
                    color = accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.width(80.dp),
                    textAlign = TextAlign.Center
                )
                StepperButton("+", accent) {
                    onValueChange((value + step).coerceIn(valueRange.start, valueRange.endInclusive))
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.07f)
            )
        )
    }
}

@Composable
private fun StepperButton(symbol: String, accent: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.25f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 15.dp, color = accent),
                onClick = onClick
            )
    ) {
        Text(symbol, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GlowButton(label: String, accent: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .glowShadow(color = accent, borderRadius = 16.dp, blurRadius = 18.dp, alpha = 0.35f)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.75f))))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .padding(vertical = 18.dp)
    ) {
        Text(
            text = label,
            color = Color.Black.copy(alpha = 0.75f),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.5.sp
        )
    }
}

private fun Modifier.glowShadow(
    color: Color,
    borderRadius: Dp = 0.dp,
    blurRadius: Dp = 0.dp,
    alpha: Float = 1f,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 4.dp
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                this.color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    blurRadius.toPx(), offsetX.toPx(), offsetY.toPx(),
                    color.copy(alpha = alpha).toArgb()
                )
            }
        }
        canvas.drawRoundRect(
            left = 0f, top = 0f, right = size.width, bottom = size.height,
            radiusX = borderRadius.toPx(), radiusY = borderRadius.toPx(),
            paint = paint
        )
    }
}

@Composable
private fun UnitToggle(selected: MeasurementUnit, onSelect: (MeasurementUnit) -> Unit, accent: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(BgSurface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(MeasurementUnit.METRIC to "Metric", MeasurementUnit.IMPERIAL to "Imperial").forEach { (u, label) ->
            val isSelected = selected == u
            val bg by animateColorAsState(if (isSelected) accent else Color.Transparent, tween(250), label = "tbg")
            val textColor by animateColorAsState(
                if (isSelected) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.35f),
                tween(250), label = "ttxt"
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .clickable { onSelect(u) }
                    .padding(horizontal = 28.dp, vertical = 10.dp)
            ) {
                Text(text = label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
    }
}

@Composable
private fun BmiHistoryItem(record: BmiRecord, prevBmi: Float?) {
    val cat     = bmiCategory(record.bmi)
    val dateStr = SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault()).format(Date(record.timestamp))
    val trend = when {
        prevBmi == null       -> null
        record.bmi < prevBmi  -> "▼"
        record.bmi > prevBmi  -> "▲"
        else                  -> null
    }
    val trendColor = when {
        prevBmi == null      -> Color.Transparent
        record.bmi < prevBmi -> Color(0xFF7FD8A0)
        else                 -> Color(0xFFFF6B6B)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(BgSurface)
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Brush.verticalGradient(listOf(cat.color, cat.color.copy(alpha = 0.4f))))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Text(text = "%.1f".format(record.bmi), color = cat.color, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(text = record.category.uppercase(), color = cat.color.copy(alpha = 0.55f), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                }
                if (trend != null) {
                    Text(text = trend, color = trendColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(text = dateStr, color = Color.White.copy(alpha = 0.18f), fontSize = 11.sp)
        }
    }
}
