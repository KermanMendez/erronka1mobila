package com.example.erronka1

import android.app.Dialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.erronka1.databinding.ActivityHomeClientBinding
import com.example.erronka1.databinding.ActivityHomeTrainerBinding
import com.example.erronka1.databinding.ActivitySettingsBinding
import com.example.erronka1.databinding.ActivityUserProfileBinding
import com.example.erronka1.db.FirebaseSingleton
import com.example.erronka1.model.Historic
import com.example.erronka1.model.User
import com.example.erronka1.model.Workout
import com.example.erronka1.rvHistoric.HistoricAdapter
import com.example.erronka1.rvWorkout.WorkoutAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class Methods (
    private val context: Context,
    private val onLogout: () -> Unit
) {
    public fun showSettingsDialog() {
        val language = listOf("Euskara", "Español", "English")
        val languageCodes = listOf("eu", "es", "en")

        val binding = ActivitySettingsBinding.inflate(layoutInflater())

        // Crear y asignar el adapter con los nombres de idiomas
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, language)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spLanguages.adapter = adapter

        // Establecer la selección actual DESPUÉS de asignar el adapter
        val currentIndex = getCurrentLanguageIndex()
        binding.spLanguages.setSelection(currentIndex)

        binding.spLanguages.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguageCode = languageCodes[position]

                // Solo cambiar si es diferente al actual
                if (position != currentIndex) {
                    setLanguage(selectedLanguageCode)
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.switchDarkMode.isChecked = isDarkModeEnabled()
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            setDarkMode(isChecked)
        }

        val dialog = Dialog(context)
        dialog.setContentView(binding.root)
        dialog.show()

        binding.btnBackSettings.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnLogout.setOnClickListener {
            FirebaseSingleton.auth.signOut()
            onLogout()
            dialog.dismiss()
        }
    }
    private fun layoutInflater() = android.view.LayoutInflater.from(context)
    private fun setLanguage(languageCode: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        prefs.edit { putString("selected_language", languageCode) }

        (context as? android.app.Activity)?.recreate()
        /*val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        // Reiniciar la actividad para aplicar el cambio
        recreate()*/
    }
    private fun getCurrentLanguageIndex(): Int {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentLanguage = prefs.getString("selected_language", "eu") ?: "eu"

        return when (currentLanguage) {
            "eu" -> 0  // Euskera
            "es" -> 1  // Español
            "en" -> 2  // English
            else -> 0
        }
    }
    private fun isDarkModeEnabled(): Boolean {
        val prefs = context.getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getBoolean("dark_mode", false)
    }

    private fun setDarkMode(enabled: Boolean) {
        val prefs = context.getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit { putBoolean("dark_mode", enabled) }

        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    public fun applyTheme() {
        val enabled = isDarkModeEnabled()
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
    public fun applyLanguage() {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("selected_language", "eu") ?: "eu"

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }


    /* ---------- Profila ---------- */
    private var currentUser: User? = null
    public fun showProfileDialog() {

        val profileBinding = ActivityUserProfileBinding.inflate(layoutInflater())

        loadUserData(profileBinding)
        setupUpdateButton(profileBinding)


        val dialog = Dialog(context)
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
                            profileBinding.editTextName.setText(user.name)
                            profileBinding.editTextSurname.setText(user.surname)
                            profileBinding.editTextSurname2.setText(user.surname2)
                            profileBinding.editTextBirthdate.text = user.birthdate
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
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



    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var historicAdapter: HistoricAdapter
    private lateinit var selectedWorkout: Workout
    private lateinit var selectedHistoric: Historic
    private var historicList = listOf<Historic>()
    private var prevSelectedPosition = -1
    private fun initAdapterWorkoutsAndHistorics(workoutList: MutableList<Workout>) {
        val binding = ActivityHomeClientBinding.inflate(layoutInflater())
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

            (context as? LifecycleOwner)?.lifecycleScope?.launch {
                historicList = loadWorkoutHistorics(selectedWorkout.id)

                historicAdapter = HistoricAdapter(historicList) { selectedPosition ->
                    selectedHistoric = historicList[selectedPosition]
                    Log.d("", "Selected historic: $selectedHistoric")
                }
                binding.rvHistorics.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL,false)
                binding.rvHistorics.adapter = historicAdapter
                binding.rvHistorics.visibility = View.VISIBLE
            }
        }
        binding.rvWorkouts.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL,false)
        binding.rvWorkouts.adapter = workoutAdapter
        Log.d("", "Historics"+historicList.toString())
    }
    private fun initAdapterWorkouts(workoutList: MutableList<Workout>) {
        val binding = ActivityHomeTrainerBinding.inflate(layoutInflater())
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
        binding.rvWorkouts.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL,false)
        binding.rvWorkouts.adapter = workoutAdapter
    }
    private suspend fun loadWorkoutHistorics(workoutId: String): List<Historic> {
        val binding = ActivityHomeClientBinding.inflate(layoutInflater())
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
                    binding.tvNoHistorics.visibility = View.GONE
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
                    withContext(Dispatchers.Main) {
                        // Oculta el RecyclerView y muestra el TextView con el mensaje
                        binding.rvHistorics.visibility = View.GONE
                        binding.tvNoHistorics.visibility = View.VISIBLE
                        binding.tvNoHistorics.text = R.string.noHistoric.toString()
                    }
                }
            } catch (e: Exception) {
                Log.w("HomeClient", "Error loading historic list for workoutId=$workoutId", e)
            }
        } else {
            Log.w("HomeClient", "No authenticated user")
        }

        return result
    }
}