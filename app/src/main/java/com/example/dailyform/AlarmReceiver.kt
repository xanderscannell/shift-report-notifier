package com.example.dailyform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // If we have "Display over other apps", we're exempt from background
        // activity-launch limits and can force the form straight to the
        // foreground even while the phone is unlocked and in active use. That
        // makes the notification redundant, so we only fall back to it when the
        // overlay permission is missing (e.g. revoked in OEM settings).
        if (canDrawOverlays(context)) {
            val formIntent = Intent(context, FormActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(formIntent)
        } else {
            showFullScreenNotification(context)
        }

        // setAlarmClock only fires once, so re-arm for tomorrow every time.
        AlarmScheduler.scheduleDaily(context)
    }

    private fun canDrawOverlays(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    private fun showFullScreenNotification(context: Context) {
        val channelId = "daily_form_reminder"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // IMPORTANCE_HIGH is required for the full-screen / heads-up behavior.
        val channel = NotificationChannel(
            channelId,
            "Daily Form Reminder",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Reminds you to fill out the daily work form" }
        manager.createNotificationChannel(channel)

        val fullScreenIntent = Intent(context, FormActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fullScreenPending = PendingIntent.getActivity(
            context, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Daily form time")
            .setContentText("Tap to fill out today's form")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // swap for your own icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            // This is the magic: when the screen is locked/off it launches
            // FormActivity full-screen; when unlocked it shows as a heads-up.
            .setFullScreenIntent(fullScreenPending, true)
            .setContentIntent(fullScreenPending)
            .build()

        manager.notify(2002, notification)
    }
}
