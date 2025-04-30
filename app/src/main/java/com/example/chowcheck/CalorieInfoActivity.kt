package com.example.chowcheck

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*

class CalorieInfoActivity : Activity() {

    // Views
    private lateinit var editTextName: EditText
    private lateinit var editTextWeightGoal: EditText
    private lateinit var editTextWeight: EditText
    private lateinit var editTextHeight: EditText
    private lateinit var editTextAge: EditText
    private lateinit var spinnerSex: Spinner
    private lateinit var spinnerActivityLevel: Spinner
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

        buttonSubmitInfo.setOnClickListener {
            // Check again if username is somehow null before proceeding
            if (loggedInUsername == null) {
                Toast.makeText(this, "Error: User session lost.", Toast.LENGTH_SHORT).show()
                finish() // Or redirect to Login
                return@setOnClickListener
            }

            // Get values from fields (existing code)
            val name = editTextName.text.toString().trim()
            val weightStr = editTextWeight.text.toString().trim()
            val weightGoalStr = editTextWeightGoal.text.toString().trim()
            val heightStr = editTextHeight.text.toString().trim()
            val ageStr = editTextAge.text.toString().trim()
            val selectedSex = spinnerSex.selectedItem.toString()
            val selectedActivityLevel = spinnerActivityLevel.selectedItem.toString()
            val selectedSexPosition = spinnerSex.selectedItemPosition
            val selectedActivityPosition = spinnerActivityLevel.selectedItemPosition

            // Validation (existing code - ensure it's robust)
            if (!validateInputs(name, weightStr, weightGoalStr, heightStr, ageStr, selectedSexPosition, selectedActivityPosition)) {
                return@setOnClickListener // Stop if validation fails
            }

            // --- Save data using USER-SPECIFIC keys ---
            val editor = userDataPreferences.edit()
            editor.putString(getUserDataKey(loggedInUsername!!, BASE_KEY_NAME), name)
            editor.putString(getUserDataKey(loggedInUsername!!, BASE_KEY_WEIGHT), weightStr)
            editor.putString(getUserDataKey(loggedInUsername!!, BASE_KEY_WEIGHT_GOAL), weightGoalStr)
            editor.putString(getUserDataKey(loggedInUsername!!, BASE_KEY_HEIGHT), heightStr)
            editor.putString(getUserDataKey(loggedInUsername!!, BASE_KEY_AGE), ageStr)
            editor.putString(getUserDataKey(loggedInUsername!!, BASE_KEY_SEX), selectedSex)
            editor.putString(getUserDataKey(loggedInUsername!!, BASE_KEY_ACTIVITY_LEVEL), selectedActivityLevel)
            editor.apply()

            Toast.makeText(this, "Information saved!", Toast.LENGTH_SHORT).show()

            // Navigate next (e.g., LandingActivity)
            val intent = Intent(this, LandingActivity::class.java)
            startActivity(intent)
            finish()
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
        buttonSubmitInfo = findViewById(R.id.buttonSubmitInfo)
    }

    private fun setupSpinners() {
        // (Spinner setup code remains the same as before)
        val sexOptions = arrayOf("Select Sex", "Male", "Female")
        val sexAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sexOptions)
        sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSex.adapter = sexAdapter

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
    }

    private fun validateInputs(name: String, weightStr: String, weightGoalStr: String, heightStr: String, ageStr: String, sexPos: Int, activityPos: Int): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show(); return false
        }
        if (weightStr.isEmpty() || weightStr.toDoubleOrNull() == null || weightStr.toDouble() <= 0) {
            Toast.makeText(this, "Please enter a valid current weight", Toast.LENGTH_SHORT).show(); return false
        }
        if (weightGoalStr.isEmpty() || weightGoalStr.toDoubleOrNull() == null || weightGoalStr.toDouble() <= 0) {
            Toast.makeText(this, "Please enter a valid weight goal", Toast.LENGTH_SHORT).show(); return false
        }
        if (heightStr.isEmpty() || heightStr.toDoubleOrNull() == null || heightStr.toDouble() <= 0) {
            Toast.makeText(this, "Please enter a valid height", Toast.LENGTH_SHORT).show(); return false
        }
        if (ageStr.isEmpty() || ageStr.toIntOrNull() == null || ageStr.toInt() <= 0) {
            Toast.makeText(this, "Please enter a valid age", Toast.LENGTH_SHORT).show(); return false
        }
        if (sexPos == 0) {
            Toast.makeText(this, "Please select your sex", Toast.LENGTH_SHORT).show(); return false
        }
        if (activityPos == 0) {
            Toast.makeText(this, "Please select your activity level", Toast.LENGTH_SHORT).show(); return false
        }
        return true // All valid
    }
}