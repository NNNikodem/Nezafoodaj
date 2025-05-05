package com.example.nezafoodaj.data

import android.widget.Toast
import com.example.nezafoodaj.models.Note
import com.google.firebase.firestore.FirebaseFirestore

class NoteRepository {

    private val db = FirebaseFirestore.getInstance().collection("notes")

    fun addOrUpdateNote(note: Note, onComplete: (Note) -> Unit) {
        db
            .whereEqualTo("userId", note.userId)
            .whereEqualTo("recipeId", note.recipeId)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val docId = docs.first().id
                    db.document(docId).set(note)
                        .addOnSuccessListener {
                            onComplete(note)
                        }
                } else {
                    db.add(note)
                        .addOnSuccessListener {
                            onComplete(note)
                        }
                }
            }
    }

    // Načítanie poznámky pre konkrétny recept a používateľa
    fun getNoteForRecipeAndUser(recipeId: String, userId: String, onComplete: (Note?) -> Unit) {
        db
            .document("${userId}_${recipeId}")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val note = document.toObject(Note::class.java)
                    onComplete(note)
                } else {
                    onComplete(null)  // Ak poznámka neexistuje
                }
            }
            .addOnFailureListener {
                onComplete(null)  // Chyba pri získavaní
            }
    }
}