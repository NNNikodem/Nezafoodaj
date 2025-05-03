package com.example.nezafoodaj.models

data class Rating(
    val userId: String = "",
    val recipeId: String = "",
    val rating: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)