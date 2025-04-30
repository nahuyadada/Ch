package com.example.chowcheck

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class RegisterActivity : Activity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var buttonRegister: Button
    private lateinit var textViewLoginLink: TextView
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user"
        const val KEY_SUFFIX_PASSWORD = "_password"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        buttonRegister = findViewById(R.id.buttonRegister)
        textViewLoginLink = findViewById(R.id.textViewLoginLink)
        sharedPreferences = getSharedPreferences(USER_DATA_PREFS, MODE_PRIVATE)


        setupLoginLink()

        buttonRegister.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            val confirmPassword = editTextConfirmPassword.text.toString().trim()


            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (username.contains(" ") || username.length < 3) {
                Toast.makeText(this, "Username must be at least 3 characters and contain no spaces", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val passwordKey = getUserPasswordKey(username)
            if (sharedPreferences.contains(passwordKey)) {
                Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val editor = sharedPreferences.edit()
            editor.putString(passwordKey, password)

            editor.putString(KEY_LOGGED_IN_USER, username)

            editor.apply()

            Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, CalorieInfoActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun getUserPasswordKey(username: String): String {
        return "${username}${KEY_SUFFIX_PASSWORD}"
    }

    private fun setupLoginLink() {
        val loginText = "Already have an account? Log in"
        val spannableString = SpannableString(loginText)
        spannableString.setSpan(UnderlineSpan(), loginText.indexOf("Log in"), loginText.length, 0)
        textViewLoginLink.text = spannableString
        textViewLoginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

}