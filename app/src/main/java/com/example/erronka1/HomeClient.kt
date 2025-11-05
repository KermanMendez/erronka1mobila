package com.example.erronka1

import android.content.Intent
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
import androidx.core.view.isVisible
import com.example.erronka1.db.FirebaseSingleton
import com.example.erronka1.databinding.ActivityHomeClientBinding
import com.example.erronka1.databinding.ActivityUserProfileBinding
import com.example.erronka1.model.Workout
import com.example.erronka1.model.Historic
import com.example.erronka1.model.User
import com.example.erronka1.rvHistoric.HistoricAdapter
import com.example.erronka1.rvWorkout.WorkoutAdapter

class HomeClient : AppCompatActivity() {

    private lateinit var binding: ActivityHomeClientBinding
    private var hideRunnable: Runnable? = null
    private var currentUser: User? = null
    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var historicAdapter: HistoricAdapter
    private lateinit var selectedWorkout: Workout
    private lateinit var selectedHistoric: Historic
    private var levels = listOf("Guztiak", "1", "2", "3", "4", "5")
    private var selectedLevelChoice: String = levels[0]
    private var historicList = listOf<Historic>()
    private var prevSelectedPosition = -1
    private lateinit var methods: Methods


    override fun onCreate(savedInstanceState: Bundle?) {
        methods = Methods(this) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        Methods(this){}.applyLanguage()
        Methods(this){}.applyTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeClientBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            return@setOnApplyWindowInsetsListener insets
        }

        // Show a centered fullscreen splash (tvSplash) while we prepare the screen
        val fallback = FirebaseSingleton.auth.currentUser?.displayName
            ?: FirebaseSingleton.auth.currentUser?.email ?: getString(R.string.hello)

        // Set the splash text (e.g., "Hola <fallback>") and ensure the overlay is visible
        binding.tvSplash.text = getString(R.string.hello_name, fallback)
        binding.splashOverlay.visibility = View.VISIBLE
        binding.splashOverlay.alpha = 1f

        // Hide header elements initially (they will be shown after splash)
        binding.ivBacktoLogin.visibility = View.GONE
        //binding.tvTitle.visibility = View.GONE
        binding.rvWorkouts.visibility = View.GONE
        //binding.tvNoWorkouts.visibility = View.GONE

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
            binding.splashOverlay.animate().alpha(0f).setDuration(350).withEndAction {
                binding.splashOverlay.visibility = View.GONE
                // Show header elements after splash is hidden
                binding.ivBacktoLogin.visibility = View.VISIBLE
                binding.rvWorkouts.visibility = View.VISIBLE
                binding.rvHistorics.visibility = View.VISIBLE
            }.start()
        }

        // Schedule the fallback hide
        hideRunnable?.let { binding.splashOverlay.postDelayed(it, splashDisplayMs) }

        FirebaseSingleton.auth.currentUser?.uid?.let { uid ->
            FirebaseSingleton.db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("username") ?: doc.getString("name") ?: fallback
                    Log.d("HomeClient", "Loaded user name for uid=$uid -> $name")
                    // Update the splash text to real name if still visible
                    if (binding.splashOverlay.isVisible) {
                        binding.tvSplash.text = getString(R.string.hello_name, name)
                        // cancel the previous hide and schedule a short delay so the user sees the real name
                        hideRunnable?.let {
                            binding.splashOverlay.removeCallbacks(it)
                            binding.splashOverlay.postDelayed(it, hideDelayAfterNameMs)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("HomeClient", "Failed to load user doc uid=$uid: ${e.message}")
                }
        }

        orderWorkouts()
        binding.ivBacktoLogin.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.ivProfile.setOnClickListener {
            methods.showProfileDialog()
        }

        binding.ivSettings.setOnClickListener {
            methods.showSettingsDialog()
        }
    }


    override fun onDestroy() {
        // remove any pending callbacks to avoid running after the activity is destroyed
        hideRunnable?.let { binding.splashOverlay.removeCallbacks(it) }
        super.onDestroy()
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
                    workout.id = document.id
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

    private fun orderWorkouts() {
        val authUser = FirebaseSingleton.auth.currentUser
        if (authUser != null) {
            FirebaseSingleton.db.collection("users").document(authUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        currentUser = document.toObject(User::class.java)
                        val userLevel = currentUser?.level ?: 1

                        // Filtrar niveles hasta el nivel del usuario
                        val availableLevels = mutableListOf("Guztiak")
                        for (i in 1..userLevel) {
                            availableLevels.add(i.toString())
                        }

                        // Configurar el spinner con los niveles disponibles
                        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableLevels)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        binding.spOrder.adapter = adapter

                        binding.spOrder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                binding.rvHistorics.visibility = View.GONE
                                binding.tvNoHistorics.visibility = View.GONE
                                selectedLevelChoice = availableLevels[position]
                                Log.d("Spinner", "Usuario seleccionó: $selectedLevelChoice")

                                if (selectedLevelChoice == "Guztiak") {
                                    loadAllWorkouts { workoutList ->
                                        methods.initAdapterWorkoutsAndHistorics(workoutList, binding)
                                    }
                                } else {
                                    loadAllWorkouts { workoutList ->
                                        val filteredWorkouts = workoutList.filter {
                                            it.level == selectedLevelChoice.toInt()
                                        }.toMutableList()
                                        Log.i("", "--------------${filteredWorkouts.toString()}")
                                        methods.initAdapterWorkoutsAndHistorics(filteredWorkouts, binding)
                                    }
                                }
                            }
                            override fun onNothingSelected(p0: AdapterView<*>?) {}
                        }

                        // Cargar workouts inicialmente con "Guztiak"
                        loadAllWorkouts { workoutList ->
                            methods.initAdapterWorkoutsAndHistorics(workoutList, binding)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("HomeClient", "Error loading user data: ${e.message}")
                    setupSpinnerWithLevels(listOf("Guztiak", "1"))
                }
        } else {
            setupSpinnerWithLevels(listOf("Guztiak", "1"))
        }
    }

    private fun setupSpinnerWithLevels(levelsList: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levelsList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spOrder.adapter = adapter

        binding.spOrder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLevelChoice = levelsList[position]
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

}