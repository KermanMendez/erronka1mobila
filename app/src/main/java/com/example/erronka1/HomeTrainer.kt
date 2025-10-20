package com.example.erronka1

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.erronka1.databinding.ActivityHomeTrainerBinding
import com.example.erronka1.modelo.Workout

class HomeTrainer : AppCompatActivity() {

    private lateinit var binding : ActivityHomeTrainerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeTrainerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        var up = false
        binding.ivBacktoLogin.setOnClickListener {
            val intent = android.content.Intent(this, Login::class.java)
            startActivity(intent)
        }
        binding.llOrder.setOnClickListener {
            if (up) {
                binding.ivUpDownArrow.setImageResource(R.drawable.outline_keyboard_arrow_down_24)
                up = false
            } else {
                binding.ivUpDownArrow.setImageResource(R.drawable.outline_keyboard_arrow_up_24)
                up = true
            }
        }
        binding.ivProfile.setOnClickListener {
            showWorkouts()
        }

    }
    private fun showWorkouts() {
        // Get user lvl to filter workouts
        val uid = FirebaseSingleton.auth.currentUser?.uid ?: return
        // FirebaseSingleton.db.collection("users").document(uid).get()
        val db = FirebaseSingleton.db

        val workoutsMap = mutableMapOf<String, Workout>()
        db.collection("workouts")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val workout = document.toObject(Workout::class.java)
                    workoutsMap[document.id] = workout
                    Log.d("HomeClient", "Workout loaded: ${document.id} ->) $workout")
                }
                Log.d("HomeClient", "Workout size = ${workoutsMap.size} ")
            }
            .addOnFailureListener { exception ->
                Log.w("HomeClient", "Error getting workouts: ", exception)
            }
    }
}