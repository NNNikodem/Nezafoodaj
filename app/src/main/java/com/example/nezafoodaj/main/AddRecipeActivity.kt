package com.example.nezafoodaj.main

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
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
import com.google.firebase.auth.FirebaseAuth

class AddRecipeActivity : AppCompatActivity() {

    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var stepsContainer: LinearLayout
    private lateinit var editFinalImage: EditText
    private lateinit var btnAddIngredient: Button
    private lateinit var btnAddStep: Button
    private lateinit var btnSave: Button

    private val unitTypes = UnitType.values().map { it.name }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_recipe)

        ingredientsContainer = findViewById(R.id.ingredientsContainer)
        stepsContainer = findViewById(R.id.stepsContainer)
        editFinalImage = findViewById(R.id.editFinalImage)
        btnAddIngredient = findViewById(R.id.btnAddIngredient)
        btnAddStep = findViewById(R.id.btnAddStep)
        btnSave = findViewById(R.id.btnSaveRecipe)

        setupToolbar()
        btnAddIngredient.setOnClickListener { addIngredientRow() }
        btnAddStep.setOnClickListener { addStepRow() }
        btnSave.setOnClickListener { saveRecipe() }

        addIngredientRow()
        addStepRow()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun addIngredientRow() {
        val row = layoutInflater.inflate(R.layout.ingredient_row, null)
        val spinner = row.findViewById<Spinner>(R.id.spinnerUnit)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, unitTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        ingredientsContainer.addView(row)
    }

    private fun addStepRow() {
        val row = layoutInflater.inflate(R.layout.step_row, null)
        stepsContainer.addView(row)
    }

    private fun saveRecipe() {
        val ingredients = mutableListOf<Ingredient>()
        val steps = mutableListOf<Step>()
        val recipeName = findViewById<EditText>(R.id.inputRecipeName).text.toString()
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

        for (i in 0 until stepsContainer.childCount) {
            val row = stepsContainer.getChildAt(i)
            val desc = row.findViewById<EditText>(R.id.editStepDescription).text.toString()
            val img = row.findViewById<EditText>(R.id.editStepImage).text.toString()

            if (desc.isNotBlank()) {
                steps.add(Step(desc, img))
            }
        }

        val finalImage = editFinalImage.text.toString()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val recipe = Recipe(
            userId = userId,
            name = recipeName,
            ingredients = ingredients,
            steps = steps,
            finalImage = finalImage,
            dateCreated = System.currentTimeMillis(),
            rating = 0.0,
            timesRated = 0
        )

        // Send to repository or return to previous activity
        RecipeRepository().addRecipe(recipe) { success ->
            if (success) {
                Toast.makeText(this, "Recipe saved!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to save recipe.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
