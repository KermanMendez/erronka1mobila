package com.example.erronka1

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.erronka1.databinding.ActivityHomeTrainerBinding
import com.example.erronka1.databinding.ActivityNewWorkoutBinding
import com.example.erronka1.databinding.ActivitySettingsBinding
import com.example.erronka1.db.FirebaseSingleton
import com.example.erronka1.model.Ariketa
import com.example.erronka1.model.Workout
import com.example.erronka1.rvWorkout.WorkoutAdapter

class HomeTrainer : AppCompatActivity() {

    private lateinit var binding : ActivityHomeTrainerBinding
    private var hideRunnable: Runnable? = null
    private lateinit var workoutAdapter: WorkoutAdapter
    private var selectedWorkout: Workout? = null
    private var language = listOf("Euskara", "Español", "English")
    private var selectedLanguageChoice: String = language[0]
    private var up: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeTrainerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            return@setOnApplyWindowInsetsListener insets
        }

        binding.ivBacktoLogin.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
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
        binding.ivSettings.setOnClickListener {
            showUserSettingsDialog()
        }
        binding.ivProfile.setOnClickListener {
            val intent = android.content.Intent(this, UserProfile::class.java)
            startActivity(intent)
            finish()
        }


        // Show a centered fullscreen splash (tvSplash) while we prepare the screen
        val fallback = FirebaseSingleton.auth.currentUser?.displayName
            ?: FirebaseSingleton.auth.currentUser?.email ?: getString(R.string.hello)

        // Set the splash text (e.g., "Hola <fallback>") and ensure the overlay is visible
        binding.tvSplash.text = getString(R.string.hello_name, fallback)
        binding.splashOverlay.visibility = View.VISIBLE
        binding.splashOverlay.alpha = 1f

        // Simple entrance animation for the splash text
        binding.tvSplash.alpha = 0f
        binding.tvSplash.scaleX = 0.95f
        binding.tvSplash.scaleY = 0.95f
        binding.tvSplash.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(400).start()

        // Start loading user data immediately in background; update splash text when ready.
        // We will keep the splash visible until either the name arrives or a timeout elapses.
        val splashDisplayMs = 1800L
        val hideDelayAfterNameMs = 800L

        // Runnable to hide the overlay (used as timeout fallback)
        hideRunnable = Runnable {
            Log.d("HomeTrainer", "hideRunnable executing: starting hide animation")
            binding.splashOverlay.animate().alpha(0f).setDuration(350).withEndAction {
                binding.splashOverlay.visibility = View.GONE
                Log.d("HomeTrainer", "splashOverlay is now GONE")
            }.start()
        }

        // Schedule the fallback hide
        hideRunnable?.let {
            Log.d("HomeTrainer", "Scheduling hideRunnable in $splashDisplayMs ms")
            binding.splashOverlay.postDelayed(it, splashDisplayMs)
        }

        // Try to load user's display name (or username) to show on the splash and re-schedule a shorter hide
        FirebaseSingleton.auth.currentUser?.uid?.let { uid ->
            FirebaseSingleton.db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("username") ?: doc.getString("name") ?: fallback
                    Log.d("HomeTrainer", "Loaded user name for uid=$uid -> $name")
                    if (binding.splashOverlay.isVisible) {
                        binding.tvSplash.text = getString(R.string.hello_name, name)
                        // cancel previous hide and schedule a short delay so the user sees the real name
                        hideRunnable?.let {
                            binding.splashOverlay.removeCallbacks(it)
                            binding.splashOverlay.postDelayed(it, hideDelayAfterNameMs)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("HomeTrainer", "Failed to load user doc uid=$uid: ${e.message}")
                }
        }

        /*val gehitu: List<Ariketa> = listOf(
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
        val workoutList: MutableList<Workout> = loadAllWorkouts()*/


        loadAllWorkouts { workoutList ->
            workoutAdapter = WorkoutAdapter(workoutList) { selectedPosition ->
                selectedWorkout = workoutList[selectedPosition]
            }
            binding.rvTableWorkouts.layoutManager = LinearLayoutManager(this)
            binding.rvTableWorkouts.adapter = workoutAdapter

            binding.btnCreateWorkout.setOnClickListener {
                showCreateWorkoutDialog(workoutList)
                workoutAdapter.notifyDataSetChanged()
            }
            binding.btnDeleteWorkout.setOnClickListener {
                val workout = selectedWorkout
                if (workout != null){
                    deleteWorkout(workout.id)
                    val index = workoutList.indexOf(workout)
                    if (index != -1) {
                        workoutList.removeAt(index)
                        workoutAdapter.notifyItemRemoved(index)
                    }
                    selectedWorkout = null
                } else {
                    Log.w("HomeTrainer", "No workout seleccionado para eliminar")
                }
            }
        }
    }

    override fun onDestroy() {
        // remove any pending callbacks to avoid running after the activity is destroyed
        hideRunnable?.let {
            Log.d("HomeTrainer", "Removing pending hideRunnable callbacks in onDestroy")
            binding.splashOverlay.removeCallbacks(it)
        }
        super.onDestroy()
    }

    private fun showAllWorkouts() {
        // Get user lvl to filter workouts
        val db = FirebaseSingleton.db

        val workoutsMap = mutableMapOf<String, Workout>()
        db.collection("workouts")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val workout = document.toObject(Workout::class.java)
                    workoutsMap[document.id] = workout
                    Log.d("HomeTrainer", "Workout loaded: ${document.id} ->) $workout")
                }
                Log.d("HomeTrainer", "Workout size = ${workoutsMap.size} ")
            }
            .addOnFailureListener { exception ->
                Log.w("HomeTrainer", "Error getting workouts: ", exception)
            }
    }
    private fun loadAllWorkouts(callback: (MutableList<Workout>) -> Unit) {
        // Get user lvl to filter workouts
        val db = FirebaseSingleton.db

        val workoutList: MutableList<Workout> = mutableListOf()

        val workoutsMap = mutableMapOf<String, Workout>()
        db.collection("workouts")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val workout = document.toObject(Workout::class.java)
                    workoutsMap[document.id] = workout
                    workoutList.add(workout)
                    Log.d("HomeTrainer", "Workout loaded: ${document.id} ->) $workout")
                }
                Log.d("HomeTrainer", "Workout size = ${workoutsMap.size} ")
                callback(workoutList)
            }
            .addOnFailureListener { exception ->
                Log.w("HomeTrainer", "Error getting workouts: ", exception)
                callback(mutableListOf())
            }
    }
    private fun addWorkoutWithExcercises(workout: Workout) {
        val db = FirebaseSingleton.db
        val docRef = db.collection("workouts").document() // ID generado
        val batch = db.batch()

        // asignamos el id generado al objeto (no se serializará por @get:Exclude)
        workout.id = docRef.id

        // `ariketak` está excluido por @get:Exclude, por tanto solo se subirán los campos restantes
        batch.set(docRef, workout)

        val exercises = workout.ariketak
        exercises.forEach { ariketa ->
            val exRef = docRef.collection("exercises").document() // doc con ID auto
            batch.set(exRef, ariketa)
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d("HomeClient", "Workout y ${exercises.size} exercises añadidos con ID: ${docRef.id}")
            }
            .addOnFailureListener { e ->
                Log.w("HomeClient", "Error añadiendo workout y exercises", e)
            }
    }

    private fun editWorkout(workout: Workout) {
        val db = FirebaseSingleton.db

        if (workout.id.isBlank()) {
            Log.w("HomeClient", "editWorkout: workout.id está vacío, no se puede actualizar")
            return
        }

        val workoutRef = db.collection("workouts").document(workout.id)

        workoutRef.update(
            "title", workout.title,
            "description", workout.description,
            "level", workout.level,
            "video", workout.video
        )
            .addOnSuccessListener {
                Log.d("HomeClient", "Workout actualizado correctamente id=${workout.id}")
            }
            .addOnFailureListener { e ->
                Log.w("HomeClient", "Error actualizando workout id=${workout.id}", e)
            }
    }

    private fun deleteWorkout(workoutId: String) {
        val db = FirebaseSingleton.db

        if (workoutId.isBlank()) {
            Log.w("HomeClient", "deleteWorkout: workoutId está vacío, no se puede eliminar")
            return
        }

        val workoutRef = db.collection("workouts").document(workoutId)

        workoutRef.delete()
            .addOnSuccessListener {
                Log.d("HomeClient", "Workout eliminado correctamente id=$workoutId")
            }
            .addOnFailureListener { e ->
                Log.w("HomeClient", "Error eliminando workout id=$workoutId", e)
            }
    }

    private fun showUserSettingsDialog() {

        val settingsBinding = ActivitySettingsBinding.inflate(layoutInflater)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, language)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        settingsBinding.spLanguages.adapter = adapter
        settingsBinding.spLanguages.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLanguageChoice = language[position]
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        val dialog = Dialog(this)
        dialog.setContentView(settingsBinding.root)
        dialog.show()
    }

    private fun showCreateWorkoutDialog(workoutList: MutableList<Workout>) {

        val newWorkoutBinding = ActivityNewWorkoutBinding.inflate(layoutInflater)

        newWorkoutBinding.npLevel.minValue = 1
        newWorkoutBinding.npLevel.maxValue = 5

        val dialog = Dialog(this)
        dialog.setContentView(newWorkoutBinding.root)
        dialog.show()

        newWorkoutBinding.btnCancel.setOnClickListener {
            dialog.cancel()
        }
        newWorkoutBinding.btnConfirm.setOnClickListener {
            val workout: Workout = Workout(
                "",
                newWorkoutBinding.etTitle.text.toString(),
                newWorkoutBinding.etDescription.text.toString(),
                newWorkoutBinding.npLevel.value,
                newWorkoutBinding.etVideo.text.toString(),
                listOf()
            )
            addWorkoutWithExcercises(workout)
            workoutList.add(workout)
            Log.i("",workoutList.toString())

            dialog.cancel()
        }
    }
}