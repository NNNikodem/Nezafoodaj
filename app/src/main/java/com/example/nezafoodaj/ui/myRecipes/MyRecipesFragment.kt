package com.example.nezafoodaj.ui.myRecipes

import android.content.Intent
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
import com.example.nezafoodaj.main.AddRecipeActivity
import com.example.nezafoodaj.main.RecipeDetailActivity
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
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
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

        recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewRecipes)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        // Set up button click listener or other logic
        btnCreate.setOnClickListener {
            val userId: String = FirebaseAuth.getInstance().currentUser?.uid.toString()
            createRecipe()
        }

        if (userId != null) {
            showRecipes(userId)
        }
        return view
    }
    fun createRecipe()
    {
        val intent = Intent(requireContext(), AddRecipeActivity::class.java)
        startActivity(intent)
    }
    fun showRecipes(userId: String) {
        rr.getAll(userId) { recipes, success ->
            if (success && recipes != null) {
                recipeList.clear()

                // Sort recipes by dateCreated (assuming dateCreated is a Date or timestamp type)
                val sortedRecipes = recipes.sortedByDescending { it.dateCreated }

                // Add sorted recipes to the list
                recipeList.addAll(sortedRecipes)

                val onRecipeClick: (String) -> Unit = { recipeId ->
                    val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
                    intent.putExtra("recipeId", recipeId)
                    startActivity(intent)
                }

                // Notify the adapter about the change in data
                if (::recipeAdapter.isInitialized) {
                    recipeAdapter.notifyDataSetChanged()
                } else {
                    recipeAdapter = RecipeAdapter(recipeList, onRecipeClick)
                    recyclerView.adapter = recipeAdapter
                }
            } else {
                Toast.makeText(context, "Failed to fetch recipes", Toast.LENGTH_SHORT).show()
            }
        }
    }

}