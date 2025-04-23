package com.example.nezafoodaj.data

import com.example.nezafoodaj.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference

class UserRepository {
    private val db = FirebaseFirestore.getInstance().collection("users")
    fun addUser(user: User) {
        db.add(user)
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

}