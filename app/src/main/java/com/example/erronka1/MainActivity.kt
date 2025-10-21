package com.example.erronka1

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.erronka1.db.FirebaseSingleton
import com.example.erronka1.databinding.ActivityMainBinding
import com.example.erronka1.model.User
import com.example.erronka1.model.Workout
import com.example.erronka1.db.Backup

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initListeners()
        initBackup()



    }

    private fun initListeners() {
        binding.btnLogin.setOnClickListener {
            val intent = android.content.Intent(this, Login::class.java)
            startActivity(intent)
        }

        binding.btnSignUp.setOnClickListener {
            val intent = android.content.Intent(this, Register::class.java)
            startActivity(intent)
        }
    }

    private fun initBackup() {
        val db = FirebaseSingleton.db
        val backup = Backup()
        var workouts: List<Workout>? = null
        var users: List<User>? = null

        db.collection("workouts").get().addOnSuccessListener { result ->
            try {
                workouts = result.toObjects(Workout::class.java)
                Log.d("MainActivity", "Workouts descargados: ${workouts?.size}")
                if (workouts != null && users != null) {
                    try {
                        backup.writeBackupToXml(workouts!!, users!!, filesDir, "backupdata.xml")
                        Log.d("MainActivity", "Backup guardado correctamente con workouts y users.")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error guardando backup: ", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error procesando workouts: ", e)
            }
        }.addOnFailureListener { e ->
            Log.e("MainActivity", "Error getting workouts: ", e)
        }

        db.collection("users").get().addOnSuccessListener { result ->
            try {
                users = result.documents.mapNotNull { doc ->
                    try {
                        val user = doc.toObject(User::class.java)
                        val birthdateObj = doc.get("birthdate")
                        val birthdateString = if (birthdateObj is String) birthdateObj else ""
                        val levelObj = doc.get("level")
                        val levelInt = when (levelObj) {
                            is Number -> levelObj.toInt()
                            is String -> levelObj.toIntOrNull() ?: 0
                            else -> 0
                        }
                        val isTrainerObj = doc.get("isTrainer")
                        val isTrainerBool = when (isTrainerObj) {
                            is Boolean -> isTrainerObj
                            is String -> isTrainerObj.equals("true", ignoreCase = true)
                            is Number -> isTrainerObj.toInt() != 0
                            else -> false
                        }
                        if (user != null) {
                            user.copy(birthdate = birthdateString, level = levelInt, isTrainer = isTrainerBool)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error procesando usuario: ", e)
                        null
                    }
                }
                Log.d("MainActivity", "Usuarios descargados: ${users?.size}")
                if (workouts != null && users != null) {
                    try {
                        backup.writeBackupToXml(workouts!!, users!!, filesDir, "backupdata.xml")
                        Log.d("MainActivity", "Backup guardado correctamente con workouts y users.")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error guardando backup: ", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error procesando users: ", e)
            }
        }.addOnFailureListener { e ->
            Log.e("MainActivity", "Error getting users: ", e)
        }
    }
}