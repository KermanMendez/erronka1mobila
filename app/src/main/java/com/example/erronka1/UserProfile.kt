package com.example.erronka1

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.erronka1.databinding.ActivityMainBinding
import com.example.erronka1.databinding.ActivityUserProfileBinding
import com.example.erronka1.db.FirebaseSingleton
import com.example.erronka1.model.User

class UserProfile : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
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
                            binding.editTextName.setText(user.name ?: "")
                            binding.editTextSurname.setText(user.surname ?: "")
                            binding.editTextSurname2.setText(user.surname2 ?: "")
                            binding.editTextBirthdate.setText(user.birthdate ?: "")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateUserProfile() {
        Log.d("UserProfile", "Updating user:")
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
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun setupUpdateButton() {
        binding.btnSaveChanges.setOnClickListener {
            updateUserProfile()
            Log.d("UserProfile", "Update button clicked")
        }
    }
}