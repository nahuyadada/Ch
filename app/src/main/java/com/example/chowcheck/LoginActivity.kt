package com.example.chowcheck

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences // Import
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
// Import NotificationHelper if it's in the same package, otherwise use full path
// import com.example.chowcheck.util.NotificationHelper // Example if in util package

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

        // --- Create Notification Channel (Needs to be done once) --- ADD THIS ---
        NotificationHelper.createNotificationChannel(this)
        // -------------------------------------------------------------------------

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

            val passwordKey = getUserPasswordKey(username)
            val storedPassword = sharedPreferences.getString(passwordKey, null)

            if (storedPassword != null && password == storedPassword) {
                // Login Successful
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

                val editor = sharedPreferences.edit()
                editor.putString(KEY_LOGGED_IN_USER, username)
                editor.apply()

                // Navigate to Landing Activity
                val intent = Intent(this, LandingActivity::class.java)
                startActivity(intent)
                finish() // Finish LoginActivity
            } else {
                // Login Failed
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
            }
        }

        buttonRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getUserPasswordKey(username: String): String {
        return "${username}${KEY_SUFFIX_PASSWORD}"
    }
}