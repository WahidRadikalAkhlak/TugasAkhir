package com.project.tugasakhir.Account

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Account.Login.LoginActivity
import com.project.tugasakhir.Account.Penjual.DaftarPenjualActivity
import com.project.tugasakhir.Account.Penjual.DaftarProductActivity
import com.project.tugasakhir.Account.Profile.LanguagesActivity
import com.project.tugasakhir.Account.Profile.SettingsActivity
import com.project.tugasakhir.databinding.FragmentAccountBinding

class AccountFragment : Fragment() {

    private lateinit var binding: FragmentAccountBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("theme_pref", Context.MODE_PRIVATE)
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val email = currentUser.email ?: ""
            val name = currentUser.displayName ?: "User"

            binding.tvUserName.text = name
            binding.email.text = email
            binding.tvLogout.text = "Logout"

            // Check business account status based on email, not uid
            checkBusinessAccountStatus(email)
            ensureBusinessAndClaim(email, name)
        } else {
            // User not logged in
            binding.tvUserName.text = "Guest"
            binding.email.text = "Silakan login"
            binding.tvLogout.text = "Login"
            binding.btnDaftarbisnis.visibility = View.GONE
            binding.btnDaftarProduct.visibility = View.GONE
        }

        binding.clLogout.setOnClickListener {
            if (currentUser != null) {
                auth.signOut() // Sign out the user
                Toast.makeText(requireContext(), "Berhasil logout", Toast.LENGTH_SHORT).show()
            }
            navigateToLogin() // Navigate to login screen
        }

        if (currentUser != null) {
            // Code that accesses currentUser properties should be inside this check
            val email = currentUser.email ?: ""
            val name = currentUser.displayName ?: "User"
            // Use these variables for UI updates
        }

        binding.clbahasa.setOnClickListener {
            navigateToLanguages()
        }

        binding.btnDaftarbisnis.setOnClickListener {
            if (currentUser == null) {
                navigateToLogin() // Navigate to login if not logged in
            } else {
                val intent = Intent(requireContext(), DaftarPenjualActivity::class.java).apply {
                    putExtra("EMAIL", currentUser.email)
                    putExtra("USERNAME", currentUser.displayName)
                }
                startActivity(intent)
            }
        }

        binding.clsettings.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                // If the user is not logged in, show a Toast and navigate to login
                Toast.makeText(requireContext(), "Harap Login Terlebih Dahulu", Toast.LENGTH_SHORT).show()
                navigateToLogin() // This method will take the user to the LoginActivity
            } else {
                // If the user is logged in, navigate to Settings
                navigateToSettings()
            }
        }

        binding.btnDaftarProduct.setOnClickListener {
            if (currentUser == null) {
                navigateToLogin() // Navigate to login if not logged in
            } else {
                checkBusinessAccountStatusForProduct(currentUser.email ?: "")
            }
        }
    }

    private fun ensureBusinessAndClaim(email: String, displayName: String) {
        if (email.isBlank()) return

        val user = auth.currentUser ?: return
        val sellerUID = user.uid
        val docId = email.replace(".", "_").replace("@", "_")

        val sellerRef = db.collection("sellers").document(docId)

        sellerRef.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    // buat seller doc otomatis
                    val data = hashMapOf(
                        "email" to email,
                        "username" to displayName,
                        "isBusinessAccount" to true,
                        "alamatToko" to "-",
                        "deskripsi" to "Akun bisnis otomatis dari login",
                        "noHp" to "-",
                        "noIzinUsaha" to "-",
                        "sosialMedia" to "-",
                        "shareLokasi" to "-",
                        "claimedDummyProducts" to false
                    )
                    sellerRef.set(data)
                }

                val alreadyClaimed = snap.getBoolean("claimedDummyProducts") ?: false
                if (!alreadyClaimed) {
                    claimDummyProductsForThisSeller(email, sellerUID, displayName) {
                        // set flag agar hanya sekali
                        sellerRef.set(mapOf("claimedDummyProducts" to true), com.google.firebase.firestore.SetOptions.merge())
                    }
                }
            }
    }

    private fun claimDummyProductsForThisSeller(
        email: String,
        sellerUID: String,
        displayName: String,
        onDone: () -> Unit
    ) {
        // cari produk yang belum punya owner (email & sellerUID tidak ada / null)
        db.collection("products")
            .whereEqualTo("isAvailable", true)
            .limit(10) // batasi agar ringan
            .get()
            .addOnSuccessListener { docs ->
                val toClaim = docs.documents.filter { d ->
                    // belum dimiliki siapa pun?
                    d.getString("email").isNullOrBlank() && d.getString("sellerUID").isNullOrBlank()
                }
                if (toClaim.isEmpty()) {
                    onDone()
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                toClaim.forEach { d ->
                    val ref = d.reference
                    batch.update(ref, mapOf(
                        "email" to email,
                        "sellerUID" to sellerUID,
                        "userName" to displayName
                    ))
                }
                batch.commit()
                    .addOnSuccessListener { onDone() }
                    .addOnFailureListener { onDone() }
            }
            .addOnFailureListener { onDone() }
    }

    private fun navigateToLanguages() {
        val intent = Intent(requireContext(), LanguagesActivity::class.java)
        startActivity(intent)
    }

    private fun checkBusinessAccountStatus(email: String) {
        if (email.isBlank()) {
            binding.btnDaftarbisnis.visibility = View.GONE
            binding.btnDaftarProduct.visibility = View.GONE
            return
        }

        val docId = email.replace(".", "_").replace("@", "_")

        db.collection("sellers").document(docId).get()
            .addOnSuccessListener { doc ->
                val isBusinessAccount = doc.exists() && (doc.getBoolean("isBusinessAccount") ?: false)

                if (isBusinessAccount) {
                    // Sudah bisnis
                    binding.btnDaftarProduct.visibility = View.VISIBLE
                    binding.btnDaftarbisnis.visibility = View.GONE
                    setupBtnDaftarProduct(true)
                } else {
                    // cek produk, mungkin dia sudah jualan
                    db.collection("products")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { products ->
                            if (!products.isEmpty) {
                                // ada produk, otomatis akun bisnis
                                val autoData = mapOf(
                                    "email" to email,
                                    "username" to (auth.currentUser?.displayName ?: "User"),
                                    "isBusinessAccount" to true,
                                    "alamatToko" to products.documents.first().getString("alamatToko").orEmpty(),
                                    "deskripsi" to "Akun bisnis otomatis dari produk yang sudah ada",
                                    "noHp" to "-",
                                    "noIzinUsaha" to "-",
                                    "sosialMedia" to "-",
                                    "shareLokasi" to "-"
                                )
                                db.collection("sellers").document(docId).set(autoData)
                                binding.btnDaftarProduct.visibility = View.VISIBLE
                                binding.btnDaftarbisnis.visibility = View.GONE
                                setupBtnDaftarProduct(true)
                            } else {
                                // memang benar belum bisnis
                                binding.btnDaftarProduct.visibility = View.GONE
                                binding.btnDaftarbisnis.visibility = View.VISIBLE
                            }
                        }
                }
            }
            .addOnFailureListener {
                binding.btnDaftarProduct.visibility = View.GONE
                binding.btnDaftarbisnis.visibility = View.VISIBLE
                setupBtnDaftarProduct(false)
            }
    }

    private fun checkBusinessAccountStatusForProduct(email: String) {
        if (email.isBlank()) {
            Toast.makeText(requireContext(), "Email tidak valid. Silakan login ulang.", Toast.LENGTH_SHORT).show()
            navigateToLogin() // Navigate to login if email is invalid
            return
        }

        val docId = email.replace(".", "_")

        db.collection("sellers").document(docId).get()
            .addOnSuccessListener { doc ->
                val isBusinessAccount = doc.exists() && (doc.getBoolean("isBusinessAccount") ?: false)
                val currentUser = auth.currentUser

                if (currentUser == null) {
                    Toast.makeText(requireContext(), "Silakan login terlebih dahulu.", Toast.LENGTH_SHORT).show()
                    navigateToLogin() // Navigate to login if not logged in
                    return@addOnSuccessListener
                }

                if (isBusinessAccount) {
                    val intent = Intent(requireContext(), DaftarProductActivity::class.java).apply {
                        putExtra("EMAIL", currentUser.email)
                        putExtra("USERNAME", currentUser.displayName)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "Anda belum terdaftar sebagai akun bisnis, silakan daftar terlebih dahulu.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), DaftarPenjualActivity::class.java).apply {
                        putExtra("EMAIL", currentUser.email)
                        putExtra("USERNAME", currentUser.displayName)
                    }
                    startActivity(intent)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupBtnDaftarProduct(isRegistered: Boolean) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.btnDaftarProduct.setOnClickListener {
                Toast.makeText(requireContext(), "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
                navigateToLogin()
            }
            return
        }

        binding.btnDaftarProduct.setOnClickListener {
            if (isRegistered) {
                val intent = Intent(requireContext(), DaftarProductActivity::class.java).apply {
                    putExtra("EMAIL", currentUser.email)
                    putExtra("USERNAME", currentUser.displayName)
                }
                startActivity(intent)
            } else {
                val intent = Intent(requireContext(), DaftarPenjualActivity::class.java).apply {
                    putExtra("EMAIL", currentUser.email)
                    putExtra("USERNAME", currentUser.displayName)
                }
                startActivity(intent)
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
        requireActivity().finish()  // Finish the current activity to prevent going back
    }

    private fun navigateToSettings() {
        val intent = Intent(requireContext(), SettingsActivity::class.java)
        startActivity(intent)
    }
}
