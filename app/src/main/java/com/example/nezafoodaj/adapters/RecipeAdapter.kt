package com.example.nezafoodaj.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nezafoodaj.models.Recipe
import com.example.nezafoodaj.R
import com.example.nezafoodaj.data.RecipeRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton

class RecipeAdapter(
    private val userId: String,
    private val recipes: MutableList<Recipe>,
    private val onRecipeClick: (String) -> Unit,
    private val onEditClick: (String) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    private val rr: RecipeRepository = RecipeRepository()

    inner class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recipeName: TextView = view.findViewById(R.id.textViewRecipeName)
        val ingredients: TextView = view.findViewById(R.id.textViewIngredients)
        val stepsCount: TextView = view.findViewById(R.id.textViewStepsCount)
        val rating: TextView = view.findViewById(R.id.textViewRating)
        val imageView_FinalImg: ImageView = view.findViewById(R.id.imageView_FinalImage)
        val btnEditRecipe: FloatingActionButton = view.findViewById(R.id.btnEditRecipe)
        init {
            view.setOnClickListener {
                val recipe = recipes[adapterPosition]
                onRecipeClick(recipe.getId())
            }
            btnEditRecipe.setOnClickListener {
                val recipe = recipes[adapterPosition]
                onEditClick(recipe.getId())
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
        checkIfOwner(recipe.userId, holder.btnEditRecipe)
        holder.recipeName.text = recipe.name
        holder.ingredients.text = "Počet ingrediencií: ${recipe.ingredients.size}"
        holder.stepsCount.text = "Počet krokov: ${recipe.steps.size}"
        holder.rating.text = "${recipe.rating} ⭐"
        Glide.with(holder.itemView.context)
            .load(recipe.finalImage) // Image URL
            .into(holder.imageView_FinalImg)
    }
    override fun getItemCount(): Int = recipes.size
    fun updateRecipes(newRecipes: List<Recipe>) {
        recipes.clear()
        recipes.addAll(newRecipes)
        notifyDataSetChanged()
    }
    private fun checkIfOwner(recipe_userId:String, editBtn:FloatingActionButton)
    {
        if(recipe_userId == userId)
        {
            editBtn.visibility = View.VISIBLE
        }
        else
        {
            editBtn.visibility = View.GONE
        }
    }
}