package com.example.erronka1

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Calendar
import java.util.Locale
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.erronka1.databinding.ActivityRegisterBinding

class Register : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ensure Firebase SDK is initialized for Firestore auth calls
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.w("Register", "FirebaseApp.initializeApp threw: ${e.message}")
        }
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initComponents()
        initListeners()

    }

    private fun initComponents() {
        binding.tvBirthdate.text = getString(R.string.select_birthdate_hint)
    }

    private fun initListeners() {

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.btnBirthdate.setOnClickListener {
            val cal = Calendar.getInstance()
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val d = cal.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val dateStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                binding.tvBirthdate.text = dateStr
            }, y, m, d).show()
        }

        binding.btnRegister.setOnClickListener {
            val etUsername = binding.etUsername.text.toString().trim()
            val etPassword = binding.etPassword.text.toString()
            val etPasswordConfirm = binding.etPasswordConfirm.text.toString()
            val etLastname1 = binding.etLastname1.text.toString().trim()
            val etLastname2 = binding.etLastname2.text.toString().trim()
            val etEmail = binding.etEmail.text.toString().trim()
            val etBirthdate = binding.tvBirthdate.text.toString().trim()
            val rbTrainer = binding.rbTrainer.isChecked

            // Validaciones básicas
            if (etEmail.isEmpty() || etPassword.isEmpty()) {
                Toast.makeText(this, "Email eta pasahitza bete behar dira", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (etPassword != etPasswordConfirm) {
                Toast.makeText(this, "Pasahitzak ez datoz bat", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Crear usuario con email (no con username)
            FirebaseSingleton.auth.createUserWithEmailAndPassword(etEmail, etPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        // Obtener UID del usuario creado; prefer the task result then fallback to currentUser
                        val uid = task.result?.user?.uid ?: FirebaseAuth.getInstance().currentUser?.uid

                        if (uid == null) {
                            Toast.makeText(this, "Usuario creado baina UID hutsa", Toast.LENGTH_LONG).show()
                            return@addOnCompleteListener
                        }

                        val user = User(
                            etUsername,
                            etLastname1,
                            etLastname2,
                            etBirthdate,
                            rbTrainer,

                        )

                        // Guardar en Firestore en collection 'users' con documento = uid (usa FirebaseSingleton.db)
                        val db = FirebaseSingleton.db
                        db.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener {
                                // Read back immediately to verify the document exists
                                db.collection("users").document(uid).get()
                                    .addOnSuccessListener { snapshot ->
                                        Log.d("Register", "Firestore readback: exists=${snapshot.exists()}, data=${snapshot.data}")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Register", "Firestore readback failed: ${e.message}")
                                    }
                                Toast.makeText(this, "Usuario registrado", Toast.LENGTH_LONG).show()
                                val intent = Intent(this, HomeClient::class.java)
                                startActivity(intent)
                            }
                            .addOnFailureListener { e ->
                                // If it's a FirestoreException, check the code for permission issues
                                if (e is FirebaseFirestoreException) {
                                    if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                        Toast.makeText(this, "Firestore PERMISSION_DENIED: comprueba las reglas de seguridad en la consola de Firebase", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(this, "Errorea erabiltzaileen datuak gordetzean: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(this, "Errorea erabiltzaileen datuak gordetzean: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        //Mostrar error
                        task.exception?.message?.let { message ->
                            Toast.makeText(this, "Errorea erabiltzailea sortzean: $message", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }
        }
    }
}
