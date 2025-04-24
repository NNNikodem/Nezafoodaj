package com.example.nezafoodaj.main

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.nezafoodaj.R
import com.example.nezafoodaj.data.RecipeRepository
import com.example.nezafoodaj.models.Recipe
import com.example.nezafoodaj.models.UnitType

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var recipeId: String
    private lateinit var rr: RecipeRepository

    private lateinit var textViewName: TextView
    private lateinit var imageViewFinal: ImageView
    private lateinit var textViewDate: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var layoutIngredients: LinearLayout
    private lateinit var layoutSteps: LinearLayout
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)
        enableEdgeToEdge()
        rr = RecipeRepository()
        recipeId = intent.getStringExtra("recipeId") ?: return

        // Bind views
        textViewName = findViewById(R.id.textViewRecipeName)
        imageViewFinal = findViewById(R.id.imageViewFinal)
        textViewDate = findViewById(R.id.textViewDate)
        textViewDescription = findViewById(R.id.textViewDescription)
        layoutIngredients = findViewById(R.id.layoutIngredients)
        layoutSteps = findViewById(R.id.layoutSteps)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        loadRecipe()
    }

    private fun loadRecipe() {
        rr.getRecipeById(recipeId) { recipe, success ->
            if (success && recipe != null) {
                showRecipe(recipe)
            } else {
                Toast.makeText(this, "Chyba pri načítaní receptu", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun showRecipe(recipe: Recipe) {
        textViewName.text = recipe.name

        // Load final image if available
        if (recipe.finalImage.isNotEmpty()) {
            Glide.with(this).load(recipe.finalImage).into(imageViewFinal)
        }
        textViewDate.text = recipe.formatTimestamp(recipe.dateCreated)
        textViewDescription.text = recipe.description
        // Clear the existing views
        layoutIngredients.removeAllViews()
        layoutSteps.removeAllViews()

        // Add ingredients dynamically using the ingredient_item layout
        for (ingredient in recipe.ingredients) {
            val unitShort = when (ingredient.unit) {
                UnitType.GRAM -> "g"
                UnitType.KILOGRAM -> "kg"
                UnitType.MILLILITER -> "ml"
                UnitType.LITER -> "l"
                UnitType.CUP -> "hrnček/y"
                UnitType.TABLESPOON -> "PL"
                UnitType.TEASPOON -> "ČL"
                UnitType.PIECE -> "ks"
                UnitType.NONE -> ""
            }

            val ingredientView = layoutInflater.inflate(R.layout.item_ingredient, layoutIngredients, false)
            val ingredientName = ingredientView.findViewById<TextView>(R.id.ingredientName)
            val ingredientAmount = ingredientView.findViewById<TextView>(R.id.ingredientAmount)
            val ingredientUnit = ingredientView.findViewById<TextView>(R.id.ingredientUnit)

            ingredientName.text = ingredient.name
            ingredientAmount.text = ingredient.amount.toString()
            ingredientUnit.text = unitShort

            layoutIngredients.addView(ingredientView)
        }

        // Add steps dynamically using the step_item layout
        for ((index, step) in recipe.steps.withIndex()) {
            val stepView = layoutInflater.inflate(R.layout.item_step, layoutSteps, false)
            val stepDescription = stepView.findViewById<TextView>(R.id.stepDescription)
            val stepImage = stepView.findViewById<ImageView>(R.id.stepImage)

            stepDescription.text = "${index + 1}. ${step.text}"

            // Load step image if available
            if (step.image.isNotEmpty()) {
                Glide.with(this).load(step.image).into(stepImage)
            } else {
                stepImage.visibility = View.GONE
            }

            layoutSteps.addView(stepView)
        }
    }
}
