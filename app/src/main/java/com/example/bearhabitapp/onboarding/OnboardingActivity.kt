package com.example.bearhabitapp.onboarding

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.bearhabitapp.R

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding)

        // Inisialisasi ViewPager2
        viewPager = findViewById(R.id.viewPager)

        // Atur Adapter untuk ViewPager2
        val onboardingAdapter = OnboardingAdapter(this)
        viewPager.adapter = onboardingAdapter

        // Set Listener untuk menangani Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.onboardingLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}