package com.example.dailyform

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * In-app editor for everything that used to live in FormConfig.kt. Values are
 * persisted through Settings (SharedPreferences). The day-to-day knobs
 * (reminder time, days, name) are shown up front; the Google-Form plumbing
 * (form ID, entry IDs, part-of-day labels/thresholds) is tucked behind an
 * "Advanced" toggle since it's set once and rarely touched.
 *
 * Saving re-arms the daily alarm so a changed time/day takes effect right away.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private val dayChecks = mutableMapOf<DayOfWeek, CheckBox>()
    private lateinit var nameField: EditText

    private lateinit var formIdField: EditText
    private lateinit var dateEntryField: EditText
    private lateinit var timeEntryField: EditText
    private lateinit var nameEntryField: EditText
    private lateinit var podEntryField: EditText
    private lateinit var morningLabelField: EditText
    private lateinit var afternoonLabelField: EditText
    private lateinit var eveningLabelField: EditText
    private lateinit var morningStartField: EditText
    private lateinit var afternoonStartField: EditText
    private lateinit var eveningStartField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(24)
            setPadding(pad, pad, pad, pad)
        }

        content.addView(title(getString(R.string.settings)))
        content.addView(spacer(16))

        // ===== Schedule =====
        content.addView(sectionHeader("Reminder time"))
        content.addView(spacer(8))
        timePicker = TimePicker(this).apply {
            setIs24HourView(true)
            hour = Prefs.reminderHour
            minute = Prefs.reminderMinute
        }
        content.addView(timePicker)
        content.addView(spacer(16))

        content.addView(sectionHeader("Reminder days"))
        content.addView(spacer(8))
        val savedDays = Prefs.reminderDays
        // DayOfWeek.values() is Monday-first, which is the order we want.
        for (day in DayOfWeek.values()) {
            val check = CheckBox(this).apply {
                text = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                isChecked = day in savedDays
            }
            dayChecks[day] = check
            content.addView(check)
        }
        content.addView(spacer(16))

        // ===== Identity =====
        content.addView(sectionHeader("Your name"))
        content.addView(spacer(8))
        nameField = textField(Prefs.yourName, "Name submitted with the form")
        content.addView(nameField)
        content.addView(spacer(24))

        // ===== Advanced (form plumbing) =====
        val advanced = buildAdvancedSection()
        content.addView(advancedToggle(advanced))
        content.addView(advanced)
        content.addView(spacer(24))

        content.addView(actionButton("Save") { save() })

        setContentView(ScrollView(this).apply {
            addView(
                content,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        })
    }

    /** The collapsible block of Google-Form wiring, hidden by default. */
    private fun buildAdvancedSection(): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        box.addView(caption(
            "These map the app to your specific Google Form. Get them from the " +
                "form's URL and its \"Get pre-filled link\" feature."
        ))
        box.addView(spacer(12))

        formIdField = labelled(box, "Form ID", Prefs.formId)
        dateEntryField = labelled(box, "Date entry ID", Prefs.dateEntryId)
        timeEntryField = labelled(box, "Time entry ID", Prefs.timeEntryId)
        nameEntryField = labelled(box, "Name entry ID", Prefs.nameEntryId)
        podEntryField = labelled(box, "Part-of-day entry ID", Prefs.partOfDayEntryId)

        box.addView(spacer(8))
        box.addView(caption(
            "Part-of-day labels must match your form's options exactly, and each " +
                "start hour (0-23) marks where that bucket begins."
        ))
        box.addView(spacer(12))

        morningLabelField = labelled(box, "Morning label", Prefs.morningLabel)
        morningStartField = labelledNumber(box, "Morning start hour", Prefs.morningStartHour)
        afternoonLabelField = labelled(box, "Afternoon label", Prefs.afternoonLabel)
        afternoonStartField = labelledNumber(box, "Afternoon start hour", Prefs.afternoonStartHour)
        eveningLabelField = labelled(box, "Evening label", Prefs.eveningLabel)
        eveningStartField = labelledNumber(box, "Evening start hour", Prefs.eveningStartHour)

        return box
    }

    private fun advancedToggle(target: View): View = TextView(this).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTypeface(typeface, Typeface.BOLD)
        val refresh = { text = if (target.visibility == View.VISIBLE) "Advanced ▾" else "Advanced ▸" }
        refresh()
        setOnClickListener {
            target.visibility = if (target.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            refresh()
        }
    }

    /** Read the fields, validate, persist, and re-arm the alarm. */
    private fun save() {
        val days = dayChecks.filterValues { it.isChecked }.keys
        if (days.isEmpty()) {
            toast("Pick at least one reminder day.")
            return
        }

        val morningStart = morningStartField.text.toString().toHourOrNull()
        val afternoonStart = afternoonStartField.text.toString().toHourOrNull()
        val eveningStart = eveningStartField.text.toString().toHourOrNull()
        if (morningStart == null || afternoonStart == null || eveningStart == null) {
            toast("Start hours must be whole numbers from 0 to 23.")
            return
        }

        if (formIdField.text.isBlank()) {
            toast("Form ID can't be empty.")
            return
        }

        Prefs.reminderHour = timePicker.hour
        Prefs.reminderMinute = timePicker.minute
        Prefs.reminderDays = days
        Prefs.yourName = nameField.text.toString().trim()

        Prefs.formId = formIdField.text.toString().trim()
        Prefs.dateEntryId = dateEntryField.text.toString().trim()
        Prefs.timeEntryId = timeEntryField.text.toString().trim()
        Prefs.nameEntryId = nameEntryField.text.toString().trim()
        Prefs.partOfDayEntryId = podEntryField.text.toString().trim()
        Prefs.morningLabel = morningLabelField.text.toString().trim()
        Prefs.afternoonLabel = afternoonLabelField.text.toString().trim()
        Prefs.eveningLabel = eveningLabelField.text.toString().trim()
        Prefs.morningStartHour = morningStart
        Prefs.afternoonStartHour = afternoonStart
        Prefs.eveningStartHour = eveningStart

        // Apply the (possibly changed) schedule immediately.
        AlarmScheduler.scheduleDaily(this)

        toast("Saved")
        finish()
    }

    private fun String.toHourOrNull(): Int? = trim().toIntOrNull()?.takeIf { it in 0..23 }

    // ===== View helpers =====

    /** Adds a "Label" + text field pair to [parent] and returns the field. */
    private fun labelled(parent: LinearLayout, label: String, value: String): EditText {
        parent.addView(fieldLabel(label))
        val field = textField(value, label)
        parent.addView(field)
        parent.addView(spacer(8))
        return field
    }

    private fun labelledNumber(parent: LinearLayout, label: String, value: Int): EditText {
        parent.addView(fieldLabel(label))
        val field = textField(value.toString(), label).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        parent.addView(field)
        parent.addView(spacer(8))
        return field
    }

    private fun textField(value: String, hintText: String) = EditText(this).apply {
        setText(value)
        hint = hintText
        inputType = InputType.TYPE_CLASS_TEXT
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun fieldLabel(text: String) = TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        alpha = 0.7f
    }

    private fun caption(text: String) = TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        alpha = 0.7f
    }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        setTypeface(typeface, Typeface.BOLD)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
    }

    private fun sectionHeader(text: String) = TextView(this).apply {
        this.text = text
        setTypeface(typeface, Typeface.BOLD)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
    }

    private fun actionButton(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setOnClickListener { onClick() }
    }

    private fun spacer(heightDp: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp)
        )
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
