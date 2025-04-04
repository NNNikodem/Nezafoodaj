package com.example.nezafoodaj.models

// Recept
data class Recipe(
    val userId: String = "",
    val name: String = "",
    val ingredients: List<Ingredient> = listOf(),
    val steps: List<Step> = listOf(),
    val finalImage: String = ""
)

// Ingrediencia
data class Ingredient(
    val name: String = "",
    val amount: Double = 0.0,
    val unit: UnitType = UnitType.NONE
)

// Krok prípravy
data class Step(
    val text: String = "",
    val image: String = ""
)

// Enum pre jednotky
enum class UnitType {
    GRAM, // g
    KILOGRAM, // kg
    MILLILITER, // ml
    LITER, // l
    CUP, // hrnček
    TABLESPOON, // PL
    TEASPOON, // KL
    PIECE, // kus
    NONE // ak nie je jednotka potrebná (napr. "štipka soli")
}