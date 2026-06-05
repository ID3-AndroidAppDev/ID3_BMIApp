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

        // view binding init
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // back navigation
        binding.toolbar.setNavigationOnClickListener { finish() }

        prefs = getSharedPreferences("bmi_history", MODE_PRIVATE)

        // initial history render
        displayHistory()

        // show best Normal weight
        binding.btnShowBest.setOnClickListener {
            val entries = getHistoryEntries()
            val best = findBest(entries)

            if (best != null) {
                binding.tvBestTitle.text    = getString(R.string.label_best_bmi_title)
                binding.tvBestBmi.text      = String.format(Locale.US, "%.1f", best.first)
                binding.tvBestBmi.visibility = View.VISIBLE
                binding.tvBestCategory.text = localizeCategory(best.second)
            } else {
                binding.tvBestTitle.text    = getString(R.string.label_best_bmi_none_title)
                binding.tvBestBmi.visibility = View.GONE
                binding.tvBestCategory.text = getString(R.string.label_best_bmi_none_body)
            }

            if (binding.cvBestResult.visibility != View.VISIBLE) {
                binding.cvBestResult.visibility = View.VISIBLE
                // fade-in animation
                val anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
                anim.duration = 300
                binding.cvBestResult.startAnimation(anim)
            }
        }

        // show average of all
        binding.btnShowAverage.setOnClickListener {
            val entries = getHistoryEntries()
            val avg = calcAverage(entries)

            if (avg != null) {
                binding.tvAvgBmi.text      = String.format(Locale.US, "%.1f", avg)
                binding.tvAvgCategory.text = localizeCategory(categorizeKey(avg))
            }

            if (binding.cvAverageResult.visibility != View.VISIBLE) {
                binding.cvAverageResult.visibility = View.VISIBLE
                val anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
                anim.duration = 300
                binding.cvAverageResult.startAnimation(anim)
            }
        }

        binding.btnViewStatistics.setOnClickListener {
            startActivity(android.content.Intent(this, StatisticsActivity::class.java))
        }

        // confirm before clearing
        binding.fabClearHistory.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_clear_title))
                .setMessage(getString(R.string.dialog_clear_message))
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .setPositiveButton(getString(R.string.dialog_clear_confirm)) { _, _ ->
                    // wipe all data
                    prefs.edit().clear().apply()
                    binding.cvBestResult.visibility    = View.GONE
                    binding.cvAverageResult.visibility  = View.GONE
                    displayHistory()
                }
                .show()
        }
    }

    // filter: normal category only, return lowest
    private fun findBest(entries: List<String>): Pair<Double, String>? {
        if (entries.isEmpty()) return null

        var bestBmi      = Double.MAX_VALUE
        var bestCategory = ""

        for (entry in entries) {
            val parts = entry.split("|")
            if (parts.size < 2) continue

            val category = parts[1]
            // normal entries only (key or legacy English)
            if (category.lowercase() !in setOf("normal", "normal weight")) continue

            val bmi = parts[0].toDoubleOrNull() ?: continue
            if (bmi < bestBmi) {
                bestBmi      = bmi
                bestCategory = category
            }
        }

        return if (bestBmi == Double.MAX_VALUE) null
        else Pair(bestBmi, bestCategory)
    }

    // sum all BMI, divide by count
    private fun calcAverage(entries: List<String>): Double? {
        if (entries.isEmpty()) return null

        var sum   = 0.0
        var count = 0

        for (entry in entries) {
            val parts = entry.split("|")
            if (parts.isEmpty()) continue
            val bmi = parts[0].toDoubleOrNull() ?: continue
            // running total
            sum += bmi
            count++
        }

        if (count == 0) return null
        return sum / count
    }

    // BMI value → category key
    private fun categorizeKey(bmi: Double): String = when {
        bmi < 18.5 -> "underweight"
        bmi < 25.0 -> "normal"
        bmi < 30.0 -> "overweight"
        else       -> "obese"
    }

    // category key → localized label
    private fun localizeCategory(key: String): String = when (key.lowercase()) {
        "normal", "normal weight" -> getString(R.string.cat_normal)
        "overweight"              -> getString(R.string.cat_overweight)
        "obese"                   -> getString(R.string.cat_obese)
        "underweight"             -> getString(R.string.cat_underweight)
        else                      -> key
    }

    private fun getHistoryEntries(): List<String> {
        val raw = prefs.getString("entries", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(";")
    }

    private fun displayHistory() {
        val entries = getHistoryEntries()
        val count   = entries.size

        // update count label
        binding.tvHistoryCount.text = if (count == 1)
            getString(R.string.history_count_one)
        else
            getString(R.string.history_count_other, count)

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

        // build history text
        val sb = StringBuilder()
        for ((index, entry) in entries.withIndex()) {
            val parts = entry.split("|")
            if (parts.size < 3) continue
            val bmiValue = parts[0]
            val catKey   = parts[1]
            val date     = parts[2]

            sb.append("${index + 1}.  $date\n")
            sb.append("     BMI: $bmiValue  ·  ${localizeCategory(catKey)}")

            if (index < entries.size - 1) sb.append("\n\n")
        }

        binding.historyLabel.text       = sb.toString()
        binding.historyLabel.visibility = View.VISIBLE
    }
}
