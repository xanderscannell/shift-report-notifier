package com.example.dailyform

import android.content.Context
import android.content.SharedPreferences
import java.time.DayOfWeek

/**
 * User-editable settings, backed by SharedPreferences and edited from
 * SettingsActivity. Every value falls back to its FormConfig counterpart until
 * the user overrides it, so FormConfig is now just the seed/defaults: a fresh
 * install behaves exactly as before, and anything the user changes in the app
 * takes precedence from then on.
 *
 * Call init() once before any access — App.onCreate does this, which runs
 * before any Activity or BroadcastReceiver.
 *
 * Named Prefs rather than Settings to avoid colliding with
 * android.provider.Settings, which several screens also use.
 */
object Prefs {

    private const val PREFS = "form_settings"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    // ===== Schedule =====

    var reminderHour: Int
        get() = prefs.getInt(KEY_HOUR, FormConfig.REMINDER_HOUR)
        set(v) = prefs.edit().putInt(KEY_HOUR, v).apply()

    var reminderMinute: Int
        get() = prefs.getInt(KEY_MINUTE, FormConfig.REMINDER_MINUTE)
        set(v) = prefs.edit().putInt(KEY_MINUTE, v).apply()

    var reminderDays: Set<DayOfWeek>
        get() {
            val stored = prefs.getStringSet(KEY_DAYS, null) ?: return FormConfig.REMINDER_DAYS
            // Ignore anything unparseable rather than crashing on a bad write.
            return stored.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }.toSet()
        }
        set(v) = prefs.edit().putStringSet(KEY_DAYS, v.map { it.name }.toSet()).apply()

    // ===== Identity =====

    var yourName: String
        get() = prefs.getString(KEY_NAME, FormConfig.YOUR_NAME)!!
        set(v) = prefs.edit().putString(KEY_NAME, v).apply()

    // ===== Form details (advanced) =====

    var formId: String
        get() = prefs.getString(KEY_FORM_ID, FormConfig.FORM_ID)!!
        set(v) = prefs.edit().putString(KEY_FORM_ID, v).apply()

    var dateEntryId: String
        get() = prefs.getString(KEY_DATE_ENTRY, FormConfig.DATE_ENTRY_ID)!!
        set(v) = prefs.edit().putString(KEY_DATE_ENTRY, v).apply()

    var timeEntryId: String
        get() = prefs.getString(KEY_TIME_ENTRY, FormConfig.TIME_ENTRY_ID)!!
        set(v) = prefs.edit().putString(KEY_TIME_ENTRY, v).apply()

    var nameEntryId: String
        get() = prefs.getString(KEY_NAME_ENTRY, FormConfig.NAME_ENTRY_ID)!!
        set(v) = prefs.edit().putString(KEY_NAME_ENTRY, v).apply()

    var partOfDayEntryId: String
        get() = prefs.getString(KEY_POD_ENTRY, FormConfig.PART_OF_DAY_ENTRY_ID)!!
        set(v) = prefs.edit().putString(KEY_POD_ENTRY, v).apply()

    var morningLabel: String
        get() = prefs.getString(KEY_MORNING_LABEL, FormConfig.MORNING_LABEL)!!
        set(v) = prefs.edit().putString(KEY_MORNING_LABEL, v).apply()

    var afternoonLabel: String
        get() = prefs.getString(KEY_AFTERNOON_LABEL, FormConfig.AFTERNOON_LABEL)!!
        set(v) = prefs.edit().putString(KEY_AFTERNOON_LABEL, v).apply()

    var eveningLabel: String
        get() = prefs.getString(KEY_EVENING_LABEL, FormConfig.EVENING_LABEL)!!
        set(v) = prefs.edit().putString(KEY_EVENING_LABEL, v).apply()

    var morningStartHour: Int
        get() = prefs.getInt(KEY_MORNING_START, FormConfig.MORNING_START_HOUR)
        set(v) = prefs.edit().putInt(KEY_MORNING_START, v).apply()

    var afternoonStartHour: Int
        get() = prefs.getInt(KEY_AFTERNOON_START, FormConfig.AFTERNOON_START_HOUR)
        set(v) = prefs.edit().putInt(KEY_AFTERNOON_START, v).apply()

    var eveningStartHour: Int
        get() = prefs.getInt(KEY_EVENING_START, FormConfig.EVENING_START_HOUR)
        set(v) = prefs.edit().putInt(KEY_EVENING_START, v).apply()

    private const val KEY_HOUR = "reminder_hour"
    private const val KEY_MINUTE = "reminder_minute"
    private const val KEY_DAYS = "reminder_days"
    private const val KEY_NAME = "your_name"
    private const val KEY_FORM_ID = "form_id"
    private const val KEY_DATE_ENTRY = "date_entry_id"
    private const val KEY_TIME_ENTRY = "time_entry_id"
    private const val KEY_NAME_ENTRY = "name_entry_id"
    private const val KEY_POD_ENTRY = "pod_entry_id"
    private const val KEY_MORNING_LABEL = "morning_label"
    private const val KEY_AFTERNOON_LABEL = "afternoon_label"
    private const val KEY_EVENING_LABEL = "evening_label"
    private const val KEY_MORNING_START = "morning_start_hour"
    private const val KEY_AFTERNOON_START = "afternoon_start_hour"
    private const val KEY_EVENING_START = "evening_start_hour"
}
