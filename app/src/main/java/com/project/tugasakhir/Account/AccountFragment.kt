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

        val currentUser = auth.currentUser
        sharedPreferences =
            requireContext().getSharedPreferences("theme_pref", Context.MODE_PRIVATE)
        if (currentUser != null) {
            val email = currentUser.email ?: ""
            val name = currentUser.displayName ?: "User"

            binding.tvUserName.text = name
            binding.email.text = email
            binding.tvLogout.text = "Logout"

            // Panggil cek status bisnis berdasarkan email, bukan uid
            checkBusinessAccountStatus(email)

        } else {
            // User belum login
            binding.tvUserName.text = "Guest"
            binding.email.text = "Silakan login"
            binding.tvLogout.text = "Login"
            binding.btnDaftarbisnis.visibility = View.GONE
            binding.btnDaftarProduct.visibility = View.GONE
        }

        binding.clLogout.setOnClickListener {
            if (currentUser != null) {
                auth.signOut()
                Toast.makeText(requireContext(), "Berhasil logout", Toast.LENGTH_SHORT).show()
            }
            navigateToLogin()
        }

        binding.clbahasa.setOnClickListener {
            navigateToLanguages()
        }

        binding.btnDaftarbisnis.setOnClickListener {
            if (currentUser == null) {
                navigateToLogin()
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


        // Tombol ini cukup arahkan langsung ke pengecekan status bisnis
        binding.btnDaftarProduct.setOnClickListener {
            if (currentUser == null) {
                navigateToLogin()
            } else {
                checkBusinessAccountStatusForProduct(currentUser.email ?: "")
            }
        }
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

        val docId = email.replace(".", "_")

        db.collection("penjual").document(docId).get()
            .addOnSuccessListener { doc ->
                val isBusinessAccount =
                    doc.exists() && (doc.getBoolean("isBusinessAccount") ?: false)

                if (isBusinessAccount) {
                    binding.btnDaftarProduct.visibility = View.VISIBLE
                    binding.btnDaftarbisnis.visibility = View.GONE
                    // Setup tombol dengan behavior untuk user sudah terdaftar
                    setupBtnDaftarProduct(true)
                } else {
                    binding.btnDaftarProduct.visibility = View.VISIBLE
                    binding.btnDaftarbisnis.visibility = View.VISIBLE
                    setupBtnDaftarProduct(false)
                }
            }
            .addOnFailureListener {
                // Jika gagal ambil data, asumsikan belum terdaftar
                binding.btnDaftarbisnis.visibility = View.VISIBLE
                binding.btnDaftarProduct.visibility = View.VISIBLE
                setupBtnDaftarProduct(false)
            }
    }

    private fun checkBusinessAccountStatusForProduct(email: String) {
        if (email.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Email tidak valid. Silakan login ulang.",
                Toast.LENGTH_SHORT
            ).show()
            navigateToLogin()
            return
        }
        val docId = email.replace(".", "_")

        db.collection("penjual").document(docId).get()
            .addOnSuccessListener { doc ->
                val isBusinessAccount =
                    doc.exists() && (doc.getBoolean("isBusinessAccount") ?: false)
                val currentUser = auth.currentUser

                if (currentUser == null) {
                    Toast.makeText(
                        requireContext(),
                        "Silakan login terlebih dahulu.",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToLogin()
                    return@addOnSuccessListener
                }

                if (isBusinessAccount) {
                    val intent = Intent(requireContext(), DaftarProductActivity::class.java).apply {
                        putExtra("EMAIL", currentUser.email)
                        putExtra("USERNAME", currentUser.displayName)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Anda belum terdaftar sebagai akun bisnis, silakan daftar terlebih dahulu.",
                        Toast.LENGTH_SHORT
                    ).show()
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
                Toast.makeText(
                    requireContext(),
                    "Silakan login terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
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
        requireActivity().finish()
    }

    private fun navigateToSettings() {
        val intent = Intent(requireContext(), SettingsActivity::class.java)
        startActivity(intent)
    }
}
