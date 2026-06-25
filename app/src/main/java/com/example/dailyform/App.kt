package com.example.dailyform

import android.app.Application

/**
 * Initializes Prefs before any Activity or BroadcastReceiver can touch it.
 * Application.onCreate is guaranteed to run first, so Prefs is always ready by
 * the time AlarmReceiver/BootReceiver/AlarmScheduler read from it.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
}
