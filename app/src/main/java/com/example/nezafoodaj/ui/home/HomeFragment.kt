package com.example.nezafoodaj.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nezafoodaj.R
import com.example.nezafoodaj.adapters.RecipeAdapter
import com.example.nezafoodaj.adapters.RecipeAdapterHome
import com.example.nezafoodaj.data.RecipeRepository
import com.example.nezafoodaj.main.RecipeDetailActivity
import com.example.nezafoodaj.models.Recipe
import com.google.android.material.chip.ChipGroup
import java.util.Calendar
import java.util.Date

class HomeFragment : Fragment() {

    private lateinit var rvLatestRecipes: RecyclerView
    private lateinit var rvTopRatedRecipes: RecyclerView
    private lateinit var latestRecipeAdapter: RecipeAdapterHome
    private lateinit var topRatedRecipeAdapter: RecipeAdapterHome
    private lateinit var chipGroupTimeFilter: ChipGroup

    private lateinit var progressBar: ProgressBar

    private val recipeRepository = RecipeRepository()
    private var allRecipes: List<Recipe> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        rvLatestRecipes = view.findViewById(R.id.rvLatestRecipes)
        rvTopRatedRecipes = view.findViewById(R.id.rvTopRatedRecipes)
        chipGroupTimeFilter = view.findViewById(R.id.chipGroupTimeFilter)
        progressBar = view.findViewById(R.id.progressBar)

        chipGroupTimeFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            updateTopRatedRecipes()
        }

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

        // Fetch recipes once when the fragment is created
        fetchRecipes()

        return view
    }

    private fun fetchRecipes() {
        progressBar.visibility = View.VISIBLE
        // Fetch the recipes once and store them in memory
        recipeRepository.getAll { recipes, success ->
            progressBar.visibility = View.GONE
            if (success && recipes != null) {
                allRecipes = recipes // Store the fetched recipes
                loadRecipes() // Call loadRecipes to update the UI
            }
        }
    }

    private fun loadRecipes() {
        val timeFilter = getSelectedTimeFilter()

        // Use the in-memory recipes instead of fetching again
        val latestRecipes = allRecipes.sortedByDescending { it.dateCreated }.take(7)
        val filteredTopRated = when (timeFilter) {
            "Týždeň" -> allRecipes.filter { it.dateCreated >= getDateDaysAgo(7) }
            "Mesiac" -> allRecipes.filter { it.dateCreated >= getDateDaysAgo(30) }
            "Rok" -> allRecipes.filter { it.dateCreated >= getDateDaysAgo(365) }
            else -> allRecipes
        }
        val topRatedRecipes = filteredTopRated.sortedByDescending { it.rating }.take(7)

        // Update the adapters with the filtered recipes
        latestRecipeAdapter.updateRecipes(latestRecipes)
        topRatedRecipeAdapter.updateRecipes(topRatedRecipes)
    }
    private fun updateTopRatedRecipes() {
        val timeFilter = getSelectedTimeFilter()
        val filteredTopRated = when (timeFilter) {
            "Týždeň" -> allRecipes.filter { it.dateCreated >= getDateDaysAgo(7) }
            "Mesiac" -> allRecipes.filter { it.dateCreated >= getDateDaysAgo(30) }
            "Rok" -> allRecipes.filter { it.dateCreated >= getDateDaysAgo(365) }
            else -> allRecipes
        }
        val topRatedRecipes = filteredTopRated.sortedByDescending { it.rating }.take(7)
        topRatedRecipeAdapter.updateRecipes(topRatedRecipes)
    }

    private fun getSelectedTimeFilter(): String {
        val checkedId = chipGroupTimeFilter.checkedChipId
        return when (checkedId) {
            R.id.chipWeek -> "Týždeň"
            R.id.chipMonth -> "Mesiac"
            R.id.chipYear -> "Rok"
            else -> "Celý čas"
        }
    }

    private fun getDateDaysAgo(days: Int): Long {
        return System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
    }


}

