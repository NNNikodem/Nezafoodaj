package com.example.nezafoodaj.ui.myRecipes

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nezafoodaj.R
import com.example.nezafoodaj.adapters.RecipeAdapter
import com.example.nezafoodaj.data.RecipeRepository
import com.example.nezafoodaj.models.Ingredient
import com.example.nezafoodaj.models.Recipe
import com.example.nezafoodaj.models.Step
import com.example.nezafoodaj.models.UnitType
import com.google.firebase.auth.FirebaseAuth

class MyRecipesFragment : Fragment() {
    private val rr = RecipeRepository()
    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipeList = mutableListOf<Recipe>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_my_recipes, container, false)

        val btnCreate: Button = view.findViewById(R.id.btnCreate)
        val btnShow: Button = view.findViewById(R.id.btnShow)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewRecipes)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        // Set up button click listener or other logic
        btnCreate.setOnClickListener {
            val userId: String = FirebaseAuth.getInstance().currentUser?.uid.toString()
            // Example Recipe
            val ingredients = listOf(
                Ingredient("Flour", 200.0, UnitType.GRAM),
                Ingredient("Sugar", 100.0, UnitType.GRAM),
                Ingredient("Eggs", 2.0, UnitType.PIECE)
            )

            val steps = listOf(
                Step("Mix all ingredients together.", ""),
                Step("Bake at 180°C for 20 minutes.", "")
            )

            val recipe = Recipe(
                userId = userId,
                name = "Simple Cake",
                ingredients = ingredients,
                steps = steps,
                finalImage = "" // You can add a URL or path to an image if you have one
            )

            // Add recipe to Firestore
            rr.addRecipe(recipe) { success ->
                if (success) {
                    // Handle success
                    Toast.makeText(context, "Recipe added successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    // Handle failure
                    Toast.makeText(context, "Failed to add recipe.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        btnShow.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                rr.getAll(userId) { recipes, success ->
                    if (success && recipes != null) {
                        recipeList.clear() // Vyčistí staré dáta
                        recipeList.addAll(recipes) // Pridá nové recepty

                        if (::recipeAdapter.isInitialized) {
                            recipeAdapter.notifyDataSetChanged() // Oznámi RecyclerView, že sa dáta zmenili
                        } else {
                            recipeAdapter = RecipeAdapter(recipeList) // Vytvorí nový adaptér
                            recyclerView.adapter = recipeAdapter // Nastaví adaptér do RecyclerView
                        }
                    } else {
                        Toast.makeText(context, "Failed to fetch recipes", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Please log in to view your recipes.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}