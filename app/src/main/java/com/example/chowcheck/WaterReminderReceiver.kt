package com.example.chowcheck // Or your package

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar // Needed for optional time check

class WaterReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WaterReminderReceiver", "onReceive triggered. Action: ${intent.action}")
        val appContext = context.applicationContext ?: return

        if (intent.action == NotificationHelper.WATER_REMINDER_ACTION) {
            val reminderType = intent.getStringExtra("reminder_type") ?: "Water"
            val requestCode = intent.getIntExtra("request_code", NotificationHelper.WATER_REQUEST_CODE) // Use constant as fallback

            Log.d("WaterReminderReceiver", "Received reminder: $reminderType, RC: $requestCode")

            // Optional: Add check to only notify during certain hours (e.g., 8 AM to 10 PM)
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (currentHour < 8 || currentHour >= 22) { // Example: 8 AM to 9:59 PM window
                Log.d("WaterReminderReceiver", "Skipping water notification outside allowed hours (Current Hour: $currentHour).")
                return // Don't show notification outside window
            }

            showWaterReminderNotification(appContext, reminderType, requestCode)
            // NOTE: No rescheduling needed here because we used setInexactRepeating
        }
    }

    @SuppressLint("MissingPermission") // Assuming check is done before notify
    private fun showWaterReminderNotification(context: Context, reminderType: String, notificationId: Int) {
        Log.d("WaterReminderReceiver", "Attempting to show notification for $reminderType")
        NotificationHelper.createNotificationChannel(context) // Ensure channel exists

        val mainActivityIntent = Intent(context, NavigationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // putExtra("NAVIGATE_TO", "SOMEWHERE_RELEVANT")
        }
        val contentPendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1, // Use slightly different code for PendingIntent uniqueness if needed
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NotificationHelper.WATER_CHANNEL_ID)
            .setSmallIcon(R.drawable.notifications) // *** YOUR ICON HERE ***
            .setContentTitle("Stay Hydrated!")
            .setContentText("Time to drink some water.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        val notificationManager = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e("WaterReminderReceiver", "POST_NOTIFICATIONS permission NOT granted.")
                return
            }
        }

        try {
            notificationManager.notify(notificationId, builder.build()) // Use constant request code as ID
            Log.i("WaterReminderReceiver", "$reminderType Reminder Notification posted (ID: $notificationId).")
        } catch (e: Exception) {
            Log.e("WaterReminderReceiver", "Error posting notification (ID: $notificationId)", e)
        }
    }
}