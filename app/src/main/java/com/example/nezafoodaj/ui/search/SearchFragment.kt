package com.example.nezafoodaj.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nezafoodaj.R
import com.example.nezafoodaj.adapters.RecipeAdapter
import com.example.nezafoodaj.data.RecipeRepository
import com.example.nezafoodaj.main.AddRecipeActivity
import com.example.nezafoodaj.main.RecipeDetailActivity
import com.example.nezafoodaj.models.Recipe
import com.google.firebase.auth.FirebaseAuth

class SearchFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var tvNoResults: TextView
    private lateinit var progressBarLoading: ProgressBar
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipeRepository = RecipeRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        etSearch = view.findViewById(R.id.etSearch)
        rvSearchResults = view.findViewById(R.id.rvSearchResults)
        tvNoResults = view.findViewById(R.id.tvNoResults)
        progressBarLoading = view.findViewById(R.id.progressBarLoading)

        rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        val onRecipeClick: (String) -> Unit = { recipeId ->
            hideKeyboard()
            val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
            intent.putExtra("recipeId", recipeId)
            startActivity(intent)
        }
        val onEditClick: (String) -> Unit = {}
        recipeAdapter = RecipeAdapter(userId?:"", mutableListOf(), onRecipeClick, onEditClick)
        rvSearchResults.adapter = recipeAdapter

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                // Trigger your search here
                val query = etSearch.text.toString().trim()
                searchRecipes(query)
                hideKeyboard()
                true
            } else {
                false
            }
        }

        return view
    }

    private fun searchRecipes(query: String) {
        showLoading(true)
        recipeRepository.searchRecipesByName(query) { recipes, success ->
            showLoading(false)
            if (success && recipes != null) {
                recipeAdapter.updateRecipes(recipes)
                checkIfEmpty(recipes)
            } else {
                recipeAdapter.updateRecipes(emptyList())
                checkIfEmpty(emptyList())
            }
        }
    }

    private fun checkIfEmpty(list: List<Recipe>) {
        if (list.isEmpty()) {
            rvSearchResults.visibility = View.GONE
            tvNoResults.visibility = View.VISIBLE
        } else {
            rvSearchResults.visibility = View.VISIBLE
            tvNoResults.visibility = View.GONE
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBarLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            rvSearchResults.visibility = View.GONE
            tvNoResults.visibility = View.GONE
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}
