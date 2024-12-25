package com.example.bearhabitapp.Model

data class Habit(
    var id: String? = null,
    val habitName: String = "",
    val iconColor: String = "",
    val repeatCount: Int = 0,
    val days: List<String> = emptyList(),
    val userId: String = "",
    val friendEmail: String? = null,
    val collaborative: Boolean = false,
    val competitive: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    var isChecked: Boolean = false,
    // Map tanggal ke list userId yang telah menyelesaikan habit
    val completedDates: MutableMap<String, MutableList<String>> = mutableMapOf()
)