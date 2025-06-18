package com.project.tugasakhir.Account.Login

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Account.AccountFragment
import com.project.tugasakhir.Account.Register.RegisterActivity
import com.project.tugasakhir.Data.User
import com.project.tugasakhir.MainActivity
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityLoginBinding
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        binding.btnLogin.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))
        binding.btnLogin.setTextColor(resources.getColor(R.color.black, null))
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid ?: ""

                    // Mengambil data pengguna dari Firestore
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            val username = doc.getString("nama") ?: "User"
                            val userAddress = doc.getString("userAddress") ?: "Alamat Tidak Tersedia"

                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("USERNAME", username)
                            intent.putExtra("userAddress", userAddress)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener {
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }


    private fun navigateToAccountFragment(username: String) {
        val accountFragment = AccountFragment()

        val bundle = Bundle().apply {
            putString("USERNAME", username)
        }
        accountFragment.arguments = bundle

        // Pastikan Anda menggunakan fragment transaction dengan benar
        supportFragmentManager.beginTransaction()
            .replace(R.id.framelayoutt, accountFragment) // Gantilah `R.id.fragment_container` sesuai dengan ID fragment container di MainActivity
            .addToBackStack(null)  // Jika ingin menambahkan ke back stack
            .commit()
    }
}