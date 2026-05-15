package com.murimgod.w2_bmi_app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.murimgod.w2_bmi_app.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("bmi_history", MODE_PRIVATE)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Calculate Current BMI
        binding.btnCalculate.setOnClickListener {
            val weight = binding.etWeight.text.toString().toDoubleOrNull()
            val heightCm = binding.etHeight.text.toString().toDoubleOrNull()

            if (weight != null && heightCm != null && heightCm > 0) {
                val (bmi, category) = calculateBMI(weight, heightCm)
                binding.tvBmiNumber.text = String.format(Locale.US, "%.1f", bmi)
                binding.tvBmiCategory.text = category
                binding.cvResult.visibility = View.VISIBLE

                saveToHistory(bmi, category)
                showWeightAdvice()
            } else {
                binding.cvResult.visibility = View.GONE
            }
        }

        // Calculate Previous BMI
        binding.btnCalculatePrevious.setOnClickListener {
            val prevWeight = binding.etPreviousWeight.text.toString().toDoubleOrNull()
            val heightCm = binding.etHeight.text.toString().toDoubleOrNull()

            if (prevWeight != null && heightCm != null && heightCm > 0) {
                val (prevBmi, prevCategory) = calculateBMI(prevWeight, heightCm)
                binding.tvPrevBmiNumber.text = String.format(Locale.US, "%.1f", prevBmi)
                binding.tvPrevBmiCategory.text = prevCategory
                binding.cvPreviousResult.visibility = View.VISIBLE

                showWeightAdvice()
            } else {
                binding.cvPreviousResult.visibility = View.GONE
            }
        }

        // Open History screen
        binding.btnViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    // Reusable BMI calculation — write formula once, call twice
    private fun calculateBMI(weightKg: Double, heightCm: Double): Pair<Double, String> {
        val heightM = heightCm / 100.0
        val bmi = weightKg / (heightM * heightM)
        val category = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal weight"
            bmi < 30.0 -> "Overweight"
            else       -> "Obese"
        }
        return Pair(bmi, category)
    }

    // Compare current vs previous weight and show advice
    private fun showWeightAdvice() {
        val current  = binding.etWeight.text.toString().toDoubleOrNull()
        val previous = binding.etPreviousWeight.text.toString().toDoubleOrNull()

        if (current != null && previous != null) {
            val advice = when {
                current > previous -> "You are gaining some kilograms recently. Why not working out 20 minutes today?"
                current < previous -> "You are losing some kilograms recently. Good job!"
                else               -> "Your weight has been constant recently."
            }
            binding.tvWeightAdvice.text = advice
            binding.tvWeightAdvice.visibility = View.VISIBLE
        } else {
            binding.tvWeightAdvice.visibility = View.GONE
        }
    }

    private fun saveToHistory(bmi: Double, category: String) {
        val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val entry = String.format(Locale.US, "%.1f|%s|%s", bmi, category, timestamp)

        val entries = getHistoryEntries().toMutableList()
        entries.add(0, entry)
        if (entries.size > 10) entries.removeAt(entries.size - 1)

        prefs.edit().putString("entries", entries.joinToString(";")).apply()
    }

    private fun getHistoryEntries(): List<String> {
        val raw = prefs.getString("entries", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(";")
    }
}