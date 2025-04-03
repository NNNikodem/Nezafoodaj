package com.example.nezafoodaj

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.nezafoodaj.databinding.ActivityAuthBinding
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private var isLogin = true  // Prepínač medzi loginom a registráciou

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAuth.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                if (isLogin) {
                    loginUser(email, password)
                } else {
                    registerUser(email, password)
                }
            } else {
                Toast.makeText(this, "Vyplň všetky polia", Toast.LENGTH_SHORT).show()
            }
        }

        // Prepínanie medzi loginom a registráciou
        binding.tvSwitchAuth.setOnClickListener {
            isLogin = !isLogin
            updateUI()
        }
    }

    private fun loginUser(email: String, password: String) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    goToMainActivity()
                } else {
                    Toast.makeText(this, "Prihlásenie zlyhalo: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerUser(email: String, password: String) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Registrácia úspešná", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Chyba pri registrácii: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateUI() {
        if (isLogin) {
            binding.tvMain.text = "Prihlásenie"
            binding.btnAuth.text = "Prihlásiť sa"
            binding.tvSwitchAuth.text = "Nemáš účet? Zaregistruj sa"
        } else {
            binding.tvMain.text = "Registrácia"
            binding.btnAuth.text = "Registrovať sa"
            binding.tvSwitchAuth.text = "Už máš účet? Prihlás sa"
        }
    }
    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Zatvorí AuthActivity, aby sa nedalo vrátiť späť
    }
}