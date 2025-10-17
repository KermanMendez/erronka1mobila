package com.example.erronka1


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.erronka1.databinding.LoginBinding

class Login : AppCompatActivity() {

    private lateinit var binding: LoginBinding

    private var language = listOf("Español", "Euskara", "English")
    private var selectedLanguageChoice: String = language[0]

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
       // initSpinner()
        initListeners()


    }

   /* fun initSpinner() {
        val spinner = findViewById<Spinner>(R.id.spLanguages)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, language)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Guardar la selección para usarla en la app si hace falta
                selectedLanguageChoice = language[position]
                // Opcional: mostrar breve confirmación
                Toast.makeText(this@Login, "Idioma: $selectedLanguageChoice", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }*/

    private fun initListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.username.text.toString()
            val password = binding.password.text.toString()

            FirebaseSingleton.auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(this, HomeClient::class.java)
                        startActivity(intent)
                        Toast.makeText(this, "Ongi etorri!", Toast.LENGTH_LONG).show()
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