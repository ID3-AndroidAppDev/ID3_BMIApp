package com.murimgod.w2_bmi_app

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.murimgod.w2_bmi_app.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize ViewBinding to access UI elements
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Adjust padding to account for system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Handle BMI calculation on button click
        binding.btnCalculate.setOnClickListener {
            val weightStr = binding.etWeight.text.toString()
            val heightStr = binding.etHeight.text.toString()

            // Parse input string to Double. Result will be null if invalid
            val weight = weightStr.toDoubleOrNull()
            val heightCm = heightStr.toDoubleOrNull()

            if (weight != null && heightCm != null && heightCm > 0) {
                // Convert height from cm to meters
                val heightM = heightCm / 100.0
                
                // Calculate BMI formula: weight (kg) / height^2 (m)
                val bmi = weight / (heightM * heightM)
                
                // Determine BMI category
                val category = when {
                    bmi < 18.5 -> "Underweight"
                    bmi < 25.0 -> "Normal weight"
                    bmi < 30.0 -> "Overweight"
                    else -> "Obese"
                }

                // Show formatted BMI result and category
                binding.tvBmiNumber.text = String.format(Locale.US, "%.1f", bmi)
                binding.tvBmiCategory.text = category
                binding.cvResult.visibility = View.VISIBLE
            } else {
                // Hide the result card if the input is incorrect
                binding.cvResult.visibility = View.GONE
            }
        }
    }
}