package com.example.nezafoodaj.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.nezafoodaj.R
import com.example.nezafoodaj.data.RatingRepository
import com.example.nezafoodaj.data.RecipeRepository
import com.example.nezafoodaj.data.UserRepository
import com.example.nezafoodaj.models.Rating
import com.example.nezafoodaj.models.Recipe
import com.example.nezafoodaj.models.UnitType
import com.example.nezafoodaj.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var recipeId: String
    private lateinit var userId: String

    private lateinit var recipe: Recipe
    private lateinit var user: User

    private lateinit var ratingRepository: RatingRepository
    private lateinit var recipeRepository: RecipeRepository
    private lateinit var userRepository: UserRepository

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private lateinit var textViewName: TextView
    private lateinit var imageViewFinal: ImageView
    private lateinit var textViewDate: TextView
    private lateinit var textViewRating: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var layoutIngredients: LinearLayout
    private lateinit var layoutSteps: LinearLayout
    private lateinit var ratingBar: RatingBar
    private lateinit var ratingBtn: Button
    private lateinit var btnDeleteRecipe: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        ratingRepository = RatingRepository()
        recipeRepository = RecipeRepository()
        userRepository = UserRepository()

        recipeId = intent.getStringExtra("recipeId") ?: return
        userId = auth.currentUser?.uid ?: return
        // Bind views
        textViewName = findViewById(R.id.textViewRecipeName)
        imageViewFinal = findViewById(R.id.imageViewFinal)
        textViewDate = findViewById(R.id.textViewDate)
        textViewRating = findViewById(R.id.textViewRating)
        textViewDescription = findViewById(R.id.textViewDescription)
        layoutIngredients = findViewById(R.id.layoutIngredients)
        layoutSteps = findViewById(R.id.layoutSteps)
        ratingBar = findViewById(R.id.ratingBar)
        ratingBtn = findViewById(R.id.btnRateRecipe)
        ratingBtn.setOnClickListener { saveRating() }

        btnDeleteRecipe = findViewById(R.id.btnDeleteRecipe)
        btnDeleteRecipe.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Potvrdenie vymazania")
                .setMessage("Naozaj chceš vymazať tento recept?")
                .setPositiveButton("Áno") { _, _ ->
                    recipeRepository.removeRecipe(recipeId, {
                        Toast.makeText(this, "Recept bol úspešne odstránený", Toast.LENGTH_SHORT).show()
                        finish()
                    }) { exception ->
                        Toast.makeText(this, "Chyba pri mazaní: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Zrušiť", null)
                .show()
        }

        setupToolbar()
        loadUser()
        loadRecipe()
    }
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.activityName_RecipeDetail)
        toolbar.setNavigationOnClickListener { finish() }
    }
    private fun loadRecipe() {
        recipeRepository.getRecipeById(recipeId) { recipe, success ->
            if (success && recipe != null) {
                this.recipe = recipe
                showRecipe(recipe)
                toggleDeleteButton()
            } else {
                Toast.makeText(this, "Chyba pri načítaní receptu", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    private fun loadUser() {
        userRepository.getCurrentUserData { fetchedUser, success ->
            if (success && fetchedUser != null) {
                user = fetchedUser
                toggleDeleteButton()
            }
            else {
                Toast.makeText(this, "Chyba pri načítaní usera", Toast.LENGTH_SHORT).show()
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

        ratingRepository.getRatingsForRecipe(recipeId) { average, count ->
            val text = if (count > 0) {
                "Hodnotenie: %.1f (%d hodnotení)".format(average, count)
            } else {
                "Zatiaľ nehodnotené"
            }
            textViewRating.text = text
        }

        textViewDescription.text = recipe.description
        // Clear the existing views
        layoutIngredients.removeAllViews()
        layoutSteps.removeAllViews()

        // Add ingredients dynamically using the ingredient_item layout
        for (ingredient in recipe.ingredients) {
            val unitShort = ingredient.unit.toString()
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
        ratingRepository.getUserRating(recipeId, userId) { userRating ->
            ratingBar.rating = userRating.toFloat()
        }
    }
    private fun toggleDeleteButton() {
        if(!::user.isInitialized || !::recipe.isInitialized) return //crash prevent
        if(user.admin)
        { btnDeleteRecipe.visibility = View.VISIBLE }
        if(recipe.userId == userId)
        { btnDeleteRecipe.visibility = View.VISIBLE }
        Log.d("TOGGLE_DELETE", "Recipe ID: ${recipe.userId}, User.ID: ${user.id}, UserID: $userId")
    }
    private fun saveRating() {
        val ratingValue = ratingBar.rating.toDouble()
        val rating = Rating(userId, recipeId, ratingValue)
        ratingRepository.addOrUpdateRating(rating) { isNewRating, success ->
            if (success) {
                Toast.makeText(this, if (isNewRating) "Hodnotenie pridané!" else "Hodnotenie upravené!", Toast.LENGTH_SHORT).show()
                // Also Update the rating
                ratingRepository.getRatingsForRecipe(recipeId) { average, count ->
                    val textViewRating = findViewById<TextView>(R.id.textViewRating)
                    textViewRating.text = "Hodnotenie: %.1f (%d hodnotení)".format(average, count)
                }

            } else {
                Toast.makeText(this, "Chyba pri ukladaní hodnotenia", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
