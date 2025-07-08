package com.project.tugasakhir.Account.Profile

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.project.tugasakhir.databinding.ActivityLanguagesBinding
import java.util.Locale

class LanguagesActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivityLanguagesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize ViewBinding
        binding = ActivityLanguagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("theme_pref", Context.MODE_PRIVATE)

        // Set up language buttons using binding
        binding.btnIndonesian.setOnClickListener {
            setLocale("id")
        }

        binding.btnEnglish.setOnClickListener {
            setLocale("en")
        }
    }

    private fun setLocale(languageCode: String) {
        // Set the language locale
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Save the selected language in SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString("LANGUAGE", languageCode)
        editor.apply()

        // Restart the activity to apply the new language
        recreate()
    }
}
