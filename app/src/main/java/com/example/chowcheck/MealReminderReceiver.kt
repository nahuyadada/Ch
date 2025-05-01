package com.example.chowcheck // Or your package

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log // Import Log
import androidx.core.app.ActivityCompat // Import ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MealReminderReceiver : BroadcastReceiver() {

    // Use different IDs for different notifications if they can appear simultaneously
    private val BASE_NOTIFICATION_ID = 100

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.MEAL_REMINDER_ACTION) {
            val mealType = intent.getStringExtra("meal_type") ?: "Meal"
            val requestCode = intent.getIntExtra("request_code", 0)
            Log.d("MealReminderReceiver", "Received reminder for: $mealType")
            showMealReminderNotification(context, mealType, requestCode)
        }
    }

    private fun showMealReminderNotification(context: Context, mealType: String, requestCode: Int) {
        // Create Intent to open LandingActivity
        val mainActivityIntent = Intent(context, LandingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Optional: Add extra to tell LandingActivity to navigate to FoodLogFragment
            // putExtra("NAVIGATE_TO", "FOOD_LOG")
        }
        // Use unique request code for PendingIntent to avoid conflicts
        val contentPendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            requestCode, // Use alarm's request code for uniqueness
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val builder = NotificationCompat.Builder(context, NotificationHelper.MEAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.notifications) // *** REPLACE with your icon ***
            .setContentTitle("$mealType Reminder")
            .setContentText("Time to log your $mealType!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent) // Intent to launch when tapped
            .setAutoCancel(true) // Dismiss notification when tapped

        // Get NotificationManagerCompat
        val notificationManager = NotificationManagerCompat.from(context)

        // --- Permission Check (Required on Android 13+) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // IMPORTANT: Cannot show notification if permission is not granted.
                // The permission should ideally be requested from the Activity UI.
                // If permission is missing here, it means something went wrong or it was revoked.
                Log.e("MealReminderReceiver", "POST_NOTIFICATIONS permission not granted. Cannot show notification.")
                return // Exit without showing notification
            }
        }
        // -----------------------------------------------

        // Show notification (use requestCode as notification ID to allow updating/canceling specific meal times if needed)
        notificationManager.notify(requestCode, builder.build())
        Log.d("MealReminderReceiver", "$mealType Reminder Notification Shown.")
    }
}