package com.project.tugasakhir.Account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Account.Login.LoginActivity
import com.project.tugasakhir.Account.Penjual.DaftarPenjualActivity  // Import activity yang benar
import com.project.tugasakhir.Account.Penjual.DaftarProductActivity
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.FragmentAccountBinding

class AccountFragment : Fragment() {

    private lateinit var binding: FragmentAccountBinding
    private val db = FirebaseFirestore.getInstance()

    // onCreateView to set up the view binding for this fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    // onViewCreated to handle logic after the fragment view is created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ambil data user dari Bundle yang diteruskan
        val userName = arguments?.getString("userName") ?: "Nama tidak ditemukan"
        val userEmail = arguments?.getString("userEmail") ?: "Email tidak ditemukan"

        // Tampilkan data pengguna
        binding.tvUserName.text = userName
        binding.email.text = userEmail

        // Set up the logout button logic
        binding.clLogout.setOnClickListener {
            logout()
        }

        binding.btnDaftarbisnis.setOnClickListener {
            navigateToDaftarPenjual()
        }
        binding.btnDaftarProduct.setOnClickListener {
            navigateToDaftarProduct()
        }
    }

    private fun logout() {
        // Perform any logout logic, such as clearing preferences or tokens
        val sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        // Navigate to the login screen after logout
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToDaftarPenjual() {
        val intent = Intent(requireContext(), DaftarPenjualActivity::class.java)
        startActivity(intent)
    }
    private fun navigateToDaftarProduct() {
        val intent = Intent(requireContext(), DaftarProductActivity::class.java)
        startActivity(intent)
    }
}
