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

class RecipeAdapter(private val recipes: MutableList<Recipe>) :
    RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {
    private val rr:RecipeRepository = RecipeRepository()
    class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recipeName: TextView = view.findViewById(R.id.textViewRecipeName)
        val ingredients: TextView = view.findViewById(R.id.textViewIngredients)
        val stepsCount: TextView = view.findViewById(R.id.textViewStepsCount)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.recipeName.text = recipe.name
        holder.ingredients.text = "Number of ingredients: " + recipe.ingredients.size.toString()
        holder.stepsCount.text = "Number of steps: " + recipe.steps.size.toString()
        holder.btnDelete.setOnClickListener {
            rr.removeRecipe(recipe.getId(), {
                Log.d("Recipe", "removing recipe at: " + position +"/"+ recipes.size)

                // Success - Remove the recipe from the local list and notify adapter
                recipes.removeAt(position)
                notifyItemRemoved(position)
                Log.d("Recipe", "Left: " + recipes.size)
                // After removing the item, shift the positions of the remaining items.
                // Notify the range to update all items after the removed item.
                notifyItemRangeChanged(position, recipes.size)
            }, { exception ->
                // Failure - Show error message or handle failure
                Log.e("RecipeAdapter", "Error deleting recipe", exception)
            })
        }
    }

    override fun getItemCount(): Int {
        return recipes.size
    }
}