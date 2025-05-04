package com.example.chowcheck

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View // Import View for OnItemSelectedListener
import android.widget.*
import java.util.* // For Calendar and Date
import java.util.concurrent.TimeUnit // For time calculations
import kotlin.math.abs // Needed for weight difference comparison


class CalorieInfoActivity : Activity() {

    // Views
    private lateinit var editTextName: EditText
    private lateinit var editTextWeightGoal: EditText
    private lateinit var editTextWeight: EditText
    private lateinit var editTextHeight: EditText
    private lateinit var editTextAge: EditText
    private lateinit var spinnerSex: Spinner
    private lateinit var spinnerActivityLevel: Spinner
    private lateinit var editTextTimeframeWeeks: EditText // Keep this
    private lateinit var imageViewCalendar: ImageView // Keep this
    private lateinit var spinnerWeeklyChange: Spinner // Keep this one
    private lateinit var buttonSubmitInfo: Button

    // SharedPreferences and Logged-in User
    private lateinit var userDataPreferences: SharedPreferences
    private var loggedInUsername: String? = null // Variable to hold the username

    // Base keys (used for constructing user-specific keys)
    companion object {
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user" // To get the current user
        // Base names for profile fields
        const val BASE_KEY_NAME = "name"
        const val BASE_KEY_AGE = "age"
        const val BASE_KEY_HEIGHT = "height"
        const val BASE_KEY_WEIGHT = "weight"
        const val BASE_KEY_WEIGHT_GOAL = "weight_goal"
        const val BASE_KEY_SEX = "sex"
        const val BASE_KEY_ACTIVITY_LEVEL = "activityLevel"
        // Key for calculated calorie goal
        const val BASE_KEY_CALORIE_GOAL = "calorie_goal"
        // Keys to save timeframe and weekly change choice
        const val BASE_KEY_TIMEFRAME_WEEKS = "timeframe_weeks" // Keep this
        const val BASE_KEY_WEEKLY_CHANGE = "weekly_change_option" // Keep this one
        // Key for starting weight (for diary)
        const val BASE_KEY_STARTING_WEIGHT = "starting_weight" // Ensure this constant exists
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calorie_info)

        userDataPreferences = getSharedPreferences(USER_DATA_PREFS, MODE_PRIVATE)

        // --- Get the currently logged-in user ---
        loggedInUsername = userDataPreferences.getString(KEY_LOGGED_IN_USER, null)

