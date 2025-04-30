package com.example.chowcheck

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences // Import
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class LoginActivity : Activity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonRegister: Button
    private lateinit var sharedPreferences: SharedPreferences // Added

    // Define constants consistent with RegisterActivity
    companion object {
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user"
        const val KEY_SUFFIX_PASSWORD = "_password"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonRegister = findViewById(R.id.buttonRegister)
        sharedPreferences = getSharedPreferences(USER_DATA_PREFS, MODE_PRIVATE) // Initialize

        buttonLogin.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter your username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- Check credentials against user-specific key ---
            val passwordKey = getUserPasswordKey(username) // e.g., "john_password"
            val storedPassword = sharedPreferences.getString(passwordKey, null) // Get password for this user

            if (storedPassword != null && password == storedPassword) {
                // --- Login Successful ---
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

                // --- Save the currently logged-in user ---
                val editor = sharedPreferences.edit()
                editor.putString(KEY_LOGGED_IN_USER, username)
                editor.apply()

                // --- Navigate to Landing Activity ---
                val intent = Intent(this, LandingActivity::class.java)
                startActivity(intent)
                finish() // Finish LoginActivity
            } else {
                // --- Login Failed ---
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
            }
        }

        buttonRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            // Should not finish() here, allow user to go back to Login if they cancel registration
        }
    }

    // Helper to generate the user-specific password key
    private fun getUserPasswordKey(username: String): String {
        return "${username}${KEY_SUFFIX_PASSWORD}"
    }
}