package com.example.chowcheck.frag

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.chowcheck.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter // For date formatting (optional)import java.text.SimpleDateFormatimport java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit

class ResultsTabFragment : Fragment() {

    // UI elements
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

    // Data storage
    private lateinit var userDataPreferences: SharedPreferences
    private var loggedInUsername: String? = null
    private val gson = Gson() // For handling JSON

    // Constants for SharedPreferences keys
    companion object {
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user"
        const val BASE_KEY_CALORIE_GOAL = "calorie_goal"
        const val BASE_KEY_WEIGHT = "weight"
        const val BASE_KEY_WEIGHT_GOAL = "weight_goal"
        const val BASE_KEY_STARTING_WEIGHT = "starting_weight"
        const val BASE_KEY_DAILY_CALORIES_EATEN = "daily_calories_eaten"
        const val BASE_KEY_WEIGHT_HISTORY = "weight_history" // Key for weight history
        const val TAG = "ResultsTabFragment" // For logging

        // Moved from the second companion object
        fun newInstance(): ResultsTabFragment {
            return ResultsTabFragment()
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

        if (loggedInUsername != null) {
            loadAndDisplayData()
        } else {
            Toast.makeText(
                requireContext(),
                "Please log in to view results.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun initializeViews(view: View) {
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

    private fun loadAndDisplayData() {
        loadCalorieData()
        loadWeightData()
        loadWeightHistory()
        calculateAndSetMotivation()
    }

    // --- Helper Functions for SharedPreferences ---

    private fun getUserDataKey(baseKey: String): String? {
        return loggedInUsername?.let { "${it}_${baseKey}" }
    }

    private fun getDailyUserDataKey(baseKey: String): String? {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return loggedInUsername?.let { "${it}_${baseKey}_$todayDate" }
    }

    // --- Calorie Data ---

    private fun loadCalorieData() {
        val calorieGoalKey = getUserDataKey(BASE_KEY_CALORIE_GOAL)
        val caloriesGoal = userDataPreferences.getInt(calorieGoalKey, 2000) // Default

        val caloriesEatenKey = getDailyUserDataKey(BASE_KEY_DAILY_CALORIES_EATEN) ?: return
        val caloriesEaten = userDataPreferences.getInt(caloriesEatenKey, 0)

        val caloriesLeft = (caloriesGoal - caloriesEaten).coerceAtLeast(0)
        val progress = if (caloriesGoal > 0) (caloriesEaten * 100 / caloriesGoal) else 0

        goalCaloriesTextView.text = "$caloriesGoal kcal"
        eatenCaloriesTextView.text = "$caloriesEaten kcal"
        leftCaloriesTextView.text = "$caloriesLeft kcal"
        caloriesProgressBar.progress = progress

        val averageCalories = calculateAverageCalories()
        averageCaloriesTextView.text = "Avg: ${String.format("%.1f", averageCalories)} kcal"
    }

    private fun calculateAverageCalories(days: Int = 7): Double {
        val calendar = Calendar.getInstance()
        var totalCalories = 0
        var validDays = 0

        for (i in 0 until days) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val caloriesEatenKey =
                loggedInUsername?.let { "${it}_${BASE_KEY_DAILY_CALORIES_EATEN}_$date" }
            if (caloriesEatenKey != null && userDataPreferences.contains(caloriesEatenKey)) {
                totalCalories += userDataPreferences.getInt(caloriesEatenKey, 0)
                validDays++
            }
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        return if (validDays > 0) totalCalories.toDouble() / validDays else 0.0
    }

    // --- Weight Data ---

    private fun loadWeightData() {
        val currentWeightKey = getUserDataKey(BASE_KEY_WEIGHT)
        val targetWeightKey = getUserDataKey(BASE_KEY_WEIGHT_GOAL)
        val startingWeightKey = getUserDataKey(BASE_KEY_STARTING_WEIGHT)

        val currentWeight =
            userDataPreferences.getString(currentWeightKey, "N/A")?.toDoubleOrNull() ?: 0.0
        val targetWeight =
            userDataPreferences.getString(targetWeightKey, "N/A")?.toDoubleOrNull() ?: 0.0
        val startingWeight =
            userDataPreferences.getString(startingWeightKey, "N/A")?.toDoubleOrNull() ?: 0.0

        currentWeightTextView.text = "${String.format("%.1f", currentWeight)} kg"
        targetWeightTextView.text = "${String.format("%.1f", targetWeight)} kg"
        startingWeightTextView.text = "Starting Weight: ${String.format("%.1f", startingWeight)} kg"

        val weightChange = currentWeight - startingWeight
        weightChangeTextView.text = "Gained/Lost: ${String.format("%.1f", weightChange)} kg"

        val distanceFromTarget = targetWeight - currentWeight
        distanceFromTargetTextView.text = "${String.format("%.1f", distanceFromTarget)} kg from target"
    }

    // --- Weight History and Chart ---

    private fun loadWeightHistory() {
        val weightHistoryKey = getUserDataKey(BASE_KEY_WEIGHT_HISTORY)
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
            emptyList() // Return an empty list in case of error
        }

        updateWeightChart(weightHistory)
    }

    // Function to update the weight chart (replace with your chart library logic)
    private fun updateWeightChart(weightHistory: List<WeightEntry>) {
        Log.d(TAG, "Weight History for Chart: $weightHistory")

        if (weightHistory.isEmpty()) {
            weightChartView.clear() // Clear previous data
            weightChartView.setNoDataText("No weight history yet.")
            weightChartView.invalidate() // Refresh the chart
            return
        }

        // 1. Convert WeightEntry list to MPAndroidChart Entry list
        // Sort by timestamp to ensure the line connects points chronologically
        val sortedHistory = weightHistory.sortedBy { it.timestamp }
        val entries = ArrayList<Entry>()
        val referenceTimestamp = sortedHistory.firstOrNull()?.timestamp ?: System.currentTimeMillis() // Use first entry or now as reference

        sortedHistory.forEach { entry ->
            // Convert timestamp to float (e.g., days since the first entry)
            // Using days might be better for visualization than raw milliseconds
            val daysSinceReference = TimeUnit.MILLISECONDS.toDays(entry.timestamp - referenceTimestamp).toFloat()
            entries.add(Entry(daysSinceReference, entry.weight.toFloat()))
        }

        // 2. Create a DataSet
        val dataSet = LineDataSet(entries, "Weight (kg)")
        dataSet.color = R.color.nav_item_selected_dark_green // Customize color
        dataSet.valueTextColor = R.color.nav_item_selected_dark_green
        dataSet.setCircleColor(R.color.nav_item_selected_dark_green) // Point color
        dataSet.circleRadius = 4f
        dataSet.lineWidth = 2f
        // Add more customization as needed (e.g., dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER)

        // 3. Create LineData object
        val lineData = LineData(dataSet)

        // 4. Configure Chart Appearance (Optional but Recommended)
        weightChartView.description.isEnabled = false // Hide description label
        weightChartView.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        weightChartView.axisRight.isEnabled = false // Hide right Y-axis
        weightChartView.legend.isEnabled = true // Show legend (e.g., "Weight (kg)")

        // Optional: Format X-axis labels to show dates (more complex)
        // You might need a custom ValueFormatter for this based on 'daysSinceReference' and 'referenceTimestamp'

        // 5. Set data and refresh
        weightChartView.data = lineData
        weightChartView.invalidate() // Refresh the chart
    }

    // --- Motivation ---

    private fun calculateAndSetMotivation() {
        // Placeholder: Implement motivational logic
        motivationTextView.text = "Looking good! Keep up the effort."
    }

    // --- Data Classes ---

    // Data class to represent a single weight entry with timestamp
    data class WeightEntry(
        val timestamp: Long, // Use Long for timestamp (milliseconds since epoch)
        val weight: Double
    )

}