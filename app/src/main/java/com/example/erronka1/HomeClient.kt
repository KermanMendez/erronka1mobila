package com.example.erronka1

import android.content.Intent
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
import com.example.erronka1.db.FirebaseSingleton
import com.example.erronka1.databinding.ActivityHomeClientBinding
import com.example.erronka1.model.Workout
import com.example.erronka1.model.User

class HomeClient : AppCompatActivity() {

    private lateinit var binding: ActivityHomeClientBinding
    private var hideRunnable: Runnable? = null
    private var currentUser: User? = null

    private var levels = listOf("Guztiak", "1", "2", "3", "4", "5")
    private var selectedLevelChoice: String = levels[0]

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

        // Erakusten du Splash overlay-a erabiltzailearen izena kargatu arte edo denbora-muga bat igaro arte
        val fallback = FirebaseSingleton.auth.currentUser?.displayName
            ?: FirebaseSingleton.auth.currentUser?.email ?: getString(R.string.hello)

        // Konfiguratu hasierako splash testua
        binding.tvSplash.text = getString(R.string.hello_name, fallback)
        binding.splashOverlay.visibility = View.VISIBLE
        binding.splashOverlay.alpha = 1f
        binding.ivBacktoLogin.visibility = View.GONE
        binding.rvWorkouts.visibility = View.GONE

        // Animazioa testuaren agerpenerako
        binding.tvSplash.alpha = 0f
        binding.tvSplash.scaleX = 0.95f
        binding.tvSplash.scaleY = 0.95f
        binding.tvSplash.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(400).start()

        // Display denbora eta izena kargatzeko denbora-muga
        val splashDisplayMs = 1800L
        val hideDelayAfterNameMs = 800L

        // Runnable splash ezkutatzeko
        hideRunnable = Runnable {
            binding.splashOverlay.animate().alpha(0f).setDuration(350).withEndAction {
                binding.splashOverlay.visibility = View.GONE
                binding.ivBacktoLogin.visibility = View.VISIBLE
                binding.rvWorkouts.visibility = View.VISIBLE
                binding.rvHistorics.visibility = View.VISIBLE
            }.start()
        }

        // Denboratu splash ezkutatzea
        hideRunnable?.let { binding.splashOverlay.postDelayed(it, splashDisplayMs) }

        FirebaseSingleton.auth.currentUser?.uid?.let { uid ->
            FirebaseSingleton.db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("username") ?: doc.getString("name") ?: fallback
                    Log.d("HomeClient", "Loaded user name for uid=$uid -> $name")
                    // Eguneratu splash testua izenarekin
                    if (binding.splashOverlay.isVisible) {
                        binding.tvSplash.text = getString(R.string.hello_name, name)
                        // Kendu aurreko ezkutatze runnable-a eta denboratu berriro izena erakutsi ondoren
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
        // Callback ezabatzea memoria ihesak saihesteko
        hideRunnable?.let { binding.splashOverlay.removeCallbacks(it) }
        super.onDestroy()
    }


    private fun loadAllWorkouts(callback: (MutableList<Workout>) -> Unit) {

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
                    Log.d("HomeTrainer", "Workout kargatuta: ${document.id} ->) $workout")
                }
                Log.d("HomeTrainer", "Workouts tamaina = ${workoutsMap.size} ")
                callback(workoutList)
            }
            .addOnFailureListener { exception ->
                Log.w("HomeTrainer", "Errorea workouts eskuratzen: ", exception)
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

                        // Erabiltzailearen mailara arteko mailak lortu
                        val availableLevels = mutableListOf("Guztiak")
                        for (i in 1..userLevel) {
                            availableLevels.add(i.toString())
                        }

                        // Konfiguratu Spinner maila aukerarekin
                        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableLevels)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        binding.spOrder.adapter = adapter

                        binding.spOrder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                binding.rvHistorics.visibility = View.GONE
                                binding.tvNoHistorics.visibility = View.GONE
                                selectedLevelChoice = availableLevels[position]
                                Log.d("Spinner", "Erabiltzaileak aukeratutakoa: $selectedLevelChoice")

                                if (selectedLevelChoice == "Guztiak") {
                                    loadAllWorkouts { workoutList ->
                                        methods.initAdapterWorkoutsAndHistorics(workoutList, binding)
                                    }
                                } else {
                                    loadAllWorkouts { workoutList ->
                                        val filteredWorkouts = workoutList.filter {
                                            it.level == selectedLevelChoice.toInt()
                                        }.toMutableList()
                                        Log.i("", "--------------${filteredWorkouts}")
                                        methods.initAdapterWorkoutsAndHistorics(filteredWorkouts, binding)
                                    }
                                }
                            }
                            override fun onNothingSelected(p0: AdapterView<*>?) {}
                        }

                        // Lehenengo karga "Guztiak" da defektuz
                        loadAllWorkouts { workoutList ->
                            methods.initAdapterWorkoutsAndHistorics(workoutList, binding)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("HomeClient", "Errorea erabiltzailearen datuak lortzen: ${e.message}")
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