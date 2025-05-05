package com.example.nezafoodaj.activities.main

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.nezafoodaj.R
import com.example.nezafoodaj.data.NoteRepository
import com.example.nezafoodaj.data.RatingRepository
import com.example.nezafoodaj.data.RecipeRepository
import com.example.nezafoodaj.data.UserRepository
import com.example.nezafoodaj.models.Note
import com.example.nezafoodaj.models.Rating
import com.example.nezafoodaj.models.Recipe
import com.example.nezafoodaj.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var recipeId: String
    private lateinit var userId: String

    private lateinit var recipe: Recipe
    private lateinit var user: User
    private lateinit var authorName: String
    private lateinit var author: User
    private lateinit var menuRef: Menu
    private var currentNote: Note? = null
    private var isFavorite = false

    private lateinit var ratingRepository: RatingRepository
    private lateinit var recipeRepository: RecipeRepository
    private lateinit var userRepository: UserRepository
    private lateinit var noteRepository: NoteRepository

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private lateinit var textViewName: TextView
    private lateinit var imageViewFinal: ImageView
    private lateinit var textViewAuthor: TextView
    private lateinit var ivAuthorPhoto: ImageView
    private lateinit var textViewDate: TextView
    private lateinit var textViewRating: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var textViewPrepTime: TextView
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
        noteRepository = NoteRepository()

        recipeId = intent.getStringExtra("recipeId") ?: return
        userId = auth.currentUser?.uid ?: return
        // Bind views
        textViewName = findViewById(R.id.textViewRecipeName)
        imageViewFinal = findViewById(R.id.imageViewFinal)
        textViewAuthor = findViewById(R.id.textViewAuthor)
        ivAuthorPhoto = findViewById(R.id.iv_authorPhoto)
        textViewDate = findViewById(R.id.textViewDate)
        textViewRating = findViewById(R.id.textViewRating)
        textViewDescription = findViewById(R.id.textViewDescription)
        textViewPrepTime = findViewById(R.id.textViewPrepTime)
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
        loadUser()
        loadRecipe()
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_recipe_detail, menu)
        if (menu != null) {
            menuRef = menu
            loadUserNote()
            checkIfFavorite()
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_note -> {
                if (currentNote == null) showWriteNoteDialog()
                else showExistingNoteDialog()
                true
            }
            R.id.action_bookmark -> {
                addToFavorites()
                true
            }
            R.id.action_bookmarkRemove -> {
                removeFromFavorites()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun loadUserNote() {
        FirebaseFirestore.getInstance().collection("notes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("recipeId", recipeId)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    currentNote = docs.first().toObject(Note::class.java)
                }
            }
    }
    private fun showWriteNoteDialog(existingText: String? = null) {
        val layout = layoutInflater.inflate(R.layout.layout_component_addnote, null)
        val editText = layout.findViewById<EditText>(R.id.editTextNote)
        existingText?.let { editText.setText(it) }

        AlertDialog.Builder(this)
            .setTitle("Tvoja poznámka k receptu")
            .setView(layout)
            .setPositiveButton("Uložiť") { _, _ ->
                val noteText = editText.text.toString().trim()
                if (noteText.isNotEmpty()) {
                    val note = Note(userId, recipeId, noteText)
                    saveNoteToFirestore(note)
                } else {
                    Toast.makeText(this, "Poznámka je prázdna!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Zrušiť", null)
            .show()
    }
    private fun showExistingNoteDialog() {
        val layout = layoutInflater.inflate(R.layout.layout_component_shownote, null)
        val textView = layout.findViewById<TextView>(R.id.textViewNote)
        textView.text = currentNote?.noteText

        AlertDialog.Builder(this)
            .setTitle("Tvoja poznámka k receptu")
            .setView(layout)
            .setPositiveButton("OK", null)
            .setNegativeButton("Upraviť") { _, _ -> showWriteNoteDialog(currentNote?.noteText) }
            .show()
    }
    private fun saveNoteToFirestore(note: Note) {
        noteRepository.addOrUpdateNote(note) { savedNote ->
            currentNote = savedNote
            Toast.makeText(this, "Poznámka uložená", Toast.LENGTH_SHORT).show()
        }
    }
    private fun checkIfFavorite() {
        recipeRepository.checkIfFavorite(userId, recipeId) { favorite ->
            isFavorite = favorite
            updateBookmarkIcons()
        }
    }
    private fun addToFavorites() {
        recipeRepository.addRecipeToFavorites(userId, recipeId) { success ->
            if (success) {
                Toast.makeText(this, "Pridané do obľúbených", Toast.LENGTH_SHORT).show()
                isFavorite = true
                updateBookmarkIcons()
            } else {
                Toast.makeText(this, "Nepodarilo sa pridať", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun removeFromFavorites() {
        recipeRepository.removeRecipeFromFavorites(userId, recipeId) { success ->
            if (success) {
                Toast.makeText(this, "Odstránené z obľúbených", Toast.LENGTH_SHORT).show()
                isFavorite = false
                updateBookmarkIcons()
            } else {
                Toast.makeText(this, "Nepodarilo sa odstrániť", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun updateBookmarkIcons() {
        setResult(Activity.RESULT_OK)
        menuRef.findItem(R.id.action_bookmark)?.isVisible = !isFavorite
        menuRef.findItem(R.id.action_bookmarkRemove)?.isVisible = isFavorite
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
        supportActionBar?.title = recipe.name
        toolbar.setNavigationOnClickListener { finish() }
    }
    private fun loadRecipe() {
        recipeRepository.getRecipeById(recipeId) { recipe, success ->
            if (success && recipe != null) {
                this.recipe = recipe
                showRecipe(recipe)
                loadAuthor(recipe.userId)
                toggleDeleteButton()
                setupToolbar()
            } else {
                Toast.makeText(this, "Chyba pri načítaní receptu", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    private fun loadAuthor(authorId: String)
    {
        userRepository.getUserById(authorId)
        { user, success ->
            if (success && user != null) {
                author = user
                authorName = author.name
                textViewAuthor.text = authorName
                if (author.profilePhotoURL.isNotEmpty()) {
                    setPhoto(author.profilePhotoURL)
                }
            }
            else {
                textViewAuthor.text = "Neznámy autor"
            }
        }
    }
    private fun setPhoto(uri: String)
    {
        val radiusInDp = 12
        val radiusInPx = (radiusInDp * resources.displayMetrics.density).toInt()
        Glide.with(this)
            .load(uri)
            .transform(RoundedCorners(radiusInPx))
            .into(ivAuthorPhoto)
    }
    private fun loadUser() {
        userRepository.getCurrentUserData { fetchedUser, success ->
            if (success && fetchedUser != null) {
                user = fetchedUser
                toggleDeleteButton()
            }
            else {
                Toast.makeText(this, "Chyba pri načítaní tvojich údajov", Toast.LENGTH_SHORT).show()
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
                "Hodnotenie: %.2f ⭐ (%d hodn.)".format(average, count)
            } else {
                "Zatiaľ nehodnotené"
            }
            textViewRating.text = text
        }

        textViewDescription.text = recipe.description
        textViewPrepTime.text = "${recipe.prepTime} min"
        // Clear the existing views
        layoutIngredients.removeAllViews()
        layoutSteps.removeAllViews()

        // Add ingredients dynamically using the ingredient_item layout
        for (ingredient in recipe.ingredients) {
            val unitShort = ingredient.unit.shortName
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
                    textViewRating.text = "Hodnotenie: %.1f ⭐ (%d hodn.)".format(average, count)
                    setResult(Activity.RESULT_OK)
                }

            } else {
                Toast.makeText(this, "Chyba pri ukladaní hodnotenia", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
