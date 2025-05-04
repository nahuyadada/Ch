package com.example.chowcheck

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog // Import TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button // Import Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView // Import TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat // For formatting time
import java.util.* // For Calendar and Locale

class NotificationActivity : AppCompatActivity() {

    // Views
    private lateinit var switchMealReminder: Switch
    private lateinit var switchWaterReminder: Switch
    private lateinit var switchProgressReminder: Switch
    private lateinit var switchGoalReminder: Switch
    private lateinit var btnBack: ImageView
    private lateinit var tvBreakfastTime: TextView
    private lateinit var tvLunchTime: TextView
    private lateinit var tvDinnerTime: TextView
    private lateinit var btnEditBreakfastTime: Button
    private lateinit var btnEditLunchTime: Button
    private lateinit var btnEditDinnerTime: Button

    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "NotificationPrefs"
        const val KEY_MEAL_REMINDER = "meal_reminder_enabled"
        const val KEY_WATER_REMINDER = "water_reminder_enabled"
        const val KEY_PROGRESS_REMINDER = "progress_reminder_enabled"
        const val KEY_GOAL_REMINDER = "goal_reminder_enabled"
        private const val TAG = "NotificationActivity"

        // Keys for storing meal times
        const val KEY_BREAKFAST_HOUR = "breakfast_hour"
        const val KEY_BREAKFAST_MINUTE = "breakfast_minute"
        const val KEY_LUNCH_HOUR = "lunch_hour"
        const val KEY_LUNCH_MINUTE = "lunch_minute"
        const val KEY_DINNER_HOUR = "dinner_hour"
        const val KEY_DINNER_MINUTE = "dinner_minute"

