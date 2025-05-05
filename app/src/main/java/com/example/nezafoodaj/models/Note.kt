package com.example.nezafoodaj.models

data class Note(
    val userId: String = "",
    val recipeId: String = "",
    val noteText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)