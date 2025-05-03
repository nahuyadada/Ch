package com.example.chowcheck.frag // Ensure this package is correct

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController // Import for Navigation Component
import com.example.chowcheck.EditProfileActivity
import com.example.chowcheck.LoginActivity
import com.example.chowcheck.R
import com.example.chowcheck.SettingsActivity
import java.text.NumberFormat // For formatting numbers

class ProfileFragment : Fragment() {

    // --- Views for the new layout ---
    // Header/Profile Info
    private lateinit var profilePicture: ImageView // Keep ImageView if needed, make sure it's ShapeableImageView in XML
    private lateinit var textViewUsername: TextView
    // Stats
    private lateinit var statWeightChangeValue: TextView
    private lateinit var statCurrentWeightValue: TextView
    // Option Rows (LinearLayouts)
    private lateinit var optionEditProfile: LinearLayout
    // NEW: BMI Option
    private lateinit var optionBmi: LinearLayout
    private lateinit var optionGoals: LinearLayout // This one exists but seems commented out in your setup
    private lateinit var optionProgress: LinearLayout
    private lateinit var optionSettings: LinearLayout
    private lateinit var optionLogout: LinearLayout
    // --- End Views ---


    // SharedPreferences and Logged-in User
    private lateinit var userDataPreferences: SharedPreferences
    private var loggedInUsername: String? = null

    // Define base key names (consistent keys) - Keep these
    companion object {
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user"
        const val BASE_KEY_NAME = "name" // Still needed for display maybe?
        const val BASE_KEY_AGE = "age"
        const val BASE_KEY_HEIGHT = "height"
        const val BASE_KEY_WEIGHT = "weight"
        const val BASE_KEY_WEIGHT_GOAL = "weight_goal"
        const val BASE_KEY_STARTING_WEIGHT = "starting_weight" // Needed for stats
        const val TAG = "ProfileFragment" // For logging
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // *** Inflate the NEW layout (the one resembling the picture) ***
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userDataPreferences = requireContext().getSharedPreferences(USER_DATA_PREFS, Context.MODE_PRIVATE)
        loggedInUsername = userDataPreferences.getString(KEY_LOGGED_IN_USER, null)

        if (loggedInUsername == null) {
            handleNotLoggedIn()
            return
        }

        initializeViews(view)
        loadUsername()
        loadStatsData() // Load stats instead of editable profile data
        setupOptionClickListeners(view) // Setup listeners for the new rows
    }

    // Reload data when returning to the fragment (e.g., after editing profile)
    override fun onResume() {
        super.onResume()
        if (isAdded && loggedInUsername != null) {
            loadUsername() // Reload username in case it changed (if editable)
            loadStatsData() // Reload stats in case weight changed
        }
    }


    private fun getUserDataKey(username: String, baseKey: String): String {
        return "${username}_${baseKey}"
    }

    private fun initializeViews(view: View) {
        // Initialize views from the NEW layout
        profilePicture = view.findViewById(R.id.profilePicture)
        textViewUsername = view.findViewById(R.id.txtUsername)
        statWeightChangeValue = view.findViewById(R.id.statWeightChangeValue)
        statCurrentWeightValue = view.findViewById(R.id.statCurrentWeightValue)

        // Find the LinearLayout containers for the options
        optionEditProfile = view.findViewById(R.id.optionEditProfile)
        optionBmi = view.findViewById(R.id.optionBmi) // Initialize the new BMI option view
        optionProgress = view.findViewById(R.id.optionProgress)
        optionSettings = view.findViewById(R.id.optionSettings)
        optionLogout = view.findViewById(R.id.optionLogout)
    }

    private fun loadUsername() {
        // Optionally load the actual name if you want to display it somewhere
        // val savedName = userDataPreferences.getString(getUserDataKey(loggedInUsername!!, BASE_KEY_NAME), loggedInUsername)
        textViewUsername.text = loggedInUsername ?: "Error"
    }

    // New function to load and display stats
    private fun loadStatsData() {
        if (loggedInUsername == null) return

        val currentWeightStr = userDataPreferences.getString(getUserDataKey(loggedInUsername!!, BASE_KEY_WEIGHT), null)
        val startingWeightStr = userDataPreferences.getString(getUserDataKey(loggedInUsername!!, BASE_KEY_STARTING_WEIGHT), null)

        val currentWeight = currentWeightStr?.toDoubleOrNull()
        val startingWeight = startingWeightStr?.toDoubleOrNull()

        // Display Current Weight
        if (currentWeight != null) {
            statCurrentWeightValue.text = "${String.format("%.1f", currentWeight)} kg"
        } else {
            statCurrentWeightValue.text = "-- kg"
        }

        // Display Weight Change
        if (currentWeight != null && startingWeight != null) {
            val change = currentWeight - startingWeight
            val nf = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
            val formattedChange = nf.format(change)
            // Add '+' sign for positive change
            statWeightChangeValue.text = if (change >= 0) "+$formattedChange kg" else "$formattedChange kg"
        } else {
            statWeightChangeValue.text = "- kg"
        }
    }

