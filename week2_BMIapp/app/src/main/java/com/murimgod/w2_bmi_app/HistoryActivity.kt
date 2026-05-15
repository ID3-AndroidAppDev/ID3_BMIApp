package com.murimgod.w2_bmi_app

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.murimgod.w2_bmi_app.databinding.ActivityHistoryBinding
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // Back navigation
        binding.toolbar.setNavigationOnClickListener { finish() }

        prefs = getSharedPreferences("bmi_history", MODE_PRIVATE)

        // Load and display history on start
        displayHistory()

        // 4.2.2 — Show Best: only "Normal weight" entries
        binding.btnShowBest.setOnClickListener {
            val entries = getHistoryEntries()
            val best = findBest(entries)

            if (best != null) {
                binding.tvBestTitle.text    = "Your best BMI was"
                binding.tvBestBmi.text      = String.format(Locale.US, "%.1f", best.first)
                binding.tvBestBmi.visibility = View.VISIBLE
                binding.tvBestCategory.text = best.second
            } else {
                // No "Normal weight" readings found
                binding.tvBestTitle.text    = "Unfortunately, no best result found"
                binding.tvBestBmi.visibility = View.GONE
                binding.tvBestCategory.text = "No readings with Normal weight yet"
            }

            if (binding.cvBestResult.visibility != View.VISIBLE) {
                binding.cvBestResult.visibility = View.VISIBLE
                val anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
                anim.duration = 300
                binding.cvBestResult.startAnimation(anim)
            }
        }

        // 4.2.1 — Show Average: calcAverage procedure
        binding.btnShowAverage.setOnClickListener {
            val entries = getHistoryEntries()
            val avg = calcAverage(entries)

            if (avg != null) {
                binding.tvAvgBmi.text      = String.format(Locale.US, "%.1f", avg)
                binding.tvAvgCategory.text = categorize(avg)
            }

            if (binding.cvAverageResult.visibility != View.VISIBLE) {
                binding.cvAverageResult.visibility = View.VISIBLE
                val anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
                anim.duration = 300
                binding.cvAverageResult.startAnimation(anim)
            }
        }

        // Navigate to Statistics screen (Exercises 1 & 2)
        binding.btnViewStatistics.setOnClickListener {
            startActivity(android.content.Intent(this, StatisticsActivity::class.java))
        }

        // Clear all with confirmation dialog
        binding.fabClearHistory.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Clear history")
                .setMessage("Delete all saved BMI readings?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear") { _, _ ->
                    prefs.edit().clear().apply()
                    binding.cvBestResult.visibility    = View.GONE
                    binding.cvAverageResult.visibility  = View.GONE
                    displayHistory()
                }
                .show()
        }
    }

    // ── 4.2.2: findBest — to-result procedure ─────────────────────────────
    // Only considers "Normal weight" entries.
    // Loops through the list tracking the minimum BMI value seen among them.
    // Returns Pair(bmiValue, category) or null if no Normal weight entries.
    private fun findBest(entries: List<String>): Pair<Double, String>? {
        if (entries.isEmpty()) return null

        var bestBmi      = Double.MAX_VALUE
        var bestCategory = ""

        for (entry in entries) {
            val parts = entry.split("|")
            if (parts.size < 2) continue

            val category = parts[1]
            if (category != "Normal weight") continue   // filter: only Normal weight

            val bmi = parts[0].toDoubleOrNull() ?: continue

            if (bmi < bestBmi) {
                bestBmi      = bmi
                bestCategory = category
            }
        }

        return if (bestBmi == Double.MAX_VALUE) null
        else Pair(bestBmi, bestCategory)
    }

    // ── 4.2.1: calcAverage — to-result procedure ──────────────────────────
    // Takes a list, loops through summing all BMI values, divides by length.
    // Returns the result rounded to 1 decimal place, or null if empty.
    private fun calcAverage(entries: List<String>): Double? {
        if (entries.isEmpty()) return null

        var sum   = 0.0
        var count = 0

        for (entry in entries) {
            val parts = entry.split("|")
            if (parts.isEmpty()) continue
            val bmi = parts[0].toDoubleOrNull() ?: continue
            sum += bmi
            count++
        }

        if (count == 0) return null
        return sum / count    // rounded to 1 decimal on display
    }

    // Helper: classify a BMI value into a category string
    private fun categorize(bmi: Double): String = when {
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal weight"
        bmi < 30.0 -> "Overweight"
        else       -> "Obese"
    }
    // ──────────────────────────────────────────────────────────────────────

    private fun getHistoryEntries(): List<String> {
        val raw = prefs.getString("entries", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(";")
    }

    private fun displayHistory() {
        val entries = getHistoryEntries()
        val count   = entries.size

        // tvHistoryCount — total saved readings
        binding.tvHistoryCount.text = if (count == 1)
            "You have 1 saved reading"
        else
            "You have $count saved readings"

        if (entries.isEmpty()) {
            binding.historyLabel.visibility     = View.GONE
            binding.divider.visibility          = View.GONE
            binding.btnShowBest.visibility      = View.GONE
            binding.btnShowAverage.visibility   = View.GONE
            binding.cvBestResult.visibility     = View.GONE
            binding.cvAverageResult.visibility  = View.GONE
            binding.llEmpty.visibility          = View.VISIBLE
            binding.fabClearHistory.hide()
            return
        }

        binding.llEmpty.visibility         = View.GONE
        binding.btnShowBest.visibility     = View.VISIBLE
        binding.btnShowAverage.visibility  = View.VISIBLE
        binding.btnViewStatistics.visibility = View.VISIBLE
        binding.divider.visibility         = View.VISIBLE
        binding.fabClearHistory.show()

        // historyLabel — build multi-line text from SharedPreferences list
        val sb = StringBuilder()
        for ((index, entry) in entries.withIndex()) {
            val parts = entry.split("|")
            if (parts.size < 3) continue
            val bmiValue = parts[0]
            val category = parts[1]
            val date     = parts[2]

            sb.append("${index + 1}.  $date\n")
            sb.append("     BMI: $bmiValue  ·  $category")

            if (index < entries.size - 1) sb.append("\n\n")
        }

        binding.historyLabel.text       = sb.toString()
        binding.historyLabel.visibility = View.VISIBLE
    }
}
