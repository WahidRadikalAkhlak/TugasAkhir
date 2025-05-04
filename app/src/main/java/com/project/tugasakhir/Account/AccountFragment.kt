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
import com.project.tugasakhir.Account.Penjual.DaftarPenjualActivity
import com.project.tugasakhir.Account.Penjual.DaftarProductActivity
import com.project.tugasakhir.databinding.FragmentAccountBinding

class AccountFragment : Fragment() {

    private lateinit var binding: FragmentAccountBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the username passed to the fragment
        val username = arguments?.getString("USERNAME") ?: "Guest" // Default to "Guest" if null

        // Display the username
        binding.tvUserName.text = username

        // Retrieve email from SharedPreferences to check login status
        val sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("USER_EMAIL", "")

        // If email exists, fetch user data from Firestore
        if (!userEmail.isNullOrEmpty()) {
            // Set logout button text if user is logged in
            binding.tvLogout.text = "Logout"  // Change button text to Logout
            getUserData(userEmail)
        } else {
            // If user is not logged in, display the Login button
            binding.tvLogout.text = "Login"
        }

        // Setup the logout button behavior
        binding.clLogout.setOnClickListener {
            if (binding.tvLogout.text == "Logout") {
                logout()
            } else {
                navigateToLogin()
            }
        }

        // Action for business registration (Daftar Bisnis)
        binding.btnDaftarbisnis.setOnClickListener {
            if (userEmail.isNullOrEmpty()) {
                // If the user is not logged in, navigate to the login screen
                navigateToLogin()
            } else {
                // If the user is logged in, navigate to Daftar Bisnis
                navigateToDaftarPenjual()
            }
        }

        // Actions for product registration
        binding.btnDaftarProduct.setOnClickListener {
            navigateToDaftarProduct()
        }
    }

    // Fetch user data from Firestore based on email
    private fun getUserData(email: String) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val document = task.result?.documents?.firstOrNull()
                    if (document != null) {
                        // Get user data from Firestore and update the UI
                        val userName = document.getString("nama") ?: "No Name"
                        val userEmail = document.getString("email") ?: "No Email"
                        binding.tvUserName.text = userName
                        binding.email.text = userEmail
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch user data", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Logout function to clear user data and navigate to login screen
    private fun logout() {
        val sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()  // Clear all data in SharedPreferences
        editor.apply()

        // Reset UI data after logout
        binding.tvUserName.text = "Guest"  // Reset user name
        binding.email.text = "No Email"   // Reset email

        // Navigate back to login screen
        navigateToLogin()
    }

    // Navigate to LoginActivity
    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
        requireActivity().finish() // Close current fragment/activity
    }

    // Navigate to DaftarPenjualActivity
    private fun navigateToDaftarPenjual() {
        val intent = Intent(requireContext(), DaftarPenjualActivity::class.java)
        startActivity(intent)
    }

    // Navigate to DaftarProductActivity
    private fun navigateToDaftarProduct() {
        val intent = Intent(requireContext(), DaftarProductActivity::class.java)
        startActivity(intent)
    }
}
