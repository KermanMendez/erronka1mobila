package com.example.erronka1

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.erronka1.db.FirebaseSingleton
import com.example.erronka1.databinding.LoginBinding

class Login : AppCompatActivity() {

    private lateinit var binding: LoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        checkCurrentUserAndRedirect()
        initListeners()


    }


    private fun logOutCurrentUser() {
        FirebaseSingleton.auth.signOut()
    }
    private fun checkCurrentUserAndRedirect() {
        val currentUser = FirebaseSingleton.auth.currentUser
        if (currentUser != null) {
            FirebaseSingleton.db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val isTrainer = document.getBoolean("trainer") ?: false
                        val intent = if (isTrainer) Intent(this, HomeTrainer::class.java)
                        else Intent(this, HomeClient::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
        }
    }
    private fun initListeners() {
        binding.btnLogin.setOnClickListener {

            val email = binding.username.text.toString()
            val password = binding.password.text.toString()

            FirebaseSingleton.auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Obtener el UID del usuario autenticado
                        val uid = FirebaseSingleton.auth.currentUser?.uid ?: return@addOnCompleteListener

                        // Consultar Firestore para obtener los datos del usuario
                        FirebaseSingleton.db.collection("users").document(uid).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val isTrainer = document.getBoolean("trainer") ?: false

                                    if (isTrainer) {
                                        val intent = Intent(this, HomeTrainer::class.java)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        val intent = Intent(this, HomeClient::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    Toast.makeText(this, "Ongi etorri!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this, "Error: Usuario no encontrado en la base de datos", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, "Error obteniendo datos del usuario: ${exception.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        task.exception?.message?.let { message ->
                            Toast.makeText(
                                this,
                                "Errorea saioa hastean: $message",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        
        binding.btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.showPassword.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Mostrar texto de la contraseña
                binding.password.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                // Ocultar texto de la contraseña (mostrar asteriscos)
                binding.password.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            // Mantener el cursor al final del texto después de cambiar la transformación
            val length = binding.password.text?.length ?: 0
            binding.password.setSelection(length)
        }
    }
}