package com.project.tugasakhir.OnBoarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.project.tugasakhir.MainActivity
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.FragmentThirdscreenBinding

class ThirdscreenFragment : Fragment() {

    private var _binding: FragmentThirdscreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment using ViewBinding
        _binding = FragmentThirdscreenBinding.inflate(inflater, container, false)

        // Set OnClickListener for the "Finish" button
        binding.finish.setOnClickListener {
            // Mark onboarding as finished
            onBoardingFinished()

            // Navigate to MainActivity
            val intent = Intent(activity, MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()  // Close the onboarding activity
        }

        // Get references for the dots
        val dot1 = binding.dot1
        val dot2 = binding.dot2
        val dot3 = binding.dot3

        // Get the reference of ViewPager2
        val viewPager = activity?.findViewById<ViewPager2>(R.id.viewPager)

        // Register OnPageChangeCallback for ViewPager2
        viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // Reset all dots to inactive
                dot1.setImageResource(R.drawable.dot_inactive)
                dot2.setImageResource(R.drawable.dot_inactive)
                dot3.setImageResource(R.drawable.dot_inactive)

                // Set active dot based on selected position
                when (position) {
                    0 -> dot1.setImageResource(R.drawable.dot_active)
                    1 -> dot2.setImageResource(R.drawable.dot_active)
                    2 -> dot3.setImageResource(R.drawable.dot_active)
                }
            }
        })

        // Return the root view of the fragment
        return binding.root
    }

    private fun onBoardingFinished() {
        val sharedPref = requireActivity().getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("Finished", true)
        editor.apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }
}