        // Default times
        const val DEFAULT_BREAKFAST_HOUR = 8
        const val DEFAULT_BREAKFAST_MINUTE = 0
        const val DEFAULT_LUNCH_HOUR = 13
        const val DEFAULT_LUNCH_MINUTE = 0
        const val DEFAULT_DINNER_HOUR = 19
        const val DEFAULT_DINNER_MINUTE = 0
    }

    // --- Permission handling state ---
    private var pendingPrefKey: String? = null
    private var pendingSwitchView: Switch? = null
    private var pendingScheduleAction: ((Boolean) -> Unit)? = null

    private val requestPostNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            Log.d(TAG, "POST_NOTIFICATIONS permission result: $isGranted")
            handlePostNotificationPermissionResult(isGranted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        NotificationHelper.createNotificationChannel(applicationContext)
        Log.d(TAG,"Ensured notification channels created from NotificationActivity")

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        initializeViews()
        loadSettings() // Load both switch states and times
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // When returning from settings (e.g., after granting exact alarm), reload times
        // because the state might affect scheduling possibilities.
        loadMealTimes() // Reload potentially changed times
        Log.d(TAG, "onResume: Checking exact alarm status: ${NotificationHelper.canScheduleExactAlarms(this)}")
    }


    private fun initializeViews() {
        switchMealReminder = findViewById(R.id.switchMealReminder)
        switchWaterReminder = findViewById(R.id.switchWaterReminder)
        switchProgressReminder = findViewById(R.id.switchProgressReminder)
        switchGoalReminder = findViewById(R.id.switchGoalReminder)
        btnBack = findViewById(R.id.btnBack)
        // Meal Time Views
        tvBreakfastTime = findViewById(R.id.tvBreakfastTime)
        tvLunchTime = findViewById(R.id.tvLunchTime)
        tvDinnerTime = findViewById(R.id.tvDinnerTime)
        btnEditBreakfastTime = findViewById(R.id.btnEditBreakfastTime)
        btnEditLunchTime = findViewById(R.id.btnEditLunchTime)
        btnEditDinnerTime = findViewById(R.id.btnEditDinnerTime)
    }

    // Renamed to load all settings
    private fun loadSettings() {
        // Load switch states
        switchMealReminder.isChecked = prefs.getBoolean(KEY_MEAL_REMINDER, false)
        switchWaterReminder.isChecked = prefs.getBoolean(KEY_WATER_REMINDER, false)
        switchProgressReminder.isChecked = prefs.getBoolean(KEY_PROGRESS_REMINDER, false)
        switchGoalReminder.isChecked = prefs.getBoolean(KEY_GOAL_REMINDER, false)
        Log.d(TAG, "Loaded switch states from SharedPreferences.")

        // Load meal times
        loadMealTimes()
    }

    // Helper to load and display meal times
    private fun loadMealTimes() {
        val breakfastHour = prefs.getInt(KEY_BREAKFAST_HOUR, DEFAULT_BREAKFAST_HOUR)
        val breakfastMinute = prefs.getInt(KEY_BREAKFAST_MINUTE, DEFAULT_BREAKFAST_MINUTE)
        val lunchHour = prefs.getInt(KEY_LUNCH_HOUR, DEFAULT_LUNCH_HOUR)
        val lunchMinute = prefs.getInt(KEY_LUNCH_MINUTE, DEFAULT_LUNCH_MINUTE)
        val dinnerHour = prefs.getInt(KEY_DINNER_HOUR, DEFAULT_DINNER_HOUR)
        val dinnerMinute = prefs.getInt(KEY_DINNER_MINUTE, DEFAULT_DINNER_MINUTE)

        tvBreakfastTime.text = formatTime(breakfastHour, breakfastMinute)
        tvLunchTime.text = formatTime(lunchHour, lunchMinute)
        tvDinnerTime.text = formatTime(dinnerHour, dinnerMinute)
        Log.d(TAG, "Loaded meal times.")
    }

    // Helper to format H:M into HH:MM string (or locale specific)
    private fun formatTime(hour: Int, minute: Int): String {
        // Consider using locale-specific time formatting for better UX
        // return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute) }.time)
        return String.format(Locale.US, "%02d:%02d", hour, minute) // Simple HH:MM format
    }

    private fun saveSwitchState(key: String, isChecked: Boolean) {
        prefs.edit().putBoolean(key, isChecked).apply()
        Log.d(TAG, "Saved switch state for $key: $isChecked")
    }

    // Helper to save meal times
    private fun saveMealTime(keyHour: String, hour: Int, keyMinute: String, minute: Int) {
        prefs.edit()
            .putInt(keyHour, hour)
            .putInt(keyMinute, minute)
            .apply()
        Log.d(TAG, "Saved time for $keyHour: $hour:$minute")
    }


    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        // --- Meal Reminder Listener (Handles Exact Alarms & Time Editing) ---
        switchMealReminder.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Meal Reminder Switch toggled: $isChecked")
            val action = { shouldSchedule: Boolean ->
                if (shouldSchedule) {
                    Log.d(TAG, "Scheduling meal reminders...")
                    NotificationHelper.scheduleMealReminder(this) // Will now read times from Prefs
                } else {
                    Log.d(TAG, "Canceling meal reminders...")
                    NotificationHelper.cancelMealReminder(this)
                }
            }
            handleReminderChangeWithPermissionChecks(
                isChecked, KEY_MEAL_REMINDER, switchMealReminder, action,
                requiresExactAlarm = true // Meal reminders need exact alarm perm
            )
        }

        // Meal Time Edit Button Listeners
        btnEditBreakfastTime.setOnClickListener {
            showTimePicker(KEY_BREAKFAST_HOUR, KEY_BREAKFAST_MINUTE, DEFAULT_BREAKFAST_HOUR, DEFAULT_BREAKFAST_MINUTE) { hour, minute ->
                tvBreakfastTime.text = formatTime(hour, minute)
                handleTimeChangeReschedule(KEY_MEAL_REMINDER) // Reschedule if switch is ON
            }
        }
        btnEditLunchTime.setOnClickListener {
            showTimePicker(KEY_LUNCH_HOUR, KEY_LUNCH_MINUTE, DEFAULT_LUNCH_HOUR, DEFAULT_LUNCH_MINUTE) { hour, minute ->
                tvLunchTime.text = formatTime(hour, minute)
                handleTimeChangeReschedule(KEY_MEAL_REMINDER)
            }
        }
        btnEditDinnerTime.setOnClickListener {
            showTimePicker(KEY_DINNER_HOUR, KEY_DINNER_MINUTE, DEFAULT_DINNER_HOUR, DEFAULT_DINNER_MINUTE) { hour, minute ->
                tvDinnerTime.text = formatTime(hour, minute)
                handleTimeChangeReschedule(KEY_MEAL_REMINDER)
            }
        }

        // --- Other Reminder Listeners ---
        switchWaterReminder.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Water Reminder Switch toggled: $isChecked")
            val action = { shouldSchedule: Boolean ->
                if (shouldSchedule) NotificationHelper.scheduleWaterReminder(this)
                else NotificationHelper.cancelWaterReminder(this)
            }
            handleReminderChangeWithPermissionChecks(isChecked, KEY_WATER_REMINDER, switchWaterReminder, action, requiresExactAlarm = false)
            Toast.makeText(this, "Water Reminder ${if(isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }
        switchProgressReminder.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Progress Reminder Switch toggled: $isChecked")
            val action = { shouldSchedule: Boolean ->
                if (shouldSchedule) NotificationHelper.scheduleProgressReminder(this)
                else NotificationHelper.cancelProgressReminder(this)
            }
            handleReminderChangeWithPermissionChecks(isChecked, KEY_PROGRESS_REMINDER, switchProgressReminder, action, requiresExactAlarm = false)
            Toast.makeText(this, "Progress Reminder ${if(isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }
        switchGoalReminder.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Goal Reminder Switch toggled: $isChecked")
            // ONLY save the preference state for event-driven goal alerts
            saveSwitchState(KEY_GOAL_REMINDER, isChecked)
            Toast.makeText(this, "Goal Achievement Alerts ${if(isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper function to show TimePickerDialog
    private fun showTimePicker(
        keyHour: String,
        keyMinute: String,
        defaultHour: Int,
        defaultMinute: Int,
        onTimeSetAction: (hour: Int, minute: Int) -> Unit
    ) {
        val currentHour = prefs.getInt(keyHour, defaultHour)
        val currentMinute = prefs.getInt(keyMinute, defaultMinute)

        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                Log.d(TAG, "Time picked for $keyHour: $hourOfDay:$minute")
                saveMealTime(keyHour, hourOfDay, keyMinute, minute) // Save the new time
                onTimeSetAction(hourOfDay, minute) // Perform UI update and reschedule
            },
            currentHour,
            currentMinute,
            android.text.format.DateFormat.is24HourFormat(this) // Use device's 12/24hr setting
        ).show()
    }

    // Helper to reschedule alarms after time change IF the main switch is on
    private fun handleTimeChangeReschedule(reminderKey: String) {
        if (prefs.getBoolean(reminderKey, false)) {
            Log.d(TAG, "$reminderKey time changed and switch is ON. Rescheduling...")
            // For simplicity, cancel and reschedule all alarms for that type
            when (reminderKey) {
                KEY_MEAL_REMINDER -> {
                    // ADD THIS LOG: Explicitly log the permission check result
                    val canSchedule = NotificationHelper.canScheduleExactAlarms(this)
                    Log.d(TAG, "Checking canScheduleExactAlarms before reschedule: $canSchedule")
                    // ---------------------------------------------

                    // Need to ensure permissions are still valid before rescheduling
                    // Use the logged variable 'canSchedule' now
                    if (canSchedule || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        NotificationHelper.cancelMealReminder(this)
                        NotificationHelper.scheduleMealReminder(this) // Reads new times from prefs
                        Log.i(TAG, "Rescheduling successful after time change.") // Add success log
                    } else {
                        Log.w(TAG, "Cannot reschedule meal reminders - exact alarm permission missing (Check returned false).") // More detailed log
                        Toast.makeText(this, "Cannot reschedule - Exact alarm permission needed.", Toast.LENGTH_SHORT).show()
                    }
                }
                // Add cases for KEY_WATER_REMINDER, KEY_PROGRESS_REMINDER if they become editable
            }
        } else {
            Log.d(TAG, "$reminderKey time changed but switch is OFF. No reschedule needed.")
        }
    }

    /**
     * Handles switch changes that require checking POST_NOTIFICATIONS and potentially SCHEDULE_EXACT_ALARM.
     */
    private fun handleReminderChangeWithPermissionChecks(
        isChecked: Boolean,
        prefKey: String,
        switchView: Switch,
        scheduleAction: (shouldSchedule: Boolean) -> Unit,
        requiresExactAlarm: Boolean
    ) {
        if (isChecked) {
            Log.d(TAG, "Switch $prefKey toggled ON. Starting permission checks (Exact Required: $requiresExactAlarm)...")
            // Store action details in case we need async permission results
            pendingPrefKey = prefKey
            pendingSwitchView = switchView
            pendingScheduleAction = scheduleAction

            checkPostNotificationsAndProceed(requiresExactAlarm)

        } else {
            // Turning OFF: No permissions needed to cancel
            Log.d(TAG, "Turning off $prefKey.")
            clearPendingAction() // Clear any pending action if user toggles off quickly
            saveSwitchState(prefKey, false)
            scheduleAction(false) // Call the cancel action
        }
    }

    /**
     * Simplified handler for switches that DON'T require exact alarm checks (yet).
     * Still includes basic POST_NOTIFICATION check.
     */
    private fun handleReminderChangeSimple(
        isChecked: Boolean,
        prefKey: String,
        switchView: Switch,
        scheduleAction: (shouldSchedule: Boolean) -> Unit
    ) {
        handleReminderChangeWithPermissionChecks(isChecked, prefKey, switchView, scheduleAction, requiresExactAlarm = false)
        // You might eventually merge this or keep it separate if simpler reminders use different logic
    }


    /** Step 1: Check POST_NOTIFICATIONS permission */
    private fun checkPostNotificationsAndProceed(requiresExactAlarm: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "1. POST_NOTIFICATIONS already granted.")
                    // Proceed to next check (Exact Alarm) or schedule if not needed
                    checkExactAlarmAndScheduleIfNeeded(requiresExactAlarm)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d(TAG, "1. POST_NOTIFICATIONS rationale needed.")
                    showPostNotificationRationaleDialog() // Dialog will launch permission request
                }
                else -> {
                    Log.d(TAG, "1. Requesting POST_NOTIFICATIONS permission.")
                    requestPostNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    // Result handled in handlePostNotificationPermissionResult
                }
            }
        } else {
            // API < 33: No POST_NOTIFICATIONS permission needed
            Log.d(TAG, "1. POST_NOTIFICATIONS not required (SDK < 33).")
            // Proceed to next check (Exact Alarm) or schedule if not needed
            checkExactAlarmAndScheduleIfNeeded(requiresExactAlarm)
        }
    }

    /** Step 2: Handle result of POST_NOTIFICATIONS request */
    private fun handlePostNotificationPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            Log.d(TAG, "2. POST_NOTIFICATIONS permission was GRANTED by user.")
            // User just granted POST_NOTIFICATIONS, now check Exact Alarm if needed for the pending action
            val requiresExact = pendingPrefKey == KEY_MEAL_REMINDER // Check if the pending action was for meals
            checkExactAlarmAndScheduleIfNeeded(requiresExact) // requiresExact should be determined based on pendingPrefKey
        } else {
            Log.w(TAG, "2. POST_NOTIFICATIONS permission was DENIED by user.")
            Toast.makeText(this, "Notifications require permission to function.", Toast.LENGTH_LONG).show()
            // Revert the switch that was waiting for this result
            pendingSwitchView?.isChecked = false
            clearPendingAction() // Clear pending action as it failed

            // Show settings dialog if user chose "Don't ask again"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                showPostNotificationSettingsDialog()
            }
        }
    }

    /** Step 3: Check SCHEDULE_EXACT_ALARM permission (if needed) and finally schedule */
    private fun checkExactAlarmAndScheduleIfNeeded(requiresExactAlarm: Boolean) {
        val canScheduleExact = NotificationHelper.canScheduleExactAlarms(this)
        Log.d(TAG,"3. Checking Exact Alarm Permission. Required: $requiresExactAlarm, Granted: $canScheduleExact")


        if (requiresExactAlarm) {
            // Exact alarm is required for this reminder type
            if (canScheduleExact || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // Permission granted or not needed (older SDK)
                Log.i(TAG, "3a. Exact Alarm permission OK. Scheduling action for ${pendingPrefKey}.")
                // All permissions OK, proceed with the stored action
                pendingScheduleAction?.invoke(true) // Schedule the reminder
                saveSwitchState(pendingPrefKey!!, true) // Save the switch state
                clearPendingAction()
            } else {
                // Exact alarm required but not granted (on SDK >= 31)
                Log.w(TAG, "3a. Exact Alarm permission needed but NOT granted for ${pendingPrefKey}. Showing dialog.")
                showExactAlarmPermissionDialog()
                // Revert switch, user needs to grant manually and toggle again
                pendingSwitchView?.isChecked = false
                clearPendingAction()
            }
        } else {
            // Exact alarm is NOT required for this reminder type
            Log.i(TAG, "3b. Exact Alarm permission not required. Scheduling action for ${pendingPrefKey}.")
            // Proceed with the stored action (only needed POST_NOTIFICATIONS check)
            pendingScheduleAction?.invoke(true)
            saveSwitchState(pendingPrefKey!!, true)
            clearPendingAction()
        }
    }

    /** Clears the stored pending action details */
    private fun clearPendingAction() {
        pendingPrefKey = null
        pendingSwitchView = null
        pendingScheduleAction = null
        Log.d(TAG, "Pending action cleared.")
    }


    // --- Dialogs ---

    private fun showPostNotificationRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Needed")
            .setMessage("Reminders require permission to show notifications. Please grant this permission to enable reminders.")
            .setPositiveButton("Grant Permission") { _, _ ->
                Log.d(TAG,"Rationale accepted. Requesting POST_NOTIFICATIONS.")
                requestPostNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                // Result will be handled by handlePostNotificationPermissionResult
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Log.d(TAG,"Rationale declined.")
                Toast.makeText(this, "Permission denied. Reminders disabled.", Toast.LENGTH_SHORT).show()
                // Revert the switch and clear pending action
                pendingSwitchView?.isChecked = false
                clearPendingAction()
            }
            .setOnCancelListener {
                Log.d(TAG,"Rationale cancelled.")
                Toast.makeText(this, "Permission needed for reminders.", Toast.LENGTH_SHORT).show()
                pendingSwitchView?.isChecked = false
                clearPendingAction()
            }
            .show()
    }

    private fun showPostNotificationSettingsDialog() {
        // Renamed from showSettingsDialog for clarity
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Notification permission was previously denied with 'Don't ask again'. Please grant it manually in App Settings to enable reminders.")
            .setPositiveButton("Go to Settings") { _, _ ->
                Log.d(TAG,"Opening App Settings for Post Notifications.")
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                try {
                    startActivity(intent)
                } catch (e: Exception) { Log.e(TAG, "Could not open app settings", e) }
                // Clear pending state, user needs to toggle again after changing settings
                clearPendingAction() // Switch should already be off
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                Log.d(TAG,"Settings dialog cancelled.")
                Toast.makeText(this, "Reminders will not function without Notification permission.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                clearPendingAction() // Switch should already be off
            }
            .show()
    }

    private fun showExactAlarmPermissionDialog() {
        // Dialog specific to the SCHEDULE_EXACT_ALARM permission
        AlertDialog.Builder(this)
            .setTitle("Precise Timing Required")
            .setMessage("To ensure meal reminders appear exactly on time, this app needs the 'Alarms & reminders' permission. Please grant this in the next settings screen.")
            .setPositiveButton("Go to Settings") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Log.d(TAG,"Opening App Settings for Exact Alarms.")
                    try {
                        // Intent to open the specific "Alarms & reminders" settings for this app
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                        Toast.makeText(this, "Please grant the 'Alarms & reminders' permission and try enabling the reminder again.", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not open exact alarm settings", e)
                        // Fallback? Maybe open general app settings?
                        Toast.makeText(this, "Could not open required settings.", Toast.LENGTH_SHORT).show()
                    }
                }
                // Clear pending state as switch was reverted, user needs to toggle again
                clearPendingAction()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                Log.d(TAG,"Exact Alarm settings dialog cancelled.")
                Toast.makeText(this, "Reminders will use approximate timing without exact alarm permission.", Toast.LENGTH_SHORT).show()
                // NOTE: Currently, the code doesn't implement fallback to inexact if exact is denied.
                // It just prevents enabling the switch.
                dialog.dismiss()
                clearPendingAction() // Switch was already reverted
            }
            .show()
    }
}