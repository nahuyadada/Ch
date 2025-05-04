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

class ProgressReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ProgressReminderRcvr", "onReceive triggered. Action: ${intent.action}")
        val appContext = context.applicationContext ?: return

        if (intent.action == NotificationHelper.PROGRESS_REMINDER_ACTION) {
            val reminderType = intent.getStringExtra("reminder_type") ?: "Progress"
            val requestCode = intent.getIntExtra("request_code", NotificationHelper.WEEKLY_PROGRESS_REQUEST_CODE)

            Log.d("ProgressReminderRcvr", "Received reminder: $reminderType, RC: $requestCode")

            // TODO: Add logic here to fetch the actual weekly progress data to show
            val progressSummary = "Check out your progress this week!" // Placeholder text

            showProgressReminderNotification(appContext, reminderType, requestCode, progressSummary)
            // NOTE: No rescheduling needed here because we used setInexactRepeating
        }
    }

    @SuppressLint("MissingPermission")
    private fun showProgressReminderNotification(context: Context, reminderType: String, notificationId: Int, summary: String) {
        Log.d("ProgressReminderRcvr", "Attempting to show notification for $reminderType")
        NotificationHelper.createNotificationChannel(context) // Ensure channel exists

        val mainActivityIntent = Intent(context, NavigationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "PROGRESS_FRAGMENT") // Navigate to progress screen
        }
        val contentPendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1, // Use slightly different code
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NotificationHelper.PROGRESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.notifications) // *** YOUR ICON HERE ***
            .setContentTitle("Weekly Progress Update")
            .setContentText(summary) // Use fetched summary here
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary)) // Allow longer text
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS) // Status category

        val notificationManager = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e("ProgressReminderRcvr", "POST_NOTIFICATIONS permission NOT granted.")
                return
            }
        }

        try {
            notificationManager.notify(notificationId, builder.build()) // Use constant request code as ID
            Log.i("ProgressReminderRcvr", "$reminderType Reminder Notification posted (ID: $notificationId).")
        } catch (e: Exception) {
            Log.e("ProgressReminderRcvr", "Error posting notification (ID: $notificationId)", e)
        }
    }
}