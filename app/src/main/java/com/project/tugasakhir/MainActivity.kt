package com.project.tugasakhir

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.project.tugasakhir.Account.AccountFragment
import com.project.tugasakhir.Cart.CartFragment
import com.project.tugasakhir.Chat.ChatFragment
import com.project.tugasakhir.Katalog.KatalogFragment
import com.project.tugasakhir.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressBar.visibility = View.VISIBLE

        val bottomNavigationView = binding.bottomnav
        loadFragment(KatalogFragment())
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.katalog -> {
                    loadFragment(KatalogFragment())
                    return@setOnItemSelectedListener true
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }

                R.id.chat -> {
                    loadFragment(ChatFragment())
                    return@setOnItemSelectedListener true
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }

                R.id.cart -> {
                    loadFragment(CartFragment())
                    return@setOnItemSelectedListener true
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }

                R.id.account -> {
                    loadFragment(AccountFragment())
                    return@setOnItemSelectedListener true
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }
            }
            false
        }
    }

    private fun loadFragment(fragment: Fragment) {
        // Show the ProgressBar while loading
        binding.progressBar.visibility = View.VISIBLE
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.framelayoutt, fragment)
        transaction.commit()
        // Hide the ProgressBar after fragment has been loaded
        // We can assume that once the fragment transaction is committed, the content is loaded
        // You can handle this more explicitly depending on your fragment loading strategy
        binding.progressBar.visibility = View.GONE
    }
}