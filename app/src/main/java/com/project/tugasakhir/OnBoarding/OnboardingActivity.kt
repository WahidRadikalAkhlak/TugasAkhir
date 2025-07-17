package com.project.tugasakhir.OnBoarding

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.project.tugasakhir.R
import com.project.tugasakhir.Adapter.OnboardingAdapter
import androidx.fragment.app.Fragment

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // List of fragments for ViewPager2
        val fragmentList = arrayListOf<Fragment>(
            FirstscreenFragment(),
            SecondscreenFragment(),
            ThirdscreenFragment()
        )

        // Set up the ViewPager2 with the adapter
        val adapter = OnboardingAdapter(fragmentList, supportFragmentManager, lifecycle)
        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        viewPager.adapter = adapter
    }
}
