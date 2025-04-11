package com.project.tugasakhir.Account.Register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Account.Login.LoginActivity
import com.project.tugasakhir.Data.User
import com.project.tugasakhir.databinding.ActivityRegisterBinding
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle the registration button click
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val name = binding.etName.text.toString()
            val phone = binding.etPhone.text.toString()

            // Validate that all fields are filled
            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && phone.isNotEmpty()) {
                registerUser(email, password, name, phone)
            } else {
                Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
            }
        }

        // Navigate to the login screen if the user already has an account
        binding.tvLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun registerUser(email: String, password: String, name: String, phone: String) {
        val hashedPassword = hashPassword(password)

        // Membuat objek User
        val user = User(id = "", email = email, nama = name, password = hashedPassword)

        // Periksa apakah email sudah ada di Firestore
        val usersRef = db.collection("users")
        usersRef.whereEqualTo("email", email).get()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result != null && task.result?.isEmpty == false) {
                    // Email sudah terdaftar
                    Toast.makeText(this, "Email sudah terdaftar", Toast.LENGTH_SHORT).show()
                } else {
                    // Simpan data user ke Firestore
                    val userData = hashMapOf(
                        "email" to user.email,
                        "nama" to user.nama,
                        "password" to user.password
                    )

                    db.collection("users").add(userData)
                        .addOnSuccessListener {
                            // Pendaftaran berhasil
                            Toast.makeText(this, "Pendaftaran berhasil", Toast.LENGTH_SHORT).show()
                            navigateToLogin() // Navigasi ke layar login setelah pendaftaran
                        }
                        .addOnFailureListener {
                            // Gagal menyimpan data user
                            Toast.makeText(this, "Gagal menyimpan data user", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    // Function to hash the password
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(StandardCharsets.UTF_8))
        val sb = StringBuilder()
        for (b in hashBytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString() // Return the hashed password
    }


    // Navigate to the login screen
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close RegisterActivity
    }
}
