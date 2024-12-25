package com.example.bearhabitapp.Model

data class Message(
    val sender: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val type: String = "text",
    val isSender: Boolean = false
)
