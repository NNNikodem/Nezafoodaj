package com.example.nezafoodaj.ui.myRecipes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nezafoodaj.R
import com.example.nezafoodaj.adapters.RecipeAdapter
import com.example.nezafoodaj.data.RecipeRepository
import com.example.nezafoodaj.data.UserRepository
import com.example.nezafoodaj.activities.main.AddRecipeActivity
import com.example.nezafoodaj.activities.main.RecipeDetailActivity
import com.example.nezafoodaj.models.Recipe
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth

class MyRecipesFragment : Fragment() {
    private val rr = RecipeRepository()
    private val userRepository = UserRepository()

    private lateinit var tvNoRecipesResult: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var tabLayout: TabLayout
    enum class TabType { MY_RECIPES, FAVORITES }
    private var currentTab = TabType.MY_RECIPES

    private lateinit var btnCreate: FloatingActionButton
    private val recipeList = mutableListOf<Recipe>()
    private val favoriteRecipeList = mutableListOf<Recipe>()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var showFavRecipes:Boolean = false
    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            when (currentTab) {
                TabType.MY_RECIPES -> loadMyRecipes()
                TabType.FAVORITES -> loadFavoriteRecipes()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_my_recipes, container, false)

        tvNoRecipesResult = view.findViewById(R.id.tvNoRecipesResult)
        btnCreate = view.findViewById(R.id.btnCreate)
        recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewRecipes)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        tabLayout = view.findViewById(R.id.tabLayout)
        // Set up button click listener or other logic
        btnCreate.setOnClickListener {
            createRecipe()
        }

        if (userId != null) {
            loadMyRecipes()
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        currentTab = TabType.MY_RECIPES
                        loadMyRecipes()
                    }
                    1 -> {
                        currentTab = TabType.FAVORITES
                        loadFavoriteRecipes()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        return view
    }
    fun createRecipe()
    {
        val intent = Intent(requireContext(), AddRecipeActivity::class.java)
        detailLauncher.launch(intent)
    }
    private fun displayRecipes(recipes: List<Recipe>) {
        // Sort by date
        val sorted = recipes.sortedByDescending { it.dateCreated }

        recipeList.clear()
        recipeList.addAll(sorted)

        val onRecipeClick: (String) -> Unit = { recipeId ->
            val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
            intent.putExtra("recipeId", recipeId)
            detailLauncher.launch(intent)
        }
        val onEditClick: (String) -> Unit = { recipeId ->
            val intent = Intent(requireContext(), AddRecipeActivity::class.java)
            intent.putExtra("edit_recipe_id", recipeId)
            detailLauncher.launch(intent)
        }

        if (::recipeAdapter.isInitialized) {
            recipeAdapter.notifyDataSetChanged()
        } else {
            recipeAdapter = RecipeAdapter(userId?:"", recipeList, onRecipeClick, onEditClick)
            recyclerView.adapter = recipeAdapter
        }
    }

    private fun loadMyRecipes() {
        btnCreate.visibility = View.VISIBLE
        rr.getAllByUserId(userId ?: "") { recipes, success ->
            if (success && recipes != null) {
                if(recipes.isEmpty())
                {
                    tvNoRecipesResult.visibility = View.VISIBLE
                    tvNoRecipesResult.text = ContextCompat.getString(requireContext(), R.string.myRecipes_noRecipesResult)
                    return@getAllByUserId
                }
                tvNoRecipesResult.visibility = View.GONE
                displayRecipes(recipes)
            } else {
                Toast.makeText(requireContext(), "Nepodarilo sa načítať moje recepty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadFavoriteRecipes() {
        btnCreate.visibility = View.GONE
        userRepository.getFavoriteRecipes(userId ?: "") { favRecipeIds, success ->
            if (!success) {
                Toast.makeText(requireContext(), "Nepodarilo sa načítať obľúbené recepty", Toast.LENGTH_SHORT).show()
                return@getFavoriteRecipes
            }
            else if (favRecipeIds.isNullOrEmpty())
            {
                tvNoRecipesResult.visibility = View.VISIBLE
                tvNoRecipesResult.text = ContextCompat.getString(requireContext(), R.string.myRecipes_noFavRecipesResult)
                displayRecipes(emptyList())
                return@getFavoriteRecipes
            }

            val favoriteRecipes = mutableListOf<Recipe>()
            var loadedCount = 0
            tvNoRecipesResult.visibility = View.GONE
            for (id in favRecipeIds) {
                rr.getRecipeById(id) { recipe, success ->
                    if (success && recipe != null) {
                        favoriteRecipes.add(recipe)
                    }

                    loadedCount++
                    if (loadedCount == favRecipeIds.size) {
                        if (favoriteRecipes.isNotEmpty()) {
                            displayRecipes(favoriteRecipes)
                        } else {
                            Toast.makeText(requireContext(), "Žiadne obľúbené recepty", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

}