        // --- Redirect to Login if no user is logged in ---
        if (loggedInUsername == null) {
            Toast.makeText(this, "Error: No user logged in.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return // Stop further execution in onCreate
        }
        // --- End Redirect ---

        // Initialize Views
        initializeViews()
        setupSpinners()
        loadUserData() // Load existing data
        setupCalendarPicker() // Setup calendar click listener (Keep this)


        buttonSubmitInfo.setOnClickListener {
            // Check again if username is somehow null before proceeding
            if (loggedInUsername == null) {
                Toast.makeText(this, "Error: User session lost.", Toast.LENGTH_SHORT).show()
                finish() // Or redirect to Login
                return@setOnClickListener
            }

            // Get raw string values from fields
            val name = editTextName.text.toString().trim()
            val weightStr = editTextWeight.text.toString().trim()
            val weightGoalStr = editTextWeightGoal.text.toString().trim()
            val heightStr = editTextHeight.text.toString().trim()
            val ageStr = editTextAge.text.toString().trim()
            val selectedSex = spinnerSex.selectedItem.toString()
            val selectedActivityLevel = spinnerActivityLevel.selectedItem.toString()
            val selectedSexPosition = spinnerSex.selectedItemPosition
            val selectedActivityPosition = spinnerActivityLevel.selectedItemPosition

            // Get raw values from BOTH timeframe and weekly change
            val timeframeStr = editTextTimeframeWeeks.text.toString().trim()
            val weeklyChangeOption = spinnerWeeklyChange.selectedItem.toString()
            val weeklyChangePosition = spinnerWeeklyChange.selectedItemPosition


            // Validation (This function uses Nullable types internally)
            if (!validateInputs(name, weightStr, weightGoalStr, heightStr, ageStr, selectedSexPosition, selectedActivityPosition, timeframeStr, weeklyChangePosition)) {
                return@setOnClickListener // Stop if validation fails
            }

            // --- If Validation Passes, Safely Parse Non-Nullable Values for Calculation ---
            val weight = weightStr.toDouble() // Now guaranteed to be valid Double
            val weightGoal = weightGoalStr.toDouble() // Now guaranteed
            val height = heightStr.toDouble() // Now guaranteed
            val age = ageStr.toInt() // Now guaranteed
            val timeframe = timeframeStr.toIntOrNull() ?: 0 // Guaranteed >=0 if weightGoal!=weight, can be 0 if weightGoal==weight
            // weeklyChangeOption and weeklyChangePosition are also valid based on validation

            // --- Calculate Calorie Goal ---
            val tdee = calculateTdee(weight, height, age, selectedSex, selectedActivityLevel)

            // Determine calculation method based on validation results
            val isTimeframeValid = timeframe > 0 // Timeframe is valid if parses to Int > 0 AND > 0
            val isWeeklyChangeSelected = weeklyChangePosition > 0 // Position 0 is "Select Weekly Change"

            val weightDifference = weightGoal - weight // Positive for gain, negative for loss


            var calculatedTargetDouble = tdee // Start with maintenance as a Double


            // Calculation logic based on which method was provided (prioritize Weekly Change if both valid)
            if (isWeeklyChangeSelected) {
                // Use weekly change method
                val weeklyChangeKg = parseWeeklyChangeFromSpinner(weeklyChangeOption)
                val dailyCalorieAdjustment = (weeklyChangeKg * 7700.0 / 7.0) // 1 kg is ~7700 kcal. Use double literals for calculation accuracy
                calculatedTargetDouble = tdee + dailyCalorieAdjustment

            } else if (isTimeframeValid) {
                // Use timeframe method (Validation ensures timeframe is valid if selected and weekly change is NOT selected)
                if (abs(weightDifference) > 0.1) { // Only calculate deficit/surplus if goal is different
                    val days = timeframe * 7
                    val totalCaloriesToChange = weightDifference * 7700.0 // 1 kg = ~7700 kcal (use double literal)

                    // Avoid division by zero if timeframe somehow becomes 0 here (validation should prevent this if weightDifference > 0.1)
                    val dailyCalorieAdjustment = if (days > 0) totalCaloriesToChange / days else 0.0
                    calculatedTargetDouble = tdee + dailyCalorieAdjustment
                } else {
                    // Goal is current weight, timeframe doesn't matter, maintain
                    calculatedTargetDouble = tdee
                }
            } else {
                // This branch should only be hit if weightDifference is negligible AND neither is selected,
                // or if validation somehow failed silently. Default to maintenance.
                calculatedTargetDouble = tdee
                if (abs(weightDifference) > 0.1) {
                    // This indicates a validation logic issue if weightDifference is not negligible
                    Toast.makeText(this, "Calculation method issue, defaulting to maintenance.", Toast.LENGTH_SHORT).show()
                }
            }


            // Convert to Int for saving/displaying
            val targetCalories: Int = calculatedTargetDouble.toInt()


            // --- Safety Check ---
            // Pass relevant inputs for safety check calculation if needed inside the function
            if (isDangerousCalorieGoal(targetCalories, calculateBmr(weight, height, age, selectedSex), selectedSex, tdee)) {
                showDangerousCalorieDialog(targetCalories,
                    onContinue = {
                        // User chose to continue, save data and navigate
                        saveUserData(loggedInUsername!!, name, weightStr, weightGoalStr, heightStr, ageStr, selectedSex, selectedActivityLevel, targetCalories, timeframeStr, weeklyChangeOption) // Save BOTH
                        navigateToLanding()
                    },
                    onAdjust = {
                        // User chose to adjust, dialog is dismissed, they can edit inputs
                        // Do nothing, let them edit
                    }
                )
            } else {
                // Goal is safe, save data and navigate directly
                saveUserData(loggedInUsername!!, name, weightStr, weightGoalStr, heightStr, ageStr, selectedSex, selectedActivityLevel, targetCalories, timeframeStr, weeklyChangeOption) // Save BOTH
                navigateToLanding()
            }
        }
    }

    // Helper to generate user-specific data keys
    private fun getUserDataKey(username: String, baseKey: String): String {
        return "${username}_${baseKey}" // e.g., "john_name", "jane_age"
    }

