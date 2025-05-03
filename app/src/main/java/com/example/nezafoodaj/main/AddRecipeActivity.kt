package com.example.nezafoodaj.main

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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import java.util.UUID

class AddRecipeActivity : AppCompatActivity() {

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
    private val unitTypes = UnitType.entries.map { it.name }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_recipe)
        
        ingredientsContainer = findViewById(R.id.ingredientsContainer)
        stepsContainer = findViewById(R.id.stepsContainer)
        imageFinal = findViewById(R.id.imageFinal)
        btnAddIngredient = findViewById(R.id.btnAddIngredient)
        btnAddStep = findViewById(R.id.btnAddStep)
        btnSelectFinalImage = findViewById(R.id.btnSelectImage)
        btnSave = findViewById(R.id.btnSaveRecipe)
        progressBar = findViewById(R.id.progressBarUpload)

        setupToolbar()
        btnAddIngredient.setOnClickListener { addIngredientRow() }
        btnAddStep.setOnClickListener { addStepRow() }
        btnSelectFinalImage.setOnClickListener { imagePickerLauncher.launch("image/*") }
        btnSave.setOnClickListener { saveRecipe() }

        addIngredientRow()
        addStepRow()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.activityName_AddRecipe)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun addIngredientRow() {
        val row = layoutInflater.inflate(R.layout.ingredient_row, null)

        val spinner = row.findViewById<Spinner>(R.id.spinnerUnit)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, unitTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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
        val finalUploadTask = if (finalImageUri != null) {
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
        } else {
            Tasks.forResult(null) // empty task
        }

        // 2. LOOP THROUGH ALL STEPS AND CHECK IF THERE IS AN IMAGE TO UPLOAD
        for (i in 0 until stepsContainer.childCount) {
            val row = stepsContainer.getChildAt(i)
            val imageUri = stepImageUris[row]  // Now using the row as the key

            if (imageUri != null) {
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("recipe_steps/$userId/$recipeId/step_$i.jpg")

                val uploadTask = storageRef.putFile(imageUri).addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    progressBar.progress = progress
                }.continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    storageRef.downloadUrl
                }.addOnSuccessListener { uri ->
                    // SET THE STEP IMAGE URI AND CHECKMARK
                    stepImageUris[row] = uri  // Store the URI using the row as the key
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
        val description = findViewById<EditText>(R.id.inputRecipeDescription).text.toString()
        val prepTime = findViewById<EditText>(R.id.inputRecipePrepTime).text.toString().toIntOrNull() ?: 0
        // INGREDIENTS SAVING
        for (i in 0 until ingredientsContainer.childCount) {
            val row = ingredientsContainer.getChildAt(i)
            val amount = row.findViewById<EditText>(R.id.editAmount).text.toString().toDoubleOrNull() ?: 0.0
            val name = row.findViewById<EditText>(R.id.editName).text.toString()
            val unitStr = row.findViewById<Spinner>(R.id.spinnerUnit).selectedItem.toString()
            val unit = UnitType.valueOf(unitStr)

            if (name.isNotBlank()) {
                ingredients.add(Ingredient(name, amount, unit))
            }
        }

        // STEPS SAVING
        for (i in 0 until stepsContainer.childCount) {
            val row = stepsContainer.getChildAt(i)
            val desc = row.findViewById<EditText>(R.id.editStepDescription).text.toString()
            val imgUrl = stepImageUris[row]?.toString() ?: ""  // Use row as the key

            if (desc.isNotBlank()) {
                steps.add(Step(desc, imgUrl))
            }
        }

        // FINAL IMAGE SAVING
        val finalImage = uploadedFinalImageUrl
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // FINAL RECIPE SAVING
        val recipe = Recipe(
            userId = userId,
            name = recipeName,
            description = description,
            prepTime = prepTime,
            ingredients = ingredients,
            steps = steps,
            finalImage = finalImage ?: "",
            dateCreated = System.currentTimeMillis(),
            rating = 0.0
        )
        recipe.setId(recipeId)

        // Send to repository for upload and return to previous activity
        RecipeRepository().addRecipe(recipe, recipeId) { success ->
            if (success) {
                Toast.makeText(this, "Recipe saved!", Toast.LENGTH_SHORT).show()
                progressBar.progress = 100
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                finish()
            } else {
                Toast.makeText(this, "Failed to save recipe.", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
            }
        }
    }
}