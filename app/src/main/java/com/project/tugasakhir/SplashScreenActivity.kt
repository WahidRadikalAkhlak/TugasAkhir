package com.project.tugasakhir

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.project.tugasakhir.databinding.ActivitySplashScreenBinding // Import binding

class SplashActivity : ComponentActivity() {

    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Menghubungkan binding dengan layout
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mengaktifkan mode edge-to-edge
        enableEdgeToEdge()

        // Mengatur padding sesuai dengan system bars menggunakan ViewCompat
        ViewCompat.setOnApplyWindowInsetsListener(binding.Splashscreen) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Menambahkan delay 3 detik sebelum berpindah ke MainActivity
        Handler().postDelayed({
            // Intent untuk berpindah ke MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()  // Menutup SplashActivity agar tidak kembali ke splash screen
        }, 3000)  // 3 detik
    }
}
