package com.example.chowcheck.frag

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText // Assuming you use standard EditText, adjust if using Material TextInputEditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.chowcheck.R
import com.example.chowcheck.viewmodel.DateViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt // To round calculated calories

// Data class (keep as before or move to shared location)
data class FoodEntry(
    val foodName: String,
    val calories: Int,
    val timestamp: Long,
    val mealType: String
)

class FoodLogFragment : Fragment() {

    // --- Views ---
    private lateinit var textViewDate: TextView
    private lateinit var buttonAddBreakfast: ImageButton
    private lateinit var buttonAddLunch: ImageButton
    private lateinit var buttonAddDinner: ImageButton
    private lateinit var layoutBreakfastItems: LinearLayout
    private lateinit var layoutLunchItems: LinearLayout
    private lateinit var layoutDinnerItems: LinearLayout
    private lateinit var textViewBreakfastTotalCalories: TextView
    private lateinit var textViewLunchTotalCalories: TextView
    private lateinit var textViewDinnerTotalCalories: TextView

    // --- SharedPreferences & User Data ---
    private lateinit var userDataPreferences: SharedPreferences
    private var loggedInUsername: String? = null
    private val gson = Gson()

    // --- Date Management ---
    // Get the Shared ViewModel instance
    private val dateViewModel: DateViewModel by activityViewModels()

    // Date formats (consistent with ViewModel and DiaryFragment)
    private val keyDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("d MMMM, EEEE", Locale.getDefault())

    // --- Simple Food Database (Placeholder) ---
    // Maps food name (case-insensitive) to calories per gram (Double)
    private val foodDatabase = mapOf(
        "chicken breast" to 1.65, // cooked, skinless
        "white rice" to 1.30,     // cooked
        "broccoli" to 0.34,       // raw
        "apple" to 0.52,          // raw
        "olive oil" to 9.00,
        "banana" to 0.89,
        "egg" to 1.55,            // average large egg
        "milk (whole)" to 0.61,
        "cheddar cheese" to 4.04

    )

    // --- Constants ---
    companion object {
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user"
        const val BASE_KEY_DAILY_FOOD = "daily_food" // Base for meal lists
        const val BASE_KEY_DAILY_CALORIES_EATEN = "daily_calories_eaten" // Base for daily total
        const val MEAL_TYPE_BREAKFAST = "Breakfast"
        const val MEAL_TYPE_LUNCH = "Lunch"
        const val MEAL_TYPE_DINNER = "Dinner"
        const val TAG = "FoodLogFragment" // For logging
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_food_log, container, false)
        initializeViews(view) // Initialize views right after inflating
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userDataPreferences = requireContext().getSharedPreferences(USER_DATA_PREFS, Context.MODE_PRIVATE)
        loggedInUsername = userDataPreferences.getString(KEY_LOGGED_IN_USER, null)

        if (loggedInUsername == null) {
            Toast.makeText(requireContext(), "Please log in to view food log.", Toast.LENGTH_SHORT).show()
            // Optionally disable UI or redirect
            return
        }

        setupClickListeners()

        // --- Observe the selectedDate LiveData from the ViewModel ---
        dateViewModel.selectedDate.observe(viewLifecycleOwner, Observer { calendar ->
            Log.d(TAG, "Observed date change: ${keyDateFormat.format(calendar.time)}")
            // When the date changes, update the display and reload meal data
            updateDateDisplay(calendar)
            loadAllMealData(calendar)
        })

        // Initial load using the current value from ViewModel
        // The observer will also trigger this once attached.
        // val initialDate = dateViewModel.getCurrentSelectedDate()
        // updateDateDisplay(initialDate)
        // loadAllMealData(initialDate)
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

    // Updates the date display TextView
    private fun updateDateDisplay(dateToDisplay: Calendar) {
        // Add "Today", "Yesterday" logic if desired, similar to DiaryFragment
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val displayStr = when {
            isSameDay(dateToDisplay, today) -> "Today"
            isSameDay(dateToDisplay, yesterday) -> "Yesterday"
            else -> displayDateFormat.format(dateToDisplay.time)
        }
        textViewDate.text = displayStr
    }

