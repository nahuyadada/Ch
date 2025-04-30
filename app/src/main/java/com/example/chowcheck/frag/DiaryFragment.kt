package com.example.chowcheck.frag // Or your correct fragment package

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.chowcheck.R // Use correct R import
import com.example.chowcheck.SettingsActivity // Import SettingsActivity
import com.example.chowcheck.LoginActivity // Optional: For redirecting if needed
import java.text.SimpleDateFormat
import java.util.*

class DiaryFragment : Fragment() {

    // Views for the dashboard
    private lateinit var textDate: TextView
    private lateinit var btnSettings: ImageButton
    private lateinit var textCaloriesGoal: TextView
    private lateinit var textCaloriesEaten: TextView
    private lateinit var textCaloriesLeft: TextView
    private lateinit var progressCaloriesEaten: ProgressBar
    private lateinit var progressCaloriesLeft: ProgressBar
    private lateinit var textCurrentWeight: TextView
    private lateinit var textTargetWeight: TextView
    private lateinit var textStartingWeight: TextView

    // SharedPreferences
    private lateinit var userDataPreferences: SharedPreferences
    private var loggedInUsername: String? = null

    // Keys for SharedPreferences (ensure consistency across fragments/activities)
    companion object {
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user"
        // Keys for data primarily managed/set elsewhere (e.g., Profile, CalorieInfo)
        const val BASE_KEY_CALORIE_GOAL = "calorie_goal" // Assuming this is set elsewhere
        const val BASE_KEY_WEIGHT = "weight"
        const val BASE_KEY_WEIGHT_GOAL = "weight_goal"
        const val BASE_KEY_STARTING_WEIGHT = "starting_weight" // Assuming this is set elsewhere
        // Key for data updated by FoodLogFragment
        const val BASE_KEY_DAILY_CALORIES_EATEN = "daily_calories_eaten"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment (the dashboard layout)
        return inflater.inflate(R.layout.fragment_diary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use requireContext() safely after onCreateView
        userDataPreferences = requireContext().getSharedPreferences(USER_DATA_PREFS, Context.MODE_PRIVATE)
        loggedInUsername = userDataPreferences.getString(KEY_LOGGED_IN_USER, null)

        // --- Initialize Views ---
        initializeViews(view)

        // --- Setup Listeners ---
        btnSettings.setOnClickListener {
            val intent = Intent(requireActivity(), SettingsActivity::class.java)
            startActivity(intent)
        }

        // --- Initial Data Load ---
        setCurrentDate()
        // Load data only if user is logged in
        if (loggedInUsername != null) {
            loadAndDisplayData() // Consolidated loading function
        } else {
            // Handle case where user isn't logged in
            showLoggedOutState()
            Toast.makeText(requireContext(), "Please log in to view diary.", Toast.LENGTH_SHORT).show()
            // Optionally redirect to LoginActivity from LandingActivity earlier
        }
    }

    // Refresh data when the fragment becomes visible again
    override fun onResume() {
        super.onResume()
        // Re-fetch and update dashboard data in case it changed while the fragment was paused
        if (isAdded && loggedInUsername != null) { // Check fragment is added and user is logged in
            loadAndDisplayData()
        }
    }

    private fun initializeViews(view: View) {
        textDate = view.findViewById(R.id.text_date)
        btnSettings = view.findViewById(R.id.btnSettings)
        textCaloriesGoal = view.findViewById(R.id.text_calories_goal)
        textCaloriesEaten = view.findViewById(R.id.text_calories_eaten)
        textCaloriesLeft = view.findViewById(R.id.text_calories_left)
        progressCaloriesEaten = view.findViewById(R.id.progress_calories_eaten)
        progressCaloriesLeft = view.findViewById(R.id.progress_calories_left)
        textCurrentWeight = view.findViewById(R.id.text_current_weight)
        textTargetWeight = view.findViewById(R.id.text_target_weight)
        textStartingWeight = view.findViewById(R.id.text_starting_weight)
    }

    // Consolidate data loading
    private fun loadAndDisplayData() {
        if (loggedInUsername == null || !isAdded) return
        updateCalorieData()
        updateWeightData()
    }

    // Helper to generate user-specific keys (not date-specific)
    private fun getUserDataKey(baseKey: String): String? {
        return loggedInUsername?.let { "${it}_${baseKey}" }
    }

    // Helper for daily keys (date-specific)
    private fun getDailyUserDataKey(baseKey: String): String? {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        // Example: john_daily_calories_eaten_2025-04-28
        return loggedInUsername?.let { "${it}_${baseKey}_$todayDate" }
    }

    private fun setCurrentDate() {
        val dateFormat = SimpleDateFormat("d MMMM, EEEE", Locale.getDefault())
        textDate.text = dateFormat.format(Date())
    }

    private fun updateCalorieData() {
        // Fetch calorie goal (assuming it's set elsewhere, like profile/initial setup)
        val calorieGoalKey = getUserDataKey(BASE_KEY_CALORIE_GOAL)
        // Provide a default goal if not set
        val caloriesGoal = if (calorieGoalKey != null) userDataPreferences.getInt(calorieGoalKey, 2000) else 2000

        // Fetch total calories eaten today (this key is updated by FoodLogFragment)
        val caloriesEatenKey = getDailyUserDataKey(BASE_KEY_DAILY_CALORIES_EATEN) ?: return // Exit if key can't be generated
        val caloriesEaten = userDataPreferences.getInt(caloriesEatenKey, 0)

        val caloriesLeft = (caloriesGoal - caloriesEaten).coerceAtLeast(0)

        val eatenProgress = if (caloriesGoal > 0) (caloriesEaten.toFloat() / caloriesGoal * 100).toInt().coerceIn(0, 100) else 0
        val leftProgress = if (caloriesGoal > 0) (caloriesLeft.toFloat() / caloriesGoal * 100).toInt().coerceIn(0, 100) else (if (caloriesGoal == 0) 0 else 100) // Handle goal = 0

        textCaloriesGoal.text = "$caloriesGoal kcal"
        textCaloriesEaten.text = "$caloriesEaten Eaten"
        textCaloriesLeft.text = "$caloriesLeft Left"
        progressCaloriesEaten.progress = eatenProgress
        // Ensure progress bar reflects remaining percentage correctly
        progressCaloriesLeft.max = 100 // Make sure max is 100
        progressCaloriesLeft.progress = leftProgress
    }


    private fun updateWeightData() {
        // Fetch weight data (assuming set elsewhere, e.g., profile)
        val currentWeightKey = getUserDataKey(BASE_KEY_WEIGHT)
        val targetWeightKey = getUserDataKey(BASE_KEY_WEIGHT_GOAL)
        val startingWeightKey = getUserDataKey(BASE_KEY_STARTING_WEIGHT)

        val currentWeightStr = if (currentWeightKey != null) userDataPreferences.getString(currentWeightKey, "N/A") else "N/A"
        val targetWeightStr = if (targetWeightKey != null) userDataPreferences.getString(targetWeightKey, "N/A") else "N/A"
        val startingWeightStr = if (startingWeightKey != null) userDataPreferences.getString(startingWeightKey, "N/A") else "N/A"

        textCurrentWeight.text = if (currentWeightStr != "N/A") "$currentWeightStr kg" else currentWeightStr
        textTargetWeight.text = if (targetWeightStr != "N/A") "$targetWeightStr kg" else targetWeightStr
        textStartingWeight.text = if (startingWeightStr != "N/A") "$startingWeightStr kg" else startingWeightStr
    }

    private fun showLoggedOutState() {
        // Display default or placeholder data when logged out
        textCaloriesGoal.text = "N/A kcal"
        textCaloriesEaten.text = "N/A Eaten"
        textCaloriesLeft.text = "N/A Left"
        progressCaloriesEaten.progress = 0
        progressCaloriesLeft.progress = 0
        textCurrentWeight.text = "N/A kg"
        textTargetWeight.text = "N/A kg"
        textStartingWeight.text = "N/A kg"
        // Optionally disable settings button or show a login prompt
        btnSettings.isEnabled = false
    }
}