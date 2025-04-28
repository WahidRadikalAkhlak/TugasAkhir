package com.project.tugasakhir.Account.Login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Account.AccountFragment
import com.project.tugasakhir.Account.Register.RegisterActivity
import com.project.tugasakhir.Data.User
import com.project.tugasakhir.Katalog.ProductPenjual.ProductBaruActivity
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityLoginBinding
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tombol login untuk memulai proses login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
            }
        }

        // Tombol untuk membuka halaman register
        binding.tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser(email: String, password: String) {
        val hashedPassword = hashPassword(password)

        // Query Firestore untuk menemukan user berdasarkan email
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result != null) {
                    val document = task.result?.documents?.firstOrNull()
                    if (document != null && document.exists()) {
                        val storedPassword = document.getString("password")
                        if (storedPassword == hashedPassword) {
                            // Membaca data dan mengonversi menjadi objek User
                            val user = User(
                                id = document.id,
                                email = document.getString("email") ?: "",
                                nama = document.getString("nama") ?: "",
                                password = document.getString("password") ?: ""
                            )

                            // Menyimpan status login
                            saveLoginState(user)

                            // Kirim data ke AccountFragment
                            val fragment = AccountFragment()

                            val bundle = Bundle().apply {
                                putString("userName", user.nama)
                                putString("userEmail", user.email)
                            }
                            fragment.arguments = bundle

                            // Gantikan activity dengan AccountFragment
                            val transaction = supportFragmentManager.beginTransaction()
                            transaction.replace(R.id.fragment_container, fragment)
                            transaction.commit()

                        } else {
                            Toast.makeText(this, "Password salah", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Tidak ada user dengan email ini", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Login gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveLoginState(user: User) {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("USER_ID", user.id)
        editor.putString("USER_EMAIL", user.email)
        editor.putString("USER_NAME", user.nama)
        editor.apply()
    }

    private fun hashPassword(password: String): String {
        return try {
            // Menggunakan SHA-256 untuk melakukan hash password
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.toByteArray(StandardCharsets.UTF_8))
            val stringBuilder = StringBuilder()
            for (byte in hashBytes) {
                stringBuilder.append(String.format("%02x", byte))
            }
            stringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
