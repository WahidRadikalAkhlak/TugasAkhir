package com.project.tugasakhir.Account.Login

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Account.Register.RegisterActivity
import com.project.tugasakhir.Data.User
import com.project.tugasakhir.MainActivity
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = FirebaseFirestore.getInstance()

    // RC_SIGN_IN is used to identify the result of Google sign-in
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Your Web Client ID from Firebase Console
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        // Configure Login button with the color you want
        binding.btnLogin.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))
        binding.btnLogin.setTextColor(resources.getColor(R.color.black, null))

        // Login button functionality
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        // Register link
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Google Sign-In button functionality
        binding.rlGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid ?: ""

                    // Retrieve user data from Firestore
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            val username = doc.getString("nama") ?: "User"
                            val userAddress = doc.getString("userAddress") ?: "Address not available"

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

    // Google Sign-In
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val result = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = result.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken ?: "")
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid ?: ""

                    // Pastikan firebaseUser tidak null sebelum mengambil data
                    firebaseUser?.let { user ->
                        // Mengambil data pengguna dari Firestore
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                if (!doc.exists()) {
                                    // Pengguna baru, tambahkan data pengguna ke Firestore
                                    val newUser = User(
                                        id = uid,
                                        nama = user.displayName ?: "User",
                                        userAddress = "Address not available"  // Anda bisa mengganti ini dengan field lain jika diperlukan
                                    )

                                    // Menyimpan data pengguna baru ke Firestore
                                    db.collection("users").document(uid)
                                        .set(newUser)
                                        .addOnSuccessListener {
                                            // Lanjutkan ke MainActivity setelah berhasil menyimpan data
                                            val intent = Intent(this, MainActivity::class.java)
                                            intent.putExtra("USERNAME", newUser.nama)
                                            intent.putExtra("userAddress", newUser.userAddress)
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    // Pengguna sudah terdaftar di Firestore
                                    val username = doc.getString("nama") ?: "User"
                                    val userAddress = doc.getString("userAddress") ?: "Address not available"

                                    val intent = Intent(this, MainActivity::class.java)
                                    intent.putExtra("USERNAME", username)
                                    intent.putExtra("userAddress", userAddress)
                                    startActivity(intent)
                                    finish()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to retrieve user data", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
