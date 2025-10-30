package com.example.erronka1

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.erronka1.db.FirebaseSingleton
import com.example.erronka1.databinding.ActivityHomeClientBinding
import com.example.erronka1.databinding.ActivitySettingsBinding
import com.example.erronka1.databinding.ActivityUserProfileBinding
import com.example.erronka1.model.Workout
import com.example.erronka1.model.Historic
import com.example.erronka1.model.User
import com.example.erronka1.rvHistoric.HistoricAdapter
import com.example.erronka1.rvWorkout.WorkoutAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeClient : AppCompatActivity() {

    private lateinit var binding: ActivityHomeClientBinding
    private var hideRunnable: Runnable? = null
    private var currentUser: User? = null
    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var historicAdapter: HistoricAdapter
    private lateinit var selectedWorkout: Workout
    private lateinit var selectedHistoric: Historic
    private var language = listOf("Euskara", "Español", "English")
    private var selectedLanguageChoice: String = language[0]

    //private var workoutsList = mutableListOf<Workout>()
    private var historicList = listOf<Historic>()
    private var prevSelectedPosition = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
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
                //binding.tvTitle.visibility = View.VISIBLE
                // Setup RecyclerView and load workouts AFTER splash is hidden
                //setupRecyclerView()
                //showWorkouts()
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
                    // let the original timeout hide the splash
                }
        } /*?: run {
            Log.d("HomeClient", "No auth UID available; using fallback for welcome")
            // Keep the splash showing the fallback; the hideRunnable will remove it after timeout
        }*/

        binding.ivBacktoLogin.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.ivProfile.setOnClickListener {
            showProfileDialog()
        }

        binding.ivSettings.setOnClickListener {
            showSettingsDialog()
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

                lifecycleScope.launch {
                    historicList = loadWorkoutHistorics(selectedWorkout.id)

                    historicAdapter = HistoricAdapter(historicList) { selectedPosition ->
                        selectedHistoric = historicList[selectedPosition]
                        Log.d("", "Selected historic: $selectedHistoric")
                    }
                    binding.rvHistorics.layoutManager = LinearLayoutManager(this@HomeClient, LinearLayoutManager.HORIZONTAL,false)
                    binding.rvHistorics.adapter = historicAdapter
                    binding.rvHistorics.visibility = View.VISIBLE
                }


            }

            binding.rvWorkouts.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false)
            binding.rvWorkouts.adapter = workoutAdapter
            Log.d("", "Historics"+historicList.toString())


        }


        /*showUserHistoric { historicList ->
            Log.d("HomeClient", "User historic loaded: ${historicList.size} entries")
            for (entry in historicList) {
                Log.d("HomeClient", "Historic entry: $entry")
            }
        }*/

    }

    override fun onDestroy() {
        // remove any pending callbacks to avoid running after the activity is destroyed
        hideRunnable?.let { binding.splashOverlay.removeCallbacks(it) }
        super.onDestroy()
    }

    /*private fun setupRecyclerView() {
        workoutAdapter = WorkoutAdapter(workoutsList) { position ->
            // Handle workout selection - you can add navigation to workout detail here
            val selectedWorkout = workoutsList[position]
            Log.d("HomeClient", "Workout selected: ${selectedWorkout.title}")
            // TODO: Navigate to workout detail or start workout activity
        }

        binding.rvWorkouts.apply {
            layoutManager = LinearLayoutManager(this@HomeClient)
            adapter = workoutAdapter
        }
    }*/

    /*private fun showWorkouts() {
        // Get user lvl to filter workouts
        val uid = FirebaseSingleton.auth.currentUser?.uid ?: return
        val db = FirebaseSingleton.db

        // Primero obtenemos el nivel del usuario
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val userLevel = userDoc.getLong("level")?.toInt() ?: 1 // nivel por defecto 1

                db.collection("workouts")
                    .get()
                    .addOnSuccessListener { result ->
                        workoutsList.clear()

                        for (document in result) {
                            val workout = document.toObject(Workout::class.java)
                            workout.id = document.id

                            // Filtrar por nivel: mostrar workouts del mismo nivel o menor
                            if (workout.level <= userLevel) {
                                workoutsList.add(workout)
                                Log.d("HomeClient", "Workout loaded: ${document.id} -> $workout")
                            }
                        }

                        // Update UI
                        if (workoutsList.isEmpty()) {
                            binding.rvTableWorkouts.visibility = View.GONE
                            binding.tvNoWorkouts.visibility = View.VISIBLE
                        } else {
                            binding.rvTableWorkouts.visibility = View.VISIBLE
                            binding.tvNoWorkouts.visibility = View.GONE
                            workoutAdapter.notifyItemRangeInserted(0, workoutsList.size)
                        }

                        Log.d("HomeClient", "Filtered workouts size = ${workoutsList.size} for user level $userLevel")
                    }
                    .addOnFailureListener { exception ->
                        Log.w("HomeClient", "Error getting workouts: ", exception)
                        binding.rvWorkouts.visibility = View.GONE
                        binding.tvNoWorkouts.visibility = View.VISIBLE
                    }
            }
            .addOnFailureListener { exception ->
                Log.w("HomeClient", "Error getting user level: ", exception)
                binding.rvWorkouts.visibility = View.GONE
                binding.tvNoWorkouts.visibility = View.VISIBLE
            }
    }*/
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

    /*private fun showUserHistoric(onComplete: (List<Historic>) -> Unit = {}) {
        val uid = FirebaseSingleton.auth.currentUser?.uid ?: return
        val db = FirebaseSingleton.db

        val historyList = mutableListOf<Historic>()

        // Order by the string `date` field (newest first). `date` is stored as String per requirement.
        db.collection("users").document(uid).collection("historic")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Log.d("HomeClient", "No hay histórico para el usuario")
                    onComplete(historyList)
                    return@addOnSuccessListener
                }

                var pending = querySnapshot.size()

                for (doc in querySnapshot) {
                    val id = doc.id
                    val workoutId = doc.getString("workoutId") ?: ""
                    val date = doc.getString("date") ?: ""
                    val totalTime = doc.getLong("totalTime")?.toInt() ?: 0
                    val totalReps = doc.getLong("totalReps")?.toInt() ?: 0
                    val completed = doc.getBoolean("completed") ?: false

                    val history = Historic(
                        id = id,
                        workoutId = workoutId,
                        workoutTitle = "",
                        date = date,
                        totalTime = totalTime,
                        totalReps = totalReps,
                        completed = completed
                    )

                    if (workoutId.isBlank()) {
                        history.workoutTitle = "Workout desconocido"
                        historyList.add(history)
                        pending--
                        if (pending == 0) {
                            Log.d("HomeClient", "Total histórico cargado: ${historyList.size}")
                            onComplete(historyList)
                        }
                        continue
                    }

                    db.collection("workouts").document(workoutId).get()
                        .addOnSuccessListener { workoutDoc ->
                            val title = workoutDoc.getString("name") ?: "Workout desconocido"
                            history.workoutTitle = title
                            historyList.add(history)
                        }
                        .addOnFailureListener { e ->
                            Log.w("HomeClient", "Error obteniendo workout title para histórico", e)
                            history.workoutTitle = "Workout desconocido"
                            historyList.add(history)
                        }
                        .addOnCompleteListener {
                            pending--
                            if (pending == 0) {
                                Log.d("HomeClient", "Total histórico cargado: ${historyList.size}")
                                onComplete(historyList)
                                // TODO: actualizar UI con historyList
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("HomeClient", "Error obteniendo histórico del usuario: ", exception)
                onComplete(historyList)
            }
    }*/

    private suspend fun loadWorkoutHistorics(workoutId: String): List<Historic> {
        var result: List<Historic> = emptyList()
        val uid = FirebaseSingleton.auth.currentUser?.uid
        if (uid != null) {
            val db = FirebaseSingleton.db
            try {
                val query = db.collection("users")
                    .document(uid)
                    .collection("historic")
                    .whereEqualTo("workoutId", workoutId)
                    .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                if (!query.isEmpty) {
                    val list = mutableListOf<Historic>()
                    for (doc in query.documents) {
                        val h = Historic(
                            id = doc.id,
                            workoutId = doc.getString("workoutId") ?: "",
                            workoutTitle = "",
                            date = doc.getString("date") ?: "",
                            totalTime = doc.getLong("totalTime")?.toInt() ?: 0,
                            totalReps = doc.getLong("totalReps")?.toInt() ?: 0,
                            completed = doc.getBoolean("completed") ?: false
                        )
                        list.add(h)
                    }

                    if (workoutId.isNotBlank()) {
                        try {
                            val workoutDoc = db.collection("workouts").document(workoutId).get().await()
                            val title = workoutDoc.getString("name")
                                ?: workoutDoc.getString("title")
                                ?: "Workout desconocido"
                            for (h in list) h.workoutTitle = title
                        } catch (e: Exception) {
                            Log.w("HomeClient", "Failed to load workout title for $workoutId", e)
                            for (h in list) h.workoutTitle = "Workout desconocido"
                        }
                    } else {
                        for (h in list) h.workoutTitle = "Workout desconocido"
                    }

                    result = list
                } else {
                    Log.d("HomeClient", "No historic entries for workoutId=$workoutId")
                }
            } catch (e: Exception) {
                Log.w("HomeClient", "Error loading historic list for workoutId=$workoutId", e)
            }
        } else {
            Log.w("HomeClient", "No authenticated user")
        }

        return result
    }

    private fun showSettingsDialog() {

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
        settingsBinding.btnBackSettings.setOnClickListener {
            dialog.cancel()
        }
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


}