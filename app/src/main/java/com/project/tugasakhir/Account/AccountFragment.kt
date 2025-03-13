package com.project.tugasakhir.Account

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.FragmentAccountBinding
import de.hdodenhof.circleimageview.CircleImageView

class AccountFragment : Fragment() {

    private lateinit var binding: FragmentAccountBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        updateUI()

        binding.tvLogout.setOnClickListener {
            if (isLoggedIn()) {
                logout()
            } else {
                login()
            }
        }
    }

    private fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }

    private fun updateUI() {
        if (isLoggedIn()) {
            // Menampilkan informasi pengguna setelah login
            binding.tvUserName.text = "Nama Pengguna"
            binding.noPhone.text = "0812...."
            binding.imageUser.setImageResource(R.drawable.avatar)
            binding.tvLogout.text = "Logout"
        } else {
            // Jika belum login
            binding.tvUserName.text = "Login untuk melihat info"
            binding.noPhone.text = ""
            binding.imageUser.setImageResource(R.drawable.avatar)
            binding.tvLogout.text = "Login"
        }
    }

    private fun login() {
        // Simulasi proses login
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", true)
        editor.apply()
        updateUI()  // Perbarui UI setelah login
    }

    private fun logout() {
        // Menghapus status login
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", false)
        editor.apply()
        updateUI()  // Perbarui UI setelah logout
    }
}
