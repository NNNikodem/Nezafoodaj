package com.example.nezafoodaj.data

import com.example.nezafoodaj.models.Recipe
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class RecipeRepository {
    private val db = FirebaseFirestore.getInstance().collection("recipes")
    private val ratingsDb = FirebaseFirestore.getInstance().collection("ratings")

    fun addRecipe(recipe: Recipe,recipeId: String, onComplete: (Boolean) -> Unit) {
        db.document(recipeId).set(recipe).addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }
    fun removeRecipe(recipeId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Get a reference to the document based on the recipe ID
        val recipeDocRef: DocumentReference = db.document(recipeId)

        recipeDocRef.delete()
            .addOnSuccessListener {
                removeAllRatings(recipeId, onSuccess, onFailure)
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Call onFailure callback if something goes wrong
                onFailure(exception)
            }
    }
    fun removeAllRatings(recipeId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        ratingsDb.whereEqualTo("recipeId", recipeId)
            .get().addOnSuccessListener { querySnapshot ->
                val batch = FirebaseFirestore.getInstance().batch()
                for (document in querySnapshot.documents) {
                    batch.delete(document.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        onFailure(exception)
                    }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
    fun getAllByUserId(userId: String, onComplete: (List<Recipe>?, Boolean) -> Unit) {
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
    fun getAll(onComplete: (List<Recipe>?, Boolean) -> Unit) {
        db.get()
            .addOnSuccessListener { querySnapshot ->
                val recipes = mutableListOf<Recipe>()
                for (document in querySnapshot.documents) {
                    val recipe = document.toObject(Recipe::class.java)
                    if (recipe != null) {
                        recipe.setId(document.id)
                        recipes.add(recipe)
                    }
                }
                onComplete(recipes, true)
            }
            .addOnFailureListener {
                onComplete(null, false)
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