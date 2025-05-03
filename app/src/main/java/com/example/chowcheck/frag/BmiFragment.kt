package com.example.chowcheck.frag

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.chowcheck.R
import java.text.DecimalFormat // For formatting BMI

class BmiFragment : Fragment() {

    private lateinit var textViewBmiWeight: TextView
    private lateinit var textViewBmiHeight: TextView
    private lateinit var textViewBmiValue: TextView
    private lateinit var textViewBmiCategory: TextView

    private lateinit var userDataPreferences: SharedPreferences
    private var loggedInUsername: String? = null

    // Define base key names (consistent keys) - Use same keys as ProfileFragment
    companion object {
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user"
        const val BASE_KEY_HEIGHT = "height" // Assuming height is stored in cm
        const val BASE_KEY_WEIGHT = "weight" // Assuming weight is stored in kg
        const val TAG = "BmiFragment" // For logging
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bmi, container, false)
        initializeViews(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userDataPreferences = requireContext().getSharedPreferences(USER_DATA_PREFS, Context.MODE_PRIVATE)
        loggedInUsername = userDataPreferences.getString(KEY_LOGGED_IN_USER, null)

        if (loggedInUsername == null) {
            // Handle case where user is not logged in (shouldn't happen if access is from ProfileFragment after login,
            // but good practice to check).
            Toast.makeText(requireContext(), "Please log in to see BMI.", Toast.LENGTH_SHORT).show()
            // Optionally navigate back or hide content
            // findNavController().navigateUp() // Example to go back
            return
        }

        loadAndCalculateBmi()
    }

    // Reload data if fragment is resumed (e.g., user goes to Edit Profile, saves new height/weight, and comes back)
    override fun onResume() {
        super.onResume()
        if (isAdded && loggedInUsername != null) {
            loadAndCalculateBmi() // Recalculate and display if data might have changed
        }
    }


    private fun initializeViews(view: View) {
        textViewBmiWeight = view.findViewById(R.id.textViewBmiWeight)
        textViewBmiHeight = view.findViewById(R.id.textViewBmiHeight)
        textViewBmiValue = view.findViewById(R.id.textViewBmiValue)
        textViewBmiCategory = view.findViewById(R.id.textViewBmiCategory)
    }

    private fun getUserDataKey(username: String, baseKey: String): String {
        return "${username}_${baseKey}"
    }

    private fun loadAndCalculateBmi() {
        if (loggedInUsername == null) return

        val heightCmStr = userDataPreferences.getString(getUserDataKey(loggedInUsername!!, BASE_KEY_HEIGHT), null)
        val weightKgStr = userDataPreferences.getString(getUserDataKey(loggedInUsername!!, BASE_KEY_WEIGHT), null)

        val heightCm = heightCmStr?.toDoubleOrNull()
        val weightKg = weightKgStr?.toDoubleOrNull()

        // Display raw values
        textViewBmiHeight.text = if (heightCm != null) "${String.format("%.0f", heightCm)} cm" else "-- cm"
        textViewBmiWeight.text = if (weightKg != null) "${String.format("%.1f", weightKg)} kg" else "-- kg"


        if (heightCm != null && weightKg != null && heightCm > 0 && weightKg > 0) {
            // Calculate BMI
            val heightM = heightCm / 100.0 // Convert cm to meters
            val bmi = weightKg / (heightM * heightM)

            // Display BMI value (formatted to one decimal place)
            val df = DecimalFormat("#.#") // Use DecimalFormat for more control
            textViewBmiValue.text = df.format(bmi)

            // Determine and display BMI category
            textViewBmiCategory.text = getBmiCategory(bmi)

        } else {
            // Data is missing or invalid
            textViewBmiValue.text = "--"
            textViewBmiCategory.text = "Please complete your profile" // Indicate missing data
            Log.w(TAG, "Height ($heightCm) or Weight ($weightKg) data is missing or invalid for BMI calculation.")
            // Optionally, provide a hint to edit profile
            // Toast.makeText(requireContext(), "Please add your Height and Weight in Edit Profile to see your BMI.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getBmiCategory(bmi: Double): String {
        return when {
            bmi < 18.5 -> "Underweight"
            bmi >= 18.5 && bmi < 25 -> "Normal weight"
            bmi >= 25 && bmi < 30 -> "Overweight"
            bmi >= 30 && bmi < 35 -> "Obesity Class I"
            bmi >= 35 && bmi < 40 -> "Obesity Class II"
            bmi >= 40 -> "Obesity Class III"
            else -> "Unknown" // Should not happen with valid numbers
        }
    }
}