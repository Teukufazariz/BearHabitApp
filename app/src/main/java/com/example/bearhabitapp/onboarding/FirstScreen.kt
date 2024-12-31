package com.example.bearhabitapp.onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.viewpager2.widget.ViewPager2
import com.example.bearhabitapp.R

class FirstScreen : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val viewPager = activity?.findViewById<ViewPager2>(R.id.viewPager)
        val view = inflater.inflate(R.layout.fragment_first_screen, container, false)
        val nextButton = view.findViewById<Button>(R.id.nextButton)

        nextButton.setOnClickListener {
            viewPager?.currentItem = 1
        }

        return view
    }
}