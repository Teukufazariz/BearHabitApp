package com.example.bearhabitapp.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.bearhabitapp.onboarding.FirstScreen
import com.example.bearhabitapp.onboarding.SecondScreen

class OnboardingAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private val onboardingFragments = listOf(
        FirstScreen(),
        SecondScreen()
    )

    override fun getItemCount(): Int = onboardingFragments.size

    override fun createFragment(position: Int): Fragment = onboardingFragments[position]
}