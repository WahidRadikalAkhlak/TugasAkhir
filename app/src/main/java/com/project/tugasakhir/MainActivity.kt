package com.project.tugasakhir

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.project.tugasakhir.Account.AccountFragment
import com.project.tugasakhir.Article.ArticleFragment
import com.project.tugasakhir.Chat.ChatFragment
import com.project.tugasakhir.Map.MapFragment
import com.project.tugasakhir.Cart.CartFragment
import com.project.tugasakhir.Katalog.KatalogFragment
import com.project.tugasakhir.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val bottomNavigationView = binding.bottomnav
        loadFragment(MapFragment())
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.map -> {
                    loadFragment(MapFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.katalog -> {
                    loadFragment(KatalogFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.chat -> {
                    loadFragment(ChatFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.cart -> {
                    loadFragment(CartFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.account -> {
                    loadFragment(AccountFragment())
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
    }
    private fun loadFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.framelayoutt,fragment)
        transaction.commit()
    }
}