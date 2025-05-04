package com.example.chowcheck // Or your package

import android.Manifest // Needed for Manifest.permission.*
import android.annotation.SuppressLint
import android.content.pm.PackageManager // Needed for PackageManager.PERMISSION_GRANTED
import androidx.core.app.ActivityCompat // Needed for checkSelfPermission

// Other existing imports
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat // Already likely there
import java.util.Calendar
object NotificationHelper {

    // --- Channel & Action Constants ---
    const val MEAL_CHANNEL_ID = "meal_reminder_channel_1"
    const val MEAL_CHANNEL_NAME = "Meal Reminders"
    const val MEAL_REMINDER_ACTION = "com.example.chowcheck.MEAL_REMINDER"

    const val WATER_CHANNEL_ID = "water_reminder_channel_1"
    const val WATER_CHANNEL_NAME = "Water Reminders"
    const val WATER_REMINDER_ACTION = "com.example.chowcheck.WATER_REMINDER"

    const val PROGRESS_CHANNEL_ID = "progress_reminder_channel_1"
    const val PROGRESS_CHANNEL_NAME = "Progress Reminders"
    const val PROGRESS_REMINDER_ACTION = "com.example.chowcheck.PROGRESS_REMINDER"

    const val GOAL_CHANNEL_ID = "goal_reminder_channel_1"
    const val GOAL_CHANNEL_NAME = "Goal Reminders"
    // No action needed if triggered directly

    // --- Request Code Constants ---
    const val BREAKFAST_REQUEST_CODE = 1001
    const val LUNCH_REQUEST_CODE = 1002
    const val DINNER_REQUEST_CODE = 1003
    const val WATER_REQUEST_CODE = 2001 // Single code for repeating inexact
    const val WEEKLY_PROGRESS_REQUEST_CODE = 3001 // Single code for repeating inexact
    private const val GOAL_NOTIFICATION_ID_BASE = 4000 // Base ID for direct goal notifications


