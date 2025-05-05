package com.example.nezafoodaj.activities.main

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.nezafoodaj.R
import com.example.nezafoodaj.data.UserRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.yalantis.ucrop.UCrop
import java.io.File

class UserSettingsActivity : AppCompatActivity() {
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var etUserName: EditText
    private lateinit var etUserAge: EditText
    private lateinit var rgUserGender: RadioGroup
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    private var selectedImageUri: Uri? = null
    private var uploadedProfilePhotoURL: String? = null
    private var tempImageUri: Uri? = null

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { startCrop(it) }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                tempImageUri?.let { startCrop(it) }
            }
        }

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val resultUri = UCrop.getOutput(result.data!!)
                if (resultUri != null) {
                    selectedImageUri = resultUri
                    setPhoto(resultUri.toString())
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_settings)

        ivProfilePhoto = findViewById(R.id.iv_profilePhoto)
        etUserAge = findViewById(R.id.et_age)
        etUserName = findViewById(R.id.et_name)
        rgUserGender = findViewById(R.id.rg_gender)
        btnSave = findViewById(R.id.btn_save)
        progressBar = findViewById(R.id.progressBarUpload)

        btnSave.setOnClickListener { uploadImageToFirebaseStorage() }

        ivProfilePhoto.setOnClickListener {
            val options = arrayOf("Vybrať z galérie", "Odfotiť")
            AlertDialog.Builder(this)
                .setTitle("Zmeniť profilovú fotku")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> galleryLauncher.launch("image/*")
                        1 -> {
                            val imageFile = File.createTempFile("temp_image", ".jpg", cacheDir)
                            tempImageUri = FileProvider.getUriForFile(
                                this,
                                "${packageName}.fileprovider",
                                imageFile
                            )
                            tempImageUri?.let {
                                cameraLauncher.launch(it)
                            }

                        }
                    }
                }.show()
        }

        setupToolbar()
        updateUI()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
        supportActionBar?.title = getString(R.string.activityName_UserSettings)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun updateUI() {
        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        if(!prefs.getString("userPhotoURL", "").isNullOrBlank()) {
            val profilePhotoUrl = prefs.getString("userPhotoURL", "")
            setPhoto(profilePhotoUrl!!)
        }
        etUserName.setText(prefs.getString("userName", ""))
        etUserAge.setText(prefs.getInt("userAge", 0).toString())
        when (prefs.getString("userGender", "")) {
            "Muž" -> rgUserGender.check(R.id.rb_male)
            "Žena" -> rgUserGender.check(R.id.rb_female)
            else -> rgUserGender.clearCheck()
        }
    }
    private fun setPhoto(uri: String)
    {
        val radiusInDp = 12
        val radiusInPx = (radiusInDp * resources.displayMetrics.density).toInt()
        Glide.with(this)
            .load(uri)
            .transform(RoundedCorners(radiusInPx))
            .into(ivProfilePhoto)
    }

    private fun saveUserData() {
        btnSave.isEnabled = false
        val username = etUserName.text.toString()
        val userAge = etUserAge.text.toString().toIntOrNull()
        val selectedGender = when (rgUserGender.checkedRadioButtonId) {
            R.id.rb_male -> "Muž"
            R.id.rb_female -> "Žena"
            else -> ""
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        UserRepository().updateUser(
            userId,
            username,
            userAge ?: 0,
            selectedGender
        ) { success ->
            if (success) {
                Toast.makeText(this, "Údaje uložené", Toast.LENGTH_SHORT).show()
                saveUserDataLocally(username, userAge ?: 0, selectedGender)
            }
        }

        if (!uploadedProfilePhotoURL.isNullOrBlank()) {
            UserRepository().updateUserPhoto(userId, uploadedProfilePhotoURL!!)
            saveUserPhotoURLLocally(uploadedProfilePhotoURL!!)
        }
    }

    private fun uploadImageToFirebaseStorage() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val imageUri = selectedImageUri ?: return

        progressBar.visibility = View.VISIBLE

        val storageRef = FirebaseStorage.getInstance().reference
            .child("profile_photos/$userId/profilePhoto.jpg")

        val uploadTask = storageRef.putFile(imageUri)
        uploadTask.addOnProgressListener {
            val progress = (100.0 * it.bytesTransferred / it.totalByteCount).toInt()
            progressBar.progress = progress
        }.continueWithTask {
            if (!it.isSuccessful) throw it.exception ?: Exception("Upload failed")
            storageRef.downloadUrl
        }.addOnSuccessListener { uri ->
            uploadedProfilePhotoURL = uri.toString()
            saveUserPhotoURLLocally(uri.toString())
            saveUserData()
        }.addOnFailureListener {
            Toast.makeText(this, "Nepodarilo sa nahrať fotku", Toast.LENGTH_SHORT).show()
        }.addOnCompleteListener {
            progressBar.visibility = View.GONE
        }
    }

    private fun saveUserDataLocally(userName: String, userAge: Int, userGender: String) {
        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("userName", userName)
            putInt("userAge", userAge)
            putString("userGender", userGender)
            apply()
        }
        setResult(Activity.RESULT_OK)
        btnSave.isEnabled = true
        finish()
    }

    private fun saveUserPhotoURLLocally(photoURL: String) {
        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("userPhotoURL", photoURL)
            apply()
        }
    }

    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(90)
            setToolbarTitle("Orež fotku")
            setHideBottomControls(true)
            // Nastavenie farby ikon v UCROPE - DENNY A TMAVY REZIM ROBILI ZLE
            setToolbarWidgetColor(ContextCompat.getColor(this@UserSettingsActivity, R.color.text_color))
        }


        val cropIntent = UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1000, 1000)
            .withOptions(options)
            .getIntent(this)

        cropLauncher.launch(cropIntent)
    }
}
