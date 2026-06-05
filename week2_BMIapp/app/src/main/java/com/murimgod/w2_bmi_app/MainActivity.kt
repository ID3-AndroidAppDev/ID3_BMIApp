package com.murimgod.w2_bmi_app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.murimgod.w2_bmi_app.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    // which field receives speech result
    private var speechTarget = SpeechTarget.WEIGHT

    private enum class SpeechTarget { WEIGHT, HEIGHT, PREV_WEIGHT }

    // handles speech recognizer result
    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: return@registerForActivityResult

            // extract digits and decimal point from spoken text
            val number = spoken.filter { it.isDigit() || it == '.' }
            if (number.isNotEmpty()) {
                // fill target field
                when (speechTarget) {
                    SpeechTarget.WEIGHT     -> binding.etWeight.setText(number)
                    SpeechTarget.HEIGHT     -> binding.etHeight.setText(number)
                    SpeechTarget.PREV_WEIGHT -> binding.etPreviousWeight.setText(number)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // view binding init
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // persistent storage handle
        prefs = getSharedPreferences("bmi_history", MODE_PRIVATE)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // show current locale code
        updateLanguageButton()

        // language picker dialog
        binding.btnLanguage.setOnClickListener {
            val langs = arrayOf(
                getString(R.string.lang_english),
                getString(R.string.lang_russian),
                getString(R.string.lang_japanese),
                getString(R.string.lang_spanish)
            )
            val tags = arrayOf("en", "ru", "ja", "es")

            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.title_language))
                .setItems(langs) { _, which ->
                    // switch locale, activity recreates
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(tags[which])
                    )
                }
                .show()
        }

        // voice input: choose field then launch speech
        binding.btnSpeakInput.setOnClickListener {
            val fields = arrayOf(
                getString(R.string.hint_weight),
                getString(R.string.hint_height),
                getString(R.string.hint_prev_weight)
            )
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.speech_choose_field))
                .setItems(fields) { _, which ->
                    speechTarget = SpeechTarget.values()[which]
                    launchSpeech()
                }
                .show()
        }

        // calculate current BMI
        binding.btnCalculate.setOnClickListener {
            val weight = binding.etWeight.text.toString().toDoubleOrNull()
            val heightCm = binding.etHeight.text.toString().toDoubleOrNull()

            if (weight != null && heightCm != null && heightCm > 0) {
                val (bmi, catKey) = calculateBMI(weight, heightCm)
                binding.tvBmiNumber.text = String.format(Locale.US, "%.1f", bmi)
                binding.tvBmiCategory.text = localizeCategory(catKey)
                // show result card
                binding.cvResult.visibility = View.VISIBLE

                // persist reading
                saveToHistory(bmi, catKey)
                showWeightAdvice()
            } else {
                binding.cvResult.visibility = View.GONE
            }
        }

        // calculate previous BMI
        binding.btnCalculatePrevious.setOnClickListener {
            val prevWeight = binding.etPreviousWeight.text.toString().toDoubleOrNull()
            val heightCm = binding.etHeight.text.toString().toDoubleOrNull()

            if (prevWeight != null && heightCm != null && heightCm > 0) {
                val (prevBmi, prevCatKey) = calculateBMI(prevWeight, heightCm)
                binding.tvPrevBmiNumber.text = String.format(Locale.US, "%.1f", prevBmi)
                binding.tvPrevBmiCategory.text = localizeCategory(prevCatKey)
                binding.cvPreviousResult.visibility = View.VISIBLE

                showWeightAdvice()
            } else {
                binding.cvPreviousResult.visibility = View.GONE
            }
        }

        // open history screen
        binding.btnViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    // launch system speech recognizer
    private fun launchSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt))
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            // no speech app on device
            Toast.makeText(this, getString(R.string.speech_not_available), Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateBMI(weightKg: Double, heightCm: Double): Pair<Double, String> {
        // cm → metres
        val heightM = heightCm / 100.0
        // BMI = kg / m²
        val bmi = weightKg / (heightM * heightM)
        // return locale-neutral key
        val catKey = when {
            bmi < 18.5 -> "underweight"
            bmi < 25.0 -> "normal"
            bmi < 30.0 -> "overweight"
            else       -> "obese"
        }
        return Pair(bmi, catKey)
    }

    // key → localized label (also handles legacy English labels)
    fun localizeCategory(key: String): String = when (key.lowercase()) {
        "normal", "normal weight"    -> getString(R.string.cat_normal)
        "overweight"                 -> getString(R.string.cat_overweight)
        "obese"                      -> getString(R.string.cat_obese)
        "underweight"                -> getString(R.string.cat_underweight)
        else                         -> key
    }

    private fun showWeightAdvice() {
        val current  = binding.etWeight.text.toString().toDoubleOrNull()
        val previous = binding.etPreviousWeight.text.toString().toDoubleOrNull()

        if (current != null && previous != null) {
            // compare weight trend
            val advice = when {
                current > previous -> getString(R.string.advice_gaining)
                current < previous -> getString(R.string.advice_losing)
                else               -> getString(R.string.advice_constant)
            }
            binding.tvWeightAdvice.text = advice
            binding.tvWeightAdvice.visibility = View.VISIBLE
        } else {
            binding.tvWeightAdvice.visibility = View.GONE
        }
    }

    private fun saveToHistory(bmi: Double, catKey: String) {
        val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val entry = String.format(Locale.US, "%.1f|%s|%s", bmi, catKey, timestamp)

        val entries = getHistoryEntries().toMutableList()
        // newest first
        entries.add(0, entry)
        // keep 10 max
        if (entries.size > 10) entries.removeAt(entries.size - 1)

        prefs.edit().putString("entries", entries.joinToString(";")).apply()
    }

    private fun getHistoryEntries(): List<String> {
        val raw = prefs.getString("entries", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(";")
    }

    // show current locale code on button
    private fun updateLanguageButton() {
        val locale = AppCompatDelegate.getApplicationLocales()
        val tag = if (locale.isEmpty) Locale.getDefault().language
                  else locale.get(0)?.language ?: "en"
        binding.btnLanguage.text = tag.uppercase()
    }
}
