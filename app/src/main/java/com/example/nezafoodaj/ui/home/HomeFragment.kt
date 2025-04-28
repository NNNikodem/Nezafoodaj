package com.example.nezafoodaj.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nezafoodaj.R
import com.example.nezafoodaj.adapters.RecipeAdapter
import com.example.nezafoodaj.adapters.RecipeAdapterHome
import com.example.nezafoodaj.data.RecipeRepository
import com.example.nezafoodaj.main.RecipeDetailActivity

class HomeFragment : Fragment() {

    private lateinit var rvLatestRecipes: RecyclerView
    private lateinit var rvTopRatedRecipes: RecyclerView
    private lateinit var latestRecipeAdapter: RecipeAdapterHome
    private lateinit var topRatedRecipeAdapter: RecipeAdapterHome

    private val recipeRepository = RecipeRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        rvLatestRecipes = view.findViewById(R.id.rvLatestRecipes)
        rvTopRatedRecipes = view.findViewById(R.id.rvTopRatedRecipes)

        // Setting horizontal layout managers
        rvLatestRecipes.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvTopRatedRecipes.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        latestRecipeAdapter = RecipeAdapterHome(mutableListOf()) { recipeId ->
            // handle click for the latest recipe
            val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
            intent.putExtra("recipeId", recipeId)
            startActivity(intent)
        }

        topRatedRecipeAdapter = RecipeAdapterHome(mutableListOf()) { recipeId ->
            // handle click for top-rated recipe
            val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
            intent.putExtra("recipeId", recipeId)
            startActivity(intent)
        }

        rvLatestRecipes.adapter = latestRecipeAdapter
        rvTopRatedRecipes.adapter = topRatedRecipeAdapter

        loadRecipes()

        return view
    }

    private fun loadRecipes() {
        // Loading the latest recipes
        recipeRepository.getAll { recipes, success ->
            if (success && recipes != null) {
                val latestRecipes = recipes.sortedByDescending { it.dateCreated }.take(7)
                latestRecipeAdapter.updateRecipes(latestRecipes)

                val topRatedRecipes = recipes.sortedByDescending { it.rating }.take(7)
                topRatedRecipeAdapter.updateRecipes(topRatedRecipes)
            }
        }
    }
}
