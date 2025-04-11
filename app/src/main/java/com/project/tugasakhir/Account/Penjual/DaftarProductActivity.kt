package com.project.tugasakhir.Account.Penjual

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityDaftarProductBinding
import com.project.tugasakhir.Katalog.ProductPenjual.ProductBaruActivity  // Import the target activity

class DaftarProductActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDaftarProductBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivityDaftarProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle Add Product button click to navigate to ProductBaruActivity
        binding.btnAddProduct.setOnClickListener {
            // Create an intent to open ProductBaruActivity
            val intent = Intent(this, ProductBaruActivity::class.java)
            startActivity(intent)  // Start the activity
        }
    }
}