    private fun initializeViews() {
        editTextName = findViewById(R.id.editTextName)
        editTextWeightGoal = findViewById(R.id.editTextWeightGoal)
        editTextWeight = findViewById(R.id.editTextWeight)
        editTextHeight = findViewById(R.id.editTextHeight)
        editTextAge = findViewById(R.id.editTextAge)
        spinnerSex = findViewById(R.id.spinnerSex)
        spinnerActivityLevel = findViewById(R.id.spinnerActivityLevel)
        editTextTimeframeWeeks = findViewById(R.id.editTextTimeframeWeeks) // Init
        imageViewCalendar = findViewById(R.id.imageViewCalendar) // Init
        spinnerWeeklyChange = findViewById(R.id.spinnerWeeklyChange) // Init
        buttonSubmitInfo = findViewById(R.id.buttonSubmitInfo)
    }

    private fun setupSpinners() {
        // Sex Spinner
        val sexOptions = arrayOf("Select Sex", "Male", "Female")
        val sexAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sexOptions)
        sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSex.adapter = sexAdapter

        // Activity Level Spinner
        val activityLevelOptions = arrayOf(
            "Select Activity Level",
            "Sedentary (little or no exercise)",
            "Lightly active (light exercise/sports 1-3 days/week)",
            "Moderately active (moderate exercise/sports 3-5 days/week)",
            "Very active (hard exercise/sports 6-7 days a week)",
            "Extra active (very hard exercise/sports & physical job)"
        )
        val activityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, activityLevelOptions)
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerActivityLevel.adapter = activityAdapter

        // Weekly Change Spinner
        val weeklyChangeOptions = arrayOf(
            "Select Weekly Change",
            "Maintain Current Weight (0 kg)", // Treat as 0 change
            "Lose 0.25 kg",
            "Lose 0.5 kg",
            "Lose 1 kg",
            "Gain 0.25 kg",
            "Gain 0.5 kg",
            "Gain 1 kg"
        )
        val weeklyChangeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, weeklyChangeOptions)
        weeklyChangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWeeklyChange.adapter = weeklyChangeAdapter
    }

    // Setup calendar click listener (Keep this)
    private fun setupCalendarPicker() {
        imageViewCalendar.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Calculate weeks difference
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

                    val todayCalendar = Calendar.getInstance()
                    // Clear time component to compare dates only
                    todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
                    todayCalendar.set(Calendar.MINUTE, 0)
                    todayCalendar.set(Calendar.SECOND, 0)
                    todayCalendar.set(Calendar.MILLISECOND, 0)

                    selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
                    selectedCalendar.set(Calendar.MINUTE, 0)
                    selectedCalendar.set(Calendar.SECOND, 0)
                    selectedCalendar.set(Calendar.MILLISECOND, 0)


                    if (selectedCalendar.before(todayCalendar)) {
                        Toast.makeText(this, "Please select a future date for your goal.", Toast.LENGTH_SHORT).show()
                        editTextTimeframeWeeks.setText("") // Clear invalid input
                    } else {
                        val diffInMillis = selectedCalendar.timeInMillis - todayCalendar.timeInMillis
                        // Add a small buffer to milliseconds to handle potential time zone/daylight savings issues near midnight
                        val bufferInMillis = TimeUnit.HOURS.toMillis(1) // 1 hour buffer
                        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis + bufferInMillis)
                        // Integer division gives whole weeks, rounding down
                        val diffInWeeks = (diffInDays / 7).toInt()

                        // Ensure minimum 1 week if diffInDays is positive but less than 7, or if days > 0
                        val finalWeeks = when {
                            diffInDays <= 0 -> 0 // If selected today or past (should be caught by before check but as safeguard)
                            diffInDays < 7 -> 1 // If in the next 6 days, call it 1 week
                            else -> diffInWeeks // Otherwise use the calculated whole weeks
                        }


                        editTextTimeframeWeeks.setText(finalWeeks.toString())
                        // Clear the weekly change spinner selection if a timeframe is picked
                        spinnerWeeklyChange.setSelection(0) // Set back to "Select Weekly Change"
                    }
                },
                year, month, day
            )

            // Optionally set the minimum date to today so past dates cannot be selected
            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000 // Subtract a second to allow today

            datePickerDialog.show()
        }

        // Clear the timeframe text when a weekly change option is selected
        spinnerWeeklyChange.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) { // If something other than the default is selected
                    editTextTimeframeWeeks.setText("") // Clear the timeframe text
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }


    private fun loadUserData() {
        loggedInUsername?.let { username ->
            editTextName.setText(userDataPreferences.getString(getUserDataKey(username, BASE_KEY_NAME), ""))
            editTextWeight.setText(userDataPreferences.getString(getUserDataKey(username, BASE_KEY_WEIGHT), ""))
            editTextWeightGoal.setText(userDataPreferences.getString(getUserDataKey(username, BASE_KEY_WEIGHT_GOAL), ""))
            editTextHeight.setText(userDataPreferences.getString(getUserDataKey(username, BASE_KEY_HEIGHT), ""))
            editTextAge.setText(userDataPreferences.getString(getUserDataKey(username, BASE_KEY_AGE), ""))

            // Load and set Sex spinner
            val savedSex = userDataPreferences.getString(getUserDataKey(username, BASE_KEY_SEX), "Select Sex")
            val sexAdapter = spinnerSex.adapter as? ArrayAdapter<String>
            val sexPosition = sexAdapter?.getPosition(savedSex) ?: 0
            spinnerSex.setSelection(sexPosition.coerceAtLeast(0))

            // Load and set Activity Level spinner
            val savedActivity = userDataPreferences.getString(getUserDataKey(username, BASE_KEY_ACTIVITY_LEVEL), "Select Activity Level")
            val activityAdapter = spinnerActivityLevel.adapter as? ArrayAdapter<String>
            val activityPosition = activityAdapter?.getPosition(savedActivity) ?: 0
            spinnerActivityLevel.setSelection(activityPosition.coerceAtLeast(0))

            // Load Timeframe (Keep this)
            editTextTimeframeWeeks.setText(userDataPreferences.getString(getUserDataKey(username, BASE_KEY_TIMEFRAME_WEEKS), ""))

            // Load Weekly Change (Keep this)
            val savedWeeklyChange = userDataPreferences.getString(getUserDataKey(username, BASE_KEY_WEEKLY_CHANGE), "Select Weekly Change")
            val weeklyChangeAdapter = spinnerWeeklyChange.adapter as? ArrayAdapter<String>
            spinnerWeeklyChange.setSelection(weeklyChangeAdapter?.getPosition(savedWeeklyChange)?.coerceAtLeast(0) ?: 0)
        }
    }

    // Updated saveUserData to save BOTH timeframe and weekly change
    private fun saveUserData(username: String, name: String, weightStr: String, weightGoalStr: String, heightStr: String, ageStr: String, sex: String, activityLevel: String, calculatedCalorieGoal: Int, timeframeStr: String, weeklyChangeOption: String) {
        val editor = userDataPreferences.edit()
        editor.putString(getUserDataKey(username, BASE_KEY_NAME), name)
        editor.putString(getUserDataKey(username, BASE_KEY_WEIGHT), weightStr)
        editor.putString(getUserDataKey(username, BASE_KEY_WEIGHT_GOAL), weightGoalStr)
        editor.putString(getUserDataKey(username, BASE_KEY_HEIGHT), heightStr)
        editor.putString(getUserDataKey(username, BASE_KEY_AGE), ageStr)
        editor.putString(getUserDataKey(username, BASE_KEY_SEX), sex)
        editor.putString(getUserDataKey(username, BASE_KEY_ACTIVITY_LEVEL), activityLevel)
        editor.putInt(getUserDataKey(username, BASE_KEY_CALORIE_GOAL), calculatedCalorieGoal) // Save the calculated goal!
        editor.putString(getUserDataKey(username, BASE_KEY_TIMEFRAME_WEEKS), timeframeStr) // Save timeframe
        editor.putString(getUserDataKey(username, BASE_KEY_WEEKLY_CHANGE), weeklyChangeOption) // Save weekly change option


        // Save starting weight if it's the first time setting weight info
        val startingWeightKey = getUserDataKey(username, BASE_KEY_STARTING_WEIGHT)
        if (!userDataPreferences.contains(startingWeightKey) || userDataPreferences.getString(startingWeightKey, null).isNullOrEmpty()) {
            editor.putString(startingWeightKey, weightStr)
        }
        editor.apply()
    }


    // Updated validation logic to require EITHER timeframe OR weekly change
    private fun validateInputs(name: String, weightStr: String, weightGoalStr: String, heightStr: String, ageStr: String, sexPos: Int, activityPos: Int, timeframeStr: String, weeklyChangePos: Int): Boolean {
        // Basic field validation (using toDoubleOrNull/toIntOrNull temporarily for checks)
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show(); return false
        }
        val weight = weightStr.toDoubleOrNull()
        if (weight == null || weight <= 0) {
            Toast.makeText(this, "Please enter a valid current weight", Toast.LENGTH_SHORT).show(); return false
        }
        val weightGoal = weightGoalStr.toDoubleOrNull()
        if (weightGoal == null || weightGoal <= 0) {
            Toast.makeText(this, "Please enter a valid weight goal", Toast.LENGTH_SHORT).show(); return false
        }
        val height = heightStr.toDoubleOrNull()
        if (height == null || height <= 0) {
            Toast.makeText(this, "Please enter a valid height", Toast.LENGTH_SHORT).show(); return false
        }
        val age = ageStr.toIntOrNull()
        if (age == null || age <= 0) {
            Toast.makeText(this, "Please enter a valid age", Toast.LENGTH_SHORT).show(); return false
        }
        if (sexPos == 0) {
            Toast.makeText(this, "Please select your sex", Toast.LENGTH_SHORT).show(); return false
        }
        if (activityPos == 0) {
            Toast.makeText(this, "Please select your activity level", Toast.LENGTH_SHORT).show(); return false
        }

        // Validation for EITHER timeframe OR weekly change
        val timeframe = timeframeStr.toIntOrNull()
        val isTimeframeValid = timeframe != null && timeframe > 0 // Timeframe must be positive if used
        val isWeeklyChangeSelected = weeklyChangePos > 0 // Position 0 is "Select Weekly Change"

        val currentWeightValidated = weightStr.toDouble() // Use validated values for goal comparison
        val weightGoalValidated = weightGoalStr.toDouble()
        val weightDifference = weightGoalValidated - currentWeightValidated // Positive for gain, negative for loss


        if (abs(weightDifference) > 0.1) { // If there is a significant weight goal difference (ignore tiny floating point diffs)
            if (!isTimeframeValid && !isWeeklyChangeSelected) {
                Toast.makeText(this, "To reach your weight goal, please enter a timeframe OR select a desired weekly change.", Toast.LENGTH_LONG).show()
                return false
            }
            // Allow BOTH to be filled, but calculation logic will prioritize weekly change (or you could add a prompt)
            // Acknowledge if both are filled
            if (isTimeframeValid && isWeeklyChangeSelected) {
                Toast.makeText(this, "Using selected weekly change rate for calculation.", Toast.LENGTH_SHORT).show()
            }


            // If weekly change IS selected, validate direction matches goal
            if (isWeeklyChangeSelected) {
                val weeklyChangeKg = parseWeeklyChangeFromSpinner(spinnerWeeklyChange.selectedItem.toString())
                // Check for conflicting goals
                if (weightDifference > 0.1 && weeklyChangeKg < -0.1) { // Need to gain significantly, selected lose
                    Toast.makeText(this, "Your goal is to gain weight, but you selected a 'Lose' rate.", Toast.LENGTH_LONG).show()
                    return false
                }
                if (weightDifference < -0.1 && weeklyChangeKg > 0.1) { // Need to lose significantly, selected gain
                    Toast.makeText(this, "Your goal is to lose weight, but you selected a 'Gain' rate.", Toast.LENGTH_LONG).show()
                    return false
                }
                // If goal is maintenance but selected gain/lose (this case is covered by the else block below)
            } else if (isTimeframeValid) {
                // If timeframe IS valid and weekly change is NOT selected, check if the *implied* weekly change is reasonable
                // Calculate the implied daily change
                val timeframeValidated = timeframe!! // Guaranteed non-null > 0 by isTimeframeValid check
                val impliedDailyChange = (weightDifference * 7700.0 / (timeframeValidated * 7.0)) // Avoid division by zero
                val impliedWeeklyChange = impliedDailyChange * 7.0 / 7700.0 // Convert back to kg per week

                // Warn if implied weekly change is significantly > 1kg loss or > 1kg gain (outside typical recommendations)
                // You can adjust these thresholds
                if (impliedWeeklyChange < -1.1 || impliedWeeklyChange > 1.1) { // Use slightly larger threshold than 1.0
                    Toast.makeText(this, "Note: Your timeframe implies a rapid change of approx. ${String.format("%.2f", abs(impliedWeeklyChange))} kg/week. Consider a longer timeframe for a more sustainable rate (e.g., 0.5-1 kg/week).", Toast.LENGTH_LONG).show()
                    // Do NOT return false, just inform the user
                }
            }


        } else { // Weight goal is essentially current weight (maintenance goal: abs(weightDifference) <= 0.1)
            // If goal is maintenance, and weekly change is selected, it *must* be the maintenance option.
            if (isWeeklyChangeSelected) {
                val weeklyChangeKg = parseWeeklyChangeFromSpinner(spinnerWeeklyChange.selectedItem.toString())
                if (abs(weeklyChangeKg) > 0.1) {
                    Toast.makeText(this, "Your goal is maintenance. Please select 'Maintain Current Weight (0 kg)' or leave timeframe and weekly change blank.", Toast.LENGTH_LONG).show()
                    return false
                }
            }
            // If timeframe is entered when goal is maintenance, it's allowed, calculation defaults to TDEE.
            // If neither is selected/entered, calculation defaults to TDEE.
        }


        return true // All valid checks passed based on inputs
    }

    // --- Calculation Logic ---

    private fun calculateBmr(weight: Double, height: Double, age: Int, sex: String): Double {
        // Using Mifflin-St Jeor Equation
        return if (sex == "Male") {
            (10 * weight) + (6.25 * height) - (5 * age) + 5
        } else { // Female
            (10 * weight) + (6.25 * height) - (5 * age) - 161
        }
    }

    private fun calculateTdee(weight: Double, height: Double, age: Int, sex: String, activityLevel: String): Double {
        val bmr = calculateBmr(weight, height, age, sex)
        val activityFactor = when (activityLevel) {
            "Sedentary (little or no exercise)" -> 1.2
            "Lightly active (light exercise/sports 1-3 days/week)" -> 1.375
            "Moderately active (moderate exercise/sports 3-5 days/week)" -> 1.55
            "Very active (hard exercise/sports 6-7 days a week)" -> 1.725
            "Extra active (very hard exercise/sports & physical job)" -> 1.9
            else -> 1.2 // Default to sedentary if somehow invalid
        }
        return bmr * activityFactor
    }

    // Helper to parse weekly change value from spinner selection
    private fun parseWeeklyChangeFromSpinner(selection: String): Double {
        return when (selection) {
            "Lose 0.25 kg" -> -0.25
            "Lose 0.5 kg" -> -0.5
            "Lose 1 kg" -> -1.0
            "Gain 0.25 kg" -> 0.25
            "Gain 0.5 kg" -> 0.5
            "Gain 1 kg" -> 1.0
            "Maintain Current Weight (0 kg)" -> 0.0
            else -> 0.0 // "Select Weekly Change" or other unexpected values
        }
    }


    // --- Safety Check Logic ---
    // Updated signature to accept TDEE
    private fun isDangerousCalorieGoal(calculatedGoal: Int, bmr: Double, sex: String, tdee: Double): Boolean {
        // General minimums (can be debated, these are common references)
        val minSafeCalories = if (sex == "Male") 1500 else 1200

        // Calculate the daily deficit/surplus from TDEE for comparison
        val dailyDiff = calculatedGoal.toDouble() - tdee // Negative for deficit, Positive for surplus
        // Using calculatedGoal.toDouble() for accurate comparison with tdee

        // Warn if below minimum safe calories OR if the daily deficit is very large (> ~1100 kcal daily which is 1kg/week)
        val extremeDeficitThreshold = -1100.0 // daily deficit more than 1100 kcal

        // Warning conditions:
        // 1. Calculated goal is below the general minimum for the sex.
        // 2. Calculated goal results in a daily deficit larger than 1 kg/week (<-1100 kcal).

        return calculatedGoal < minSafeCalories || dailyDiff < extremeDeficitThreshold
    }


    private fun showDangerousCalorieDialog(calculatedGoal: Int, onContinue: () -> Unit, onAdjust: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Warning: Calorie Goal Recommendation")
            .setMessage("The calculated daily calorie goal of $calculatedGoal kcal may be very low. This implies a very rapid weight loss or is below general minimum recommendations. It may be dangerous or unsustainable without medical supervision. Are you sure you want to proceed with this goal?")
            .setPositiveButton("Continue Anyway") { dialog, _ ->
                onContinue.invoke()
                dialog.dismiss()
            }
            .setNegativeButton("Adjust Input") { dialog, _ ->
                onAdjust.invoke()
                dialog.dismiss()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun navigateToLanding() {
        // Ensure user data is saved before navigating (handled in the calling logic)
        val intent = Intent(this, NavigationActivity::class.java)
        startActivity(intent)
        finish() // Close this activity
    }
}