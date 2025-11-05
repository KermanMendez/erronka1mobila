package com.example.erronka1

import android.app.Dialog
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.erronka1.databinding.ActivityHomeTrainerBinding
import com.example.erronka1.databinding.ActivityNewWorkoutBinding
import com.example.erronka1.db.FirebaseSingleton
import com.example.erronka1.model.Workout
import com.example.erronka1.rvWorkout.WorkoutAdapter

class HomeTrainer : AppCompatActivity() {

    private lateinit var binding : ActivityHomeTrainerBinding
    private var hideRunnable: Runnable? = null

    private lateinit var workoutAdapter: WorkoutAdapter
    private var selectedWorkout: Workout? = null
    private var levels = listOf("Guztiak", "1", "2", "3", "4", "5")
    private var selectedLevelChoice: String = levels[0]
    private var prevSelectedPosition = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        Methods(this){}.applyLanguage()
        Methods(this){}.applyTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeTrainerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            return@setOnApplyWindowInsetsListener insets
        }
        orderWorkouts()
        binding.ivBacktoLogin.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.ivSettings.setOnClickListener {
            Methods(this) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }.showSettingsDialog()
        }

        binding.ivProfile.setOnClickListener {
            Methods(this){}.showProfileDialog()
        }


        // Splash
        val fallback = FirebaseSingleton.auth.currentUser?.displayName
            ?: FirebaseSingleton.auth.currentUser?.email ?: getString(R.string.hello)

        // Set the splash text (e.g., "Hola <fallback>") and ensure the overlay is visible
        binding.tvSplash.text = getString(R.string.hello_name, fallback)
        binding.splashOverlay.visibility = View.VISIBLE
        binding.splashOverlay.alpha = 1f

        // Animazioa
        binding.tvSplash.alpha = 0f
        binding.tvSplash.scaleX = 0.95f
        binding.tvSplash.scaleY = 0.95f
        binding.tvSplash.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(400).start()

        // Display denbora eta izena kargatzeko denbora-muga
        val splashDisplayMs = 1800L
        val hideDelayAfterNameMs = 800L

        // Runnable splash ezkutatzeko
        hideRunnable = Runnable {
            Log.d("HomeTrainer", "hideRunnable exekutatzen: animazioa hasieratzen")
            binding.splashOverlay.animate().alpha(0f).setDuration(350).withEndAction {
                binding.splashOverlay.visibility = View.GONE
                Log.d("HomeTrainer", "splashOverlay bukatuta")
            }.start()
        }

        hideRunnable?.let {
            Log.d("HomeTrainer", "Scheduling hideRunnable in $splashDisplayMs ms")
            binding.splashOverlay.postDelayed(it, splashDisplayMs)
        }

        // Erabiltzailearen izena kargatu
        FirebaseSingleton.auth.currentUser?.uid?.let { uid ->
            FirebaseSingleton.db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("username") ?: doc.getString("name") ?: fallback
                    Log.d("HomeTrainer", "Erabiltzailea kargatuta uid-rekin=$uid -> $name")
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
                    Log.e("HomeTrainer", "Errorea Erabiltzailea kargatzen uid-rekin=$uid: ${e.message}")
                }
        }
        binding.spOrder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLevelChoice = levels[position]
                Log.d("Spinner", "Erabiltzailea aukeratuta: $selectedLevelChoice")
                if (binding.spOrder.selectedItem == "Guztiak") {
                    loadAllWorkouts { workoutList ->
                        initAdapter(workoutList)
                    }
                } else {
                    loadAllWorkouts { workoutList ->
                        val filteredWorkouts = workoutList.filter { it.level == selectedLevelChoice.toInt() }.toMutableList()
                        initAdapter(filteredWorkouts)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

    }
    private fun initAdapter(workoutList: MutableList<Workout>) {
            workoutAdapter = WorkoutAdapter(workoutList) { selectedPosition ->
                if (selectedWorkout != null) {
                    if (selectedWorkout!!.isSelected && prevSelectedPosition != -1) {
                        selectedWorkout!!.isSelected = false
                        workoutAdapter.notifyItemChanged(prevSelectedPosition)
                    }
                }
                selectedWorkout = workoutList[selectedPosition]
                selectedWorkout!!.isSelected = true
                workoutAdapter.notifyItemChanged(selectedPosition)
                prevSelectedPosition = selectedPosition
            }
            binding.rvWorkouts.layoutManager = LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL,false)
            binding.rvWorkouts.adapter = workoutAdapter

            binding.btnCreateWorkout.setOnClickListener {
                showCreateWorkoutDialog(workoutList)
            }
            binding.btnEditWorkout.setOnClickListener {
                if (selectedWorkout != null) {
                    Log.i("","Workout editatzen: ${selectedWorkout!!.id} - ${selectedWorkout!!.name}")
                    showEditWorkoutDialog(workoutList,prevSelectedPosition)
                    workoutAdapter.notifyItemChanged(prevSelectedPosition)
                } else {
                    Toast.makeText(this, "Aukeratu workout bat, mesedez", Toast.LENGTH_SHORT).show()
                }
            }
            binding.btnDeleteWorkout.setOnClickListener {
                if (selectedWorkout != null) {
                    Log.i("","Workout ezabatzen: ${selectedWorkout!!.id} - ${selectedWorkout!!.name}")
                    deleteWorkout(selectedWorkout!!.id)
                    workoutList.remove(selectedWorkout)
                    workoutAdapter.notifyItemChanged(prevSelectedPosition)
                    selectedWorkout = null
                    prevSelectedPosition = -1
                } else {
                    Toast.makeText(this, "Aukeratu workout bat, mesedez", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onDestroy() {
        // Ezkutatu runnable pendienteak kentzea
        hideRunnable?.let {
            Log.d("HomeTrainer", "Removing pending hideRunnable callbacks in onDestroy")
            binding.splashOverlay.removeCallbacks(it)
        }
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
                    workoutsMap[document.id] = workout
                    workout.id = document.id
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
    private fun addWorkoutWithExcercises(workout: Workout) {
        val db = FirebaseSingleton.db
        val docRef = db.collection("workouts").document() // ID generado
        val batch = db.batch()

        // Id asignatzen dugu baina ez da Firestore-ra igoko (getExclude)
        workout.id = docRef.id
        Log.i("","Workout igotzen: ${workout.id}, ${docRef.id}")

        // `ariketak` ez da igoko, soilik `workout` dokumentua
        batch.set(docRef, workout)

        val exercises = workout.ariketak
        exercises.forEach { ariketa ->
            val exRef = docRef.collection("exercises").document() // doc con ID auto
            batch.set(exRef, ariketa)
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d("HomeClient", "Workout e ${exercises.size} exercises gehituta. ID: ${docRef.id}")
            }
            .addOnFailureListener { e ->
                Log.w("HomeClient", "Errorea workout gehitzen", e)
            }
    }

    private fun editWorkout(workout: Workout) {
        val db = FirebaseSingleton.db
        Log.i("","Workout editatzen Dialog-ean: ${workout.id} -------------- ${workout.name}")
        if (workout.id.isBlank()) {
            Log.w("HomeClient", "editWorkout: workout.id hutsa dago, ezin da eguneratu")
            return
        }

        val workoutRef = db.collection("workouts").document(workout.id)

        workoutRef.update(
            "name", workout.name,
            "description", workout.description,
            "level", workout.level,
            "video", workout.video
        )
            .addOnSuccessListener {
                Log.d("HomeClient", "Workout ondo eguneratuta id=${workout.id}")
            }
            .addOnFailureListener { e ->
                Log.w("HomeClient", "Error workout eguneratzen id=${workout.id}", e)
            }
    }

    private fun deleteWorkout(workoutId: String) {
        val db = FirebaseSingleton.db

        if (workoutId.isBlank()) {
            Log.w("HomeClient", "deleteWorkout: workoutId hutsa dago eta ezin da ezabatu")
            return
        }

        val workoutRef = db.collection("workouts").document(workoutId)

        workoutRef.delete()
            .addOnSuccessListener {
                Log.d("HomeClient", "Workout ondo ezabatuta id=$workoutId")
            }
            .addOnFailureListener { e ->
                Log.w("HomeClient", "Errorea workout ezabatzen id=$workoutId", e)
            }
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
            val workout = Workout(
                "",
                newWorkoutBinding.etTitle.text.toString(),
                newWorkoutBinding.etDescription.text.toString(),
                newWorkoutBinding.npLevel.value,
                newWorkoutBinding.etVideo.text.toString(),
                false,
                listOf()
            )
            addWorkoutWithExcercises(workout)
            workoutList.add(workout)
            Log.i("",workoutList.toString())
            workoutAdapter.notifyItemChanged(workoutList.size-1)
            dialog.cancel()
        }
    }
    private fun showEditWorkoutDialog(workoutList: MutableList<Workout>, position: Int) {

        val newWorkoutBinding = ActivityNewWorkoutBinding.inflate(layoutInflater)

        newWorkoutBinding.npLevel.minValue = 1
        newWorkoutBinding.npLevel.maxValue = 5
        val workouteditatu = "Workout editatu"
        newWorkoutBinding.tvTitle.text = workouteditatu
        newWorkoutBinding.etTitle.setText(selectedWorkout!!.name)
        newWorkoutBinding.etDescription.setText(selectedWorkout!!.description)
        newWorkoutBinding.npLevel.value = selectedWorkout!!.level
        newWorkoutBinding.etVideo.setText(selectedWorkout!!.video)


        val dialog = Dialog(this)
        dialog.setContentView(newWorkoutBinding.root)
        dialog.show()

        newWorkoutBinding.btnCancel.setOnClickListener {
            dialog.cancel()
        }
        newWorkoutBinding.btnConfirm.setOnClickListener {
            selectedWorkout!!.name = newWorkoutBinding.etTitle.text.toString()
            selectedWorkout!!.description = newWorkoutBinding.etDescription.text.toString()
            selectedWorkout!!.level = newWorkoutBinding.npLevel.value
            selectedWorkout!!.video = newWorkoutBinding.etVideo.text.toString()
            editWorkout(selectedWorkout!!)
            workoutList[position] = selectedWorkout!!
            workoutAdapter.notifyItemChanged(position)
            dialog.cancel()
        }
    }



    private fun orderWorkouts() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spOrder.adapter = adapter
        binding.spOrder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLevelChoice = levels[position]
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

}