    // --- createNotificationChannel (Ensure all channels are created) ---
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Define channels - ensure IDs match constants above
            val channels = listOf(
                NotificationChannel(MEAL_CHANNEL_ID, MEAL_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Daily meal time reminders" },
                NotificationChannel(WATER_CHANNEL_ID, WATER_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Periodic water intake reminders" },
                NotificationChannel(PROGRESS_CHANNEL_ID, PROGRESS_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Weekly progress summary notifications" },
                NotificationChannel(GOAL_CHANNEL_ID, GOAL_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply { description = "Goal achievement alerts" }
            )

            channels.forEach { notificationManager.createNotificationChannel(it) }
            Log.d("NotificationHelper", "All notification channels created/ensured.")
        }
    }

    // --- Meal Reminder Functions (Exact Alarms - Requires Permission Handling) ---
    fun scheduleMealReminder(context: Context) {
        // Get SharedPreferences
        val prefs = context.getSharedPreferences(NotificationActivity.PREFS_NAME, Context.MODE_PRIVATE)

        // Read user-set times (or defaults)
        val breakfastHour = prefs.getInt(NotificationActivity.KEY_BREAKFAST_HOUR, NotificationActivity.DEFAULT_BREAKFAST_HOUR)
        val breakfastMinute = prefs.getInt(NotificationActivity.KEY_BREAKFAST_MINUTE, NotificationActivity.DEFAULT_BREAKFAST_MINUTE)
        val lunchHour = prefs.getInt(NotificationActivity.KEY_LUNCH_HOUR, NotificationActivity.DEFAULT_LUNCH_HOUR)
        val lunchMinute = prefs.getInt(NotificationActivity.KEY_LUNCH_MINUTE, NotificationActivity.DEFAULT_LUNCH_MINUTE)
        val dinnerHour = prefs.getInt(NotificationActivity.KEY_DINNER_HOUR, NotificationActivity.DEFAULT_DINNER_HOUR)
        val dinnerMinute = prefs.getInt(NotificationActivity.KEY_DINNER_MINUTE, NotificationActivity.DEFAULT_DINNER_MINUTE)

        Log.d("NotificationHelper", "Scheduling Meal Reminders with times: B=$breakfastHour:$breakfastMinute, L=$lunchHour:$lunchMinute, D=$dinnerHour:$dinnerMinute")


        // Check for exact alarm permission before scheduling
        if (canScheduleExactAlarms(context)) {
            Log.d("NotificationHelper", "Exact alarm permission granted. Scheduling exact meal reminders.")
            // Schedule using the loaded times
            scheduleExactReminderForTime(context, BREAKFAST_REQUEST_CODE, breakfastHour, breakfastMinute, "Breakfast", MEAL_REMINDER_ACTION, MealReminderReceiver::class.java)
            scheduleExactReminderForTime(context, LUNCH_REQUEST_CODE, lunchHour, lunchMinute, "Lunch", MEAL_REMINDER_ACTION, MealReminderReceiver::class.java)
            scheduleExactReminderForTime(context, DINNER_REQUEST_CODE, dinnerHour, dinnerMinute, "Dinner", MEAL_REMINDER_ACTION, MealReminderReceiver::class.java)
            Log.d("NotificationHelper", "Exact Meal Reminders Scheduled with custom/default times.")
        } else {
            Log.w("NotificationHelper", "SCHEDULE_EXACT_ALARM permission not granted. Cannot schedule exact meal reminders.")
            Toast.makeText(context, "Exact alarm permission needed for precise meal reminders.", Toast.LENGTH_LONG).show()
        }
    }

    fun cancelMealReminder(context: Context) {
        // Cancellation logic remains the same - cancels based on request code
        cancelReminderForTime(context, BREAKFAST_REQUEST_CODE, MEAL_REMINDER_ACTION, MealReminderReceiver::class.java)
        cancelReminderForTime(context, LUNCH_REQUEST_CODE, MEAL_REMINDER_ACTION, MealReminderReceiver::class.java)
        cancelReminderForTime(context, DINNER_REQUEST_CODE, MEAL_REMINDER_ACTION, MealReminderReceiver::class.java)
        Log.d("NotificationHelper", "Meal Reminders Canceled.")
    }


    // --- Water Reminder Functions (Inexact Repeating - Every 2 Hours) ---
    fun scheduleWaterReminder(context: Context) {
        Log.d("NotificationHelper", "Scheduling inexact water reminder (every 2 hours).")
        // Start time: Let's start roughly on the next hour to make it feel a bit more regular
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            // If current minute > 0, move to next hour, otherwise use current hour
            if(get(Calendar.MINUTE) > 0 || get(Calendar.SECOND) > 0 || get(Calendar.MILLISECOND) > 0) {
                add(Calendar.HOUR_OF_DAY, 1)
            }
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerAtMillis = calendar.timeInMillis
        val intervalMillis = AlarmManager.INTERVAL_HOUR * 2

        scheduleInexactRepeatingReminder(
            context,
            WATER_REQUEST_CODE,
            triggerAtMillis,
            intervalMillis,
            "Drink Water", // Reminder name used in helper log
            WATER_REMINDER_ACTION,
            WaterReminderReceiver::class.java // Ensure you created this Receiver
        )
    }

    fun cancelWaterReminder(context: Context) {
        Log.d("NotificationHelper", "Canceling water reminder.")
        cancelReminderForTime(context, WATER_REQUEST_CODE, WATER_REMINDER_ACTION, WaterReminderReceiver::class.java)
    }

    // --- Progress Reminder Functions (Inexact Repeating - Weekly on Sunday 9 AM) ---
    fun scheduleProgressReminder(context: Context) {
        Log.d("NotificationHelper", "Scheduling inexact weekly progress reminder (Sunday 9 AM).")

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) // Target Sunday
            set(Calendar.HOUR_OF_DAY, 9)              // Target 9 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If Sunday 9 AM has already passed this week, schedule for next week
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1) // Add 1 week
        }

        val triggerAtMillis = calendar.timeInMillis
        val intervalMillis = AlarmManager.INTERVAL_DAY * 7 // Weekly interval

