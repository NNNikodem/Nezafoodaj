package com.example.nezafoodaj.data

import com.example.nezafoodaj.models.Recipe
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class RecipeRepository {
    private val db = FirebaseFirestore.getInstance().collection("recipes")

    fun addRecipe(recipe: Recipe, onComplete: (Boolean) -> Unit) {
        db.add(recipe).addOnSuccessListener { documentReference ->
            val recipeWithId = recipe.copy(id = documentReference.id) // skopíruj recept s novým ID

            // Prepíš ho rovno do Firestore pod týmto ID (nahradíme pôvodný dokument)
            db.document(documentReference.id).set(recipeWithId)
                .addOnSuccessListener {
                    onComplete(true)
                }
                .addOnFailureListener {
                    onComplete(false)
                }

        }.addOnFailureListener {
            onComplete(false)
        }
    }
    fun removeRecipe(recipeId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Get a reference to the document based on the recipe ID
        val recipeDocRef: DocumentReference = db.document(recipeId)

        // Delete the document from Firestore
        recipeDocRef.delete()
            .addOnSuccessListener {
                // Call onSuccess callback if deletion is successful
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Call onFailure callback if something goes wrong
                onFailure(exception)
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
                        recipe.setId(document.id)
                        recipes.add(recipe)
                    }
                }
                onComplete(recipes, true) // Return the list of recipes and success
            }
            .addOnFailureListener {
                onComplete(null, false) // Return null and false if query fails
            }
    }
    fun getRecipeById(id: String, onComplete: (Recipe?, Boolean) -> Unit) {
        db.document(id).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val recipe = document.toObject(Recipe::class.java)
                    onComplete(recipe, true)
                } else {
                    onComplete(null, false)
                }
            }
            .addOnFailureListener {
                onComplete(null, false)
            }
    }
}