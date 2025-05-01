package com.example.chowcheck // Or your package

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object NotificationHelper {

    const val MEAL_CHANNEL_ID = "meal_reminder_channel_1"
    const val MEAL_CHANNEL_NAME = "Meal Reminders"
    private const val BREAKFAST_REQUEST_CODE = 1001
    private const val LUNCH_REQUEST_CODE = 1002
    private const val DINNER_REQUEST_CODE = 1003
    // --- REMOVED 'private' from the line below ---
    const val MEAL_REMINDER_ACTION = "com.example.chowcheck.MEAL_REMINDER"

    // Create Notification Channel (Call this once, e.g., in Application class or MainActivity)
    fun createNotificationChannel(context: Context) {
        // ... (rest of the function is unchanged) ...
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val descriptionText = "Channel for daily meal time reminders"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(MEAL_CHANNEL_ID, MEAL_CHANNEL_NAME, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationHelper", "Notification channel created.")
        }
    }

    // Schedule daily meal reminders (e.g., 8 AM, 1 PM, 7 PM)
    fun scheduleMealReminder(context: Context) {
        scheduleReminderForTime(context, BREAKFAST_REQUEST_CODE, 8, 0, "Breakfast")
        scheduleReminderForTime(context, LUNCH_REQUEST_CODE, 13, 0, "Lunch") // 1 PM
        scheduleReminderForTime(context, DINNER_REQUEST_CODE, 19, 0, "Dinner") // 7 PM
        Log.d("NotificationHelper", "Meal Reminders Scheduled.")
    }

    // Cancel all meal reminders
    fun cancelMealReminder(context: Context) {
        cancelReminderForTime(context, BREAKFAST_REQUEST_CODE)
        cancelReminderForTime(context, LUNCH_REQUEST_CODE)
        cancelReminderForTime(context, DINNER_REQUEST_CODE)
        Log.d("NotificationHelper", "Meal Reminders Canceled.")
    }

    // --- Private Helper Functions --- (Keep as before) ---
    private fun scheduleReminderForTime(context: Context, requestCode: Int, hour: Int, minute: Int, mealName: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            action = MEAL_REMINDER_ACTION // Uses the now public constant
            putExtra("meal_type", mealName)
            putExtra("request_code", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
        Log.d("NotificationHelper", "$mealName Reminder Scheduled for ~${hour}:${String.format("%02d", minute)} daily.")
    }

    private fun cancelReminderForTime(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            action = MEAL_REMINDER_ACTION // Uses the now public constant
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d("NotificationHelper", "Reminder with request code $requestCode Canceled.")
    }
}