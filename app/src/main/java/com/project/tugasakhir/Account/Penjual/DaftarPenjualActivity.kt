package com.project.tugasakhir.Account.Penjual

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityDaftarPenjualBinding

class DaftarPenjualActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDaftarPenjualBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var isRegistering = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaftarPenjualBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnDaftar.backgroundTintList =
            ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))
        binding.btnDaftar.setOnClickListener {
            if (!isRegistering) {
                registerSeller()
            }
        }
    }

    private fun registerSeller() {
        showLoadingState(true)

        val noHp = binding.etNoHp.text.toString().trim()
        val alamatToko = binding.etAlamatToko.text.toString().trim()
        val noIzinUsaha = binding.etNoIzinusaha.text.toString().trim()
        val deskripsi = binding.etDeskripsi.text.toString().trim()
        val sosialMedia = binding.etSosialMedia.text.toString().trim()

        if (noHp.isEmpty() || alamatToko.isEmpty() || noIzinUsaha.isEmpty() || deskripsi.isEmpty() || sosialMedia.isEmpty()) {
            Toast.makeText(this, "Harap isi semua kolom!", Toast.LENGTH_SHORT).show()
            showLoadingState(false)
            return
        }

        if (!TextUtils.isDigitsOnly(noHp)) {
            Toast.makeText(this, "Nomor HP hanya boleh angka!", Toast.LENGTH_SHORT).show()
            showLoadingState(false)
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(
                this,
                "User belum login, silakan login terlebih dahulu",
                Toast.LENGTH_SHORT
            ).show()
            showLoadingState(false)
            return
        }

        val email = currentUser.email ?: ""
        if (email.isBlank()) {
            Toast.makeText(this, "Email user tidak tersedia", Toast.LENGTH_SHORT).show()
            showLoadingState(false)
            return
        }

        val username = currentUser.displayName ?: ""

        val sellerData = hashMapOf(
            "noHp" to noHp,
            "alamatToko" to alamatToko,
            "noIzinUsaha" to noIzinUsaha,
            "deskripsi" to deskripsi,
            "sosialMedia" to sosialMedia,
            "email" to email,
            "username" to username,
            "isBusinessAccount" to true
        )

        // Gunakan email sebagai ID dokumen, tapi ganti karakter '.' supaya valid di Firestore
        val docId = email.replace(".", "_")

        db.collection("penjual")
            .document(docId)
            .set(sellerData)
            .addOnSuccessListener {
                updateUserStatus(email)

                Toast.makeText(this, "Akun bisnis berhasil dibuat!", Toast.LENGTH_SHORT).show()
                clearInputFields()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal membuat akun bisnis: ${e.message}", Toast.LENGTH_LONG)
                    .show()
                e.printStackTrace()
                showLoadingState(false)
            }
    }

    private fun updateUserStatus(email: String) {
        val userRef = db.collection("users")
        val docId = email.replace(".", "_")

        // Coba update dokumen berdasarkan email sebagai ID dokumen
        userRef.document(docId).update("isBusinessAccount", true)
            .addOnSuccessListener {
                // update sukses
            }
            .addOnFailureListener {
                // Kalau gagal update (mungkin dokumen belum ada), buat dokumen baru
                userRef.document(docId).set(mapOf("email" to email, "isBusinessAccount" to true))
                    .addOnSuccessListener { /* sukses buat baru */ }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Gagal update data user: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        showLoadingState(false)
                    }
            }
    }

    private fun showLoadingState(isLoading: Boolean) {
        isRegistering = isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnDaftar.isEnabled = !isLoading
    }

    private fun clearInputFields() {
        binding.etNoHp.text?.clear()
        binding.etAlamatToko.text?.clear()
        binding.etNoIzinusaha.text?.clear()
        binding.etDeskripsi.text?.clear()
        binding.etSosialMedia.text?.clear()
    }
}
