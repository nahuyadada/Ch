package com.example.chowcheck.frag

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.chowcheck.R // Use correct R import
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

// Data class to hold information about a single logged food item
data class FoodEntry(
    val foodName: String,
    val calories: Int,
    val timestamp: Long, // Store time as milliseconds for easier sorting/formatting
    val mealType: String // "Breakfast", "Lunch", or "Dinner"
)

class FoodLogFragment : Fragment() {

    // --- Views ---
    private lateinit var textViewDate: TextView
    private lateinit var buttonAddBreakfast: ImageButton
    private lateinit var buttonAddLunch: ImageButton
    private lateinit var buttonAddDinner: ImageButton
    private lateinit var textViewWaterDetails: TextView
    // Layouts to hold logged items
    private lateinit var layoutBreakfastItems: LinearLayout
    private lateinit var layoutLunchItems: LinearLayout
    private lateinit var layoutDinnerItems: LinearLayout
    // TextViews for total meal calories
    private lateinit var textViewBreakfastTotalCalories: TextView
    private lateinit var textViewLunchTotalCalories: TextView
    private lateinit var textViewDinnerTotalCalories: TextView

    // --- SharedPreferences & User Data ---
    private lateinit var userDataPreferences: SharedPreferences
    private var loggedInUsername: String? = null
    private val gson = Gson() // For JSON conversion

    // --- Constants for SharedPreferences Keys ---
    companion object {
        const val USER_DATA_PREFS = "UserData" // Matches LoginActivity
        const val KEY_LOGGED_IN_USER = "logged_in_user" // Matches LoginActivity
        // Base key for daily food logs (append date and meal type)
        const val BASE_KEY_DAILY_FOOD = "daily_food"
        // Base key for total daily calories (append date) - used by DiaryFragment
        const val BASE_KEY_DAILY_CALORIES_EATEN = "daily_calories_eaten" // Matches DiaryFragment
        // Keys for specific meals
        const val MEAL_TYPE_BREAKFAST = "Breakfast"
        const val MEAL_TYPE_LUNCH = "Lunch"
        const val MEAL_TYPE_DINNER = "Dinner"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_food_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize SharedPreferences and get logged-in user
        userDataPreferences = requireContext().getSharedPreferences(USER_DATA_PREFS, Context.MODE_PRIVATE)
        loggedInUsername = userDataPreferences.getString(KEY_LOGGED_IN_USER, null)

        // Handle case where user is not logged in (optional, but good practice)
        if (loggedInUsername == null) {
            Toast.makeText(requireContext(), "Please log in to view food log.", Toast.LENGTH_SHORT).show()
            // Optionally redirect to login or disable interaction
            return
        }

        // Initialize views using the inflated view
        initializeViews(view)

        // Set current date
        setCurrentDate()

        // Load initial data (water, meals)
        loadAndDisplayWaterData() // Renamed for clarity
        loadAndDisplayMealData(MEAL_TYPE_BREAKFAST)
        loadAndDisplayMealData(MEAL_TYPE_LUNCH)
        loadAndDisplayMealData(MEAL_TYPE_DINNER)

        // Setup button listeners
        setupClickListeners()
    }

    // Helper to get the SharedPreferences key for a specific meal's data for the current day
    private fun getDailyMealDataKey(username: String, mealType: String): String {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        // e.g., "john_daily_food_2025-04-28_Breakfast"
        return "${username}_${BASE_KEY_DAILY_FOOD}_${todayDate}_$mealType"
    }

    // Helper to get the SharedPreferences key for total calories eaten today
    private fun getDailyTotalCaloriesKey(username: String) : String {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        // e.g., "john_daily_calories_eaten_2025-04-28"
        return "${username}_${BASE_KEY_DAILY_CALORIES_EATEN}_$todayDate"
    }


