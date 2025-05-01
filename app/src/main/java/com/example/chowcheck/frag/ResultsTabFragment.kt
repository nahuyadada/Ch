package com.example.chowcheck.frag

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // Import activityViewModels
import androidx.lifecycle.Observer // Import Observer
import com.example.chowcheck.R
import com.example.chowcheck.viewmodel.DateViewModel // Import DateViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min // Import min if using setLabelCount

class ResultsTabFragment : Fragment() {

    // --- UI elements --- (Keep as before)
    private lateinit var goalCaloriesTextView: TextView
    private lateinit var eatenCaloriesTextView: TextView
    private lateinit var leftCaloriesTextView: TextView
    private lateinit var caloriesProgressBar: ProgressBar
    private lateinit var averageCaloriesTextView: TextView
    private lateinit var weightChartView: LineChart
    private lateinit var currentWeightTextView: TextView
    private lateinit var targetWeightTextView: TextView
    private lateinit var startingWeightTextView: TextView
    private lateinit var weightChangeTextView: TextView
    private lateinit var distanceFromTargetTextView: TextView
    private lateinit var motivationTextView: TextView

    // --- Data storage --- (Keep as before)
    private lateinit var userDataPreferences: SharedPreferences
    private var loggedInUsername: String? = null
    private val gson = Gson()

    // --- Date Management --- NEW ---
    // Get the Shared ViewModel instance
    private val dateViewModel: DateViewModel by activityViewModels()
    // Format for generating SharedPreferences keys
    private val keyDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    // ---------------------------

    // --- Constants --- (Keep as before)
    companion object {
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user"
        const val BASE_KEY_CALORIE_GOAL = "calorie_goal"
        const val BASE_KEY_WEIGHT = "weight"
        const val BASE_KEY_WEIGHT_GOAL = "weight_goal"
        const val BASE_KEY_STARTING_WEIGHT = "starting_weight"
        const val BASE_KEY_DAILY_CALORIES_EATEN = "daily_calories_eaten"
        const val BASE_KEY_WEIGHT_HISTORY = "weight_history"
        const val TAG = "ResultsTabFragment"

        fun newInstance(): ResultsTabFragment {
            return ResultsTabFragment()
        }
    }

    // --- Data class --- (Keep as before)
    data class WeightEntry(
        val timestamp: Long,
        val weight: Double
    )

