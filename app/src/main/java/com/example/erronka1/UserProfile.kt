package com.example.erronka1

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.erronka1.databinding.ActivityUserProfileBinding
import com.example.erronka1.db.FirebaseSingleton
import com.example.erronka1.model.User
import java.util.Locale

class UserProfile : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLanguage()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        loadUserData()
        setupUpdateButton()
    }

    private fun loadUserData() {
        val authUser = FirebaseSingleton.auth.currentUser

        if (authUser != null) {
            FirebaseSingleton.db.collection("users").document(authUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        currentUser = document.toObject(User::class.java)
                        currentUser?.let { user ->
                            binding.editTextName.setText(user.name)
                            binding.editTextSurname.setText(user.surname)
                            binding.editTextSurname2.setText(user.surname2)
                            binding.editTextBirthdate.text = user.birthdate
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Errorea profila kargatzen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateUserProfile() {
        currentUser?.let { user ->

            user.name = binding.editTextName.text.toString().trim()
            user.surname = binding.editTextSurname.text.toString().trim()
            user.surname2 = binding.editTextSurname2.text.toString().trim()
            user.birthdate = binding.editTextBirthdate.text.toString().trim()



            val authUser = FirebaseSingleton.auth.currentUser
            if (authUser != null) {
                FirebaseSingleton.db.collection("users").document(authUser.uid)
                    .set(user)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profila ondo eguneratuta", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Errorea profila eguneratzen: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun setupUpdateButton() {
        binding.btnSaveChanges.setOnClickListener {
            updateUserProfile()

        }
    }
    @Suppress("DEPRECATION")
    private fun applyLanguage() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val languageCode = prefs.getString("selected_language", "eu") ?: "eu"

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        resources.updateConfiguration(config, resources.displayMetrics)
    }
}