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
    private var isEditing = false
    private var isRegistering = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaftarPenjualBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnDaftar.backgroundTintList =
            ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))
        binding.btnDaftar.setOnClickListener {
            if (isEditing) {
                updateSellerProfile()
            } else {
                registerSeller()
            }
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        }
        loadSellerProfile()
    }

    private fun registerSeller() {
        showLoadingState(true)

        val noHp = binding.etNoHp.text.toString().trim()
        val alamatToko = binding.etAlamatToko.text.toString().trim()
        val noIzinUsaha = binding.etNoIzinusaha.text.toString().trim()
        val deskripsi = binding.etDeskripsi.text.toString().trim()
        val sosialMedia = binding.etSosialMedia.text.toString().trim()
        val shareLokasi = binding.etShareLokasi.text.toString().trim()

        // Validasi input
        if (noHp.isEmpty() || alamatToko.isEmpty() || noIzinUsaha.isEmpty() || deskripsi.isEmpty() || sosialMedia.isEmpty() || shareLokasi.isEmpty()) {
            Toast.makeText(this, "Harap isi semua kolom!", Toast.LENGTH_SHORT).show()
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

        // Menyimpan data penjual ke Firestore
        val sellerData = hashMapOf(
            "noHp" to noHp,
            "alamatToko" to alamatToko,
            "noIzinUsaha" to noIzinUsaha,
            "deskripsi" to deskripsi,
            "sosialMedia" to sosialMedia,
            "email" to email,
            "username" to username,
            "isBusinessAccount" to true,
            "shareLokasi" to shareLokasi // Menyimpan Link Lokasi
        )

        // Ganti karakter '.' dalam email agar valid di Firestore
        val docId = email.replace(".", "_").replace("@", "_")

        db.collection("seller")
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

    private fun loadSellerProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email ?: return

        val docId = email.replace(".", "_").replace("@", "_")

        db.collection("seller").document(docId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val noHp = document.getString("noHp") ?: ""
                    val alamatToko = document.getString("alamatToko") ?: ""
                    val noIzinUsaha = document.getString("noIzinUsaha") ?: ""
                    val deskripsi = document.getString("deskripsi") ?: ""
                    val sosialMedia = document.getString("sosialMedia") ?: ""
                    val shareLokasi = document.getString("shareLokasi") ?: ""

                    // Menampilkan data di formulir
                    binding.etNoHp.setText(noHp)
                    binding.etAlamatToko.setText(alamatToko)
                    binding.etNoIzinusaha.setText(noIzinUsaha)
                    binding.etDeskripsi.setText(deskripsi)
                    binding.etSosialMedia.setText(sosialMedia)
                    binding.etShareLokasi.setText(shareLokasi)

                    // Mengubah status isEditing menjadi true karena data profil sudah ada
                    isEditing = true

                    // Ubah teks tombol menjadi "Simpan Perubahan"
                    binding.btnDaftar.text = "Simpan Perubahan"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memuat profil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateSellerProfile() {
        showLoadingState(true)

        val noHp = binding.etNoHp.text.toString().trim()
        val alamatToko = binding.etAlamatToko.text.toString().trim()
        val noIzinUsaha = binding.etNoIzinusaha.text.toString().trim()
        val deskripsi = binding.etDeskripsi.text.toString().trim()
        val sosialMedia = binding.etSosialMedia.text.toString().trim()
        val shareLokasi = binding.etShareLokasi.text.toString().trim()

        // Validasi input
        if (noHp.isEmpty() || alamatToko.isEmpty() || noIzinUsaha.isEmpty() || deskripsi.isEmpty() || sosialMedia.isEmpty() || shareLokasi.isEmpty()) {
            Toast.makeText(this, "Harap isi semua kolom!", Toast.LENGTH_SHORT).show()
            showLoadingState(false)
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email ?: ""
        if (email.isBlank()) {
            Toast.makeText(this, "Email user tidak tersedia", Toast.LENGTH_SHORT).show()
            showLoadingState(false)
            return
        }

        val docId = email.replace(".", "_").replace("@", "_")

        // Update data profil penjual di Firestore
        val sellerData: MutableMap<String, Any> = hashMapOf(
            "noHp" to noHp,
            "alamatToko" to alamatToko,
            "noIzinUsaha" to noIzinUsaha,
            "deskripsi" to deskripsi,
            "sosialMedia" to sosialMedia,
            "shareLokasi" to shareLokasi
        )

        db.collection("seller").document(docId)
            .update(sellerData)
            .addOnSuccessListener {
                Toast.makeText(this, "Profil bisnis berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                showLoadingState(false)
                finish() // Menutup activity setelah berhasil update
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memperbarui profil: ${e.message}", Toast.LENGTH_LONG).show()
                showLoadingState(false)
            }
    }

    private fun updateUserStatus(email: String) {
        val userRef = db.collection("users")
        val docId = email.replace(".", "_").replace("@", "_")

        userRef.document(docId).update("isBusinessAccount", true)
            .addOnSuccessListener {
                // update sukses
            }
            .addOnFailureListener {
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
