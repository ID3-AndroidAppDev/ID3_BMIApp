package com.murimgod.w2_bmi_app

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.murimgod.w2_bmi_app.databinding.ActivityStatisticsBinding

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
        prefs = getSharedPreferences("bmi_history", MODE_PRIVATE)

        loadStatistics()
    }

    private fun loadStatistics() {
        val raw = prefs.getString("entries", "") ?: ""
        if (raw.isEmpty()) return

        val entries = raw.split(";")
        val bmiValues = mutableListOf<Double>()
        
        var underweight = 0
        var normal = 0
        var overweight = 0
        var obese = 0

        // Process data for Distribution and Chart
        // entries are stored as "bmi|category|date"
        // in MainActivity.saveToHistory, new entries are added at index 0.
        // For the chart, we want chronological order (oldest first).
        val chronologicalEntries = entries.reversed()

        for (entryString in chronologicalEntries) {
            val parts = entryString.split("|")
            if (parts.size < 2) continue
            
            val bmi = parts[0].toDoubleOrNull() ?: continue
            val category = parts[1]
            
            bmiValues.add(bmi)

            // Exercise 1: Categorization
            when {
                bmi < 18.5 -> underweight++
                bmi < 25.0 -> normal++
                bmi < 30.0 -> overweight++
                else       -> obese++
            }
        }

        // Update UI for Exercise 1
        binding.tvUnderweightCount.text = "Underweight: $underweight"
        binding.tvNormalCount.text = "Normal: $normal"
        binding.tvOverweightCount.text = "Overweight: $overweight"
        binding.tvObeseCount.text = "Obese: $obese"
        binding.tvTotalReadings.text = "Total readings: ${bmiValues.size}"

        // Update UI for Exercise 2: Line Chart
        setupChart(bmiValues)
    }

    private fun setupChart(bmiValues: List<Double>) {
        val chartEntries = mutableListOf<Entry>()
        for ((index, value) in bmiValues.withIndex()) {
            // x = index + 1, y = BMI value
            chartEntries.add(Entry((index + 1).toFloat(), value.toFloat()))
        }

        val dataSet = LineDataSet(chartEntries, "BMI History").apply {
            color = Color.parseColor("#FF5722") // Orange line
            setCircleColor(Color.parseColor("#FF5722"))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(true)
            valueTextSize = 10f
            valueTextColor = Color.WHITE // Changed text color for points to white
            setDrawFilled(true)
            fillAlpha = 50
            fillColor = Color.parseColor("#FF5722")
            mode = LineDataSet.Mode.CUBIC_BEZIER // Smooth curves
        }

        binding.lineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            
            xAxis.apply {
                granularity = 1f
                setDrawGridLines(false)
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                axisLineColor = Color.WHITE
            }
            
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#40FFFFFF") // Semi-transparent white
                textColor = Color.WHITE
                axisLineColor = Color.WHITE
            }

            animateX(1000)
            invalidate() // Refresh
        }
    }
}