    private fun initializeViews(view: View) {
        textViewDate = view.findViewById(R.id.textViewDate)

        buttonAddBreakfast = view.findViewById(R.id.buttonAddBreakfast)
        buttonAddLunch = view.findViewById(R.id.buttonAddLunch)
        buttonAddDinner = view.findViewById(R.id.buttonAddDinner)

        layoutBreakfastItems = view.findViewById(R.id.layoutBreakfastItems)
        layoutLunchItems = view.findViewById(R.id.layoutLunchItems)
        layoutDinnerItems = view.findViewById(R.id.layoutDinnerItems)

        textViewBreakfastTotalCalories = view.findViewById(R.id.textViewBreakfastTotalCalories)
        textViewLunchTotalCalories = view.findViewById(R.id.textViewLunchTotalCalories)
        textViewDinnerTotalCalories = view.findViewById(R.id.textViewDinnerTotalCalories)
    }

    private fun setCurrentDate() {
        val dateFormat = SimpleDateFormat("d MMMM, EEEE", Locale.getDefault())
        textViewDate.text = dateFormat.format(Date())
    }

    private fun loadAndDisplayWaterData() {
        // --- Placeholder Data Loading (Replace with your actual logic if needed) ---
        val waterIntakeLiters = 0.9 // Replace with actual loaded value
        val waterGoalLiters = 1.5 // Replace with actual loaded value

        val waterProgressPercent = if (waterGoalLiters > 0) {
            (waterIntakeLiters / waterGoalLiters * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        textViewWaterDetails.text = "Water %.1fL (%d%%)".format(waterIntakeLiters, waterProgressPercent)
    }

    // Loads food entries for a specific meal and updates the UI
    private fun loadAndDisplayMealData(mealType: String) {
        if (loggedInUsername == null || !isAdded) return // Check fragment is attached

        val mealDataKey = getDailyMealDataKey(loggedInUsername!!, mealType)
        val jsonString = userDataPreferences.getString(mealDataKey, null)
        val foodList: MutableList<FoodEntry> = if (jsonString != null) {
            try {
                val type = object : TypeToken<MutableList<FoodEntry>>() {}.type
                gson.fromJson(jsonString, type) ?: mutableListOf() // Handle null fromJson result
            } catch (e: Exception) {
                // Handle potential JSON parsing errors
                // Log.e("FoodLogFragment", "Error parsing JSON for $mealType", e)
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        val itemsLayout: LinearLayout
        val totalCaloriesTextView: TextView

        when (mealType) {
            MEAL_TYPE_BREAKFAST -> {
                itemsLayout = layoutBreakfastItems
                totalCaloriesTextView = textViewBreakfastTotalCalories
            }
            MEAL_TYPE_LUNCH -> {
                itemsLayout = layoutLunchItems
                totalCaloriesTextView = textViewLunchTotalCalories
            }
            MEAL_TYPE_DINNER -> {
                itemsLayout = layoutDinnerItems
                totalCaloriesTextView = textViewDinnerTotalCalories
            }
            else -> return // Unknown meal type
        }

        // Clear previous items
        itemsLayout.removeAllViews()

        var totalCalories = 0
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()) // Format for display time

        // Sort by timestamp if needed (optional)
        // foodList.sortBy { it.timestamp }

        for (entry in foodList) {
            totalCalories += entry.calories

            // Inflate the list item layout
            val inflater = LayoutInflater.from(context) ?: return // Check context validity
            val itemView = inflater.inflate(R.layout.list_item_food, itemsLayout, false)

            // Find views within the item layout
            val textViewName = itemView.findViewById<TextView>(R.id.textViewFoodName)
            val textViewCalories = itemView.findViewById<TextView>(R.id.textViewFoodCalories)
            val textViewTime = itemView.findViewById<TextView>(R.id.textViewFoodTime)

            // Set data
            textViewName.text = entry.foodName
            textViewCalories.text = "${entry.calories} kcal"
            textViewTime.text = timeFormat.format(Date(entry.timestamp))

            // Add the item view to the layout
            itemsLayout.addView(itemView)
        }

        // Update the total calories display for the meal
        totalCaloriesTextView.text = "$totalCalories kcal"
    }

    // Saves a new food entry and updates the total daily calories
    private fun saveFoodEntry(entry: FoodEntry) {
        if (loggedInUsername == null) return

        val mealDataKey = getDailyMealDataKey(loggedInUsername!!, entry.mealType)
        val jsonString = userDataPreferences.getString(mealDataKey, null)
        val foodList: MutableList<FoodEntry> = if (jsonString != null) {
            try {
                val type = object : TypeToken<MutableList<FoodEntry>>() {}.type
                gson.fromJson(jsonString, type) ?: mutableListOf() // Handle null fromJson result
            } catch (e: Exception) {
                // Handle potential JSON parsing errors
                // Log.e("FoodLogFragment", "Error parsing JSON for saving $mealType", e)
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        foodList.add(entry)

        // Save updated list back to SharedPreferences
        val editor = userDataPreferences.edit()
        val updatedJsonString = gson.toJson(foodList)
        editor.putString(mealDataKey, updatedJsonString)

        // --- Update Total Daily Calories ---
        val totalCaloriesKey = getDailyTotalCaloriesKey(loggedInUsername!!)
        val currentTotalCalories = userDataPreferences.getInt(totalCaloriesKey, 0)
        val newTotalCalories = currentTotalCalories + entry.calories
        editor.putInt(totalCaloriesKey, newTotalCalories)
        // -----------------------------------

        editor.apply() // Apply changes

        // Refresh the display for the meal type that was updated
        loadAndDisplayMealData(entry.mealType)

        // Optional: Notify DiaryFragment to update its display immediately
        // This requires communication between fragments. DiaryFragment currently updates in onResume.
    }


    private fun setupClickListeners() {

        buttonAddBreakfast.setOnClickListener {
            showAddMealDialog(MEAL_TYPE_BREAKFAST)
        }

        buttonAddLunch.setOnClickListener {
            showAddMealDialog(MEAL_TYPE_LUNCH)
        }

        buttonAddDinner.setOnClickListener {
            showAddMealDialog(MEAL_TYPE_DINNER)
        }
    }

    // Shows the dialog to add a food item
    private fun showAddMealDialog(mealType: String) {
        if (!isAdded || context == null) return // Ensure fragment is attached and context is available

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_meal, null)
        val editTextFood = dialogView.findViewById<EditText>(R.id.edtFood)
        val editTextCalories = dialogView.findViewById<EditText>(R.id.edtCalories)

        AlertDialog.Builder(requireContext())
            .setTitle("Add $mealType")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val foodName = editTextFood.text.toString().trim()
                val caloriesStr = editTextCalories.text.toString().trim()

                if (foodName.isEmpty()) {
                    Toast.makeText(context, "Please enter food name", Toast.LENGTH_SHORT).show()
                    // Consider how to handle validation failure - maybe keep dialog open
                    // For simplicity, we let it close here. Could re-show dialog or use a different approach.
                } else {
                    val calories = caloriesStr.toIntOrNull()
                    if (calories == null || calories < 0) {
                        Toast.makeText(context, "Please enter valid calories (0 or more)", Toast.LENGTH_SHORT).show()
                        // Handle validation failure
                    } else {
                        // Validation passed, create and save the entry
                        val newEntry = FoodEntry(
                            foodName = foodName,
                            calories = calories,
                            timestamp = System.currentTimeMillis(), // Current time
                            mealType = mealType
                        )
                        saveFoodEntry(newEntry)
                        dialog.dismiss() // Dismiss only on success
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .setCancelable(false) // Prevent closing dialog on outside touch before explicit action
            .show()
    }


    // Method to refresh data from outside (if needed, e.g., after water log)
    fun refreshData() {
        if (isAdded && loggedInUsername != null) {
            loadAndDisplayWaterData()
            loadAndDisplayMealData(MEAL_TYPE_BREAKFAST)
            loadAndDisplayMealData(MEAL_TYPE_LUNCH)
            loadAndDisplayMealData(MEAL_TYPE_DINNER)
        }
    }
}