    // New function to setup listeners for the option rows
    private fun setupOptionClickListeners(view: View) {
        // Setup "Edit Profile" Option
        setupOptionRow(optionEditProfile, R.drawable.edit, "Edit Profile") {
            // Navigate to EditProfileActivity
            val intent = Intent(requireActivity(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Setup "BMI" Option
        setupOptionRow(optionBmi, R.drawable.body_mass_index, "BMI") { // Make sure you have a drawable named 'body_mass_index' or similar
            // Navigate to BmiFragment using Nav Component
            try {
                // ** IMPORTANT: Define this action in your main_nav.xml **
                // <fragment android:id="@+id/profileFragment" ...>
                //      <action android:id="@+id/action_profileFragment_to_bmiFragment"
                //              app:destination="@id/bmiFragment" />
                // </fragment>
                findNavController().navigate(R.id.action_profileFragment_to_bmiFragment)
            } catch (e: Exception) {
                Log.e(TAG, "Navigation to BMI failed: ${e.message}")
                Toast.makeText(context, "Could not open BMI page", Toast.LENGTH_SHORT).show()
            }
        }


        // Setup "Goals" Option
        // setupOptionRow(optionGoals, R.drawable.goals_icon, "Goals") { /* Goals Navigation/Action */ }


        // Setup "Progress" Option
        setupOptionRow(optionProgress, R.drawable.results, "Progress") {
            // Navigate to ResultsTabFragment using Nav Component
            try {
                // ** IMPORTANT: Define this action in your main_nav.xml **
                // <fragment android:id="@+id/profileFragment" ...>
                //      <action android:id="@+id/action_profileFragment_to_resultsTabFragment"
                //              app:destination="@id/resultsTabFragment" />
                // </fragment>
                findNavController().navigate(R.id.action_profileFragment_to_resultsTabFragment)
            } catch (e: Exception) {
                Log.e(TAG, "Navigation to Progress failed: ${e.message}")
                Toast.makeText(context, "Could not open Progress", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup "Settings" Option
        setupOptionRow(optionSettings, R.drawable.settings, "Settings") {
            // Navigate to SettingsActivity
            val intent = Intent(requireActivity(), SettingsActivity::class.java)
            startActivity(intent)
        }

        // Setup "Logout" Option
        setupOptionRow(optionLogout, R.drawable.logoutart, "Logout") {
            showLogoutDialog()
        }
    }

    // Helper function to configure each option row
    private fun setupOptionRow(layout: LinearLayout?, iconResId: Int, title: String, onClickAction: () -> Unit) {
        // Added null check for layout just in case include ID is wrong
        if (layout == null) {
            Log.e(TAG, "Layout for option '$title' not found!")
            return
        }
        try {
            val icon = layout.findViewById<ImageView>(R.id.optionIcon)
            val text = layout.findViewById<TextView>(R.id.optionTitle)
            icon.setImageResource(iconResId)
            text.text = title
            layout.setOnClickListener { onClickAction() }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up option row for '$title': ${e.message}")
            // Hide the row if setup fails?
            layout.visibility = View.GONE
        }
    }

    // Logout Dialog and Logic (similar to SettingsActivity)
    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                // Clear logged-in user status
                val editor = userDataPreferences.edit()
                editor.remove(KEY_LOGGED_IN_USER)
                // Optionally remove other user-specific data
                editor.apply()

                // Navigate to LoginActivity and clear task stack
                val intent = Intent(requireActivity(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finishAffinity() // Finish this activity and parents
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun handleNotLoggedIn() {
        // Redirect or show appropriate UI when not logged in
        if (isAdded) { // Check if fragment is attached before showing toast/intent
            Toast.makeText(requireContext(), "Please log in.", Toast.LENGTH_LONG).show()
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish() // Finish the current activity holding this fragment
        }
    }

    // REMOVED: saveDataToPrefs, setupButtonClickListeners (for edit button), validateProfileInputs
    // REMOVED: loadProfileData (loading into EditTexts)
}