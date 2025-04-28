package com.example.nezafoodaj.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.nezafoodaj.main.RecipeDetailActivity
import com.example.nezafoodaj.models.Recipe
import com.google.firebase.auth.FirebaseAuth

class SearchFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var tvNoResults: TextView
    private lateinit var progressBarLoading: ProgressBar

    private lateinit var recipeAdapter: RecipeAdapter

    private var allRecipes = mutableListOf<Recipe>()
    private val recipeRepository = RecipeRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        // Inicializácia UI komponentov
        etSearch = view.findViewById(R.id.etSearch)
        rvSearchResults = view.findViewById(R.id.rvSearchResults)
        tvNoResults = view.findViewById(R.id.tvNoResults)
        progressBarLoading = view.findViewById(R.id.progressBarLoading)

        // Inicializácia RecyclerView a adaptéra
        rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        recipeAdapter = RecipeAdapter(mutableListOf()) { recipeId ->
            hideKeyboard()

            // Vytvorenie intentu na otvorenie detailu receptu
            val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
            intent.putExtra("recipeId", recipeId)
            startActivity(intent)
        }

        rvSearchResults.adapter = recipeAdapter

        // Načítanie všetkých receptov
        loadRecipes()

        // Pridanie TextWatcher pre EditText (vyhľadávanie)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterRecipes(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        return view
    }

    // Načítanie všetkých receptov (bez obmedzenia na používateľa)
    private fun loadRecipes() {
        showLoading(true)

        recipeRepository.getAll { recipes, success ->
            showLoading(false)
            if (success && recipes != null) {
                allRecipes = recipes.toMutableList()
                recipeAdapter.updateRecipes(allRecipes)
                checkIfEmpty(allRecipes)
            } else {
                checkIfEmpty(emptyList())
            }
        }
    }

    // Filtrovanie receptov podľa názvu
    private fun filterRecipes(query: String) {
        val filteredList = if (query.isEmpty()) {
            allRecipes
        } else {
            allRecipes.filter { recipe ->
                recipe.name.contains(query, ignoreCase = true)
            }
        }
        recipeAdapter.updateRecipes(filteredList)
        checkIfEmpty(filteredList)
    }

    // Skontrolovanie, či je zoznam prázdny
    private fun checkIfEmpty(list: List<Recipe>) {
        if (list.isEmpty()) {
            rvSearchResults.visibility = View.GONE
            tvNoResults.visibility = View.VISIBLE
        } else {
            rvSearchResults.visibility = View.VISIBLE
            tvNoResults.visibility = View.GONE
        }
    }

    // Zobrazenie načítavacieho indikátora
    private fun showLoading(isLoading: Boolean) {
        progressBarLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        rvSearchResults.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    // Skrytie klávesnice
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}

