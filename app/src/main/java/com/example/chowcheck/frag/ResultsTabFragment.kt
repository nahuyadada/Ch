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
import com.example.chowcheck.R
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
    private val gson = Gson()

    // Constants for SharedPreferences keys
    companion object {
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user"
        const val BASE_KEY_CALORIE_GOAL = "calorie_goal"
        const val BASE_KEY_WEIGHT = "weight" // Most recent weight string
        const val BASE_KEY_WEIGHT_GOAL = "weight_goal"
        const val BASE_KEY_STARTING_WEIGHT = "starting_weight"
        const val BASE_KEY_DAILY_CALORIES_EATEN = "daily_calories_eaten" // Base for daily totals
        const val BASE_KEY_WEIGHT_HISTORY = "weight_history" // Key for weight history list
        const val TAG = "ResultsTabFragment"

        // Moved from the second companion object
        fun newInstance(): ResultsTabFragment {
            return ResultsTabFragment()
        }
    }

    // Data class for weight entries (should match DiaryFragment)
    // Place in a shared file ideally
    data class WeightEntry(
        val timestamp: Long, // Milliseconds since epoch
        val weight: Double
    )

    // Custom ValueFormatter for displaying dates on the X-axis
    // *** USES INDEX-BASED APPROACH ***
    class DateAxisFormatter(private val history: List<WeightEntry>) : ValueFormatter() {
        // Format for displaying dates (e.g., "May 1")
        private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
            val index = value.toInt() // Convert the float index from the chart back to an integer
            // Check if the index is valid within the bounds of our history list
            return if (index >= 0 && index < history.size) {
                // Get the timestamp from the original WeightEntry using the index
                val millis = history[index].timestamp
                // Format the timestamp into a readable date string
                dateFormat.format(Date(millis))
            } else {
                // If the index is out of bounds, return an empty string
                ""
            }
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
            // Handle not logged in state (e.g., show message, disable views)
            Toast.makeText(requireContext(), "Please log in to view results.", Toast.LENGTH_SHORT).show()
            clearUI() // Clear potentially stale data
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload data when fragment becomes visible again, in case weight was logged elsewhere
        if (isAdded && loggedInUsername != null) {
            loadAndDisplayData()
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
        loadCalorieData() // Loads today's calorie summary
        loadWeightData() // Loads overall weight stats (start, goal, current)
        loadWeightHistoryAndSetupChart() // Loads history list and updates the chart
        calculateAndSetMotivation()
    }

    // Clears UI elements if user is not logged in or data is unavailable
    private fun clearUI() {
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
        weightChartView.clear()
        weightChartView.setNoDataText("Log in to see weight chart.")
        weightChartView.invalidate()
        motivationTextView.text = ""
    }

    // --- Helper Functions for SharedPreferences ---

    private fun getUserDataKey(baseKey: String): String? {
        return loggedInUsername?.let { "${it}_${baseKey}" }
    }

    // Gets key for data specific to TODAY's date
    private fun getTodaysDataKey(baseKey: String): String? {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return loggedInUsername?.let { "${it}_${baseKey}_$todayDate" }
    }

    // --- Calorie Data ---

    private fun loadCalorieData() {
        // Goal is user-specific, not date-specific
        val calorieGoalKey = getUserDataKey(BASE_KEY_CALORIE_GOAL)
        val caloriesGoal = userDataPreferences.getInt(calorieGoalKey, 2000) // Default

        // Eaten calories are specific to TODAY for this summary view
        val caloriesEatenKey = getTodaysDataKey(BASE_KEY_DAILY_CALORIES_EATEN) ?: return
        val caloriesEaten = userDataPreferences.getInt(caloriesEatenKey, 0)

        val caloriesLeft = (caloriesGoal - caloriesEaten).coerceAtLeast(0)
        val progress = if (caloriesGoal > 0) (caloriesEaten * 100 / caloriesGoal).coerceIn(0,100) else 0

        goalCaloriesTextView.text = "$caloriesGoal kcal"
        eatenCaloriesTextView.text = "$caloriesEaten kcal"
        leftCaloriesTextView.text = "$caloriesLeft kcal"
        caloriesProgressBar.progress = progress

        val averageCalories = calculateAverageCalories()
        averageCaloriesTextView.text = "Avg: ${String.format("%.1f", averageCalories)} kcal"
    }

    // Calculates average daily calories over the last 'days'
    private fun calculateAverageCalories(days: Int = 7): Double {
        val calendar = Calendar.getInstance()
        var totalCalories = 0
        var validDays = 0
        val keyDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in 0 until days) {
            val dateStr = keyDateFormat.format(calendar.time)
            val caloriesEatenKey = loggedInUsername?.let { "${it}_${BASE_KEY_DAILY_CALORIES_EATEN}_$dateStr" }

            if (caloriesEatenKey != null && userDataPreferences.contains(caloriesEatenKey)) {
                totalCalories += userDataPreferences.getInt(caloriesEatenKey, 0)
                validDays++
            }
            calendar.add(Calendar.DAY_OF_MONTH, -1) // Go back one day
        }

        return if (validDays > 0) totalCalories.toDouble() / validDays else 0.0
    }

    // --- Weight Data (Overall Stats) ---

    private fun loadWeightData() {
        // These keys are user-specific, not date-specific
        val currentWeightKey = getUserDataKey(BASE_KEY_WEIGHT)
        val targetWeightKey = getUserDataKey(BASE_KEY_WEIGHT_GOAL)
        val startingWeightKey = getUserDataKey(BASE_KEY_STARTING_WEIGHT)

        // Use the most recent weight stored under the simple key
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

    // --- Weight History and Chart ---

    // Loads the history list and passes it to updateWeightChart
    private fun loadWeightHistoryAndSetupChart() {
        if (loggedInUsername == null) {
            weightChartView.clear()
            weightChartView.setNoDataText("Log in to see weight chart.")
            weightChartView.invalidate()
            return
        }

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

        // Log the loaded history for debugging
        Log.d(TAG, "Loaded Weight History: ${weightHistory.joinToString { "(${Date(it.timestamp)}, ${it.weight})" }}")


        updateWeightChart(weightHistory)
    }

    // Updates the chart using the loaded weight history list
    // *** USES INDEX-BASED APPROACH FOR X-AXIS ***
    private fun updateWeightChart(weightHistory: List<WeightEntry>) {
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
        Log.d(TAG, "Chart Entries (Index, Weight): ${entries.joinToString { "(${it.x}, ${it.y})" }}")


        // 3. Create a DataSet
        val dataSet = LineDataSet(entries, "Weight (kg)")
        // Use ContextCompat to safely get colors
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.nav_item_selected_dark_green)
        dataSet.color = primaryColor
        dataSet.valueTextColor = primaryColor
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

        // ---> Apply the custom DateAxisFormatter <---
        xAxis.valueFormatter = DateAxisFormatter(sortedHistory) // Pass the sorted list

        xAxis.setLabelRotationAngle(-45f) // Rotate labels to prevent overlap
        xAxis.setAvoidFirstLastClipping(true) // Try to show first/last labels

        // Adjust label count dynamically (optional, MPAndroidChart tries to do this automatically)
        // xAxis.setLabelCount(min(sortedHistory.size, 6), true) // Show max 6 labels, force count

        weightChartView.axisRight.isEnabled = false // Hide right Y-axis
        weightChartView.axisLeft.textColor = Color.DKGRAY
        weightChartView.legend.isEnabled = true
        weightChartView.legend.textColor = Color.DKGRAY

        // Add some padding
        weightChartView.setExtraOffsets(5f, 10f, 5f, 10f)


        // 6. Set data and refresh
        weightChartView.data = lineData
        weightChartView.invalidate() // Refresh the chart to display the data
    }


    // --- Motivation ---

    private fun calculateAndSetMotivation() {
        // Placeholder: Implement motivational logic based on progress
        motivationTextView.text = "Keep up the great work!"
    }
}
