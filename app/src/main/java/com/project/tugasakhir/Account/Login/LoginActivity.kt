package com.project.tugasakhir.Account.Login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Account.AccountFragment
import com.project.tugasakhir.Account.Register.RegisterActivity
import com.project.tugasakhir.Data.User
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

        // Handle login button click
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // Navigate to register activity
        binding.tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser(email: String, password: String) {
        val hashedPassword = hashPassword(password)

        // Query Firestore to find the user by email
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result != null) {
                    val document = task.result?.documents?.firstOrNull()
                    if (document != null && document.exists()) {
                        val storedPassword = document.getString("password")
                        if (storedPassword == hashedPassword) {
                            val user = User(
                                id = document.id,
                                email = document.getString("email") ?: "",
                                nama = document.getString("nama") ?: "",
                                password = document.getString("password") ?: ""
                            )
                            saveLoginState(user)

                            // Pastikan username tidak null
                            val username =
                                user.nama ?: "Unknown" // Memberikan nilai default jika null

                            // Navigasi ke AccountFragment
                            navigateToAccountFragment(username)
                        } else {
                            Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "No user found with this email", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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
        // Hash the password using SHA-256
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(StandardCharsets.UTF_8))
        val stringBuilder = StringBuilder()
        for (byte in hashBytes) {
            stringBuilder.append(String.format("%02x", byte))
        }
        return stringBuilder.toString()
    }

    private fun navigateToAccountFragment(username: String) {
        // Pastikan Activity Login tidak memiliki FragmentContainerView
        // Ganti fragment menggunakan FragmentTransaction
        val accountFragment = AccountFragment()

        // Menyertakan data username ke dalam fragment
        val bundle = Bundle().apply {
            putString("USERNAME", username) // Pass username to AccountFragment
        }

        // Set the arguments for the fragment
        accountFragment.arguments = bundle

        // Use FragmentTransaction to replace the current fragment
        supportFragmentManager.beginTransaction()
            .replace(
                android.R.id.content,
                accountFragment
            )  // Replacing the root view of the activity
            .commit()
    }
}