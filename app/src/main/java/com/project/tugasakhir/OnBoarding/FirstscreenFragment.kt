package com.project.tugasakhir.OnBoarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.FragmentFirstscreenBinding

class FirstscreenFragment : Fragment() {

    private var _binding: FragmentFirstscreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstscreenBinding.inflate(inflater, container, false)

        // Get the reference of ViewPager2
        val viewPager = activity?.findViewById<ViewPager2>(R.id.viewPager)

        // Set OnClickListener for the "Next" button
        binding.next.setOnClickListener {
            viewPager?.currentItem = 1  // Move to the next page (SecondscreenFragment)
        }

        // Get references for the dots
        val dot1 = binding.dot1
        val dot2 = binding.dot2
        val dot3 = binding.dot3

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }
}
