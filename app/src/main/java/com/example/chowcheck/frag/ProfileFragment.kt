package com.example.chowcheck.frag // Ensure this package is correct

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
// import android.widget.ImageButton // No longer needed if btnBack is removed
import android.widget.TextView
import android.widget.Toast
import com.example.chowcheck.LoginActivity // Import LoginActivity if needed for redirect
import com.example.chowcheck.R // Import R

class ProfileFragment : Fragment() {

    // Views
    private lateinit var editTextName: EditText
    private lateinit var editTextAge: EditText
    private lateinit var editTextHeight: EditText
    private lateinit var editTextWeight: EditText
    private lateinit var editTextWeightGoal: EditText
    private lateinit var buttonEditProfile: Button
    // private lateinit var buttonGoBack: ImageButton // Removed
    private lateinit var textViewUsername: TextView

    // SharedPreferences and Logged-in User
    private lateinit var userDataPreferences: SharedPreferences
    private var loggedInUsername: String? = null

    // Define base key names (consistent keys)
    companion object {
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user"
        const val BASE_KEY_NAME = "name"
        const val BASE_KEY_AGE = "age"
        const val BASE_KEY_HEIGHT = "height"
        const val BASE_KEY_WEIGHT = "weight"
        const val BASE_KEY_WEIGHT_GOAL = "weight_goal"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userDataPreferences = requireContext().getSharedPreferences(USER_DATA_PREFS, Context.MODE_PRIVATE)
        loggedInUsername = userDataPreferences.getString(KEY_LOGGED_IN_USER, null)

        if (loggedInUsername == null) {
            // Handle not logged in case
            Toast.makeText(requireContext(), "Please log in to view your profile.", Toast.LENGTH_LONG).show()
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
            return
        }

        initializeViews(view)
        loadUsername()
        loadProfileData()
        setupButtonClickListeners()
    }

    private fun getUserDataKey(username: String, baseKey: String): String {
        return "${username}_${baseKey}"
    }

    private fun initializeViews(view: View) {
        // Crash happens around here (line 101 was likely btnEditProfile)
        editTextName = view.findViewById(R.id.editTextName)
        editTextAge = view.findViewById(R.id.editTextAge)
        editTextHeight = view.findViewById(R.id.editTextHeight)
        editTextWeight = view.findViewById(R.id.editTextWeight)
        editTextWeightGoal = view.findViewById(R.id.editTextWeightGoal)
        buttonEditProfile = view.findViewById(R.id.btnEditProfile) // Still check this ID carefully!
        // buttonGoBack = view.findViewById(R.id.btnBack) // Removed initialization
        textViewUsername = view.findViewById(R.id.txtUsername) // Check this ID carefully!
    }

    private fun loadUsername() {
        textViewUsername.text = loggedInUsername ?: "Error"
    }

    private fun loadProfileData() {
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

    private fun saveDataToPrefs(baseKey: String, value: String) {
        if (loggedInUsername == null) return
        val editor = userDataPreferences.edit()
        editor.putString(getUserDataKey(loggedInUsername!!, baseKey), value)
        editor.apply()
    }

    private fun setupButtonClickListeners() {
        buttonEditProfile.setOnClickListener {
            if (loggedInUsername == null) {
                Toast.makeText(requireContext(), "Error: Cannot save profile. Not logged in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val name = editTextName.text.toString().trim()
            val age = editTextAge.text.toString().trim()
            val height = editTextHeight.text.toString().trim()
            val weight = editTextWeight.text.toString().trim()
            val weightGoal = editTextWeightGoal.text.toString().trim()

            if (!validateProfileInputs(name, age, height, weight, weightGoal)) {
                return@setOnClickListener
            }

            saveDataToPrefs(BASE_KEY_NAME, name)
            saveDataToPrefs(BASE_KEY_AGE, age)
            saveDataToPrefs(BASE_KEY_HEIGHT, height)
            saveDataToPrefs(BASE_KEY_WEIGHT, weight)
            saveDataToPrefs(BASE_KEY_WEIGHT_GOAL, weightGoal)

            Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
        }

        // Removed listener for buttonGoBack
        // buttonGoBack.setOnClickListener { ... }
    }

    private fun validateProfileInputs(name: String, ageStr: String, heightStr: String, weightStr: String, weightGoalStr: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter your name", Toast.LENGTH_SHORT).show(); return false
        }
        // Added check for non-numeric characters and range for age
        val age = ageStr.toIntOrNull()
        if (age == null || age <= 0 || age > 120) {
            Toast.makeText(requireContext(), "Please enter a valid age (1-120)", Toast.LENGTH_SHORT).show(); return false
        }
        // Added range checks for height/weight
        val height = heightStr.toDoubleOrNull()
        if (height == null || height <= 0 || height > 300) { // Assuming cm
            Toast.makeText(requireContext(), "Please enter a valid height (cm)", Toast.LENGTH_SHORT).show(); return false
        }
        val weight = weightStr.toDoubleOrNull()
        if (weight == null || weight <= 0 || weight > 500) { // Assuming kg
            Toast.makeText(requireContext(), "Please enter a valid weight (kg)", Toast.LENGTH_SHORT).show(); return false
        }
        val weightGoal = weightGoalStr.toDoubleOrNull()
        if (weightGoal == null || weightGoal <= 0 || weightGoal > 500) { // Assuming kg
            Toast.makeText(requireContext(), "Please enter a valid weight goal (kg)", Toast.LENGTH_SHORT).show(); return false
        }
        return true
    }
}