    // Helper to load data for all meals for the specified date
    private fun loadAllMealData(dateToLoad: Calendar) {
        Log.d(TAG, "Loading all meal data for: ${keyDateFormat.format(dateToLoad.time)}")
        loadAndDisplayMealData(MEAL_TYPE_BREAKFAST, dateToLoad)
        loadAndDisplayMealData(MEAL_TYPE_LUNCH, dateToLoad)
        loadAndDisplayMealData(MEAL_TYPE_DINNER, dateToLoad)
    }


    // --- Key Generation (Uses the passed-in date) ---
    private fun getDailyMealDataKey(username: String, mealType: String, date: Calendar): String {
        val dateKeyString = keyDateFormat.format(date.time)
        return "${username}_${BASE_KEY_DAILY_FOOD}_${dateKeyString}_$mealType"
    }

    private fun getDailyTotalCaloriesKey(username: String, date: Calendar): String {
        val dateKeyString = keyDateFormat.format(date.time)
        return "${username}_${BASE_KEY_DAILY_CALORIES_EATEN}_$dateKeyString"
    }

    // --- Data Loading (Now accepts the date to load) ---
    private fun loadAndDisplayMealData(mealType: String, dateToLoad: Calendar) {
        if (loggedInUsername == null || !isAdded) return

        val mealDataKey = getDailyMealDataKey(loggedInUsername!!, mealType, dateToLoad)
        val jsonString = userDataPreferences.getString(mealDataKey, null)
        val foodList: MutableList<FoodEntry> = if (jsonString != null) {
            try {
                val type = object : TypeToken<MutableList<FoodEntry>>() {}.type
                gson.fromJson(jsonString, type) ?: mutableListOf()
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing JSON for $mealType on ${keyDateFormat.format(dateToLoad.time)}", e)
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        val itemsLayout: LinearLayout
        val totalCaloriesTextView: TextView

        when (mealType) {
            MEAL_TYPE_BREAKFAST -> { itemsLayout = layoutBreakfastItems; totalCaloriesTextView = textViewBreakfastTotalCalories }
            MEAL_TYPE_LUNCH -> { itemsLayout = layoutLunchItems; totalCaloriesTextView = textViewLunchTotalCalories }
            MEAL_TYPE_DINNER -> { itemsLayout = layoutDinnerItems; totalCaloriesTextView = textViewDinnerTotalCalories }
            else -> return
        }

        itemsLayout.removeAllViews() // Clear previous items

        var totalCalories = 0
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        for (entry in foodList) {
            totalCalories += entry.calories
            val inflater = LayoutInflater.from(context) ?: return
            val itemView = inflater.inflate(R.layout.list_item_food, itemsLayout, false)
            // Find and set views in itemView (textViewName, textViewCalories, textViewTime)
            itemView.findViewById<TextView>(R.id.textViewFoodName).text = entry.foodName
            itemView.findViewById<TextView>(R.id.textViewFoodCalories).text = "${entry.calories} kcal"
            itemView.findViewById<TextView>(R.id.textViewFoodTime).text = timeFormat.format(Date(entry.timestamp))
            itemsLayout.addView(itemView)
        }

        totalCaloriesTextView.text = "$totalCalories kcal"
    }

    // --- Data Saving ---
    private fun saveFoodEntry(entry: FoodEntry) {
        if (loggedInUsername == null) return
        val dateToSave = dateViewModel.getCurrentSelectedDate() // Get the date from ViewModel

        val mealDataKey = getDailyMealDataKey(loggedInUsername!!, entry.mealType, dateToSave)
        val jsonString = userDataPreferences.getString(mealDataKey, null)
        val foodList: MutableList<FoodEntry> = if (jsonString != null) {
            try {
                val type = object : TypeToken<MutableList<FoodEntry>>() {}.type
                gson.fromJson(jsonString, type) ?: mutableListOf()
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing JSON for saving ${entry.mealType} on ${keyDateFormat.format(dateToSave.time)}", e)
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        foodList.add(entry)
        foodList.sortBy { it.timestamp } // Optional: keep sorted by time added

        val editor = userDataPreferences.edit()
        val updatedJsonString = gson.toJson(foodList)
        editor.putString(mealDataKey, updatedJsonString)

        // --- Update Total Daily Calories for the correct date ---
        val totalCaloriesKey = getDailyTotalCaloriesKey(loggedInUsername!!, dateToSave) // Use correct date
        val currentTotalCalories = userDataPreferences.getInt(totalCaloriesKey, 0)
        val newTotalCalories = currentTotalCalories + entry.calories
        editor.putInt(totalCaloriesKey, newTotalCalories)
        // ---------------------------------------------------------

        editor.apply()

        // Refresh the display for the meal type that was updated for the specific date
        loadAndDisplayMealData(entry.mealType, dateToSave)
        Log.d(TAG, "Saved ${entry.foodName} to ${entry.mealType} for ${keyDateFormat.format(dateToSave.time)}. New total: $newTotalCalories")

        // Optional: Could use a SharedFlow or similar in ViewModel to notify DiaryFragment immediately
    }


    private fun setupClickListeners() {
        buttonAddBreakfast.setOnClickListener { showAddMealDialog(MEAL_TYPE_BREAKFAST) }
        buttonAddLunch.setOnClickListener { showAddMealDialog(MEAL_TYPE_LUNCH) }
        buttonAddDinner.setOnClickListener { showAddMealDialog(MEAL_TYPE_DINNER) }
    }

    // Shows the dialog to add a food item
    private fun showAddMealDialog(mealType: String) {
        if (!isAdded || context == null) return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_meal, null)
        val editTextFood = dialogView.findViewById<EditText>(R.id.edtFood)
        val editTextGrams = dialogView.findViewById<EditText>(R.id.edtGrams) // New grams field
        val editTextCalories = dialogView.findViewById<EditText>(R.id.edtCalories)
        val dateToLog = dateViewModel.getCurrentSelectedDate() // Get date from ViewModel

        AlertDialog.Builder(requireContext())
            .setTitle("Add $mealType for ${keyDateFormat.format(dateToLog.time)}") // Show date in title
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val foodName = editTextFood.text.toString().trim()
                val gramsStr = editTextGrams.text.toString().trim() // Get grams input
                val caloriesStr = editTextCalories.text.toString().trim()

                if (foodName.isEmpty()) {
                    Toast.makeText(context, "Please enter food name", Toast.LENGTH_SHORT).show()
                    // Keep dialog open? Requires custom dialog handling
                    return@setPositiveButton // Prevent dialog dismissal
                }

                var calculatedCalories: Int? = null

                // 1. Try to calculate calories from grams if grams are provided
                val grams = gramsStr.toDoubleOrNull()
                if (grams != null && grams >= 0) {
                    val calorieDensity = foodDatabase[foodName.toLowerCase(Locale.getDefault())] // Lookup case-insensitively
                    if (calorieDensity != null) {
                        calculatedCalories = (grams * calorieDensity).roundToInt() // Calculate and round
                        Log.d(TAG, "Calculated calories for $foodName ($grams g): $calculatedCalories kcal")
                    } else {
                        // Food found in DB but grams entered, but food not in our *specific* calorie density map (shouldn't happen if map is used).
                        // More likely: Food not found in the DB at all.
                        Toast.makeText(context, "Food '$foodName' not found in database. Please enter calories manually.", Toast.LENGTH_LONG).show()
                        // Keep dialog open? Or rely on the manual entry fallback below.
                        // For simplicity here, we let it fall through to manual entry check.
                        return@setPositiveButton // Prevent dialog dismissal if food not found but grams entered
                    }
                }

                // 2. If calories weren't calculated from grams (grams empty/invalid, or food not in db),
                //    try to use the manually entered calories.
                val finalCalories = calculatedCalories ?: caloriesStr.toIntOrNull()

                if (finalCalories == null || finalCalories < 0) {
                    // Failed to calculate from grams, and manual calories are invalid/missing
                    Toast.makeText(context, "Please enter valid grams (or calories manually if food not found)", Toast.LENGTH_LONG).show()
                    return@setPositiveButton // Prevent dialog dismissal
                }

                // --- If we reach here, we have a valid food name and valid calorie count ---

                val newEntry = FoodEntry(
                    foodName = foodName,
                    calories = finalCalories,
                    timestamp = System.currentTimeMillis(), // Use current time for the entry timestamp itself
                    mealType = mealType
                )
                saveFoodEntry(newEntry) // Save function uses ViewModel date for keys
                dialog.dismiss()

            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .setCancelable(false)
            .show()
    }

    // Helper to check if two Calendar instances represent the same day
    private fun isSameDay(cal1: Calendar?, cal2: Calendar?): Boolean {
        if (cal1 == null || cal2 == null) return false
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

}