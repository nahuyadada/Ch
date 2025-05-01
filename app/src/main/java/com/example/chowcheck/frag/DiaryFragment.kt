package com.example.chowcheck.frag // Ensure this is your correct package

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.chowcheck.R
import com.example.chowcheck.viewmodel.DateViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class DiaryFragment : Fragment() {

    // --- UI Elements ---
    private lateinit var dateTextView: TextView
    private lateinit var calendarIconImageView: ImageView
    private lateinit var caloriesProgressBar: ProgressBar
    private lateinit var eatenValueTextView: TextView
    private lateinit var goalValueTextView: TextView
    private lateinit var leftValueTextView: TextView
    private lateinit var logFoodButton: Button
    private lateinit var lastWeightTextView: TextView
    private lateinit var logWeightButton: Button
    private lateinit var weeklyWeightPromptTextView: TextView
    private lateinit var notesEditText: EditText
    private lateinit var saveNotesButton: Button

    // --- Data & SharedPreferences ---
    private lateinit var userDataPreferences: SharedPreferences
    private var loggedInUsername: String? = null
    private val gson = Gson()

    // --- Date Management ---
    // Get the Shared ViewModel instance, scoped to the LandingActivity
    private val dateViewModel: DateViewModel by activityViewModels()

    // Format for displaying the date to the user
    // *** CORRECTED PATTERN HERE ***
    private val displayDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    // Format for keys (consistent with ViewModel)
    private val keyDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())


    // --- Keys and Constants ---
    companion object {
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user"
        const val BASE_KEY_CALORIE_GOAL = "calorie_goal"
        const val BASE_KEY_WEIGHT = "weight"
        const val BASE_KEY_WEIGHT_GOAL = "weight_goal"
        const val BASE_KEY_STARTING_WEIGHT = "starting_weight"
        const val BASE_KEY_DAILY_CALORIES_EATEN = "daily_calories_eaten"
        const val BASE_KEY_WEIGHT_HISTORY = "weight_history"
        const val BASE_KEY_DAILY_NOTES = "daily_notes"
        const val TAG = "DiaryFragment"

        data class WeightEntry(
            val timestamp: Long,
            val weight: Double
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout *before* accessing views
        val view = inflater.inflate(R.layout.fragment_diary, container, false)
        initializeViews(view) // Initialize views immediately after inflation
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
            return
        }

        setupListeners()

        // --- Observe the selectedDate LiveData from the ViewModel ---
        dateViewModel.selectedDate.observe(viewLifecycleOwner, Observer { calendar ->
            Log.d(TAG, "Observed date change: ${keyDateFormat.format(calendar.time)}")
            // When the date changes in the ViewModel, reload the data for this fragment
            loadAndDisplayData(calendar) // Pass the new date to the loading function
        })

        // Initial load using the current value from ViewModel (if already set)
        // The observer will also trigger this once it's attached
        // loadAndDisplayData(dateViewModel.getCurrentSelectedDate()) // Load initial data
    }

    // Removed onResume override for data loading, as the Observer handles updates now.

    private fun initializeViews(view: View) {
        // Find views using the passed-in view object
        dateTextView = view.findViewById(R.id.dateTextView)
        calendarIconImageView = view.findViewById(R.id.calendarIconImageView)
        caloriesProgressBar = view.findViewById(R.id.caloriesProgressBar)
        eatenValueTextView = view.findViewById(R.id.eatenValueTextView)
        goalValueTextView = view.findViewById(R.id.goalValueTextView)
        leftValueTextView = view.findViewById(R.id.leftValueTextView)
        logFoodButton = view.findViewById(R.id.logFoodButton)
        lastWeightTextView = view.findViewById(R.id.lastWeightTextView)
        logWeightButton = view.findViewById(R.id.logWeightButton)
        weeklyWeightPromptTextView = view.findViewById(R.id.weeklyWeightPromptTextView)
        notesEditText = view.findViewById(R.id.notesEditText)
        saveNotesButton = view.findViewById(R.id.saveNotesButton)
    }


    private fun setupListeners() {
        calendarIconImageView.setOnClickListener {
            showDatePickerDialog()
        }

        logFoodButton.setOnClickListener {
            // No need to pass date argument anymore, FoodLogFragment will get it from ViewModel
            try {
                findNavController().navigate(R.id.action_diaryFragment_to_foodLogFragment)
            } catch (e: Exception) {
                Log.e(TAG, "Navigation to FoodLog failed: ${e.message}.")
                Toast.makeText(context, "Error navigating to Food Log.", Toast.LENGTH_SHORT).show()
            }
        }

        logWeightButton.setOnClickListener {
            showLogWeightDialog()
        }

        saveNotesButton.setOnClickListener {
            saveDailyNotes()
        }
    }

    // --- Date Picker ---
    private fun showDatePickerDialog() {
        val currentSelectedDate = dateViewModel.getCurrentSelectedDate() // Get current date from VM
        val year = currentSelectedDate.get(Calendar.YEAR)
        val month = currentSelectedDate.get(Calendar.MONTH)
        val day = currentSelectedDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Update the date in the SHARED VIEWMODEL
                dateViewModel.updateSelectedDate(selectedYear, selectedMonth, selectedDay)
                // The observer will automatically call loadAndDisplayData
            }, year, month, day)
        datePickerDialog.show()
    }

    // --- Data Loading (Now accepts the date to load) ---
    private fun loadAndDisplayData(dateToLoad: Calendar) {
        if (loggedInUsername == null || !isAdded) return

        Log.d(TAG, "Loading data for date: ${keyDateFormat.format(dateToLoad.time)}")
        updateDateDisplay(dateToLoad) // Show the selected date
        loadCalorieData(dateToLoad)   // Load calories for the selected date
        loadWeightData()              // Load weight history (shows last logged, independent of selected date)
        loadDailyNotes(dateToLoad)    // Load notes for the selected date
    }

    // Updates the TextView showing the selected date
    private fun updateDateDisplay(dateToDisplay: Calendar) {
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val displayStr = when {
            isSameDay(dateToDisplay, today) -> "Today"
            isSameDay(dateToDisplay, yesterday) -> "Yesterday"
            else -> displayDateFormat.format(dateToDisplay.time)
        }
        dateTextView.text = displayStr
    }

    // Loads calorie data for the given date
    private fun loadCalorieData(dateToLoad: Calendar) {
        val calorieGoalKey = getUserDataKey(BASE_KEY_CALORIE_GOAL) // Goal is not date specific
        val caloriesGoal = userDataPreferences.getInt(calorieGoalKey, 2000)

        // Use the specific date for the eaten key
        val dateKeyString = keyDateFormat.format(dateToLoad.time)
        val caloriesEatenKey = loggedInUsername?.let { "${it}_${BASE_KEY_DAILY_CALORIES_EATEN}_$dateKeyString" } ?: return
        val caloriesEaten = userDataPreferences.getInt(caloriesEatenKey, 0)

        val caloriesLeft = (caloriesGoal - caloriesEaten).coerceAtLeast(0)
        val progress = if (caloriesGoal > 0) (caloriesEaten * 100f / caloriesGoal).toInt().coerceIn(0, 100) else 0

        goalValueTextView.text = "$caloriesGoal"
        eatenValueTextView.text = "$caloriesEaten"
        leftValueTextView.text = "$caloriesLeft"
        caloriesProgressBar.progress = progress
    }

    // Weight history display remains the same (shows last logged overall)
    private fun loadWeightData() {
        val weightHistory = loadWeightHistory()
        val lastEntry = weightHistory.maxByOrNull { it.timestamp }

        if (lastEntry != null) {
            val lastWeightFormatted = String.format("%.1f", lastEntry.weight)
            val lastDate = Date(lastEntry.timestamp)
            val lastDateFormatted = SimpleDateFormat("MMM d", Locale.getDefault()).format(lastDate)
            lastWeightTextView.text = "Last: $lastWeightFormatted kg on $lastDateFormatted"

            val daysSinceLastLog = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastEntry.timestamp)
            weeklyWeightPromptTextView.isVisible = daysSinceLastLog >= 7
        } else {
            lastWeightTextView.text = "Log your weight to get started!"
            weeklyWeightPromptTextView.isVisible = true
        }
    }

    // Loads notes for the given date
    private fun loadDailyNotes(dateToLoad: Calendar) {
        val dateKeyString = keyDateFormat.format(dateToLoad.time)
        val notesKey = loggedInUsername?.let { "${it}_${BASE_KEY_DAILY_NOTES}_$dateKeyString" } ?: return
        val savedNote = userDataPreferences.getString(notesKey, "")
        // Avoid setting text if it's already the same to prevent cursor jumps
        if (notesEditText.text.toString() != savedNote) {
            notesEditText.setText(savedNote)
        }
    }

    // --- Data Saving ---

    private fun showLogWeightDialog() {
        if (!isAdded || context == null) return
        val dateToLog = dateViewModel.getCurrentSelectedDate() // Get date from ViewModel

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Log Weight for ${keyDateFormat.format(dateToLog.time)} (kg)") // Use formatted date

        // Create EditText programmatically
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Enter weight in kg"
        }
        // Create a container for padding
        val container = FrameLayout(requireContext()).apply {
            val paddingDp = 16
            val paddingPx = (paddingDp * resources.displayMetrics.density).toInt()
            setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2) // Add some padding
            addView(input)
        }
        builder.setView(container) // Set the container with the EditText

        builder.setPositiveButton("Save") { dialog, _ ->
            val weightString = input.text.toString()
            val weight = weightString.toDoubleOrNull()
            if (weight != null && weight > 0) {
                saveWeightEntry(weight, dateToLog) // Pass the specific date
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Please enter a valid positive weight.", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    // saveWeightEntry remains mostly the same as the previous version, accepting the date
    private fun saveWeightEntry(weight: Double, dateToLog: Calendar) {
        if (loggedInUsername == null) return

        val entryCalendar = Calendar.getInstance().apply {
            timeInMillis = dateToLog.timeInMillis
            set(Calendar.HOUR_OF_DAY, 12) // Noon
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val entryTimestamp = entryCalendar.timeInMillis

        Log.d(TAG, "Saving weight $weight for timestamp: $entryTimestamp (${Date(entryTimestamp)})")
        val newEntry = WeightEntry(timestamp = entryTimestamp, weight = weight)

        val history = loadWeightHistory().toMutableList()
        val existingEntryIndex = history.indexOfFirst { isSameDay(Calendar.getInstance().apply { timeInMillis = it.timestamp }, entryCalendar) }

        if (existingEntryIndex != -1) {
            history[existingEntryIndex] = newEntry
            Log.d(TAG, "Updated existing weight entry for ${keyDateFormat.format(entryCalendar.time)}")
        } else {
            history.add(newEntry)
            Log.d(TAG, "Added new weight entry for ${keyDateFormat.format(entryCalendar.time)}")
        }
        history.sortBy { it.timestamp } // Sort after modification

        val weightHistoryKey = getUserDataKey(BASE_KEY_WEIGHT_HISTORY) ?: return
        try {
            val json = gson.toJson(history)
            val editor = userDataPreferences.edit()
            editor.putString(weightHistoryKey, json)

            // Update current weight ONLY if this is the latest entry
            val latestTimestamp = history.maxByOrNull { it.timestamp }?.timestamp ?: -1L
            if (entryTimestamp >= latestTimestamp) {
                val currentWeightKey = getUserDataKey(BASE_KEY_WEIGHT)
                if (currentWeightKey != null) {
                    editor.putString(currentWeightKey, weight.toString())
                    Log.d(TAG, "Updated current weight key.")
                }
            }
            editor.apply() // Apply all changes

        } catch (e: Exception) {
            Log.e(TAG, "Error saving weight history JSON: ${e.message}")
            Toast.makeText(context, "Error saving weight history.", Toast.LENGTH_SHORT).show()
            return
        }

        loadWeightData() // Refresh the weight display section
        Toast.makeText(context, "Weight saved for ${keyDateFormat.format(dateToLog.time)}!", Toast.LENGTH_SHORT).show()
    }


    private fun saveDailyNotes() {
        if (loggedInUsername == null || !isAdded) return
        val dateToSave = dateViewModel.getCurrentSelectedDate() // Get date from ViewModel

        val dateKeyString = keyDateFormat.format(dateToSave.time)
        val notesKey = loggedInUsername?.let { "${it}_${BASE_KEY_DAILY_NOTES}_$dateKeyString" } ?: return
        val notesText = notesEditText.text.toString()

        try {
            userDataPreferences.edit().putString(notesKey, notesText).apply()
            Toast.makeText(context, "Notes saved for ${keyDateFormat.format(dateToSave.time)}!", Toast.LENGTH_SHORT).show()

            // Hide keyboard
            notesEditText.clearFocus()
            val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            inputMethodManager?.hideSoftInputFromWindow(view?.windowToken, 0)

        } catch (e: Exception) {
            Log.e(TAG, "Error saving notes: ${e.message}")
            Toast.makeText(context, "Error saving notes.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Helper Functions ---

    // Gets key specific to user (e.g., "john_weight_goal") - Not date specific
    private fun getUserDataKey(baseKey: String): String? {
        return loggedInUsername?.let { "${it}_${baseKey}" }
    }

    // Helper to check if two Calendar instances represent the same day
    private fun isSameDay(cal1: Calendar?, cal2: Calendar?): Boolean {
        if (cal1 == null || cal2 == null) return false
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    // *** CORRECTED loadWeightHistory function ***
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
            // Show toast only if fragment is added to avoid crashes
            if (isAdded) {
                Toast.makeText(context, "Error loading weight history.", Toast.LENGTH_SHORT).show()
            }
            emptyList() // Return empty list on error
        }
    }


    private fun handleNotLoggedIn() {
        // ... (same as before) ...
        if (!isAdded) return
        Toast.makeText(requireContext(), "Please log in.", Toast.LENGTH_LONG).show()
        // Disable UI elements
        logFoodButton.isEnabled = false
        logWeightButton.isEnabled = false
        saveNotesButton.isEnabled = false
        notesEditText.isEnabled = false
        calendarIconImageView.isEnabled = false
        dateTextView.text = displayDateFormat.format(Date()) // Show today's date
        // ... clear other fields ...
    }
}
