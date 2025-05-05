package com.example.nezafoodaj.data

import android.util.Log
import com.example.nezafoodaj.models.Rating
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.math.RoundingMode
import java.text.DecimalFormat

class RatingRepository {

    private val db = FirebaseFirestore.getInstance().collection("ratings")
    private val recipeDb = FirebaseFirestore.getInstance().collection("recipes")

    // Funkcia na pridanie hodnotenia
    fun addOrUpdateRating(rating: Rating, callback: (isNewRating: Boolean, success: Boolean) -> Unit) {
        val docId = "${rating.userId}-${rating.recipeId}"
        val ratingRef = db.document(docId)
        ratingRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                // Update existujúceho hodnotenia
                ratingRef.update(
                    "rating", rating.rating,
                    "timestamp", FieldValue.serverTimestamp()
                ).addOnSuccessListener {
                    callback(false, true)
                }.addOnFailureListener {
                    callback(false, false)
                }
            } else {
                // Pridaj nové hodnotenie
                val ratingData = hashMapOf(
                    "userId" to rating.userId,
                    "recipeId" to rating.recipeId,
                    "rating" to rating.rating,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                ratingRef.set(ratingData).addOnSuccessListener {
                    callback(true, true)
                }.addOnFailureListener {
                    callback(true, false)
                }
            }
        }.addOnFailureListener {
            callback(false, false)
        }
    }

    // Funkcia na získanie hodnotenia pre konkrétny recept od konkrétneho používateľa
    fun getUserRating(recipeId: String, userId: String, callback: (Double) -> Unit) {
        db.whereEqualTo("recipeId", recipeId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    callback(0.0) // Ak ešte používateľ nehodnotil, vrátime 0
                } else {
                    for (document in documents) {
                        val rating = document.getDouble("rating") ?: 0.0
                        callback(rating)
                    }
                }
            }
            .addOnFailureListener {
                callback(0.0) // V prípade chyby vrátime 0
            }
    }
    fun getRatingsForRecipe(recipeId: String, onComplete: (Double, Int) -> Unit) {
        db.whereEqualTo("recipeId", recipeId)
            .get()
            .addOnSuccessListener { result ->
                var total = 0.0
                val count = result.size()
                for (doc in result) {
                    val rating = doc.getDouble("rating") ?: 0.0
                    total += rating
                }
                val average = if (count > 0) total / count else 0.0
                val flooredRating = roundOffDecimal(average)

                recipeDb.document(recipeId).update("rating", flooredRating)
                recipeDb.document(recipeId).update("ratingCount", count)
                onComplete(flooredRating, count)
            }
            .addOnFailureListener {
                onComplete(0.0, 0)
            }
    }
    private fun roundOffDecimal(number: Double): Double {
        val df = DecimalFormat("#.##").apply {
            roundingMode = RoundingMode.CEILING
        }
        val formatted = df.format(number).replace(",", ".")
        return formatted.toDouble()
    }
}
