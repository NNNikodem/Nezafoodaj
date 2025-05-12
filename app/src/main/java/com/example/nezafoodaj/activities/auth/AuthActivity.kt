package com.example.nezafoodaj.activities.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.nezafoodaj.R
import com.example.nezafoodaj.data.UserRepository
import com.example.nezafoodaj.databinding.ActivityAuthBinding
import com.example.nezafoodaj.activities.main.MainActivity
import com.example.nezafoodaj.models.User
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userRepo:UserRepository = UserRepository()
    private var isLogin = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(isSignedIn()){
            goToMainActivity()
        }
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_moss_green)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.isAppearanceLightStatusBars = false
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
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    userRepo.getCurrentUserData(
                        onComplete = { user, success ->
                            if (success && user != null) {
                                if(!auth.currentUser!!.isEmailVerified){
                                    Toast.makeText(this, "Prosíme, overte svoj účet kliknutím na odkaz v schránke: $email"
                                        , Toast.LENGTH_SHORT).show()
                                    auth.signOut()
                                }
                                else{
                                    saveUserDataLocally(user)
                                    goToMainActivity()
                                }
                            } else {
                                Toast.makeText(this, "Nepodarilo sa načítať údaje používateľa", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    Toast.makeText(this, "Prihlásenie zlyhalo: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerUser(email: String,name: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                Toast.makeText(this, "Overovací e-mail bol odoslaný na $email", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Chyba pri odosielaní overovacieho e-mailu: ${verificationTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    val user = User(name, email, "", 0, "", System.currentTimeMillis(), auth.currentUser?.uid ?: "", false, emptyList())
                    userRepo.addUser(user)
                    loginUser(email, password)
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
    private fun isSignedIn(): Boolean {
        return auth.currentUser != null
    }
    private fun saveUserDataLocally(user: User) {
        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        Log.d("Auth_Activity", "Saving user data: $user")
        with(prefs.edit()) {
            putString("userName", user.name)
            putString("userEmail", user.email)
            putString("userId", user.id)
            putBoolean("admin", user.admin)
            putInt("userAge", user.age)
            putString("userGender", user.gender)
            putLong("userSince", user.dateCreated)
            putString("userPhotoURL", user.profilePhotoURL)
            apply()
        }
    }
}