        scheduleInexactRepeatingReminder(
            context,
            WEEKLY_PROGRESS_REQUEST_CODE,
            triggerAtMillis,
            intervalMillis,
            "Weekly Progress", // Reminder name used in helper log
            PROGRESS_REMINDER_ACTION,
            ProgressReminderReceiver::class.java // Ensure you created this Receiver
        )
    }

    fun cancelProgressReminder(context: Context) {
        Log.d("NotificationHelper", "Canceling weekly progress reminder.")
        cancelReminderForTime(context, WEEKLY_PROGRESS_REQUEST_CODE, PROGRESS_REMINDER_ACTION, ProgressReminderReceiver::class.java)
    }

    // --- Goal Reminder Functions ---
    // These are NOT scheduled via AlarmManager. The switch in the Activity just enables/disables the showing.
    fun scheduleGoalReminder(context: Context) {
        Log.d("NotificationHelper", "Goal Reminders are event-driven. Switch enables/disables showing notifications. No scheduling needed here.")
        // Intentionally empty - no timed alarm to schedule
    }

    fun cancelGoalReminder(context: Context) {
        Log.d("NotificationHelper", "Goal Reminders are event-driven. No scheduled alarm to cancel.")
        // Intentionally empty - no timed alarm to cancel
    }

    // --- Function to call directly when a goal (e.g., 5kg gained) is achieved ---
    @SuppressLint("MissingPermission") // Add suppress lint if needed after manual check
    fun showGoalAchievedNotification(context: Context, weightGainDetail: String) {
        // Check the preference stored by NotificationActivity's switch BEFORE showing
        val prefs = context.getSharedPreferences(NotificationActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val goalNotificationsEnabled = prefs.getBoolean(NotificationActivity.KEY_GOAL_REMINDER, false)

        if (!goalNotificationsEnabled) {
            Log.d("NotificationHelper", "Goal achieved ($weightGainDetail), but Goal Achievement Alerts are disabled by user settings.")
            return // Don't show notification if the switch is off
        }

        Log.i("NotificationHelper", "Attempting to show goal achievement notification: $weightGainDetail")
        createNotificationChannel(context) // Ensure channel exists

        // Optional: Intent to open the app
        val mainActivityIntent = Intent(context, NavigationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val uniqueRequestCode = GOAL_NOTIFICATION_ID_BASE + System.currentTimeMillis().toInt()
        val contentPendingIntent: PendingIntent = PendingIntent.getActivity(
            context, uniqueRequestCode, mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, GOAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.notifications) // *** YOUR ICON HERE ***
            .setContentTitle("Goal Achieved!")
            .setContentText(weightGainDetail) // e.g., "Congratulations! You've gained 5kg."
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)

        val notificationManager = NotificationManagerCompat.from(context)

        // Check POST_NOTIFICATIONS permission (must be granted via Activity first)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e("NotificationHelper", "Cannot show goal notification - POST_NOTIFICATIONS permission missing.")
                return // Cannot show without permission
            }
        }

        // Show the notification
        val uniqueNotificationId = GOAL_NOTIFICATION_ID_BASE + weightGainDetail.hashCode()
        try {
            notificationManager.notify(uniqueNotificationId, builder.build())
            Log.i("NotificationHelper", "Goal Achievement Notification successfully posted (ID: $uniqueNotificationId).")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error posting Goal Achievement notification (ID: $uniqueNotificationId)", e)
        }
    }


    // --- Helper Functions ---
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        return alarmManager?.canScheduleExactAlarms() ?: false
    }

    fun scheduleExactReminderForTime(
        context: Context, requestCode: Int, hour: Int, minute: Int,
        reminderName: String, action: String, receiverClass: Class<*>
    ) {
        // ... (implementation unchanged from previous version using setExactAndAllowWhileIdle) ...
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, receiverClass).apply {
            this.action = action
            putExtra("reminder_type", reminderName)
            putExtra("request_code", requestCode)
            putExtra("reminder_hour", hour)
            putExtra("reminder_minute", minute)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        val triggerTime = calendar.timeInMillis
        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                Log.i("NotificationHelper", "$reminderName EXACT Reminder Scheduled for ${String.format("%tc", calendar)} (RC: $requestCode)")
            } else {
                Log.e("NotificationHelper", "Attempted exact schedule without permission: $reminderName")
            }
        } catch (se: SecurityException) {
            Log.e("NotificationHelper", "SecurityException: $reminderName", se)
            Toast.makeText(context,"Could not schedule exact reminder. Check permissions.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) { Log.e("NotificationHelper", "Exception: $reminderName", e) }
    }


    private fun scheduleInexactRepeatingReminder(
        context: Context, requestCode: Int, triggerAtMillis: Long, intervalMillis: Long,
        reminderName: String, action: String, receiverClass: Class<*>
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, receiverClass).apply {
            this.action = action
            putExtra("reminder_type", reminderName)
            putExtra("request_code", requestCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            // Use setInexactRepeating for battery efficiency
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, intervalMillis, pendingIntent
            )
            Log.i("NotificationHelper", "$reminderName INEXACT Reminder Scheduled. Trigger: ~${String.format("%tc", triggerAtMillis)}, Interval: ${intervalMillis / 1000}s (RC: $requestCode)")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Exception scheduling inexact repeating alarm for $reminderName (RC: $requestCode)", e)
            Toast.makeText(context,"Could not schedule $reminderName reminder.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelReminderForTime(
        context: Context, requestCode: Int, action: String, receiverClass: Class<*>
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, receiverClass).apply { this.action = action }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // Must match flags used in creation
        )
        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel() // Also cancel the PendingIntent itself
            Log.d("NotificationHelper", "Reminder Cancelled (RC: $requestCode Action: $action)")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Exception cancelling reminder (RC: $requestCode Action: $action)", e)
        }
    }
}