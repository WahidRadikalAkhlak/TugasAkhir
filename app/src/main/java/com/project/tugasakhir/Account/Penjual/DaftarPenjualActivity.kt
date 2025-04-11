package com.project.tugasakhir.Account.Penjual

import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.databinding.ActivityDaftarPenjualBinding  // Import View Binding
import com.project.tugasakhir.R

class DaftarPenjualActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDaftarPenjualBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi View Binding
        binding = ActivityDaftarPenjualBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set listener untuk tombol Daftar
        binding.btnDaftar.setOnClickListener {
            registerSeller()
        }
    }

    private fun registerSeller() {
        val noHp = binding.etNoHp.text.toString().trim()
        val alamatToko = binding.etAlamatToko.text.toString().trim()
        val noIzinUsaha = binding.etNoIzinusaha.text.toString().trim()
        val deskripsi = binding.etDeskripsi.text.toString().trim()
        val sosialMedia = binding.etSosialMedia.text.toString().trim()

        // Validasi input
        if (noHp.isEmpty() || alamatToko.isEmpty() || noIzinUsaha.isEmpty() || deskripsi.isEmpty() || sosialMedia.isEmpty()) {
            Toast.makeText(this, "Harap isi semua kolom!", Toast.LENGTH_SHORT).show()
            return
        }

        // Menyiapkan data untuk disimpan ke Firestore
        val sellerData = hashMapOf(
            "noHp" to noHp,
            "alamatToko" to alamatToko,
            "noIzinUsaha" to noIzinUsaha,
            "deskripsi" to deskripsi,
            "sosialMedia" to sosialMedia
        )

        // Menyimpan data penjual ke Firestore
        db.collection("penjual")
            .add(sellerData)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Akun berhasil dibuat!", Toast.LENGTH_SHORT).show()
                finish() // Kembali ke aktivitas sebelumnya atau tampilkan halaman sukses
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal membuat akun: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
