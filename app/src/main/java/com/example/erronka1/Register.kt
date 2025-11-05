package com.example.erronka1

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Calendar
import java.util.Locale
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.erronka1.db.FirebaseSingleton
import com.example.erronka1.databinding.ActivityRegisterBinding
import com.example.erronka1.model.User

class Register : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.w("Register", "FirebaseApp.initializeApp botatzen du: ${e.message}")
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

            // Balioztatu sarrera eremuak
            if (etEmail.isEmpty() || etPassword.isEmpty()) {
                Toast.makeText(this, "Email eta pasahitza bete behar dira", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (etPassword != etPasswordConfirm) {
                Toast.makeText(this, "Pasahitzak ez datoz bat", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            FirebaseSingleton.auth.createUserWithEmailAndPassword(etEmail, etPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        // Erabiltzailearen UID lortu
                        val uid = task.result?.user?.uid ?: FirebaseAuth.getInstance().currentUser?.uid

                        if (uid == null) {
                            Toast.makeText(this, "Erabiltzailea sortuta baina UID hutsa", Toast.LENGTH_LONG).show()
                            return@addOnCompleteListener
                        }

                        val user = User(
                            uid,
                            etUsername,
                            etLastname1,
                            etLastname2,
                            etBirthdate,
                            rbTrainer,

                            )

                        // firestore-ra gorde erabiltzailearen datuak
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
                                Toast.makeText(this, "Erabiltzailea erregistratua", Toast.LENGTH_LONG).show()
                                val intent = Intent(this, HomeClient::class.java)
                                startActivity(intent)
                            }
                            .addOnFailureListener { e ->
                                if (e is FirebaseFirestoreException) {
                                    if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                        Toast.makeText(this, "Firestore PERMISSION_DENIED: konprobatu permisoak", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(this, "Errorea erabiltzaileen datuak gordetzean: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(this, "Errorea erabiltzaileen datuak gordetzean: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        task.exception?.message?.let { message ->
                            Toast.makeText(this, "Errorea erabiltzailea sortzean: $message", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }
        }
    }
}
