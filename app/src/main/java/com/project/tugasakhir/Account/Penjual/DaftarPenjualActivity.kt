package com.project.tugasakhir.Account.Penjual

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Account.AccountFragment
import com.project.tugasakhir.databinding.ActivityDaftarPenjualBinding  // Import View Binding
import com.project.tugasakhir.R

class DaftarPenjualActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDaftarPenjualBinding
    private val db = FirebaseFirestore.getInstance()

    private lateinit var userEmail: String
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi View Binding
        binding = ActivityDaftarPenjualBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ambil data dari Intent
        userEmail = intent.getStringExtra("EMAIL") ?: ""
        username = intent.getStringExtra("USERNAME") ?: ""

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
            "sosialMedia" to sosialMedia,
            "email" to userEmail,
            "username" to username,
            "isBusinessAccount" to true
        )

        // Menyimpan data penjual ke Firestore
        db.collection("penjual")
            .add(sellerData)
            .addOnSuccessListener { documentReference ->
                // Setelah berhasil mendaftar sebagai akun bisnis, update status akun
                updateUserStatus()

                // Beri tahu pengguna bahwa pendaftaran berhasil
                Toast.makeText(this, "Akun bisnis berhasil dibuat!", Toast.LENGTH_SHORT).show()

                // Kembali ke FragmentAccount dan sembunyikan tombol Daftar Bisnis
                val intent = Intent(this, AccountFragment::class.java) // Asumsi MainActivity adalah tempat AccountFragment
                intent.putExtra("IS_BUSINESS_ACCOUNT", true)
                startActivity(intent)
                finish()  // Menutup activity DaftarPenjualActivity
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal membuat akun bisnis: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserStatus() {
        // Update status akun pengguna di koleksi "users" untuk menandai sebagai akun bisnis
        val userRef = db.collection("users").whereEqualTo("email", userEmail).get()
        userRef.addOnSuccessListener { documents ->
            for (document in documents) {
                document.reference.update("isBusinessAccount", true) // Update status sebagai akun bisnis
            }
        }
    }
}
