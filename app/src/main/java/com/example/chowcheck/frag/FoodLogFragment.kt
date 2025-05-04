package com.example.chowcheck.frag // Adjust if needed

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface // Import DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button // Import Button for Manage Foods
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible // For visibility extension
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.chowcheck.R
import com.example.chowcheck.viewmodel.DateViewModel
import com.google.android.material.button.MaterialButton // Import MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout // Import TextInputLayout for errors
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
    private lateinit var buttonManageFoodDefinitions: MaterialButton // Added Button
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
    private val dateViewModel: DateViewModel by activityViewModels()
    private val keyDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("d MMMM, EEEE", Locale.getDefault())

    // --- Food Database (Mutable and Persistent) ---
    private val initialFoodDatabase = mapOf(
        "chicken breast" to 1.65, "white rice" to 1.30, "broccoli" to 0.34, "apple" to 0.52,
        "olive oil" to 9.00, "banana" to 0.89, "egg" to 1.55, "milk (whole)" to 0.61,
        "cheddar cheese" to 4.04, "brown rice" to 1.12, "pasta" to 1.58, "bread white slice" to 2.65,
        "bread whole wheat slice" to 2.50, "salmon fillet" to 2.08, "ground beef lean" to 1.80,
        "pork chop lean" to 1.65, "potato" to 0.77, "sweet potato" to 0.90, "yogurt plain" to 0.61,
        "milk skim" to 0.35, "mozzarella cheese" to 2.80, "orange" to 0.47, "carrots" to 0.41,
        "onion" to 0.40, "tomato" to 0.18, "peanut butter" to 5.88, "almonds" to 5.79,
        "beef sirloin steak" to 2.20, "tilapia" to 1.05, "bangus" to 1.80, "galunggong" to 1.35,
        "kangkong" to 0.25, "malunggay leaves" to 0.75, "coconut milk" to 2.30, "tofu firm" to 0.70,
        "chicken thigh" to 1.70, "pork belly" to 5.00, "shrimp" to 0.99, "tuna canned in water" to 1.16,
        "lentils cooked" to 1.16, "chickpeas cooked" to 1.64, "quinoa cooked" to 1.20,
        "oatmeal cooked" to 0.68, "soy sauce" to 0.53, "fish sauce" to 0.40, "sugar white" to 3.87,
        "honey" to 3.04, "mango" to 0.60, "pineapple" to 0.50, "watermelon" to 0.30,
        "cucumber" to 0.15, "lettuce" to 0.15, "bell pepper" to 0.31, "ginger" to 0.80,
        "garlic" to 1.49, "coconut oil" to 8.62, "butter" to 7.17, "cream cheese" to 3.42,
        "sour cream" to 1.98, "ice cream vanilla" to 2.07, "chocolate milk whole" to 0.83,
        "coffee black" to 0.02, "tea black unsweetened" to 0.01, "cola regular" to 0.41,
        "orange juice" to 0.45, "red wine" to 0.85, "beer regular" to 0.43, "pizza cheese slice" to 2.67,
        "burger single patty" to 2.50, "french fries" to 3.20, "doughnut glazed" to 4.17,
        "cookie chocolate chip" to 4.90, "potato chips" to 5.36, "pretzels" to 3.80
        // Add more initial foods here if needed
    )
    private lateinit var foodDatabase: MutableMap<String, Double>
    private lateinit var foodNameAdapter: ArrayAdapter<String>


    // --- Constants ---
    companion object {
        const val USER_DATA_PREFS = "UserData"
        const val KEY_LOGGED_IN_USER = "logged_in_user"
        const val BASE_KEY_DAILY_FOOD = "daily_food"
        const val BASE_KEY_DAILY_CALORIES_EATEN = "daily_calories_eaten"
        const val KEY_USER_CUSTOM_FOOD_DB = "user_custom_food_db"
        const val MEAL_TYPE_BREAKFAST = "Breakfast"
        const val MEAL_TYPE_LUNCH = "Lunch"
        const val MEAL_TYPE_DINNER = "Dinner"
        const val TAG = "FoodLogFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_food_log, container, false)
        initializeViews(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userDataPreferences = requireContext().getSharedPreferences(USER_DATA_PREFS, Context.MODE_PRIVATE)
        loggedInUsername = userDataPreferences.getString(KEY_LOGGED_IN_USER, null)

        if (loggedInUsername == null) {
            Toast.makeText(requireContext(), "Please log in", Toast.LENGTH_SHORT).show()
            view.isVisible = false // Hide content if not logged in
            return
        }

        // Load the combined food database (initial + custom)
        loadFoodDatabase()
        // Initialize the adapter for AutoCompleteTextView
        setupFoodNameAdapter()

        setupClickListeners()

        dateViewModel.selectedDate.observe(viewLifecycleOwner, Observer { calendar ->
            Log.d(TAG, "Observed date change: ${keyDateFormat.format(calendar.time)}")
            updateDateDisplay(calendar)
            loadAllMealData(calendar)
        })

        // Initial load for the current date in ViewModel
        val initialDate = dateViewModel.getCurrentSelectedDate()
        updateDateDisplay(initialDate)
        loadAllMealData(initialDate)
    }

    private fun initializeViews(view: View) {
        textViewDate = view.findViewById(R.id.textViewDate)
        buttonAddBreakfast = view.findViewById(R.id.buttonAddBreakfast)
        buttonAddLunch = view.findViewById(R.id.buttonAddLunch)
        buttonAddDinner = view.findViewById(R.id.buttonAddDinner)
        buttonManageFoodDefinitions = view.findViewById(R.id.buttonManageFoodDefinitions) // Init manage button
        layoutBreakfastItems = view.findViewById(R.id.layoutBreakfastItems)
        layoutLunchItems = view.findViewById(R.id.layoutLunchItems)
        layoutDinnerItems = view.findViewById(R.id.layoutDinnerItems)
        textViewBreakfastTotalCalories = view.findViewById(R.id.textViewBreakfastTotalCalories)
        textViewLunchTotalCalories = view.findViewById(R.id.textViewLunchTotalCalories)
        textViewDinnerTotalCalories = view.findViewById(R.id.textViewDinnerTotalCalories)
    }

    // --- Food Database Management ---

    private fun getCustomFoodDbKey(username: String): String {
        return "${username}_$KEY_USER_CUSTOM_FOOD_DB"
    }

    private fun loadFoodDatabase() {
        foodDatabase = initialFoodDatabase.toMutableMap() // Start fresh each time
        val username = loggedInUsername ?: return // Need username

        val customDbKey = getCustomFoodDbKey(username)
        val jsonString = userDataPreferences.getString(customDbKey, null)
        if (jsonString != null) {
            try {
                val type = object : TypeToken<Map<String, Double>>() {}.type
                val customFoods: Map<String, Double> = gson.fromJson(jsonString, type) ?: emptyMap()
                foodDatabase.putAll(customFoods) // Merge custom foods
                Log.d(TAG, "Loaded ${customFoods.size} custom food entries for $username.")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing custom food database JSON for $username", e)
            }
        } else {
            Log.d(TAG,"No custom food database found for user $username")
        }
        Log.d(TAG, "Total food items in database: ${foodDatabase.size}")
    }

    private fun saveCustomFoodDatabase() {
        val username = loggedInUsername ?: run {
            Log.w(TAG,"Cannot save custom food DB, user not logged in.")
            return
        }

        try {
            val jsonString = gson.toJson(foodDatabase) // Save the whole map
            val customDbKey = getCustomFoodDbKey(username)
            userDataPreferences.edit().putString(customDbKey, jsonString).apply()
            Log.d(TAG, "Saved ${foodDatabase.size} food entries to custom DB for user $username.")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving custom food database JSON for $username", e)
        }
    }

    private fun setupFoodNameAdapter() {
        val foodNames = foodDatabase.keys.map { it.capitalizeWords() }.sorted()
        // Ensure context is available
        if (context != null) {
            foodNameAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, foodNames)
        } else {
            Log.e(TAG, "Context is null during setupFoodNameAdapter")
            // Initialize with empty adapter to avoid crash, though suggestions won't work
            foodNameAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        }
    }

    private fun updateFoodNameAdapter() {
        if (!::foodNameAdapter.isInitialized) {
            Log.w(TAG, "Adapter not initialized, cannot update.")
            setupFoodNameAdapter() // Try to set it up again
            if (!::foodNameAdapter.isInitialized) return // Still failed
        }
        val foodNames = foodDatabase.keys.map { it.capitalizeWords() }.sorted()
        // Use Handler or runOnUiThread if this could be called from a background thread (though unlikely here)
        activity?.runOnUiThread {
            foodNameAdapter.clear()
            foodNameAdapter.addAll(foodNames)
            foodNameAdapter.notifyDataSetChanged()
            Log.d(TAG, "Food name adapter updated with ${foodNames.size} items.")
        }
    }


    // --- Date and UI Update ---
    private fun updateDateDisplay(dateToDisplay: Calendar) {
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val displayStr = when {
            isSameDay(dateToDisplay, today) -> "Today"
            isSameDay(dateToDisplay, yesterday) -> "Yesterday"
            else -> displayDateFormat.format(dateToDisplay.time)
        }
        textViewDate.text = displayStr
    }

    private fun loadAllMealData(dateToLoad: Calendar) {
        if (loggedInUsername == null) return // Check login status
        Log.d(TAG, "Loading all meal data for: ${keyDateFormat.format(dateToLoad.time)}")
        loadAndDisplayMealData(MEAL_TYPE_BREAKFAST, dateToLoad)
        loadAndDisplayMealData(MEAL_TYPE_LUNCH, dateToLoad)
        loadAndDisplayMealData(MEAL_TYPE_DINNER, dateToLoad)
    }

    // --- Key Generation ---
    private fun getDailyMealDataKey(username: String, mealType: String, date: Calendar): String {
        val dateKeyString = keyDateFormat.format(date.time)
        return "${username}_${BASE_KEY_DAILY_FOOD}_${dateKeyString}_$mealType"
    }

    private fun getDailyTotalCaloriesKey(username: String, date: Calendar): String {
        val dateKeyString = keyDateFormat.format(date.time)
        return "${username}_${BASE_KEY_DAILY_CALORIES_EATEN}_$dateKeyString"
    }

    // --- Data Loading (Meal Items) ---
    private fun loadAndDisplayMealData(mealType: String, dateToLoad: Calendar) {
        val username = loggedInUsername ?: return // Ensure user is logged in
        if (!isAdded || context == null) {
            Log.w(TAG, "Fragment not added or context is null in loadAndDisplayMealData for $mealType")
            return
        }

        val mealDataKey = getDailyMealDataKey(username, mealType, dateToLoad)
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

        val (itemsLayout, totalCaloriesTextView) = when (mealType) {
            MEAL_TYPE_BREAKFAST -> layoutBreakfastItems to textViewBreakfastTotalCalories
            MEAL_TYPE_LUNCH -> layoutLunchItems to textViewLunchTotalCalories
            MEAL_TYPE_DINNER -> layoutDinnerItems to textViewDinnerTotalCalories
            else -> return // Should not happen
        }

        itemsLayout.removeAllViews() // Clear previous items

        var totalCalories = 0
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val inflater = LayoutInflater.from(context) // Get inflater once

        // Add Header Row if list is not empty
        if (foodList.isNotEmpty()) {
            try {
                val headerView = inflater.inflate(R.layout.list_item_food_header, itemsLayout, false)
                itemsLayout.addView(headerView)
            } catch (e: Exception) {
                Log.e(TAG, "Error inflating header view for $mealType", e)
            }
        }

        for (entry in foodList) {
            totalCalories += entry.calories
            try {
                val itemView = inflater.inflate(R.layout.list_item_food, itemsLayout, false)
                itemView.findViewById<TextView>(R.id.textViewFoodName).text = entry.foodName
                itemView.findViewById<TextView>(R.id.textViewFoodCalories).text = "${entry.calories} kcal"
                itemView.findViewById<TextView>(R.id.textViewFoodTime).text = timeFormat.format(Date(entry.timestamp))
                itemsLayout.addView(itemView)
            } catch (e: Exception) {
                Log.e(TAG, "Error inflating item view for ${entry.foodName}", e)
            }
        }

        totalCaloriesTextView.text = "$totalCalories kcal"
    }

    // --- Data Saving (Meal Entry) ---
    private fun saveFoodEntry(entry: FoodEntry) {
        val username = loggedInUsername ?: return
        val dateToSave = dateViewModel.getCurrentSelectedDate()

        val mealDataKey = getDailyMealDataKey(username, entry.mealType, dateToSave)
        val jsonString = userDataPreferences.getString(mealDataKey, null)
        val foodList: MutableList<FoodEntry> = if (jsonString != null) {
            try {
                val type = object : TypeToken<MutableList<FoodEntry>>() {}.type
                gson.fromJson(jsonString, type) ?: mutableListOf()
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing JSON before saving ${entry.mealType} on ${keyDateFormat.format(dateToSave.time)}", e)
                mutableListOf() // Start fresh if parsing fails
            }
        } else {
            mutableListOf()
        }

        foodList.add(entry)
        foodList.sortBy { it.timestamp }

        val editor = userDataPreferences.edit()
        val updatedJsonString = gson.toJson(foodList)
        editor.putString(mealDataKey, updatedJsonString)

        // Update Total Daily Calories
        val totalCaloriesKey = getDailyTotalCaloriesKey(username, dateToSave)
        // Recalculate total from the list to ensure accuracy, rather than incrementing
        val newTotalCalories = foodList.sumOf { it.calories }
        val breakfastKey = getDailyMealDataKey(username, MEAL_TYPE_BREAKFAST, dateToSave)
        val lunchKey = getDailyMealDataKey(username, MEAL_TYPE_LUNCH, dateToSave)
        val dinnerKey = getDailyMealDataKey(username, MEAL_TYPE_DINNER, dateToSave)

        val breakfastList: List<FoodEntry> = gson.fromJson(userDataPreferences.getString(breakfastKey, "[]"), object : TypeToken<List<FoodEntry>>() {}.type)
        val lunchList: List<FoodEntry> = gson.fromJson(userDataPreferences.getString(lunchKey, "[]"), object : TypeToken<List<FoodEntry>>() {}.type)
        val dinnerList: List<FoodEntry> = gson.fromJson(userDataPreferences.getString(dinnerKey, "[]"), object : TypeToken<List<FoodEntry>>() {}.type)

        val finalDailyTotal = breakfastList.sumOf { it.calories } + lunchList.sumOf { it.calories } + dinnerList.sumOf { it.calories }

        editor.putInt(totalCaloriesKey, finalDailyTotal)

        editor.apply()

        // Refresh the display for the affected meal type
        loadAndDisplayMealData(entry.mealType, dateToSave)
        Log.d(TAG, "Saved ${entry.foodName} to ${entry.mealType} for ${keyDateFormat.format(dateToSave.time)}. New Daily Total: $finalDailyTotal")
        // TODO: Consider notifying DiaryFragment if it needs immediate update
    }


    private fun setupClickListeners() {
        buttonAddBreakfast.setOnClickListener { showAddMealDialog(MEAL_TYPE_BREAKFAST) }
        buttonAddLunch.setOnClickListener { showAddMealDialog(MEAL_TYPE_LUNCH) }
        buttonAddDinner.setOnClickListener { showAddMealDialog(MEAL_TYPE_DINNER) }
        // Listener for the new manage foods button
        buttonManageFoodDefinitions.setOnClickListener { showManageFoodDefinitionsDialog() }
    }

    // --- Add Meal Dialog ---
    private fun showAddMealDialog(mealType: String) {
        if (!isAdded || context == null) {
            Log.w(TAG, "Fragment not added or context null in showAddMealDialog")
            return
        }
        if (!::foodNameAdapter.isInitialized) { // Ensure adapter is ready
            Log.e(TAG, "Food name adapter not initialized before showing dialog!")
            Toast.makeText(context, "Error initializing food list. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_meal, null)
        val autoCompleteFood = dialogView.findViewById<AutoCompleteTextView>(R.id.actvFood)
        val tilFood = dialogView.findViewById<TextInputLayout>(R.id.tilFood) // Get TextInputLayout for error
        val editTextGrams = dialogView.findViewById<TextInputEditText>(R.id.edtGrams)
        val tilGrams = dialogView.findViewById<TextInputLayout>(R.id.tilGrams)
        val editTextCalories = dialogView.findViewById<TextInputEditText>(R.id.edtCalories)
        val tilCalories = dialogView.findViewById<TextInputLayout>(R.id.tilCalories)
        val tvAddNewFoodInfo = dialogView.findViewById<TextView>(R.id.tvAddNewFoodInfo)
        val dateToLog = dateViewModel.getCurrentSelectedDate()

        autoCompleteFood.setAdapter(foodNameAdapter)
        autoCompleteFood.threshold = 1

        autoCompleteFood.setOnItemClickListener { _, _, _, _ ->
            editTextGrams.text?.clear()
            editTextCalories.text?.clear()
            tilFood.error = null // Clear error on selection
            tvAddNewFoodInfo.visibility = View.GONE
        }

        autoCompleteFood.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().trim().lowercase(Locale.getDefault())
                val foodExists = foodDatabase.containsKey(input)
                tvAddNewFoodInfo.visibility = if (input.isNotEmpty() && !foodExists) View.VISIBLE else View.GONE
                if (foodExists) tilFood.error = null // Clear error if text matches existing food
            }
        })

        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Add $mealType for ${displayDateFormat.format(dateToLog.time)}")
            .setView(dialogView)
            .setPositiveButton("Add", null) // Override below
            .setNegativeButton("Cancel", null) // Simple cancel
            .create() // Create dialog first

        alertDialog.setOnShowListener {
            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                // Clear previous errors
                tilFood.error = null
                tilGrams.error = null
                tilCalories.error = null

                val foodNameInput = autoCompleteFood.text.toString().trim()
                val gramsStr = editTextGrams.text.toString().trim()
                val caloriesStr = editTextCalories.text.toString().trim()

                if (foodNameInput.isEmpty()) {
                    tilFood.error = "Food name required"
                    return@setOnClickListener
                }

                val grams = gramsStr.toDoubleOrNull()?.takeIf { it >= 0 } // Ensure non-negative
                val manualCalories = caloriesStr.toIntOrNull()?.takeIf { it >= 0 } // Ensure non-negative
                val foodNameLower = foodNameInput.lowercase(Locale.getDefault())
                val foodDensity = foodDatabase[foodNameLower] // Case-insensitive lookup

                var finalCalories: Int? = null
                var calculatedDensity: Double? = null // For potentially adding new food

                if (foodDensity != null) {
                    // --- Food FOUND in Database ---
                    if (grams != null) {
                        finalCalories = (grams * foodDensity).roundToInt()
                        Log.d(TAG, "Calculated calories for known food '${foodNameInput.capitalizeWords()}' ($grams g): $finalCalories kcal")
                    } else if (manualCalories != null) {
                        finalCalories = manualCalories
                        Log.d(TAG, "Used manual calories for known food '${foodNameInput.capitalizeWords()}': $finalCalories kcal")
                    } else {
                        tilGrams.error = "Enter grams"
                        tilCalories.error = "or calories"
                        Toast.makeText(context, "For '${foodNameInput.capitalizeWords()}', enter grams or manual calories.", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                } else {
                    // --- Food NOT FOUND in Database ---
                    if (grams != null && grams > 0 && manualCalories != null && manualCalories > 0) {
                        // Can calculate density and potentially add
                        calculatedDensity = manualCalories.toDouble() / grams
                        finalCalories = manualCalories // Log the calories entered for this meal
                        Log.d(TAG, "Food '${foodNameInput.capitalizeWords()}' not found. Grams ($grams) and Cals ($manualCalories) provided. Potential density: $calculatedDensity kcal/g")

                    } else if (manualCalories != null && manualCalories >= 0) {
                        // Can only log manually this time
                        finalCalories = manualCalories
                        Log.d(TAG, "Food '${foodNameInput.capitalizeWords()}' not found. Using manual calories ($finalCalories kcal) for this entry only.")
                        Toast.makeText(context, "Logging '${foodNameInput.capitalizeWords()}' manually. To save it for future use, enter both grams and calories next time.", Toast.LENGTH_LONG).show()

                    } else {
                        // Not enough info
                        tilGrams.error = "Enter grams"
                        tilCalories.error = "and calories to add new food, or just calories to log once"
                        Toast.makeText(context, "Food '${foodNameInput.capitalizeWords()}' not found. Enter grams AND calories to add it, or just calories to log this time.", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                }

                // --- Validation Check ---
                if (finalCalories == null || finalCalories < 0) {
                    Log.e(TAG, "Failed to determine valid calories for '$foodNameInput'")
                    Toast.makeText(context, "Could not determine valid calories.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // --- Handle Adding New Food Definition (if applicable) ---
                if (calculatedDensity != null) { // Only show if density could be calculated
                    // Show confirmation dialog to add the food permanently
                    val densityFormatted = String.format(Locale.US, "%.2f", calculatedDensity)
                    AlertDialog.Builder(requireContext())
                        .setTitle("Add New Food?")
                        .setMessage("Food '${foodNameInput.capitalizeWords()}' is not in your list.\n\nAdd it with $densityFormatted kcal/gram?")
                        .setPositiveButton("Yes, Add") { _, _ ->
                            foodDatabase[foodNameLower] = calculatedDensity // Add to map
                            saveCustomFoodDatabase() // Persist the map
                            updateFoodNameAdapter() // Update suggestions
                            Toast.makeText(context, "'${foodNameInput.capitalizeWords()}' added to your food list.", Toast.LENGTH_SHORT).show()
                            proceedToSaveEntry(foodNameInput, finalCalories, mealType, alertDialog) // Save the meal entry
                        }
                        .setNegativeButton("No, Just Log") { _, _ ->
                            Log.d(TAG, "User chose not to save new food '$foodNameLower' definition.")
                            proceedToSaveEntry(foodNameInput, finalCalories, mealType, alertDialog) // Save only this meal entry
                        }
                        .show()
                } else {
                    // If no density calculated (food existed or only manual cals for new food), just save the entry
                    proceedToSaveEntry(foodNameInput, finalCalories, mealType, alertDialog)
                }
            } // End positiveButton.setOnClickListener
        } // End alertDialog.setOnShowListener

        alertDialog.show()
    }


    // Helper function to create FoodEntry and save it
    private fun proceedToSaveEntry(foodName: String, calories: Int, mealType: String, dialogToDismiss: DialogInterface) {
        val newEntry = FoodEntry(
            foodName = foodName.capitalizeWords(), // Capitalize for display
            calories = calories,
            timestamp = System.currentTimeMillis(),
            mealType = mealType
        )
        saveFoodEntry(newEntry)
        dialogToDismiss.dismiss()
    }


    // --- Manage Food Definitions Dialog ---
    private fun showManageFoodDefinitionsDialog() {
        if (!isAdded || context == null) return
        val username = loggedInUsername ?: return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_food_definition, null)
        val edtNewFoodName = dialogView.findViewById<TextInputEditText>(R.id.edtNewFoodName)
        val tilNewFoodName = dialogView.findViewById<TextInputLayout>(R.id.tilNewFoodName)
        // --- Find new views ---
        val edtNewFoodGramsPerServing = dialogView.findViewById<TextInputEditText>(R.id.edtNewFoodGramsPerServing)
        val tilNewFoodGramsPerServing = dialogView.findViewById<TextInputLayout>(R.id.tilNewFoodGramsPerServing)
        val edtNewFoodCaloriesPerServing = dialogView.findViewById<TextInputEditText>(R.id.edtNewFoodCaloriesPerServing)
        val tilNewFoodCaloriesPerServing = dialogView.findViewById<TextInputLayout>(R.id.tilNewFoodCaloriesPerServing)


        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Add/Edit Food Definition")
            .setView(dialogView)
            .setPositiveButton("Save", null) // Override below
            .setNegativeButton("Cancel", null)
            .create()

        alertDialog.setOnShowListener {
            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                // --- Clear previous errors ---
                tilNewFoodName.error = null
                tilNewFoodGramsPerServing.error = null // Clear error for new field
                tilNewFoodCaloriesPerServing.error = null // Clear error for new field

                val foodName = edtNewFoodName.text.toString().trim()
                // --- Get values from new fields ---
                val gramsPerServingStr = edtNewFoodGramsPerServing.text.toString().trim()
                val caloriesPerServingStr = edtNewFoodCaloriesPerServing.text.toString().trim()

                if (foodName.isEmpty()) {
                    tilNewFoodName.error = "Food name required"
                    return@setOnClickListener
                }

                // --- Validate new fields ---
                val gramsPerServing = gramsPerServingStr.toDoubleOrNull()
                if (gramsPerServing == null || gramsPerServing <= 0) {
                    tilNewFoodGramsPerServing.error = "Enter valid positive grams per serving"
                    return@setOnClickListener
                }

                val caloriesPerServing = caloriesPerServingStr.toDoubleOrNull() // Use toDouble for consistency in calc
                if (caloriesPerServing == null || caloriesPerServing < 0) { // Allow 0 calories? Adjusted to <0
                    tilNewFoodCaloriesPerServing.error = "Enter valid calories per serving (>= 0)"
                    return@setOnClickListener
                }

                val foodNameLower = foodName.lowercase(Locale.getDefault())
                // --- Calculate density (calories per gram) ---
                val density = caloriesPerServing / gramsPerServing // gramsPerServing already checked > 0

                // Check if overwriting (logic remains the same)
                if (foodDatabase.containsKey(foodNameLower)) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Overwrite Existing Food?")
                        .setMessage("'${foodName.capitalizeWords()}' already exists. Overwrite its calorie data?")
                        .setPositiveButton("Overwrite") { _, _ ->
                            // Pass calculated density to saveDefinition
                            saveDefinition(foodNameLower, density, foodName.capitalizeWords(), alertDialog)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    // Just save the new definition with calculated density
                    saveDefinition(foodNameLower, density, foodName.capitalizeWords(), alertDialog)
                }
            }
        }
        alertDialog.show()
    }

    // Helper for saving food definition - NO CHANGE NEEDED HERE
    // It already accepts the calculated density (kcal/gram)
    private fun saveDefinition(key: String, density: Double, displayName: String, dialogToDismiss: DialogInterface) {
        foodDatabase[key] = density
        saveCustomFoodDatabase()
        updateFoodNameAdapter()
        Toast.makeText(context, "'$displayName' saved.", Toast.LENGTH_SHORT).show()
        dialogToDismiss.dismiss()
    }
    // --- Utility Functions ---

    private fun String.capitalizeWords(): String =
        split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() } }


    private fun isSameDay(cal1: Calendar?, cal2: Calendar?): Boolean {
        if (cal1 == null || cal2 == null) return false
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) // Simpler check
    }

    // Make sure to handle fragment lifecycle (e.g., context becoming null)
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Re-initialize adapter if needed, though onViewCreated should handle it
        if (::foodDatabase.isInitialized && !::foodNameAdapter.isInitialized) {
            setupFoodNameAdapter()
        }
    }
}