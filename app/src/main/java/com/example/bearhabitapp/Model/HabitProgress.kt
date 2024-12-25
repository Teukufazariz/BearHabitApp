package com.example.bearhabitapp.Model

data class HabitProgress(
    val habitName: String,
    val userEmail: String,
    val progress: Float,
    val totalTasks: Int,
    val completedTasks: Int
)