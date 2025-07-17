package com.project.tugasakhir

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.project.tugasakhir.Account.AccountFragment
import com.project.tugasakhir.Cart.CartFragment
import com.project.tugasakhir.Chat.ChatFragment
import com.project.tugasakhir.Katalog.KatalogFragment
import com.project.tugasakhir.OnBoarding.OnboardingActivity
import com.project.tugasakhir.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if the user has completed onboarding
        val sharedPreferences = getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
        val isOnboardingFinished = sharedPreferences.getBoolean("Finished", false)

        // If onboarding is not finished, navigate to OnboardingActivity
        if (!isOnboardingFinished) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()  // Close MainActivity to prevent navigating back
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        val bottomNavigationView = binding.bottomnav
        loadFragment(KatalogFragment())
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.katalog -> {
                    loadFragment(KatalogFragment())
                    updateItemBackground(item, R.color.chip_bg_states) // Custom background color
                    binding.bottomnav.setBackgroundColor(Color.TRANSPARENT)
                    return@setOnItemSelectedListener true
                }

                R.id.chat -> {
                    loadFragment(ChatFragment())
                    updateItemBackground(item, R.color.chip_bg_states) // Custom background color
                    binding.bottomnav.setBackgroundColor(Color.TRANSPARENT)
                    return@setOnItemSelectedListener true
                }

                R.id.cart -> {
                    loadFragment(CartFragment())
                    updateItemBackground(item, R.color.chip_bg_states) // Custom background color
                    binding.bottomnav.setBackgroundColor(Color.TRANSPARENT)
                    return@setOnItemSelectedListener true
                }

                R.id.account -> {
                    loadFragment(AccountFragment())
                    updateItemBackground(item, R.color.chip_bg_states) // Custom background color
                    binding.bottomnav.setBackgroundColor(Color.TRANSPARENT)
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
    }

        private fun updateItemBackground(item: MenuItem, color: Int) {
            item.icon?.setTint(getColor(color)) // Optionally change icon color
            binding.bottomnav.setBackgroundColor(getColor(color)) // Set background color dynamically
        }

    private fun loadFragment(fragment: Fragment) {
        binding.progressBar.visibility = View.VISIBLE
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.framelayoutt, fragment)
        transaction.commit()
        binding.progressBar.visibility = View.GONE
    }
}
