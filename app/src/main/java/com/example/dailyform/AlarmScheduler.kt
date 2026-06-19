package com.example.dailyform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    private const val REQUEST_CODE = 1001

    // Schedule (time + days) lives in FormConfig.

    fun scheduleDaily(context: Context) {
        if (FormConfig.REMINDER_DAYS.isEmpty()) {
            Log.w(TAG, "REMINDER_DAYS is empty; no reminder scheduled.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextTriggerTime()

        // The "show" intent is what opens when the user taps the alarm icon
        // in the status bar. Point it at the form itself.
        val showIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, FormActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        // The intent that actually fires when the alarm goes off.
        val alarmIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // setAlarmClock: fires at the exact time and punches through Doze.
        // It needs an exact-alarm permission (declared in the manifest:
        // USE_EXACT_ALARM on API 33+, SCHEDULE_EXACT_ALARM on 31-32). If the
        // OS still refuses (e.g. user revoked it), fall back to an inexact
        // alarm instead of crashing the app.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            Log.w(TAG, "No exact-alarm permission; using inexact alarm.")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, alarmIntent)
            return
        }

        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                alarmIntent
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "setAlarmClock denied; using inexact alarm.", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, alarmIntent)
        }
    }

    private fun nextTriggerTime(): Long {
        val now = LocalDateTime.now()
        var candidate = now.toLocalDate()
            .atTime(FormConfig.REMINDER_HOUR, FormConfig.REMINDER_MINUTE)

        // Advance a day at a time until the slot is both in the future and on
        // an allowed day. Caller guarantees REMINDER_DAYS is non-empty, so this
        // terminates within at most 8 iterations.
        while (!candidate.isAfter(now) || candidate.dayOfWeek !in FormConfig.REMINDER_DAYS) {
            candidate = candidate.plusDays(1)
        }

        return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
