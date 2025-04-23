package com.example.nezafoodaj.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nezafoodaj.data.UserRepository
import com.example.nezafoodaj.databinding.ActivityAuthBinding
import com.example.nezafoodaj.main.MainActivity
import com.example.nezafoodaj.models.User
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private val auth = FirebaseAuth.getInstance()
    private val userRepo = UserRepository()
    private var isLogin = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAuth.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val name = binding.etName.text.toString().trim()
            val passwordRepeat = binding.etPasswordRepeat.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || (!isLogin && (name.isEmpty() || passwordRepeat.isEmpty()))) {
                Toast.makeText(this, "Vyplň všetky polia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isLogin) {
                loginUser(email, password)
            } else {
                checkInputs(email,name, password, passwordRepeat){valid->
                    if(valid){
                        registerUser(email,name, password)
                    }
                }
            }
        }

        binding.tvSwitchAuth.setOnClickListener {
            isLogin = !isLogin
            updateUI()
        }

        updateUI()
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

    private fun registerUser(email: String,name: String, password: String) {
        val user = User(name, email)
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    userRepo.addUser(user)
                    Toast.makeText(this, "Registrácia úspešná", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Chyba pri registrácii: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun checkInputs(
        email: String,
        name: String,
        password: String,
        passwordRepeat: String,
        onComplete: (Boolean) -> Unit
    ) {
        // Flag to track if any validation failed
        var isValid = true

        // Check if email is in use
        userRepo.checkIfEmailInUse(email) { exists ->
            if (exists) {
                Toast.makeText(this, "Email je obsadený", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            // Check if name is in use
            userRepo.checkIfNameInUse(name) { exists ->
                if (exists) {
                    Toast.makeText(this, "Meno je obsadené", Toast.LENGTH_SHORT).show()
                    isValid = false
                }

                // Check if passwords match
                if (password != passwordRepeat) {
                    Toast.makeText(this, "Heslá sa nezhodujú", Toast.LENGTH_SHORT).show()
                    isValid = false
                }

                // Check if password is long enough
                if (password.length < 6) {
                    Toast.makeText(this, "Heslo musí mať aspon 6 znakov", Toast.LENGTH_SHORT).show()
                    isValid = false
                }

                // Only call onComplete after all checks are done
                onComplete(isValid)
            }
        }
    }

    private fun updateUI() {
        binding.tvMain.text = if (isLogin) "Prihlásenie" else "Registrácia"
        binding.btnAuth.text = if (isLogin) "Prihlásiť sa" else "Registrovať sa"
        binding.tvSwitchAuth.text = if (isLogin) "Nemáš účet? Zaregistruj sa" else "Už máš účet? Prihlás sa"

        // Zobraz/Skry nové polia pri prepínaní
        val visibility = if (isLogin) View.GONE else View.VISIBLE
        binding.etName.visibility = visibility
        binding.tvName.visibility = visibility
        binding.etPasswordRepeat.visibility = visibility
        binding.tvPasswordRepeat.visibility = visibility
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
