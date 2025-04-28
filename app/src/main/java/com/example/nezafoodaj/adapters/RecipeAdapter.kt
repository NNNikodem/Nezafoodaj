package com.example.nezafoodaj.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nezafoodaj.models.Recipe
import com.example.nezafoodaj.R
import com.example.nezafoodaj.data.RecipeRepository

class RecipeAdapter(
    private val recipes: MutableList<Recipe>,
    private val onRecipeClick: (String) -> Unit // tu pridáme callback na kliknutie
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    private val rr: RecipeRepository = RecipeRepository()

    inner class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recipeName: TextView = view.findViewById(R.id.textViewRecipeName)
        val ingredients: TextView = view.findViewById(R.id.textViewIngredients)
        val stepsCount: TextView = view.findViewById(R.id.textViewStepsCount)

        init {
            view.setOnClickListener {
                val recipe = recipes[adapterPosition]
                onRecipeClick(recipe.getId()) // spustíme callback s ID
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.recipeName.text = recipe.name
        holder.ingredients.text = "Počet ingrediencií: ${recipe.ingredients.size}"
        holder.stepsCount.text = "Počet krokov: ${recipe.steps.size}"
    }

    override fun getItemCount(): Int = recipes.size
    fun updateRecipes(newRecipes: List<Recipe>) {
        recipes.clear()
        recipes.addAll(newRecipes)
        notifyDataSetChanged()
    }
}