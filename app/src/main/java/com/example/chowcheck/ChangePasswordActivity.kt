package com.example.chowcheck

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast

class ChangePasswordActivity : Activity() {

    // Views
    private lateinit var editTextCurrentPassword: EditText
    private lateinit var editTextNewPassword: EditText
    private lateinit var editTextConfirmNewPassword: EditText
    private lateinit var buttonChangePassword: Button
    private lateinit var buttonBack: ImageButton

    // SharedPreferences and Logged-in User
    private lateinit var userDataPreferences: SharedPreferences
    private var loggedInUsername: String? = null

    // Constants consistent with other activities
    companion object {
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user"
        const val KEY_SUFFIX_PASSWORD = "_password"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        userDataPreferences = getSharedPreferences(USER_DATA_PREFS, MODE_PRIVATE)

        // Get the logged-in user
        loggedInUsername = userDataPreferences.getString(KEY_LOGGED_IN_USER, null)

        // Redirect if not logged in
        if (loggedInUsername == null) {
            Toast.makeText(this, "Please log in to change password.", Toast.LENGTH_LONG).show()
            redirectToLogin()
            return
        }

        // Initialize Views
        initializeViews()

        // Set Listeners
        setupButtonClickListeners()
    }

    private fun initializeViews() {
        editTextCurrentPassword = findViewById(R.id.editTextCurrentPassword)
        editTextNewPassword = findViewById(R.id.editTextNewPassword)
        editTextConfirmNewPassword = findViewById(R.id.editTextConfirmNewPassword)
        buttonChangePassword = findViewById(R.id.buttonChangePassword)
        buttonBack = findViewById(R.id.buttonBack)
    }

    private fun setupButtonClickListeners() {
        buttonBack.setOnClickListener {
            finish() // Simply close this activity
        }

        buttonChangePassword.setOnClickListener {
            handleChangePassword()
        }
    }

    private fun handleChangePassword() {
        val currentPassword = editTextCurrentPassword.text.toString() // No trim, passwords can have spaces
        val newPassword = editTextNewPassword.text.toString()
        val confirmNewPassword = editTextConfirmNewPassword.text.toString()

        // --- Validation ---
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all password fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 6) {
            Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmNewPassword) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // Verify current password (check against loggedInUsername which is confirmed not null here)
        val passwordKey = getUserPasswordKey(loggedInUsername!!)
        val storedPassword = userDataPreferences.getString(passwordKey, null)

        if (storedPassword == null || currentPassword != storedPassword) {
            Toast.makeText(this, "Incorrect current password", Toast.LENGTH_SHORT).show()
            return
        }

        // Prevent setting the same password
        if (newPassword == currentPassword) {
            Toast.makeText(this, "New password cannot be the same as the current password", Toast.LENGTH_SHORT).show()
            return
        }
        // --- End Validation ---


        // --- Update Password in SharedPreferences ---
        val editor = userDataPreferences.edit()
        editor.putString(passwordKey, newPassword) // Save the new password using the user-specific key
        editor.apply()

        Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show()
        finish() // Close the activity after successful change
    }

    // Helper to generate the user-specific password key
    private fun getUserPasswordKey(username: String): String {
        return "${username}${KEY_SUFFIX_PASSWORD}"
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}