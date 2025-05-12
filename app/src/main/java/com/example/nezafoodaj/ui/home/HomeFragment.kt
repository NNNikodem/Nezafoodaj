package com.example.nezafoodaj.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nezafoodaj.R
import com.example.nezafoodaj.adapters.RecipeAdapterHome
import com.example.nezafoodaj.data.RecipeRepository
import com.example.nezafoodaj.activities.main.RecipeDetailActivity
import com.example.nezafoodaj.models.Recipe
import com.google.android.material.chip.ChipGroup

class HomeFragment : Fragment() {


    private lateinit var rvLatestRecipes: RecyclerView
    private lateinit var rvTopRatedRecipes: RecyclerView
    private lateinit var latestRecipeAdapter: RecipeAdapterHome
    private lateinit var topRatedRecipeAdapter: RecipeAdapterHome
    private lateinit var chipGroupTimeFilter: ChipGroup

    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoRecipes: TextView
    private lateinit var tvLatestRecipes: TextView
    private lateinit var tvTopRatedRecipes: TextView

    private val recipeRepository = RecipeRepository()
    private var allRecipes: List<Recipe> = listOf()
    private var recipeDetailLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){ result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchRecipes()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        rvLatestRecipes = view.findViewById(R.id.rvLatestRecipes)
        rvTopRatedRecipes = view.findViewById(R.id.rvTopRatedRecipes)
        chipGroupTimeFilter = view.findViewById(R.id.chipGroupTimeFilter)
        progressBar = view.findViewById(R.id.progressBar)
        tvNoRecipes = view.findViewById(R.id.tvHomeNoRecipes)
        tvLatestRecipes = view.findViewById(R.id.textView_latest)
        tvTopRatedRecipes = view.findViewById(R.id.textView_best)
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
            recipeDetailLauncher.launch(intent)
        }

        topRatedRecipeAdapter = RecipeAdapterHome(mutableListOf()) { recipeId ->
            // handle click for top-rated recipe
            val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
            intent.putExtra("recipeId", recipeId)
            recipeDetailLauncher.launch(intent)
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
            if (success && !recipes.isNullOrEmpty()) {
                allRecipes = recipes // Store the fetched recipes
                loadRecipes() // Call loadRecipes to update the UI
            }
            else{
                tvNoRecipes.visibility = View.VISIBLE
                tvLatestRecipes.visibility = View.GONE
                tvTopRatedRecipes.visibility = View.GONE
                chipGroupTimeFilter.visibility = View.GONE
                rvLatestRecipes.visibility = View.GONE
                rvTopRatedRecipes.visibility = View.GONE
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

