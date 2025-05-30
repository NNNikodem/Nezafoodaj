package com.example.nezafoodaj.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nezafoodaj.models.Recipe
import com.example.nezafoodaj.R
import com.example.nezafoodaj.data.RecipeRepository
import com.google.firebase.firestore.FirebaseFirestore

class RecipeAdapterHome(
private var recipes: List<Recipe>,  // Užívame immutable List na zaistenie integrity dát
private val onRecipeClick: (String) -> Unit // Callback pre kliknutie
) : RecyclerView.Adapter<RecipeAdapterHome.RecipeViewHolder>() {

    // Na vytvorenie nového viewHolderu
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe_home, parent, false)
        return RecipeViewHolder(view)
    }

    // Bindovanie dát k položke v RecyclerView
    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.bind(recipe)
    }

    // Počet položiek v RecyclerView
    override fun getItemCount(): Int = recipes.size

    // Funkcia na aktualizáciu receptov
    fun updateRecipes(newRecipes: List<Recipe>) {
        recipes = newRecipes
        notifyDataSetChanged()  // Aktualizujeme zoznam v adaptéri
    }

    // ViewHolder pre recept
    inner class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivRecipeImage: ImageView = itemView.findViewById(R.id.ivRecipeImage)
        private val tvRecipeName: TextView = itemView.findViewById(R.id.tvRecipeName)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)

        // Funkcia na binding dát
        fun bind(recipe: Recipe) {
            Glide.with(itemView.context)
                .load(recipe.finalImage)
                .into(ivRecipeImage)

            tvRecipeName.text = recipe.name
            ratingBar.rating = 0f

            // Načítanie hodnotenia
            val ratingsDb = FirebaseFirestore.getInstance().collection("ratings")
            ratingsDb.whereEqualTo("recipeId", recipe.getId())
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val ratings = querySnapshot.documents.mapNotNull { it.getDouble("rating") }
                    if (ratings.isNotEmpty()) {
                        val avgRating = ratings.average().toFloat()
                        ratingBar.rating = avgRating
                    } else {
                        ratingBar.rating = 0f
                    }
                }
                .addOnFailureListener {
                    ratingBar.rating = 0f
                }

            itemView.setOnClickListener {
                onRecipeClick(recipe.getId())
            }
        }
    }
}
