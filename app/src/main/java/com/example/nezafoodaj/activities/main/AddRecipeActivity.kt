package com.example.nezafoodaj.activities.main

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.nezafoodaj.R
import com.example.nezafoodaj.data.RecipeRepository
import com.example.nezafoodaj.models.Ingredient
import com.example.nezafoodaj.models.Recipe
import com.example.nezafoodaj.models.Step
import com.example.nezafoodaj.models.UnitType
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.Normalizer
import java.util.UUID
import java.util.regex.Pattern

class AddRecipeActivity : AppCompatActivity() {
    //Edit recipe section
    private var isEditMode = false
    private var editingRecipeId: String? = null
    private var recipeToEdit: Recipe? = null
    private val recipeRepository = RecipeRepository()
    //
    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var stepsContainer: LinearLayout
    private lateinit var imageFinal: ImageView
    private var uploadedFinalImageUrl: String? = null
    private lateinit var btnAddIngredient: Button
    private lateinit var btnAddStep: Button
    private lateinit var btnSelectFinalImage: Button
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar
    // FINAL Image section upload...
    private var finalImageUri: Uri? = null
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                finalImageUri = uri
                imageFinal.setImageURI(uri)
            }
        }
    //STEP Images upload section...
    private val stepImageUris = mutableMapOf<View, Uri>()
    private var pendingRowView: View? = null

    private val stepImagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null && pendingRowView != null) {
                // Store the URI in the map using the view as key
                stepImageUris[pendingRowView!!] = uri

                // Find the ImageView in the current row and update it
                val preview = pendingRowView!!.findViewById<ImageView>(R.id.imageStepPreview)
                preview.setImageURI(uri)
                preview.visibility = View.VISIBLE
            }
            pendingRowView = null
        }
    //...
    private val unitTypes = UnitType.entries.map { it.sk_name }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_recipe)
        //Edit recipe section
        editingRecipeId = intent.getStringExtra("edit_recipe_id")
        isEditMode = editingRecipeId != null
        //
        ingredientsContainer = findViewById(R.id.ingredientsContainer)
        stepsContainer = findViewById(R.id.stepsContainer)
        imageFinal = findViewById(R.id.imageFinal)
        btnAddIngredient = findViewById(R.id.btnAddIngredient)
        btnAddStep = findViewById(R.id.btnAddStep)
        btnSelectFinalImage = findViewById(R.id.btnSelectImage)
        btnSave = findViewById(R.id.btnSaveRecipe)
        progressBar = findViewById(R.id.progressBarUpload)

        btnAddIngredient.setOnClickListener { addIngredientRow() }
        btnAddStep.setOnClickListener { addStepRow() }
        btnSelectFinalImage.setOnClickListener { imagePickerLauncher.launch("image/*") }
        btnSave.setOnClickListener { saveRecipe() }

        if(isEditMode){
            loadRecipeData(editingRecipeId!!)
        }
        else{
            setupToolbar()
            addIngredientRow()
            addStepRow()
        }

    }

    private fun setupToolbar(recipeTitle: String? = null) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
        if(recipeTitle != null)
        {
            supportActionBar?.title = recipeTitle
        }
        else
        {
            supportActionBar?.title = getString(R.string.activityName_AddRecipe)
        }
        toolbar.setNavigationOnClickListener { finish() }
    }
    private fun addIngredientRow() {
        val row = layoutInflater.inflate(R.layout.ingredient_row, null)

        val spinner = row.findViewById<Spinner>(R.id.spinnerUnit)
        val adapter = ArrayAdapter(this, R.layout.spinner_item, unitTypes)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinner.adapter = adapter

        val btnRemoveIngredient = row.findViewById<Button>(R.id.btnRemoveIngredient)
        btnRemoveIngredient.setOnClickListener {
            ingredientsContainer.removeView(row)
        }

        ingredientsContainer.addView(row)
    }
    private fun addStepRow() {
        val row = layoutInflater.inflate(R.layout.step_row, null)

        // Assign a tag to each row for tracking
        row.tag = UUID.randomUUID().toString()  // Use a unique identifier for each row
        // Find the select button and set an onClickListener
        val btnSelect = row.findViewById<Button>(R.id.btnSelectStepImage)
        btnSelect.setOnClickListener {
            pendingRowView = row  // Set the current row as pending
            stepImagePickerLauncher.launch("image/*")
        }

        // Find the remove button and set an onClickListener
        val btnRemoveStep = row.findViewById<Button>(R.id.btnRemoveStep)
        btnRemoveStep.setOnClickListener {
            // Remove the row from the container
            stepsContainer.removeView(row)

            // Also remove the image URI from the map
            stepImageUris.remove(row)
        }
        // Add the row to the container
        stepsContainer.addView(row)
    }
    private fun saveRecipe() {
        btnSave.isEnabled = false
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val recipeId = FirebaseFirestore.getInstance().collection("recipes").document().id
        val stepImageUploadTasks = mutableListOf<Task<Uri>>()

        // 1. CHECK IF THERE IS A FINAL IMAGE AND UPLOAD
        val finalUploadTask = if (finalImageUri != null && finalImageUri.toString().startsWith("content://")) {
            val finalStorageRef = FirebaseStorage.getInstance().reference
                .child("recipe_images/$userId/$recipeId/finalImage.jpg")
            finalStorageRef.putFile(finalImageUri!!).addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                progressBar.progress = progress
            }.continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")

                finalStorageRef.downloadUrl
            }.addOnSuccessListener { uri ->
                uploadedFinalImageUrl = uri.toString()
            }
        }
        else {
            Tasks.forResult(null) // empty task
        }
        // 2. LOOP THROUGH ALL STEPS AND CHECK IF THERE IS AN IMAGE TO UPLOAD
        for (i in 0 until stepsContainer.childCount) {
            val row = stepsContainer.getChildAt(i)
            val imageUri = stepImageUris[row]
            Log.d("AddRecipeActivity", "imageUri: $imageUri")
            //check if there is an imageUri and if it is new from device (content://)
            if (imageUri != null && imageUri.toString().startsWith("content://")) {
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("recipe_steps/$userId/$recipeId/step_$i.jpg")

                val uploadTask = storageRef.putFile(imageUri).addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    progressBar.progress = progress
                }.continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    storageRef.downloadUrl
                }.addOnSuccessListener { uri ->
                    stepImageUris[row] = uri
                    row.findViewById<ImageView>(R.id.imageUploadCheckmark).visibility = View.VISIBLE
                }
                stepImageUploadTasks.add(uploadTask)
            }
        }


        // 3. WAIT FOR ALL TASKS AND THEN SAVE RECIPE
        Tasks.whenAllComplete(listOfNotNull(finalUploadTask) + stepImageUploadTasks)
            .addOnSuccessListener {
                saveRecipeToFirestore(recipeId)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Nahrávanie obrázkov zlyhalo", Toast.LENGTH_SHORT).show()
            }
    }
    private fun saveRecipeToFirestore(recipeId: String) {
        val ingredients = mutableListOf<Ingredient>()
        val steps = mutableListOf<Step>()
        val recipeName = findViewById<EditText>(R.id.inputRecipeName).text.toString()
        //normalized name (lowercase, diacritics free)
        val normalizedName = normalizeText(recipeName)
        val description = findViewById<EditText>(R.id.inputRecipeDescription).text.toString()
        val prepTime = findViewById<EditText>(R.id.inputRecipePrepTime).text.toString().toIntOrNull() ?: 0
        // INGREDIENTS SAVING
        for (i in 0 until ingredientsContainer.childCount) {
            val row = ingredientsContainer.getChildAt(i)
            val amount = row.findViewById<EditText>(R.id.editAmount).text.toString().toDoubleOrNull() ?: 0.0
            val name = row.findViewById<EditText>(R.id.editName).text.toString()
            val unitStr = row.findViewById<Spinner>(R.id.spinnerUnit).selectedItem.toString()
            val unit = UnitType.fromSkName(unitStr)
            ingredients.add(Ingredient(name, amount, unit))
        }

        // STEPS SAVING
        for (i in 0 until stepsContainer.childCount) {
            val row = stepsContainer.getChildAt(i)
            val desc = row.findViewById<EditText>(R.id.editStepDescription).text.toString()
            val imgUrl = stepImageUris[row]?.toString() ?: ""  // Use row as the key
            steps.add(Step(desc, imgUrl))
        }

        // FINAL IMAGE SAVING
        val finalImage = uploadedFinalImageUrl
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // FINAL RECIPE SAVING
            val recipeDataToUpload = Recipe(
                userId = userId,
                name = recipeName,
                name_search = normalizedName,
                description = description,
                prepTime = prepTime,
                ingredients = ingredients,
                steps = steps,
                finalImage = finalImage ?: "",
                dateCreated = System.currentTimeMillis(),
                rating = 0.0,
                ratingCount = 0
            )
            recipeDataToUpload.setId(recipeId)
        if (validateUploadData(recipeDataToUpload))
        {
            if(isEditMode) {updateRecipe(recipeDataToUpload, editingRecipeId?:"")}
            else {createRecipe(recipeDataToUpload, recipeId)}
        }
        else{
            progressBar.visibility = View.GONE
            btnSave.isEnabled = true
        }
    }
    fun normalizeText(input: String): String {
        val normalized = Normalizer.normalize(input.lowercase(), Normalizer.Form.NFD)
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("")
    }

    //Edit recipe section
    private fun loadRecipeData(recipeId: String) {
        recipeRepository.getRecipeById(recipeId) { recipe, success ->
            if (success && recipe != null) {
                recipeToEdit = recipe
                setupToolbar(recipe.name)
                populateUIWithRecipe(recipe)
            }
            else {
                Toast.makeText(this, "Chyba pri načítaní receptu", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    private fun populateUIWithRecipe(recipe: Recipe) {
        // basic info
        findViewById<EditText>(R.id.inputRecipeName).setText(recipe.name)
        findViewById<EditText>(R.id.inputRecipeDescription).setText(recipe.description)
        findViewById<EditText>(R.id.inputRecipePrepTime).setText(recipe.prepTime.toString())

        // final image
        if (recipe.finalImage.isNotBlank()) {
            uploadedFinalImageUrl = recipe.finalImage
            Glide.with(this).load(recipe.finalImage).into(imageFinal)
        }
        // ingredients
        populateIngredients(recipe)
        // steps
        populateSteps(recipe)
    }
    private fun populateSteps(recipe: Recipe){
        stepsContainer.removeAllViews()
        recipe.steps.forEach { step ->
            val row = layoutInflater.inflate(R.layout.step_row, null)

            row.tag = UUID.randomUUID().toString()
            row.findViewById<EditText>(R.id.editStepDescription).setText(step.text)

            if (step.image.isNotBlank()) {
                val preview = row.findViewById<ImageView>(R.id.imageStepPreview)
                Glide.with(this).load(step.image).into(preview)
                preview.visibility = View.VISIBLE

                // Uloženie URL ako Uri (len dočasne na znovunahratie)
                stepImageUris[row] = Uri.parse(step.image)

                val checkmark = row.findViewById<ImageView>(R.id.imageUploadCheckmark)
                checkmark.visibility = View.VISIBLE
            }

            val btnSelect = row.findViewById<Button>(R.id.btnSelectStepImage)
            btnSelect.setOnClickListener {
                pendingRowView = row
                stepImagePickerLauncher.launch("image/*")
            }

            val btnRemoveStep = row.findViewById<Button>(R.id.btnRemoveStep)
            btnRemoveStep.setOnClickListener {
                stepsContainer.removeView(row)
                stepImageUris.remove(row)
            }

            stepsContainer.addView(row)
        }
    }
    private fun populateIngredients(recipe: Recipe){
        ingredientsContainer.removeAllViews()
        recipe.ingredients.forEach { ingredient ->
            val row = layoutInflater.inflate(R.layout.ingredient_row, null)

            row.findViewById<EditText>(R.id.editAmount).setText(ingredient.amount.toString())
            row.findViewById<EditText>(R.id.editName).setText(ingredient.name)

            val spinner = row.findViewById<Spinner>(R.id.spinnerUnit)
            val adapter = ArrayAdapter(this, R.layout.spinner_item, unitTypes)
            adapter.setDropDownViewResource(R.layout.spinner_item)
            spinner.adapter = adapter
            spinner.setSelection(unitTypes.indexOf(ingredient.unit.sk_name))

            val btnRemoveIngredient = row.findViewById<Button>(R.id.btnRemoveIngredient)
            btnRemoveIngredient.setOnClickListener {
                ingredientsContainer.removeView(row)
            }

            ingredientsContainer.addView(row)
        }
    }

    //Validation section
    private fun validateUploadData(recipe:Recipe):Boolean
    {
        if (recipe.name.isBlank() || recipe.name.length < 5) {
            Toast.makeText(this, "Názov receptu musí mať aspoň 5 znakov", Toast.LENGTH_SHORT).show()
            return false
        }
        if (recipe.description.isBlank() || recipe.description.length < 10) {
            Toast.makeText(this, "Popis receptu musí mať aspoň 10 znakov", Toast.LENGTH_SHORT).show()
            return false
        }
        if (recipe.prepTime <= 0) {
            Toast.makeText(this, "Zadaj odhadovaný čas prípravy", Toast.LENGTH_SHORT).show()
            return false
        }
        if (recipe.ingredients.isEmpty()) {
            Toast.makeText(this, "Pridaj aspoň jednu ingredienciu", Toast.LENGTH_SHORT).show()
            return false
        }
        for ((index, ingredient) in recipe.ingredients.withIndex()) {
            if (ingredient.name.isBlank()) {
                Toast.makeText(this, "Ingrediencia č. ${index + 1} nemá názov", Toast.LENGTH_SHORT)
                    .show()
                return false
            }
            if (ingredient.amount == 0.0) {
                Toast.makeText(
                    this,
                    "Ingrediencia č. ${index + 1} nemá zadané množstvo",
                    Toast.LENGTH_SHORT
                )
                    .show()
                return false
            }
        }
        if (recipe.steps.isEmpty()) {
            Toast.makeText(this, "Pridaj aspoň jeden krok", Toast.LENGTH_SHORT).show()
            return false
        }

        for ((index, step) in recipe.steps.withIndex()) {
            if (step.text.isBlank()) {
                Toast.makeText(this, "Krok č. ${index + 1} nemá popis", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        if (recipe.finalImage.isBlank()) {
            Toast.makeText(this, "Pridaj výsledný obrázok.", Toast.LENGTH_SHORT).show()
            return false
        }

        // Ak všetko prešlo
        return true
    }
    //SEND TO REPO
    private fun createRecipe(recipe:Recipe, recipeId:String)
    {
        recipeRepository.addRecipe(recipe, recipeId) { success ->
            if (success) {
                Toast.makeText(this, "Recipe saved!", Toast.LENGTH_SHORT).show()
                progressBar.progress = 100
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Failed to save recipe.", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
            }
        }
    }
    private fun updateRecipe(recipeToUpload:Recipe, recipeId:String) {
        val recipeDataToUpload = mapOf(
            "name" to recipeToUpload.name,
            "name_search" to recipeToUpload.name_search,
            "description" to recipeToUpload.description,
            "prepTime" to recipeToUpload.prepTime,
            "ingredients" to recipeToUpload.ingredients,
            "steps" to recipeToUpload.steps,
            "finalImage" to (recipeToUpload.finalImage),
            "dateUpdated" to System.currentTimeMillis()
        )
        recipeRepository.updateRecipe(recipeId, recipeDataToUpload) { success ->
            if (success) {
                Toast.makeText(this, "Recept bol úspešne zmenený.", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Nepodarilo sa zmeniť recept.", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
            }
        }
    }
}