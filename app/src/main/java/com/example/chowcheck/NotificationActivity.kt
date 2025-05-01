package com.example.chowcheck

// --- Import AppCompatActivity ---
import androidx.appcompat.app.AppCompatActivity
// --- Keep other necessary imports ---
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog // Use AppCompat AlertDialog
import androidx.core.content.ContextCompat


// --- Change inheritance from Activity to AppCompatActivity ---
class NotificationActivity : AppCompatActivity() {

    private lateinit var switchMealReminder: Switch
    private lateinit var switchWaterReminder: Switch
    private lateinit var switchProgressReminder: Switch
    private lateinit var switchGoalReminder: Switch
    private lateinit var btnBack: ImageView // Keep using ImageView if it works, consider Toolbar later

    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "NotificationPrefs"
        const val KEY_MEAL_REMINDER = "meal_reminder_enabled"
        const val KEY_WATER_REMINDER = "water_reminder_enabled"
        const val KEY_PROGRESS_REMINDER = "progress_reminder_enabled"
        const val KEY_GOAL_REMINDER = "goal_reminder_enabled"
    }

    // Notification Permission Launcher (Should work now)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show()
                if (switchMealReminder.isChecked) {
                    NotificationHelper.scheduleMealReminder(this)
                }
                // TODO: Add similar logic for other switches if needed
            } else {
                // Check if rationale should be shown (user denied previously) - Optional advanced handling
                if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    // User selected "Don't ask again" or policy prevents asking.
                    showSettingsDialog() // Offer to take user to settings
                } else {
                    // User denied, but didn't select "Don't ask again".
                    Toast.makeText(this, "Notifications require permission to function.", Toast.LENGTH_LONG).show()
                }
                // Revert switch state if permission denied
                if (switchMealReminder.isChecked) {
                    switchMealReminder.isChecked = false
                    saveSwitchState(KEY_MEAL_REMINDER, false) // Also save the reverted state
                }
                // TODO: Turn off other switches if they were just enabled and save state
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        initializeViews()
        loadSwitchStates()
        setupListeners()
    }

    private fun initializeViews() {
        switchMealReminder = findViewById(R.id.switchMealReminder)
        switchWaterReminder = findViewById(R.id.switchWaterReminder)
        switchProgressReminder = findViewById(R.id.switchProgressReminder)
        switchGoalReminder = findViewById(R.id.switchGoalReminder)
        btnBack = findViewById(R.id.btnBack) // Assuming this ID exists in your layout
    }

    private fun loadSwitchStates() {
        switchMealReminder.isChecked = prefs.getBoolean(KEY_MEAL_REMINDER, false)
        switchWaterReminder.isChecked = prefs.getBoolean(KEY_WATER_REMINDER, false)
        switchProgressReminder.isChecked = prefs.getBoolean(KEY_PROGRESS_REMINDER, false)
        switchGoalReminder.isChecked = prefs.getBoolean(KEY_GOAL_REMINDER, false)
    }

    private fun saveSwitchState(key: String, isChecked: Boolean) {
        prefs.edit().putBoolean(key, isChecked).apply()
    }

    private fun setupListeners() {
        // Consider using a Toolbar with setSupportActionBar for standard back handling
        btnBack.setOnClickListener {
            finish()
        }

        switchMealReminder.setOnCheckedChangeListener { _, isChecked ->
            handleReminderChange(isChecked, KEY_MEAL_REMINDER) { shouldSchedule ->
                if (shouldSchedule) NotificationHelper.scheduleMealReminder(this)
                else NotificationHelper.cancelMealReminder(this)
            }
        }

        switchWaterReminder.setOnCheckedChangeListener { _, isChecked ->
            handleReminderChange(isChecked, KEY_WATER_REMINDER) { shouldSchedule ->
                // TODO: Call water schedule/cancel helpers
                val action = if(shouldSchedule) "Scheduling TODO" else "Canceling TODO"
                Toast.makeText(this, "Water Reminder ${if(isChecked) "Enabled" else "Disabled"} ($action)", Toast.LENGTH_SHORT).show()
            }
        }

        switchProgressReminder.setOnCheckedChangeListener { _, isChecked ->
            handleReminderChange(isChecked, KEY_PROGRESS_REMINDER) { shouldSchedule ->
                // TODO: Call progress schedule/cancel helpers
                val action = if(shouldSchedule) "Scheduling TODO" else "Canceling TODO"
                Toast.makeText(this, "Progress Reminder ${if(isChecked) "Enabled" else "Disabled"} ($action)", Toast.LENGTH_SHORT).show()
            }
        }

        switchGoalReminder.setOnCheckedChangeListener { _, isChecked ->
            handleReminderChange(isChecked, KEY_GOAL_REMINDER) { shouldSchedule ->
                // TODO: Call goal schedule/cancel helpers
                val action = if(shouldSchedule) "Scheduling TODO" else "Canceling TODO"
                Toast.makeText(this, "Goal Reminder ${if(isChecked) "Enabled" else "Disabled"} ($action)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Generic handler for switch changes
    private fun handleReminderChange(
        isChecked: Boolean,
        prefKey: String,
        scheduleAction: (shouldSchedule: Boolean) -> Unit
    ) {
        saveSwitchState(prefKey, isChecked) // Save state first
        if (isChecked) {
            askNotificationPermission { permissionGranted ->
                if(permissionGranted) {
                    scheduleAction(true) // Call the specific schedule function
                } else {
                    // Permission denied, revert switch state and don't schedule
                    findViewById<Switch>(resources.getIdentifier(prefKey.replace("_enabled", ""), "id", packageName)).isChecked = false
                    saveSwitchState(prefKey, false)
                }
            }
        } else {
            scheduleAction(false) // Call the specific cancel function
        }
    }

    // Updated Permission Handling with Rationale and Settings Dialog
    private fun askNotificationPermission(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    onResult(true)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showRationaleDialog(onResult) // Show explanation
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    // Result handled by launcher, assume false for now if immediately needed
                    // Consider disabling switch until launcher returns? Or just let launcher handle scheduling on grant.
                    // onResult(false) // Let launcher callback handle the outcome
                }
            }
        } else {
            onResult(true) // Granted on older versions
        }
    }

    private fun showRationaleDialog(onResult: (Boolean) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Permission Needed")
            .setMessage("Reminders require permission to show notifications. Please grant this permission to enable reminders.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                // Result handled by launcher
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Permission denied. Reminders disabled.", Toast.LENGTH_SHORT).show()
                onResult(false) // Explicitly denied by user in rationale
            }
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Notification permission was previously denied. Please grant it in App Settings to enable reminders.")
            .setPositiveButton("Go to Settings") { _, _ ->
                // Intent to open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}