    // --- Chart Formatter --- (Keep as before)
    class DateAxisFormatter(private val history: List<WeightEntry>) : ValueFormatter() {
        private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
            val index = value.toInt()
            return if (index >= 0 && index < history.size) {
                dateFormat.format(Date(history[index].timestamp))
            } else { "" }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_results_tab, container, false)
        initializeViews(view)
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
            Toast.makeText(requireContext(), "Please log in to view results.", Toast.LENGTH_SHORT).show()
            clearUI()
            return // Don't proceed if not logged in
        }

        // --- Observe the selectedDate LiveData from the ViewModel --- NEW ---
        dateViewModel.selectedDate.observe(viewLifecycleOwner, Observer { calendar ->
            Log.d(TAG, "Observed date change in Results: ${keyDateFormat.format(calendar.time)}")
            // When the date changes in the ViewModel, reload the relevant data
            loadAndDisplayData(calendar) // Pass the selected date
        })
        // --------------------------------------------------------------------

        // Initial load can happen here or rely on the observer triggering
        // loadAndDisplayData(dateViewModel.getCurrentSelectedDate())
    }

    // --- Removed onResume data loading, Observer handles updates for selected date ---
    // override fun onResume() { ... }

    private fun initializeViews(view: View) {
        // Keep as before
        goalCaloriesTextView = view.findViewById(R.id.goalCaloriesTextView)
        eatenCaloriesTextView = view.findViewById(R.id.eatenCaloriesTextView)
        leftCaloriesTextView = view.findViewById(R.id.leftCaloriesTextView)
        caloriesProgressBar = view.findViewById(R.id.caloriesProgressBar)
        averageCaloriesTextView = view.findViewById(R.id.averageCaloriesTextView)
        weightChartView = view.findViewById(R.id.weightChartView)
        currentWeightTextView = view.findViewById(R.id.currentWeightTextView)
        targetWeightTextView = view.findViewById(R.id.targetWeightTextView)
        startingWeightTextView = view.findViewById(R.id.startingWeightTextView)
        weightChangeTextView = view.findViewById(R.id.weightChangeTextView)
        distanceFromTargetTextView = view.findViewById(R.id.distanceFromTargetTextView)
        motivationTextView = view.findViewById(R.id.motivationTextView)
    }

    // --- Modified to accept the date ---
    private fun loadAndDisplayData(dateToLoad: Calendar) {
        if (loggedInUsername == null || !isAdded) return
        Log.d(TAG, "Results loading data for date: ${keyDateFormat.format(dateToLoad.time)}")

        loadCalorieData(dateToLoad) // Pass the selected date for calorie summary
        loadWeightData() // Overall weight stats are likely not date-specific
        loadWeightHistoryAndSetupChart() // Weight history chart shows all data
        calculateAverageCalories() // Average usually calculated based on current date range
        calculateAndSetMotivation()
    }
    // ------------------------------------

    private fun clearUI() {
        // Keep as before
        if (!isAdded) return // Check if fragment is attached
        goalCaloriesTextView.text = "N/A"
        eatenCaloriesTextView.text = "N/A"
        leftCaloriesTextView.text = "N/A"
        caloriesProgressBar.progress = 0
        averageCaloriesTextView.text = "Avg: N/A kcal"
        currentWeightTextView.text = "N/A kg"
        targetWeightTextView.text = "N/A kg"
        startingWeightTextView.text = "Starting Weight: N/A kg"
        weightChangeTextView.text = "Gained/Lost: N/A kg"
        distanceFromTargetTextView.text = "N/A kg from target"
        if (::weightChartView.isInitialized) { // Check if chartView is initialized
            weightChartView.clear()
            weightChartView.setNoDataText("Log in to see weight chart.")
            weightChartView.invalidate()
        }
        motivationTextView.text = ""
    }

    // --- Helper Functions for SharedPreferences ---

    // Gets user-specific key (NOT date specific)
    private fun getUserDataKey(baseKey: String): String? {
        return loggedInUsername?.let { "${it}_${baseKey}" }
    }

    // --- NEW --- Gets key for data specific to the PROVIDED date
    private fun getDailyDataKeyForDate(baseKey: String, date: Calendar): String? {
        val dateKeyString = keyDateFormat.format(date.time) // Use passed-in date
        return loggedInUsername?.let { "${it}_${baseKey}_$dateKeyString" }
    }
    // -----------

    // --- Calorie Data (Modified to accept date) ---

    private fun loadCalorieData(dateToLoad: Calendar) {
        // Goal is still user-specific
        val calorieGoalKey = getUserDataKey(BASE_KEY_CALORIE_GOAL)
        val caloriesGoal = userDataPreferences.getInt(calorieGoalKey, 2000)

        // --- CHANGE: Use the passed-in date to get the key ---
        val caloriesEatenKey = getDailyDataKeyForDate(BASE_KEY_DAILY_CALORIES_EATEN, dateToLoad) ?: return
        // -----------------------------------------------------
        val caloriesEaten = userDataPreferences.getInt(caloriesEatenKey, 0) // Defaults to 0

        val caloriesLeft = (caloriesGoal - caloriesEaten).coerceAtLeast(0)
        val progress = if (caloriesGoal > 0) (caloriesEaten * 100f / caloriesGoal).toInt().coerceIn(0, 100) else 0

        goalCaloriesTextView.text = "$caloriesGoal kcal"
        eatenCaloriesTextView.text = "$caloriesEaten kcal" // Shows calories for SELECTED date
        leftCaloriesTextView.text = "$caloriesLeft kcal"   // Shows calories left for SELECTED date
        caloriesProgressBar.progress = progress
    }
    // ------------------------------------------------

    // Calculates average daily calories over the last 'days' relative to TODAY
    // (Keeping this logic based on current date, as it's usually what 'average' means here)
    private fun calculateAverageCalories(days: Int = 7): Double {
        if (loggedInUsername == null) return 0.0

        val calendar = Calendar.getInstance() // Start from today
        var totalCalories = 0
        var validDays = 0
        // Use the keyDateFormat defined at the class level
        // val keyDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in 0 until days) {
            val dateStr = keyDateFormat.format(calendar.time)
            // Use a local helper or directly construct the key
            val caloriesEatenKey = "${loggedInUsername}_${BASE_KEY_DAILY_CALORIES_EATEN}_$dateStr"

            // No need for getDailyDataKeyForDate here as we iterate back from today
            if (userDataPreferences.contains(caloriesEatenKey)) {
                totalCalories += userDataPreferences.getInt(caloriesEatenKey, 0)
                validDays++
            }
            calendar.add(Calendar.DAY_OF_MONTH, -1) // Go back one day
        }

        val average = if (validDays > 0) totalCalories.toDouble() / validDays else 0.0
        // Update the average TextView here as this function calculates it
        if(isAdded) { // Check if fragment is attached before updating UI
            averageCaloriesTextView.text = "Avg (last $days days): ${String.format("%.1f", average)} kcal"
        }
        return average // Return value if needed elsewhere
    }


    // --- Weight Data (Overall Stats) --- (Keep as before) ---
    private fun loadWeightData() {
        if (loggedInUsername == null || !isAdded) return
        // ... (rest of the function remains the same, loading overall weights) ...
        val currentWeightKey = getUserDataKey(BASE_KEY_WEIGHT)
        val targetWeightKey = getUserDataKey(BASE_KEY_WEIGHT_GOAL)
        val startingWeightKey = getUserDataKey(BASE_KEY_STARTING_WEIGHT)

        val currentWeightStr = userDataPreferences.getString(currentWeightKey, null)
        val currentWeight = currentWeightStr?.toDoubleOrNull() ?: 0.0

        val targetWeightStr = userDataPreferences.getString(targetWeightKey, null)
        val targetWeight = targetWeightStr?.toDoubleOrNull() ?: 0.0

        val startingWeightStr = userDataPreferences.getString(startingWeightKey, null)
        val startingWeight = startingWeightStr?.toDoubleOrNull() ?: 0.0

        currentWeightTextView.text = if (currentWeight > 0) "${String.format("%.1f", currentWeight)} kg" else "N/A kg"
        targetWeightTextView.text = if (targetWeight > 0) "${String.format("%.1f", targetWeight)} kg" else "N/A kg"
        startingWeightTextView.text = if (startingWeight > 0) "Starting Weight: ${String.format("%.1f", startingWeight)} kg" else "Starting Weight: N/A kg"

        if (currentWeight > 0 && startingWeight > 0) {
            val weightChange = currentWeight - startingWeight
            weightChangeTextView.text = "Gained/Lost: ${String.format("%+.1f", weightChange)} kg" // Always show sign
        } else {
            weightChangeTextView.text = "Gained/Lost: N/A kg"
        }

        if (currentWeight > 0 && targetWeight > 0) {
            val distanceFromTarget = targetWeight - currentWeight
            distanceFromTargetTextView.text = "${String.format("%.1f", distanceFromTarget)} kg from target"
        } else {
            distanceFromTargetTextView.text = "N/A kg from target"
        }
    }

    // --- Weight History and Chart --- (Keep as before) ---
    private fun loadWeightHistoryAndSetupChart() {
        if (loggedInUsername == null || !isAdded) {
            if(::weightChartView.isInitialized){ // Check initialization before accessing
                weightChartView.clear()
                weightChartView.setNoDataText("Log in to see weight chart.")
                weightChartView.invalidate()
            }
            return
        }
        // ... (rest of the function remains the same, loading full history) ...
        val weightHistoryKey = getUserDataKey(BASE_KEY_WEIGHT_HISTORY) ?: return
        val json = userDataPreferences.getString(weightHistoryKey, null)

        val weightHistory: List<WeightEntry> = try {
            if (json != null) {
                val typeToken = object : TypeToken<List<WeightEntry>>() {}.type
                gson.fromJson(json, typeToken) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing weight history: ${e.message}")
            emptyList()
        }
        updateWeightChart(weightHistory)
    }

    private fun updateWeightChart(weightHistory: List<WeightEntry>) {
        // ... (chart update logic remains the same) ...
        if (!isAdded) return // Ensure fragment is attached

        if (weightHistory.isEmpty()) {
            weightChartView.clear()
            weightChartView.setNoDataText("No weight history yet.")
            weightChartView.invalidate()
            return
        }

        // 1. Sort history by timestamp (important for line connection and formatter)
        val sortedHistory = weightHistory.sortedBy { it.timestamp }

        // 2. Create chart entries using INDEX as X-value
        val entries = ArrayList<Entry>()
        sortedHistory.forEachIndexed { index, weightEntry ->
            entries.add(Entry(index.toFloat(), weightEntry.weight.toFloat()))
        }

        // 3. Create a DataSet
        val dataSet = LineDataSet(entries, "Weight (kg)")
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.nav_item_selected_dark_green) // Make sure this color exists
        dataSet.color = primaryColor
        dataSet.valueTextColor = primaryColor // Or use a different color like Color.BLACK
        dataSet.setCircleColor(primaryColor)
        dataSet.circleRadius = 4f
        dataSet.lineWidth = 2f
        dataSet.setDrawValues(false) // Hide numerical values on points

        // 4. Create LineData object
        val lineData = LineData(dataSet)

        // 5. Configure Chart Appearance
        weightChartView.description.isEnabled = false // Hide description label
        val xAxis = weightChartView.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.DKGRAY
        xAxis.granularity = 1f // Show label for each entry index
        xAxis.isGranularityEnabled = true
        xAxis.valueFormatter = DateAxisFormatter(sortedHistory) // Pass the sorted list
        xAxis.setLabelRotationAngle(-45f) // Rotate labels to prevent overlap
        xAxis.setAvoidFirstLastClipping(true) // Try to show first/last labels

        // Adjust label count dynamically
        // val labelCount = min(sortedHistory.size, 7) // Show max 7 labels
        // if (labelCount > 0) {
        //     xAxis.setLabelCount(labelCount, false) // Don't force, let chart decide best fit
        // }


        weightChartView.axisRight.isEnabled = false // Hide right Y-axis
        weightChartView.axisLeft.textColor = Color.DKGRAY
        weightChartView.legend.isEnabled = true // Keep legend for "Weight (kg)" label
        weightChartView.legend.textColor = Color.DKGRAY

        // Add some padding
        weightChartView.setExtraOffsets(5f, 10f, 5f, 10f)

        // 6. Set data and refresh
        weightChartView.data = lineData
        // Consider adding animation
        // weightChartView.animateX(500)
        weightChartView.invalidate() // Refresh the chart
    }


    // --- Motivation --- (Keep as before) ---
    private fun calculateAndSetMotivation() {
        if (!isAdded) return
        // Placeholder logic
        motivationTextView.text = "Keep up the great work!"
    }
}