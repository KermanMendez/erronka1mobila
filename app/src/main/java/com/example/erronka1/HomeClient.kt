package com.example.erronka1

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.erronka1.databinding.ActivityHomeClientBinding
import com.example.erronka1.databinding.ActivityUserProfileBinding
import com.example.erronka1.modelo.Ariketa
import com.example.erronka1.modelo.Level
import com.example.erronka1.modelo.Workout
import com.example.erronka1.rvLevel.LevelAdapter
import com.example.erronka1.rvWorkout.WorkoutAdapter

class HomeClient : AppCompatActivity() {

    private lateinit var binding: ActivityHomeClientBinding

    private var language = listOf("Español", "Euskara", "English")
    private var levels: List<Level> = listOf()
    private var selectedLanguageChoice: String = language[0]
    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var levelAdapter: LevelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeClientBinding.inflate(layoutInflater)
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
            print("Clicked")
            showWorkouts()
            if (up) {
                binding.ivUpDownArrow.setImageResource(R.drawable.outline_keyboard_arrow_down_24)
                up = false
            } else {
                binding.ivUpDownArrow.setImageResource(R.drawable.outline_keyboard_arrow_up_24)
                up = true
            }
        }
        binding.ivProfile.setOnClickListener {
            println("--------------------------------------------------------")
            showUserProfileDialog()
        }

        val gehitu: List<Ariketa> = listOf(
            Ariketa(izena = "Jumping Jacks", reps = 20, sets = 3),
            Ariketa(izena = "Push-ups", reps = 10, sets = 3),
            Ariketa(izena = "Bodyweight Squats", reps = 15, sets = 3),
            Ariketa(izena = "Plank", reps = 30, sets = 3)
        )

        val workout: Workout = Workout(
            title = "Full Body Beginner",
            description = "A",
            level = 1,
            ariketak = gehitu
        )
        val workout2: Workout = Workout(
            title = "Egunon",
            description = "Oso txarto",
            level = 2,
            ariketak = gehitu
        )
        val workoutList: List<Workout> = listOf(workout, workout2)
        levels = listOf(
            Level(1),
            Level(2),
            Level(3)
        )

        levelAdapter = LevelAdapter(levels, {onItemCategoriaSelected(it) })
        binding.rvLevels.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvLevels.adapter = levelAdapter

        workoutAdapter = WorkoutAdapter(workoutList) {}
        binding.rvTableWorkouts.layoutManager = LinearLayoutManager(this)
        binding.rvTableWorkouts.adapter = workoutAdapter
    }
    private fun showUserProfileDialog() {

        val userBinding = ActivityUserProfileBinding.inflate(layoutInflater)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, language)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        userBinding.spLanguages.adapter = adapter
        userBinding.spLanguages.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLanguageChoice = language[position]
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        val dialog = Dialog(this)
        dialog.setContentView(userBinding.root)
        dialog.show()
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

    private fun onItemCategoriaSelected(position: Int){
        levels[position].isSelected = !levels[position].isSelected
        levelAdapter.notifyItemChanged(position)
        updateTasks()
    }
    private fun updateTasks(){
//        val selectedCategories: List<Categoria> = categorias.filter { it.isSelected }
//        val newTasks = tareas.filter { selectedCategories.contains(categoria) }
//        tareasAdapter.tareas = newTasks
    }

}