package com.example.nezafoodaj.models

data class User(
    val name:String = "",
    val email:String = "",
    val id:String = "",
    val admin:Boolean = false,
    val favRecipes: List<String> = emptyList()
) {}
