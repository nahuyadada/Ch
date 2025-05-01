package com.example.chowcheck.frag // Ensure this is your correct package

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController // Use this if you use Navigation Component
import com.example.chowcheck.LoginActivity // For potential redirects
import com.example.chowcheck.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DiaryFragment : Fragment() {

    // --- UI Elements (match the XML provided previously) ---
    private lateinit var dateTextView: TextView
    // Calorie Card Views
    private lateinit var caloriesProgressBar: ProgressBar
    private lateinit var eatenValueTextView: TextView
    private lateinit var goalValueTextView: TextView
    private lateinit var leftValueTextView: TextView
    // Action Button
    private lateinit var logFoodButton: Button
    // Weight Card Views
    private lateinit var lastWeightTextView: TextView
    private lateinit var logWeightButton: Button
    private lateinit var weeklyWeightPromptTextView: TextView
    // Notes Card Views
    private lateinit var notesEditText: EditText
    private lateinit var saveNotesButton: Button
    // Add other views like water tracker if you included them in the XML

    // --- Data & SharedPreferences ---
    private lateinit var userDataPreferences: SharedPreferences
    private var loggedInUsername: String? = null
    private val gson = Gson()
    private val todayDateString: String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private val displayDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) // Adjusted format

    // --- Keys and Constants (using conventions from your project) ---
    companion object {
        // Use constants defined elsewhere if possible, otherwise define consistently
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user"
        const val BASE_KEY_CALORIE_GOAL = "calorie_goal"
        const val BASE_KEY_WEIGHT = "weight"
        const val BASE_KEY_WEIGHT_GOAL = "weight_goal" // Used for context, not directly displayed here
        const val BASE_KEY_STARTING_WEIGHT = "starting_weight" // Used for context, not directly displayed here
        const val BASE_KEY_DAILY_CALORIES_EATEN = "daily_calories_eaten"
        const val BASE_KEY_WEIGHT_HISTORY = "weight_history" // Key for the list of weights
        const val BASE_KEY_DAILY_NOTES = "daily_notes" // New key for notes
        const val TAG = "DiaryFragment"

        // Data class for weight entries (matches ResultsTabFragment)
        // Ideally, place this in a shared file if used by multiple fragments
        data class WeightEntry(
            val timestamp: Long,
            val weight: Double
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // *** IMPORTANT: Inflate the NEW XML layout (the one with input features) ***
        val view = inflater.inflate(R.layout.fragment_diary, container, false)
        initializeViews(view) // Initialize views from the NEW layout
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userDataPreferences = requireContext().getSharedPreferences(
            USER_DATA_PREFS,
            Context.MODE_PRIVATE
        )
        loggedInUsername = userDataPreferences.getString(KEY_LOGGED_IN_USER, null)

        if (loggedInUsername == null) {
            handleNotLoggedIn()
            return // Stop further setup
        }

        setupListeners()
        loadAndDisplayData() // Initial load
    }

    // Refresh data when the fragment becomes visible again
    override fun onResume() {
        super.onResume()
        if (isAdded && loggedInUsername != null) {
            // Reload data in case food was added or settings changed elsewhere
            loadAndDisplayData()
        }
    }

    private fun initializeViews(view: View) {
        // --- Find views from the NEW XML layout ---
        dateTextView = view.findViewById(R.id.dateTextView)
        // Calorie Card
        caloriesProgressBar = view.findViewById(R.id.caloriesProgressBar)
        eatenValueTextView = view.findViewById(R.id.eatenValueTextView)
        goalValueTextView = view.findViewById(R.id.goalValueTextView)
        leftValueTextView = view.findViewById(R.id.leftValueTextView)
        // Action Button
        logFoodButton = view.findViewById(R.id.logFoodButton)
        // Weight Card
        lastWeightTextView = view.findViewById(R.id.lastWeightTextView)
        logWeightButton = view.findViewById(R.id.logWeightButton)
        weeklyWeightPromptTextView = view.findViewById(R.id.weeklyWeightPromptTextView)
        // Notes Card
        notesEditText = view.findViewById(R.id.notesEditText)
        saveNotesButton = view.findViewById(R.id.saveNotesButton)
        // Initialize water tracker views if added
    }

    private fun setupListeners() {
        logFoodButton.setOnClickListener { // Assuming logFoodButton is the ID in your interactive Diary layout
            // Navigate to FoodLogFragment using Navigation Component
            try {
                // *** Replace with YOUR action ID from nav_graph.xml ***
                // This action must go FROM DiaryFragment TO FoodLogFragment
                findNavController().navigate(R.id.action_diaryFragment_to_foodLogFragment)
            } catch (e: Exception) {
                android.util.Log.e("DiaryFragment", "Navigation to FoodLog failed: ${e.message}. Check Nav Graph Action ID.")
                Toast.makeText(context, "Error navigating to Food Log.", Toast.LENGTH_SHORT).show()
            }
        }

        logWeightButton.setOnClickListener {
            showLogWeightDialog()
        }

        saveNotesButton.setOnClickListener {
            saveDailyNotes()
        }
        // ... other listeners ...
    }

    private fun loadAndDisplayData() {
        if (loggedInUsername == null || !isAdded) return

        // Display Date
        dateTextView.text = displayDateFormat.format(Date())

        // Load and Display Calories
        loadCalorieData()

        // Load and Display Weight Info & Check Prompt
        loadWeightData()

        // Load and Display Notes
        loadDailyNotes()

        // Load water data if implemented
    }

    // --- Data Loading Functions ---

    private fun loadCalorieData() {
        val calorieGoalKey = getUserDataKey(BASE_KEY_CALORIE_GOAL)
        val caloriesGoal = userDataPreferences.getInt(calorieGoalKey, 2000) // Default

        val caloriesEatenKey = getDailyUserDataKey(BASE_KEY_DAILY_CALORIES_EATEN) ?: return
        val caloriesEaten = userDataPreferences.getInt(caloriesEatenKey, 0)

        val caloriesLeft = (caloriesGoal - caloriesEaten).coerceAtLeast(0)
        // Ensure progress calculation is safe and within bounds
        val progress = if (caloriesGoal > 0) {
            (caloriesEaten.toFloat() / caloriesGoal * 100).toInt().coerceIn(0, 100)
        } else {
            0 // Or 100 if goal is 0 and eaten > 0, depending on desired behavior
        }


        goalValueTextView.text = "$caloriesGoal" // Display only the number
        eatenValueTextView.text = "$caloriesEaten" // Display only the number
        leftValueTextView.text = "$caloriesLeft" // Display only the number
        caloriesProgressBar.progress = progress
    }

    private fun loadWeightData() {
        val weightHistory = loadWeightHistory() // Use the function to load the list
        val lastEntry = weightHistory.maxByOrNull { it.timestamp } // Get the most recent entry

        if (lastEntry != null) {
            val lastWeightFormatted = String.format("%.1f", lastEntry.weight)
            val lastDate = Date(lastEntry.timestamp)
            // Format date like "Apr 28"
            val lastDateFormatted = SimpleDateFormat("MMM d", Locale.getDefault()).format(lastDate)
            lastWeightTextView.text = "Last: $lastWeightFormatted kg on $lastDateFormatted"

            // Check for weekly prompt based on the last entry's timestamp
            val daysSinceLastLog = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastEntry.timestamp)
            weeklyWeightPromptTextView.isVisible = daysSinceLastLog >= 7

        } else {
            // Handle case where there's no weight history yet
            lastWeightTextView.text = "Log your weight to get started!"
            weeklyWeightPromptTextView.isVisible = true // Prompt if no history
        }
    }

    private fun loadDailyNotes() {
        val notesKey = getDailyUserDataKey(BASE_KEY_DAILY_NOTES) ?: return
        val savedNote = userDataPreferences.getString(notesKey, "") // Default to empty string
        notesEditText.setText(savedNote)
    }

    // --- Data Saving Functions ---

    private fun showLogWeightDialog() {
        // Ensure context is available
        if (!isAdded || context == null) return

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Log Today's Weight (kg)")

        // Set up the input field
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.setHint("Enter weight in kg")

        // Add padding to the EditText within the dialog
        val container = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        // Add margins (requires dimens.xml or use hardcoded dp)
        val margin = (16 * resources.displayMetrics.density).toInt() // Example: 16dp margin
        params.leftMargin = margin
        params.rightMargin = margin
        input.layoutParams = params
        container.addView(input)
        builder.setView(container)

        // Set up the dialog buttons
        builder.setPositiveButton("Save") { dialog, _ ->
            val weightString = input.text.toString()
            val weight = weightString.toDoubleOrNull()

            if (weight != null && weight > 0) {
                saveWeightEntry(weight) // Call the function to save the weight
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Please enter a valid positive weight.", Toast.LENGTH_SHORT).show()
                // Don't dismiss, let the user correct the input
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun saveWeightEntry(weight: Double) {
        if (loggedInUsername == null) return // Should not happen if checked earlier, but safe check

        val currentTimestamp = System.currentTimeMillis()
        val newEntry = WeightEntry(timestamp = currentTimestamp, weight = weight)

        // --- 1. Update the simple 'current weight' key ---
        // This key is used by ProfileFragment, CalorieInfoActivity etc.
        val currentWeightKey = getUserDataKey(BASE_KEY_WEIGHT) ?: return
        userDataPreferences.edit().putString(currentWeightKey, weight.toString()).apply() // Apply immediately

        // --- 2. Add entry to the Weight History list ---
        val history = loadWeightHistory().toMutableList() // Load existing history
        history.add(newEntry) // Add the new entry
        // Optional: Sort history if needed, although appending usually maintains order
        // history.sortBy { it.timestamp }

        // --- 3. Save the updated history list back to SharedPreferences ---
        val weightHistoryKey = getUserDataKey(BASE_KEY_WEIGHT_HISTORY) ?: return
        try {
            val json = gson.toJson(history) // Convert updated list to JSON
            userDataPreferences.edit().putString(weightHistoryKey, json).apply() // Save JSON string
        } catch (e: Exception) {
            Log.e(TAG, "Error saving weight history JSON: ${e.message}")
            Toast.makeText(context, "Error saving weight history.", Toast.LENGTH_SHORT).show()
            // Decide if you want to revert the current weight save if history fails
            return // Stop if history saving fails
        }


        // --- 4. Update the UI immediately ---
        val lastWeightFormatted = String.format("%.1f", newEntry.weight)
        val lastDateFormatted = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(newEntry.timestamp))
        lastWeightTextView.text = "Last: $lastWeightFormatted kg on $lastDateFormatted"
        weeklyWeightPromptTextView.isVisible = false // Hide prompt after successful logging
        Toast.makeText(context, "Weight saved!", Toast.LENGTH_SHORT).show()

        // Consider notifying ResultsTabFragment if it needs real-time updates (more complex)
    }

    private fun saveDailyNotes() {
        if (loggedInUsername == null || !isAdded) return

        val notesKey = getDailyUserDataKey(BASE_KEY_DAILY_NOTES) ?: return
        val notesText = notesEditText.text.toString() // Get text, no trim needed usually for notes

        try {
            userDataPreferences.edit().putString(notesKey, notesText).apply()
            Toast.makeText(context, "Notes saved!", Toast.LENGTH_SHORT).show()

            // Optionally clear focus and hide keyboard
            notesEditText.clearFocus()
            val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            inputMethodManager?.hideSoftInputFromWindow(view?.windowToken, 0)

        } catch (e: Exception) {
            Log.e(TAG, "Error saving notes: ${e.message}")
            Toast.makeText(context, "Error saving notes.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Helper Functions ---

    // Gets key specific to user (e.g., "john_weight")
    private fun getUserDataKey(baseKey: String): String? {
        return loggedInUsername?.let { "${it}_${baseKey}" }
    }

    // Gets key specific to user AND today's date (e.g., "john_daily_calories_eaten_2025-05-01")
    private fun getDailyUserDataKey(baseKey: String): String? {
        // Ensures todayDateString is current when key is needed
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return loggedInUsername?.let { "${it}_${baseKey}_$todayDate" }
    }

    // Loads the list of WeightEntry objects from SharedPreferences
    private fun loadWeightHistory(): List<WeightEntry> {
        if (loggedInUsername == null) return emptyList()

        val weightHistoryKey = getUserDataKey(BASE_KEY_WEIGHT_HISTORY) ?: return emptyList()
        val json = userDataPreferences.getString(weightHistoryKey, null)

        return try {
            if (json != null) {
                // Use TypeToken to specify the type (List<WeightEntry>) for Gson
                val typeToken = object : TypeToken<List<WeightEntry>>() {}.type
                gson.fromJson(json, typeToken) ?: emptyList() // Handle null result from fromJson
            } else {
                emptyList() // No history saved yet
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing weight history JSON: ${e.message}")
            Toast.makeText(context, "Error loading weight history.", Toast.LENGTH_SHORT).show()
            emptyList() // Return empty list on error
        }
    }

    private fun handleNotLoggedIn() {
        // Show logged-out state in UI or redirect
        Toast.makeText(requireContext(), "Please log in.", Toast.LENGTH_LONG).show()
        // Example: Redirect to Login
        // val intent = Intent(requireActivity(), LoginActivity::class.java)
        // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        // startActivity(intent)
        // requireActivity().finish()

        // Or just disable UI elements
        logFoodButton.isEnabled = false
        logWeightButton.isEnabled = false
        saveNotesButton.isEnabled = false
        notesEditText.isEnabled = false
        // Display placeholder text
        dateTextView.text = displayDateFormat.format(Date())
        goalValueTextView.text = "N/A"
        eatenValueTextView.text = "N/A"
        leftValueTextView.text = "N/A"
        caloriesProgressBar.progress = 0
        lastWeightTextView.text = "Log in to track weight"
        weeklyWeightPromptTextView.isVisible = false
        notesEditText.setText("")
    }
}