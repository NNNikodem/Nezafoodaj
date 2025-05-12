package com.example.nezafoodaj.activities.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.nezafoodaj.ProfileFragment
import com.example.nezafoodaj.ui.home.HomeFragment
import com.example.nezafoodaj.ui.myRecipes.MyRecipesFragment
import com.example.nezafoodaj.R
import com.example.nezafoodaj.databinding.ActivityMainBinding
import com.example.nezafoodaj.ui.myRecipes.MyRecipesFragment.TabType
import com.example.nezafoodaj.ui.search.SearchFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val addRecipeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            replaceFragment(MyRecipesFragment())
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        replaceFragment(HomeFragment())


        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_search -> {
                    replaceFragment(SearchFragment())
                    true
                }
                R.id.nav_addRecipe -> {
                    val intent = Intent(this, AddRecipeActivity::class.java)
                    addRecipeLauncher.launch(intent)
                    false
                }
                R.id.nav_myRecipes -> {
                    replaceFragment(MyRecipesFragment())
                    true
                }
                R.id.nav_profile ->
                {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }
    private fun replaceFragment(fragment: Fragment)
    {
        val fragmentManager:FragmentManager =  supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }
}