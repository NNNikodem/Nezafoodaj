package com.example.nezafoodaj.models

data class User(
    val name:String = "",
    val email:String = "",
    val profilePhotoURL:String = "",
    val age:Int = 0,
    val gender:String = "",
    val dateCreated:Long = System.currentTimeMillis(),
    val id:String = "",
    val admin:Boolean = false,
    val favRecipes: List<String> = emptyList()
) {}
