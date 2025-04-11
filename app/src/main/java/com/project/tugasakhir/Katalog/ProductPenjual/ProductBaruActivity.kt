package com.project.tugasakhir.Katalog.ProductPenjual

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityProductBaruBinding

class ProductBaruActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductBaruBinding  // Declare the binding variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivityProductBaruBinding.inflate(layoutInflater)
        setContentView(binding.root)  // Set the root view for this activity

        // Apply edge-to-edge settings
        enableEdgeToEdge()

        // Handle window insets (padding for system bars)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets  // Return the insets to indicate the operation is done
        }
    }
}