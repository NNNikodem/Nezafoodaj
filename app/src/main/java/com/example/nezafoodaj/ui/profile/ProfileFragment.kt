package com.example.nezafoodaj

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.nezafoodaj.activities.auth.AuthActivity
import com.example.nezafoodaj.activities.main.UserSettingsActivity
import com.example.nezafoodaj.data.UserRepository
import com.example.nezafoodaj.ui.myRecipes.MyRecipesFragment.TabType
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date

class ProfileFragment : Fragment() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userRepo: UserRepository = UserRepository()
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserAge: TextView
    private lateinit var tvUserGender: TextView
    private lateinit var tvUserSince: TextView
    private lateinit var btnUserSettings: ImageButton
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            updateUI()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_profile, container, false)
        ivProfilePhoto = view.findViewById(R.id.iv_profilePhoto)
        tvUserName = view.findViewById(R.id.tv_userName)
        tvUserEmail = view.findViewById(R.id.tv_userEmail)
        tvUserAge = view.findViewById(R.id.et_userAge)
        tvUserGender = view.findViewById(R.id.et_userGender)
        tvUserSince = view.findViewById(R.id.tv_userSince)
        btnUserSettings = view.findViewById(R.id.btnUserSettings)
        btnUserSettings.setOnClickListener {
            val intent = Intent(requireContext(), UserSettingsActivity::class.java)
            settingsLauncher.launch(intent)
        }
        updateUI()
        view.findViewById<ImageButton>(R.id.btnLogout).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Odhlásiť sa?")
                .setMessage("Naozaj sa chceš odhlásiť?")
                .setPositiveButton("Áno") { _, _ -> logOut() }
                .setNegativeButton("Zrušiť", null)
                .show()
        }
        return view
    }
    private fun logOut()
    {
        auth.signOut()
        deleteUserDataLocally()
        // AuthActivity and backtrack deletion
        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        Toast.makeText(requireContext(), "Odhlásený", Toast.LENGTH_SHORT).show()
        startActivity(intent)
    }
    private fun updateUI()
    {
        val prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        if(!prefs.getString("userPhotoURL", "").isNullOrBlank()) {
            val radiusInDp = 12
            val radiusInPx = (radiusInDp * resources.displayMetrics.density).toInt()
            val profilePhotoUrl = prefs.getString("userPhotoURL", "")
            Glide.with(this)
                .load(profilePhotoUrl)
                .transform(RoundedCorners(radiusInPx))
                .into(ivProfilePhoto)
        }
        tvUserName.text = prefs.getString("userName", "")
        tvUserEmail.text = prefs.getString("userEmail", "")

        if(prefs.getInt("userAge", 0) == 0) { tvUserAge.text = "--Vek neznámy--" }
        else { tvUserAge.text = prefs.getInt("userAge", 0).toString() }

        if(prefs.getString("userGender", "") == "") { tvUserGender.text = "--Pohlavie neznáme--" }
        else { tvUserGender.text = prefs.getString("userGender", "") }

        val userSinceMillis = prefs.getLong("userSince", 0)
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm")
        val dateString = if (userSinceMillis != 0L) {
            dateFormat.format(Date(userSinceMillis))
        } else ""
        tvUserSince.text = "Používateľ od: " + dateString
    }
    private fun deleteUserDataLocally() {
        val prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            remove("userName")
            remove("userEmail")
            apply()
        }
    }

}