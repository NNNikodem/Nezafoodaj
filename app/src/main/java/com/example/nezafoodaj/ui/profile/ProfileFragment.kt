package com.example.nezafoodaj

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.example.nezafoodaj.auth.AuthActivity
import com.example.nezafoodaj.data.UserRepository
import com.example.nezafoodaj.main.MainActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userRepo: UserRepository = UserRepository()
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_profile, container, false)
        tvUserName = view.findViewById(R.id.tv_userName)
        tvUserEmail = view.findViewById(R.id.tv_userEmail)
        updateUI()
        view.findViewById<ImageButton>(R.id.btnLogout).setOnClickListener {
            logOut()
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
        tvUserName.text = prefs.getString("userName", "")
        tvUserEmail.text = prefs.getString("userEmail", "")
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