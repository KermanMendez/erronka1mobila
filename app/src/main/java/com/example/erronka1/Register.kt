package com.example.erronka1

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Register : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        //private lateinit var binding: RegisterActivityBinding

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //binding.RegisterActivityBinding.inflate(layoutInflater)
        //setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initComponents()
        initListeners()

    }

    private fun initComponents(){

    }

    private fun initListeners(){

        /*binding.btnSignUp.setOnClickListener {
            val etEmail=etEmail.text.toString()
            val etPassword=etPassword.text.toString()
            val etPassword2=etPassword2.text.toString()
            val etIzena=etIzena.text.toString()
            val etAbizena=etAbizena.text.toString()
            val etAbizena2=etAbizena2.text.toString()
            val isTrainer=binding.switch1.isChecked

            FirebaseSingleton.auth.createUserWithEmailAndPassword(etEmail,etPassword).addOnCompleteListener { task->
                if (task.isSuccessful && etPassword==etPassword2){
                    Toast.makeText(this, "Usuario registrado", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, HomeClient::class.java)
                    startActivity(intent)
                }else{
                    //Mostrar error
                    task.exception?.message?.let { message ->
                        Toast.makeText(this, "Error al crear usuario", Toast.LENGTH_LONG).show()
                    }
                }
            }

        }*/
    }
}