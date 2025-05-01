package com.example.chowcheck

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity // Import AppCompatActivity
import androidx.appcompat.widget.Toolbar // Import Toolbar


class EditProfileActivity : AppCompatActivity() {

    // --- Views ---
    private lateinit var editTextName: EditText
    private lateinit var editTextAge: EditText
    private lateinit var editTextHeight: EditText
    private lateinit var editTextWeight: EditText
    private lateinit var editTextWeightGoal: EditText
    private lateinit var buttonSaveChanges: Button
    private lateinit var toolbar: Toolbar

    // --- SharedPreferences & User Data ---
    private lateinit var userDataPreferences: SharedPreferences
    private var loggedInUsername: String? = null

    // --- Constants ---
    // Should match those used in ProfileFragment and other classes
    companion object {
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user"
        const val BASE_KEY_NAME = "name"
        const val BASE_KEY_AGE = "age"
        const val BASE_KEY_HEIGHT = "height"
        const val BASE_KEY_WEIGHT = "weight"
        const val BASE_KEY_WEIGHT_GOAL = "weight_goal"
        const val BASE_KEY_STARTING_WEIGHT = "starting_weight" // Keep track of starting weight
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        userDataPreferences = getSharedPreferences(USER_DATA_PREFS, Context.MODE_PRIVATE)
        loggedInUsername = userDataPreferences.getString(KEY_LOGGED_IN_USER, null)

        // Redirect if not logged in
        if (loggedInUsername == null) {
            Toast.makeText(this, "Please log in to edit profile.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        initializeViews()
        setupToolbar()
        loadProfileData()
        setupButtonClickListeners()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.editProfileToolbar)
        editTextName = findViewById(R.id.editTextName)
        editTextAge = findViewById(R.id.editTextAge)
        editTextHeight = findViewById(R.id.editTextHeight)
        editTextWeight = findViewById(R.id.editTextWeight)
        editTextWeightGoal = findViewById(R.id.editTextWeightGoal)
        buttonSaveChanges = findViewById(R.id.btnSaveChanges)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar) // Use Toolbar as the ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back arrow
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            // Handle back button press
            finish() // Simply close this activity
        }
    }


    private fun loadProfileData() {
        // Reuse logic from old ProfileFragment
        if (loggedInUsername == null) return

        val savedName = userDataPreferences.getString(getUserDataKey(loggedInUsername!!, BASE_KEY_NAME), "")
        val savedAge = userDataPreferences.getString(getUserDataKey(loggedInUsername!!, BASE_KEY_AGE), "")
        val savedHeight = userDataPreferences.getString(getUserDataKey(loggedInUsername!!, BASE_KEY_HEIGHT), "")
        val savedWeight = userDataPreferences.getString(getUserDataKey(loggedInUsername!!, BASE_KEY_WEIGHT), "")
        val savedWeightGoal = userDataPreferences.getString(getUserDataKey(loggedInUsername!!, BASE_KEY_WEIGHT_GOAL), "")

        editTextName.setText(savedName)
        editTextAge.setText(savedAge)
        editTextHeight.setText(savedHeight)
        editTextWeight.setText(savedWeight)
        editTextWeightGoal.setText(savedWeightGoal)
    }

    private fun setupButtonClickListeners() {
        buttonSaveChanges.setOnClickListener {
            if (loggedInUsername == null) {
                Toast.makeText(this, "Error: Cannot save profile. Not logged in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Get data from fields
            val name = editTextName.text.toString().trim()
            val age = editTextAge.text.toString().trim()
            val height = editTextHeight.text.toString().trim()
            val weight = editTextWeight.text.toString().trim()
            val weightGoal = editTextWeightGoal.text.toString().trim()

            // Validate (reuse logic from old ProfileFragment)
            if (!validateProfileInputs(name, age, height, weight, weightGoal)) {
                return@setOnClickListener
            }

            // Save Data (reuse logic from old ProfileFragment)
            val editor = userDataPreferences.edit()
            editor.putString(getUserDataKey(loggedInUsername!!, BASE_KEY_NAME), name)
            editor.putString(getUserDataKey(loggedInUsername!!, BASE_KEY_AGE), age)
            editor.putString(getUserDataKey(loggedInUsername!!, BASE_KEY_HEIGHT), height)
            editor.putString(getUserDataKey(loggedInUsername!!, BASE_KEY_WEIGHT), weight)
            editor.putString(getUserDataKey(loggedInUsername!!, BASE_KEY_WEIGHT_GOAL), weightGoal)

            // --- Update Starting Weight Logic ---
            // If starting weight isn't set OR if the current saved weight is empty/invalid,
            // set the current weight as the starting weight.
            // This ensures starting weight is captured when profile is first saved/updated.
            val startingWeightKey = getUserDataKey(loggedInUsername!!, BASE_KEY_STARTING_WEIGHT)
            val existingStartingWeight = userDataPreferences.getString(startingWeightKey, null)
            if (existingStartingWeight.isNullOrEmpty() || existingStartingWeight == "N/A") {
                editor.putString(startingWeightKey, weight)
            }
            // --- End Starting Weight Logic ---

            editor.apply()

            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            finish() // Close the activity after saving
        }
    }

    // Validation function (from old ProfileFragment, adapt context if needed)
    private fun validateProfileInputs(name: String, ageStr: String, heightStr: String, weightStr: String, weightGoalStr: String): Boolean {
        // Use 'this' for context instead of 'requireContext()'
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show(); return false
        }
        val age = ageStr.toIntOrNull()
        if (age == null || age <= 0 || age > 120) {
            Toast.makeText(this, "Please enter a valid age (1-120)", Toast.LENGTH_SHORT).show(); return false
        }
        val height = heightStr.toDoubleOrNull()
        if (height == null || height <= 0 || height > 300) { // Assuming cm
            Toast.makeText(this, "Please enter a valid height (cm)", Toast.LENGTH_SHORT).show(); return false
        }
        val weight = weightStr.toDoubleOrNull()
        if (weight == null || weight <= 0 || weight > 500) { // Assuming kg
            Toast.makeText(this, "Please enter a valid weight (kg)", Toast.LENGTH_SHORT).show(); return false
        }
        val weightGoal = weightGoalStr.toDoubleOrNull()
        if (weightGoal == null || weightGoal <= 0 || weightGoal > 500) { // Assuming kg
            Toast.makeText(this, "Please enter a valid weight goal (kg)", Toast.LENGTH_SHORT).show(); return false
        }
        return true
    }

    // Helper function (from old ProfileFragment)
    private fun getUserDataKey(username: String, baseKey: String): String {
        return "${username}_${baseKey}"
    }

    // --- Optional: Handle back press from ActionBar if not using setNavigationOnClickListener ---
    /*
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish() // Close activity when Up button is pressed
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    */
}