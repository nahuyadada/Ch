package com.example.chowcheck // Or your package

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context // Needed for SharedPreferences
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MealReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MealReminderReceiver", "onReceive triggered for EXACT alarm. Action: ${intent.action}")

        val appContext = context.applicationContext ?: run {
            Log.e("MealReminderReceiver", "Context is null in onReceive")
            return
        }

        if (intent.action == NotificationHelper.MEAL_REMINDER_ACTION) {
            val mealType = intent.getStringExtra("reminder_type") ?: "Meal"
            val requestCode = intent.getIntExtra("request_code", 0)
            // Get original scheduled hour/minute from intent ONLY as fallback maybe?
            // val originalHour = intent.getIntExtra("reminder_hour", -1)
            // val originalMinute = intent.getIntExtra("reminder_minute", -1)


            if (requestCode == 0) { // || originalHour == -1 || originalMinute == -1) { // Don't strictly need original time now
                Log.e("MealReminderReceiver", "Received invalid intent data. RC: $requestCode. Cannot process or reschedule.")
                return
            }

            Log.d("MealReminderReceiver", "Received EXACT reminder via intent. Type: $mealType, RC: $requestCode") // Removed Time: H:M as it's not used directly

            // --- Show the notification ---
            showMealReminderNotification(appContext, mealType, requestCode)

            // --- RESCHEDULE FOR THE NEXT DAY using time from SharedPreferences ---
            if (NotificationHelper.canScheduleExactAlarms(appContext)) {
                // Get SharedPreferences to read the user's set time
                val prefs = appContext.getSharedPreferences(NotificationActivity.PREFS_NAME, Context.MODE_PRIVATE)
                var rescheduleHour = -1
                var rescheduleMinute = -1

                // Determine which time to fetch based on the request code
                when (requestCode) {
                    NotificationHelper.BREAKFAST_REQUEST_CODE -> {
                        rescheduleHour = prefs.getInt(NotificationActivity.KEY_BREAKFAST_HOUR, NotificationActivity.DEFAULT_BREAKFAST_HOUR)
                        rescheduleMinute = prefs.getInt(NotificationActivity.KEY_BREAKFAST_MINUTE, NotificationActivity.DEFAULT_BREAKFAST_MINUTE)
                    }
                    NotificationHelper.LUNCH_REQUEST_CODE -> {
                        rescheduleHour = prefs.getInt(NotificationActivity.KEY_LUNCH_HOUR, NotificationActivity.DEFAULT_LUNCH_HOUR)
                        rescheduleMinute = prefs.getInt(NotificationActivity.KEY_LUNCH_MINUTE, NotificationActivity.DEFAULT_LUNCH_MINUTE)
                    }
                    NotificationHelper.DINNER_REQUEST_CODE -> {
                        rescheduleHour = prefs.getInt(NotificationActivity.KEY_DINNER_HOUR, NotificationActivity.DEFAULT_DINNER_HOUR)
                        rescheduleMinute = prefs.getInt(NotificationActivity.KEY_DINNER_MINUTE, NotificationActivity.DEFAULT_DINNER_MINUTE)
                    }
                    else -> {
                        Log.e("MealReminderReceiver", "Unknown request code ($requestCode) for rescheduling.")
                    }
                }

                if(rescheduleHour != -1) {
                    Log.d("MealReminderReceiver", "Rescheduling EXACT reminder for $mealType for the next day at $rescheduleHour:$rescheduleMinute.")
                    NotificationHelper.scheduleExactReminderForTime(
                        appContext,
                        requestCode,      // Use the same request code
                        rescheduleHour,   // Use hour loaded from Prefs
                        rescheduleMinute, // Use minute loaded from Prefs
                        mealType,
                        NotificationHelper.MEAL_REMINDER_ACTION,
                        MealReminderReceiver::class.java
                    )
                }
            } else {
                Log.w("MealReminderReceiver", "Cannot reschedule exact alarm for $mealType. Permission SCHEDULE_EXACT_ALARM not granted.")
            }
            // --------------------------------

        } else {
            Log.w("MealReminderReceiver", "Received intent with unexpected action: ${intent.action}")
        }
    }

    // --- showMealReminderNotification function remains unchanged ---
    @SuppressLint("MissingPermission")
    private fun showMealReminderNotification(context: Context, mealType: String, requestCode: Int) {
        /* ... unchanged ... */
        Log.d("MealReminderReceiver", "Attempting to show notification for $mealType (RC: $requestCode)")
        NotificationHelper.createNotificationChannel(context)
        val mainActivityIntent = Intent(context, NavigationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "FOOD_LOG_FRAGMENT")
            putExtra("notification_request_code", requestCode)
        }
        val contentPendingIntent: PendingIntent = PendingIntent.getActivity(
            context, requestCode, mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, NotificationHelper.MEAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.notifications) // *** YOUR ICON HERE ***
            .setContentTitle("Meal Reminder").setContentText("Time to log your $mealType!")
            .setPriority(NotificationCompat.PRIORITY_HIGH).setContentIntent(contentPendingIntent)
            .setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER)
        val notificationManager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e("MealReminderReceiver", "POST_NOTIFICATIONS permission NOT granted. Cannot show notification for $mealType.")
                return
            }
        }
        try {
            notificationManager.notify(requestCode, builder.build())
            Log.i("MealReminderReceiver", "$mealType Reminder Notification successfully posted (ID: $requestCode).")
        } catch (e: Exception) {
            Log.e("MealReminderReceiver", "Error posting notification (ID: $requestCode)", e)
        }
    }
}