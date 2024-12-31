package com.example.bearhabitapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.example.bearhabitapp.onboarding.OnboardingActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Access SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("User Prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            // Redirect to HomePageActivity if logged in
            startActivity(Intent(this, HomePageActivity::class.java))
        } else {
            // Redirect to OnboardingActivity if not logged in
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
        finish()
    }
}
