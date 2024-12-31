package com.example.bearhabitapp.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.bearhabitapp.LoginActivity
import com.example.bearhabitapp.R

class SecondScreen : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_second_screen, container, false)
        val nextButton = view.findViewById<Button>(R.id.nextButton)

        nextButton.setOnClickListener {
            // Start LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            // Optional: Clear the activity stack
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            // Close current activity if necessary
            activity?.finish()
        }

        return view
    }
}