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
        db.document(user.id)
            .set(user)
    }
    fun updateUser(
        userId: String,
        userName: String,
        userAge: Int,
        userGender: String,
        onComplete: (Boolean) -> Unit
    ) {
        db.document(userId).update(
            "name", userName,
            "age", userAge,
            "gender", userGender
        ).addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }
    fun updateUserPhoto(userId: String, photoUrl: String) {
        db.document(userId).update(
            "profilePhotoURL", photoUrl
        )
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
    fun getUserNameById(userId: String, onComplete: (String?, Boolean) -> Unit) {
        db.document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    onComplete(user?.name, true)
                    } else {
                    onComplete(null, false)
                }
            }
            .addOnFailureListener {
                onComplete(null, false)
            }
    }
    fun getUserById(userId: String, onComplete: (User?, Boolean) -> Unit) {
        db.document(userId).get()
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


    fun getFavoriteRecipes(userId: String, onComplete: (List<String>?, Boolean) -> Unit) {
        db.document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val favRecipes = document.get("favRecipes") as? List<String> ?: emptyList()
                    onComplete(favRecipes, true)
                } else {
                    onComplete(emptyList(), false)
                }
            }
            .addOnFailureListener {
                onComplete(null, false)
            }
    }
}