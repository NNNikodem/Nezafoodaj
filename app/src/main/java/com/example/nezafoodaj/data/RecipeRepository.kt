package com.example.nezafoodaj.data

import android.util.Log
import com.example.nezafoodaj.models.Recipe
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.Normalizer
import java.util.regex.Pattern

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
    fun removeRecipe(userId: String, recipeId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Get a reference to the document based on the recipe ID
        val recipeDocRef: DocumentReference = db.document(recipeId)

        recipeDocRef.delete()
            .addOnSuccessListener {
                removeAllRatings(recipeId, onSuccess, onFailure)
                deleteRecipeStorageData(userId, recipeId, onSuccess, onFailure)
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Call onFailure callback if something goes wrong
                onFailure(exception)
            }
    }
    fun updateRecipe(recipeId: String, updatedData: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        db.document(recipeId).update(updatedData)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
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
    fun searchRecipesByName(query: String, limit: Long = 100, onComplete: (List<Recipe>?, Boolean) -> Unit) {
        val normalizedQuery = normalizeText(query)
        val queryWords = normalizedQuery.split(" ")

        db
            .limit(limit)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val recipes = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Recipe::class.java)
                }.filter { recipe ->
                    recipe.name_keywords.any { keyword ->
                        queryWords.any { word ->
                            if (word.length < 3) {
                                keyword.startsWith(word)
                            } else {
                                keyword.contains(word)
                            }
                        }
                    }
                }

                onComplete(recipes, true)
            }
            .addOnFailureListener {
                onComplete(null, false)
            }
    }

    fun addRecipeToFavorites(userId: String, recipeId: String, onComplete: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(userId)
            .update("favRecipes", FieldValue.arrayUnion(recipeId))
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
            }
    }
    fun removeRecipeFromFavorites(userId: String, recipeId: String, onComplete: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(userId)
            .update("favRecipes", FieldValue.arrayRemove(recipeId))
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
            }
    }
    fun checkIfFavorite(userId: String, recipeId: String, onComplete: (Boolean) -> Unit) {
        var isFavorite = false
        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val favs = doc.get("favRecipes") as? List<*>
                isFavorite = favs?.contains(recipeId) == true
                onComplete(isFavorite)
            }
    }
    fun normalizeText(input: String): String {
        val normalized = Normalizer.normalize(input.lowercase(), Normalizer.Form.NFD)
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("")
    }
    fun deleteRecipeStorageData(
        userId: String,
        recipeId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
        val foldersToCheck = listOf("recipe_images", "recipe_steps")
        var pendingDeletions = foldersToCheck.size
        var deletionFailed = false

        // Function to check if all deletions have been processed
        fun checkCompletion() {
            pendingDeletions--
            if (pendingDeletions == 0 && !deletionFailed) {
                onSuccess()
            }
        }

        for (folder in foldersToCheck) {
            val targetPath = "$folder/$userId/$recipeId"
            val targetRef = storageRef.child(targetPath)

            targetRef.listAll()
                .addOnSuccessListener { listResult ->
                    val files = listResult.items
                    if (files.isEmpty()) {
                        checkCompletion()
                    } else {
                        var filesDeleted = 0
                        for (fileRef in files) {
                            fileRef.delete()
                                .addOnSuccessListener {
                                    filesDeleted++
                                    if (filesDeleted == files.size) {
                                        checkCompletion()
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    if (!deletionFailed) {
                                        deletionFailed = true
                                        onFailure(exception)
                                    }
                                }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    if (!deletionFailed) {
                        deletionFailed = true
                        onFailure(exception)
                    }
                }
        }
    }
}