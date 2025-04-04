package com.example.nezafoodaj.data

import com.example.nezafoodaj.models.Recipe
import com.google.firebase.firestore.FirebaseFirestore

class RecipeRepository {
    private val db = FirebaseFirestore.getInstance().collection("recipes")

    fun addRecipe(recipe: Recipe, onComplete: (Boolean) -> Unit) {
        db.add(recipe)
            .addOnSuccessListener { documentReference ->
                // Vratenie ID receptu
                val id = documentReference.id
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
    fun getAll(userId: String, onComplete: (List<Recipe>?, Boolean) -> Unit) {
        db.whereEqualTo("userId", userId) // Query to filter recipes by userId
            .get()
            .addOnSuccessListener { querySnapshot ->
                val recipes = mutableListOf<Recipe>()
                for (document in querySnapshot.documents) {
                    val recipe = document.toObject(Recipe::class.java) // Convert document to Recipe object
                    if (recipe != null) {
                        recipes.add(recipe)
                    }
                }
                onComplete(recipes, true) // Return the list of recipes and success
            }
            .addOnFailureListener {
                onComplete(null, false) // Return null and false if query fails
            }
    }
}