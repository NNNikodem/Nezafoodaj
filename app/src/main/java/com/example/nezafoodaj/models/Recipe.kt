package com.example.nezafoodaj.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Recept
data class Recipe(
    private var id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val ingredients: List<Ingredient> = listOf(),
    val steps: List<Step> = listOf(),
    val finalImage: String = "",
    val dateCreated: Long = System.currentTimeMillis(),
    val rating: Double = 0.0,
    val timesRated: Int = 0
) {
    // Custom setter method for setting the id, if needed
    fun setId(documentId: String) {
        this.id = documentId  // This will set the id field of the Recipe object
    }
    fun getId(): String
    {
        return this.id
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val date = Date(timestamp)
        return sdf.format(date)
    }
}

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
enum class UnitType(val shortName: String) {
    GRAM("g"),
    KILOGRAM("kg"),
    MILLILITER("ml"),
    LITER("l"),
    CUP("hrnček"),
    TABLESPOON("PL"),
    TEASPOON("KL"),
    PIECE("ks"),
    NONE("");

    override fun toString(): String {
        return shortName
    }
}