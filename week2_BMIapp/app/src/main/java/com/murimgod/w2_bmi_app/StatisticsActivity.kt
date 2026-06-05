package com.murimgod.w2_bmi_app

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.murimgod.w2_bmi_app.databinding.ActivityStatisticsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        // load saved BMI
        prefs = getSharedPreferences("bmi_history", MODE_PRIVATE)

        loadDashboard()
    }

    private fun loadDashboard() {
        val raw = prefs.getString("entries", "") ?: ""
        if (raw.isEmpty()) return

        // parse stored format
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val bmiValues = mutableListOf<Double>()
        val rawPoints = mutableListOf<Pair<Long, Float>>()
        var tooLow = 0
        var normal = 0
        var tooHigh = 0

        // oldest first for chart
        for (entry in raw.split(";").reversed()) {
            val parts = entry.split("|")
            if (parts.size < 3) continue
            val bmi = parts[0].toDoubleOrNull() ?: continue
            val date = sdf.parse(parts[2]) ?: continue

            bmiValues.add(bmi)
            rawPoints.add(Pair(date.time / 1000, bmi.toFloat()))

            // count categories
            when {
                bmi < 18.5 -> tooLow++
                bmi < 25.0 -> normal++
                else       -> tooHigh++
            }
        }

        if (bmiValues.isEmpty()) return

        // offset x from first point — avoids float precision loss
        val baseEpoch = rawPoints.first().first
        val chartPoints = rawPoints.map { (epochSec, bmi) ->
            Entry((epochSec - baseEpoch).toFloat(), bmi)
        }

        // stat cards
        val avg = bmiValues.average()
        val min = bmiValues.min()
        val max = bmiValues.max()

        binding.tvAvgValue.text = String.format(Locale.US, "%.1f", avg)
        binding.tvAvgValue.setTextColor(categoryColor(avg))
        binding.tvAvgCategory.text = categorize(avg)

        binding.tvMinValue.text = String.format(Locale.US, "%.1f", min)
        binding.tvMinCategory.text = categorize(min)

        binding.tvMaxValue.text = String.format(Locale.US, "%.1f", max)
        binding.tvMaxCategory.text = categorize(max)

        setupLineChart(chartPoints, baseEpoch)
        setupPieChart(tooLow, normal, tooHigh)
    }

    private fun setupLineChart(chartPoints: List<Entry>, baseEpoch: Long) {
        val dataSet = LineDataSet(chartPoints, getString(R.string.chart_dataset_label)).apply {
            color = Color.parseColor("#FF5722")
            setCircleColor(Color.parseColor("#FF5722"))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(true)
            valueTextSize = 9f
            valueTextColor = Color.WHITE
            setDrawFilled(true)
            fillAlpha = 50
            fillColor = Color.parseColor("#FF5722")
            // no overshoot on spikes
            mode = LineDataSet.Mode.LINEAR
        }

        val displayFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        binding.lineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.apply {
                setDrawGridLines(false)
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                axisLineColor = Color.WHITE
                labelRotationAngle = -45f
                setLabelCount(5, true)
                // restore real timestamp from offset
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String =
                        displayFormat.format(Date((baseEpoch + value.toLong()) * 1000))
                }
            }
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#40FFFFFF")
                textColor = Color.WHITE
                axisLineColor = Color.WHITE
            }
            setExtraBottomOffset(16f)
            animateX(1000)
            invalidate()
        }
    }

    private fun setupPieChart(tooLow: Int, normal: Int, tooHigh: Int) {
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        // only add non-zero slices
        if (normal > 0) {
            entries.add(PieEntry(normal.toFloat(), getString(R.string.cat_short_normal)))
            colors.add(Color.parseColor("#4CAF50"))
        }
        if (tooHigh > 0) {
            entries.add(PieEntry(tooHigh.toFloat(), getString(R.string.cat_short_too_high)))
            colors.add(Color.parseColor("#F44336"))
        }
        if (tooLow > 0) {
            entries.add(PieEntry(tooLow.toFloat(), getString(R.string.cat_short_too_low)))
            colors.add(Color.parseColor("#2196F3"))
        }

        if (entries.isEmpty()) return

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 3f
            selectionShift = 8f
            valueTextSize = 13f
            valueTextColor = Color.WHITE
        }

        binding.pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            // donut style
            isDrawHoleEnabled = true
            holeRadius = 50f
            transparentCircleRadius = 55f
            setTransparentCircleColor(Color.parseColor("#20000000"))
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
            legend.isEnabled = false
            animateY(900)
            invalidate()
        }
    }

    // BMI → short display label
    private fun categorize(bmi: Double): String = when {
        bmi < 18.5 -> getString(R.string.cat_short_too_low)
        bmi < 25.0 -> getString(R.string.cat_short_normal)
        else       -> getString(R.string.cat_short_too_high)
    }

    // BMI → color
    private fun categoryColor(bmi: Double): Int = when {
        bmi < 18.5 -> Color.parseColor("#2196F3")
        bmi < 25.0 -> Color.parseColor("#4CAF50")
        else       -> Color.parseColor("#F44336")
    }
}
