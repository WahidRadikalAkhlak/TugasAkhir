package com.project.tugasakhir

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.project.tugasakhir.OnBoarding.OnboardingActivity
import com.project.tugasakhir.MainActivity
import com.project.tugasakhir.databinding.ActivitySplashScreenBinding

class SplashScreenActivity : ComponentActivity() {

    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.Splashscreen) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Handler().postDelayed({
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val isFirstTime = sharedPreferences.getBoolean("isFirstTime", true)

            val intent = if (isFirstTime) {
                // Set the preference that onboarding has been completed
                sharedPreferences.edit().putBoolean("isFirstTime", false).apply()
                Intent(this, OnboardingActivity::class.java)  // Show onboarding
            } else {
                // Check if the onboarding has been completed
                val isOnboardingFinished = sharedPreferences.getBoolean("Finished", false)
                if (isOnboardingFinished) {
                    Intent(this, MainActivity::class.java)  // Skip onboarding and go to main
                } else {
                    Intent(this, OnboardingActivity::class.java)  // Show onboarding if not finished
                }
            }

            startActivity(intent)
            finish()  // Close SplashActivity
        }, 3000)  // 3 seconds delay
    }
}
