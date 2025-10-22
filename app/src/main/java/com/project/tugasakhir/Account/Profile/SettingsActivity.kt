package com.project.tugasakhir.Account.Profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set initial data from Firestore
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Cek apakah pengguna login menggunakan Google
            val isGoogleLogin = currentUser.providerData.any { it.providerId == "google.com" }

            if (isGoogleLogin) {
                // Jika login dengan Google, tampilkan hint text di email dan password
                binding.etEmail.hint = "Akun Google tidak dapat merubah Email"
                binding.etNewPassword.isEnabled = false
                binding.etConfirmPassword.isEnabled = false
                binding.etEmail.isEnabled = false
            } else {
                // Untuk akun lain, biarkan email dan password bisa diubah
                binding.etEmail.isEnabled = true
                binding.etNewPassword.isEnabled = true
                binding.etConfirmPassword.isEnabled = true
            }

            binding.etEmail.setText(currentUser.email)

            binding.btnUpdateProfile.setOnClickListener {
                val nama = binding.etNama.text.toString().trim()
                val phone = binding.etPhone.text.toString().trim()
                val userAddress = binding.etUserAddress.text.toString().trim()
                val newPassword = binding.etNewPassword.text.toString().trim()
                val confirmPassword = binding.etConfirmPassword.text.toString().trim()

                if (newPassword != confirmPassword) {
                    Toast.makeText(this, "Password tidak cocok", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Update profile data
                val userUpdates = hashMapOf<String, Any>(
                    "nama" to nama,
                    "phone" to phone,
                    "userAddress" to userAddress
                )

                val userRef = db.collection("users").document(currentUser.uid)
                userRef.update(userUpdates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update profile: ${it.message}", Toast.LENGTH_SHORT).show()
                    }

                // If password is changed, update password
                if (newPassword.isNotEmpty()) {
                    currentUser.updatePassword(newPassword)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to update password: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }
        } else {
            // Handle case when no user is logged in
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
}
