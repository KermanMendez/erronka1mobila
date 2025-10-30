package com.example.erronka1

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.motion.widget.KeyPosition
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.erronka1.databinding.ActivityHomeTrainerBinding
import com.example.erronka1.databinding.ActivityNewWorkoutBinding
import com.example.erronka1.databinding.ActivitySettingsBinding
import com.example.erronka1.databinding.ActivityUserProfileBinding
import com.example.erronka1.db.FirebaseSingleton
import com.example.erronka1.model.Historic
import com.example.erronka1.model.User
import com.example.erronka1.model.Workout
import com.example.erronka1.rvHistoric.HistoricAdapter
import com.example.erronka1.rvWorkout.WorkoutAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeTrainer : AppCompatActivity() {

    private lateinit var binding : ActivityHomeTrainerBinding
    private var hideRunnable: Runnable? = null
    private var currentUser: User? = null
    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var historicAdapter: HistoricAdapter
    private lateinit var selectedWorkout: Workout

    private lateinit var selectedHistoric: Historic
    private var language = listOf("Euskara", "Español", "English")
    private var selectedLanguageChoice: String = language[0]
    private var prevSelectedPosition = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
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

        binding.ivSettings.setOnClickListener {
            showUserSettingsDialog()
        }

        binding.ivProfile.setOnClickListener {
            showProfileDialog()
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

        loadAllWorkouts { workoutList ->
            workoutAdapter = WorkoutAdapter(workoutList) { selectedPosition ->
                if (::selectedWorkout.isInitialized) {
                    if (selectedWorkout.isSelected && prevSelectedPosition != -1) {
                        selectedWorkout.isSelected = false
                        workoutAdapter.notifyItemChanged(prevSelectedPosition)
                    }
                }
                selectedWorkout = workoutList[selectedPosition]
                selectedWorkout.isSelected = true
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
                if (::selectedWorkout.isInitialized) {
                    Log.i("","Editing workout: ${selectedWorkout.id} - ${selectedWorkout.name}")
                    showEditWorkoutDialog(workoutList, prevSelectedPosition)
                } else {
                    Toast.makeText(this, "Selecciona un entrenamiento primero", Toast.LENGTH_SHORT).show()
                }
            }
            binding.btnDeleteWorkout.setOnClickListener {
                if (::selectedWorkout.isInitialized) {
                    Log.i("","Deleting workout: ${selectedWorkout.id} - ${selectedWorkout.name}")
                    deleteWorkout(selectedWorkout.id)
                    workoutList.remove(selectedWorkout)
                    workoutAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Selecciona un entrenamiento primero", Toast.LENGTH_SHORT).show()
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
                    workout.id = document.id
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
        Log.i("","Adding workout with generated ID: ${workout.id}, aaaaaaaaaaa: ${docRef.id}")

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
        Log.i("","Editing workout in dialog: ${workout.id} -------------- ${workout.name}")
        if (workout.id.isBlank()) {
            Log.w("HomeClient", "editWorkout: workout.id está vacío, no se puede actualizar")
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

        settingsBinding.switchDarkMode.isChecked = isDarkModeEnabled()
        settingsBinding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            setDarkMode(isChecked)
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
                false,
                listOf()
            )
            addWorkoutWithExcercises(workout)
            workoutList.add(workout)
            Log.i("",workoutList.toString())
            workoutAdapter.notifyDataSetChanged()
            dialog.cancel()
        }
    }
    private fun showEditWorkoutDialog(workoutList: MutableList<Workout>, selectedPosition: Int) {

        val newWorkoutBinding = ActivityNewWorkoutBinding.inflate(layoutInflater)

        newWorkoutBinding.npLevel.minValue = 1
        newWorkoutBinding.npLevel.maxValue = 5

        newWorkoutBinding.tvTitle.setText("Workout editatu")
        newWorkoutBinding.etTitle.setText(selectedWorkout.name)
        newWorkoutBinding.etDescription.setText(selectedWorkout.description)
        newWorkoutBinding.npLevel.value = selectedWorkout.level
        newWorkoutBinding.etVideo.setText(selectedWorkout.video)


        val dialog = Dialog(this)
        dialog.setContentView(newWorkoutBinding.root)
        dialog.show()

        newWorkoutBinding.btnCancel.setOnClickListener {
            dialog.cancel()
        }
        newWorkoutBinding.btnConfirm.setOnClickListener {
            val workoutNew: Workout = Workout(
                selectedWorkout.id,
                newWorkoutBinding.etTitle.text.toString(),
                newWorkoutBinding.etDescription.text.toString(),
                newWorkoutBinding.npLevel.value,
                newWorkoutBinding.etVideo.text.toString(),
                false,
                listOf()
            )
            editWorkout(workoutNew)
            workoutList[selectedPosition] = workoutNew
            workoutAdapter.notifyDataSetChanged()
            dialog.cancel()
        }
    }

    private fun isDarkModeEnabled(): Boolean {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("dark_mode", false)
    }

    private fun setDarkMode(enabled: Boolean) {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("dark_mode", enabled).apply()

        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun applyTheme() {
        val enabled = isDarkModeEnabled()
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
    private fun showProfileDialog() {

        val profileBinding = ActivityUserProfileBinding.inflate(layoutInflater)

        loadUserData(profileBinding)
        setupUpdateButton(profileBinding)


        val dialog = Dialog(this)
        dialog.setContentView(profileBinding.root)
        dialog.window!!.setLayout(1000, 1500)
        dialog.show()
        profileBinding.btnBackProfile.setOnClickListener {
            dialog.cancel()
        }
    }
    private fun loadUserData(profileBinding: ActivityUserProfileBinding) {
        val authUser = FirebaseSingleton.auth.currentUser

        if (authUser != null) {
            FirebaseSingleton.db.collection("users").document(authUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        currentUser = document.toObject(User::class.java)
                        currentUser?.let { user ->
                            profileBinding.editTextName.setText(user.name ?: "")
                            profileBinding.editTextSurname.setText(user.surname ?: "")
                            profileBinding.editTextSurname2.setText(user.surname2 ?: "")
                            profileBinding.editTextBirthdate.setText(user.birthdate ?: "")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateUserProfile(profileBinding: ActivityUserProfileBinding) {
        Log.d("UserProfile", "Updating user:")
        currentUser?.let { user ->

            user.name = profileBinding.editTextName.text.toString().trim()
            user.surname = profileBinding.editTextSurname.text.toString().trim()
            user.surname2 = profileBinding.editTextSurname2.text.toString().trim()
            user.birthdate = profileBinding.editTextBirthdate.text.toString().trim()



            val authUser = FirebaseSingleton.auth.currentUser
            if (authUser != null) {
                FirebaseSingleton.db.collection("users").document(authUser.uid)
                    .set(user)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
    private fun setupUpdateButton(profileBinding: ActivityUserProfileBinding) {
        profileBinding.btnSaveChanges.setOnClickListener {
            updateUserProfile(profileBinding)
            Log.d("UserProfile", "Update button clicked")
        }
    }

}