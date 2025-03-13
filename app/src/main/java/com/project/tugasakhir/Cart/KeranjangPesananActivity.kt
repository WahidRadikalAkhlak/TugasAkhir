package com.project.tugasakhir.Cart

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.project.tugasakhir.R

class KeranjangPesananActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keranjang_pesanan)

        val radioGroup: RadioGroup = findViewById(R.id.metode_pembayaran)

        // Menambahkan listener untuk menangani padding sistem bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Menambahkan data ke Spinner
        val bankNames = arrayOf("Bank BNI", "Bank BCA", "Bank BRI", "Bank Mandiri")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bankNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
}