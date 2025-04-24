package com.example.nezafoodaj.data

import android.util.Log
import com.example.nezafoodaj.models.Recipe
import com.example.nezafoodaj.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference

class UserRepository {
    private val db = FirebaseFirestore.getInstance().collection("users")
    private val auth = FirebaseAuth.getInstance()
    fun addUser(user: User) {
        db.document(user.id)  // 👈 Use the UID as the document ID
            .set(user)
            .addOnSuccessListener {
                Log.d("UserRepository", "User saved with UID: ${user.id}")
            }
            .addOnFailureListener {
                Log.e("UserRepository", "Failed to save user: ${it.message}")
            }
    }
    fun checkIfEmailInUse(email: String, onComplete: (Boolean) -> Unit) {
        db.whereEqualTo("email", email)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val documentExists = task.result?.documents?.isNotEmpty() == true
                    onComplete(documentExists)  //document is found, false otherwise
                } else {
                    onComplete(false)  //an error
                }
            }
    }
    fun checkIfNameInUse(name: String, onComplete: (Boolean) -> Unit) {
        db.whereEqualTo("name", name)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val documentExists = task.result?.documents?.isNotEmpty() == true
                    onComplete(documentExists)  //document is found, false otherwise
                } else {
                    onComplete(false)  //an error
                }
            }
    }
    fun getCurrentUserData(onComplete: (User?, Boolean) -> Unit) {
        val id = auth.currentUser?.uid.toString()
        db.document(id).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {

                    val user = document.toObject(User::class.java)
                    onComplete(user, true)
                } else {
                    onComplete(null, false)
                }
            }
            .addOnFailureListener {
                onComplete(null, false)
            }
    }

}