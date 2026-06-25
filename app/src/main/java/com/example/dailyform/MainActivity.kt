package com.example.dailyform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * The home screen — what you get when you tap the app icon. It does NOT open
 * the form; the form is reserved for the daily alarm (and the explicit "Open
 * form now" button below). Instead this screen lets you:
 *   - see when the next reminder will fire and on which days,
 *   - check/grant the permissions the alarm relies on, and
 *   - open the form on demand.
 *
 * It also re-arms the daily alarm every time it's shown, so simply opening the
 * app is enough to (re)schedule the reminder.
 */
class MainActivity : AppCompatActivity() {

    private companion object {
        const val REQUEST_POST_NOTIFICATIONS = 1
    }

    // Rebuilt on every onResume so the status reflects whatever the user just
    // changed in a Settings screen they bounced out to.
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(24)
            setPadding(pad, pad, pad, pad)
        }

        val scroll = ScrollView(this).apply {
            addView(
                content,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        // Opening the app is enough to (re)arm the reminder.
        AlarmScheduler.scheduleDaily(this)
        render()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Refresh the notification-permission row once the dialog is answered.
        if (requestCode == REQUEST_POST_NOTIFICATIONS) render()
    }

    /** Wipe and rebuild the whole screen from current state. */
    private fun render() {
        content.removeAllViews()

        content.addView(title(getString(R.string.app_name)))
        content.addView(spacer(8))
        content.addView(body(scheduleSummary()))
        content.addView(spacer(24))

        // The action that actually shows the form. Everything else on this
        // screen is status/setup.
        content.addView(
            actionButton("Open form now") {
                startActivity(Intent(this, FormActivity::class.java))
            }
        )
        content.addView(spacer(24))

        content.addView(sectionHeader("Permissions"))
        content.addView(spacer(8))
        addPermissionRows()
    }

    /** A line describing the next fire time and which days the alarm runs. */
    private fun scheduleSummary(): String {
        val time = LocalDateTime.now()
            .withHour(FormConfig.REMINDER_HOUR)
            .withMinute(FormConfig.REMINDER_MINUTE)
            .format(DateTimeFormatter.ofPattern("h:mm a"))

        if (FormConfig.REMINDER_DAYS.isEmpty()) {
            return "No reminder days are configured, so the form will never " +
                "fire automatically. Use \"Open form now\" below."
        }

        val days = FormConfig.REMINDER_DAYS
            .sorted() // DayOfWeek sorts Monday-first
            .joinToString(", ") {
                it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }

        val next = AlarmScheduler.nextTriggerTime()
            .format(DateTimeFormatter.ofPattern("EEE d MMM 'at' h:mm a"))

        return "Reminder at $time on: $days.\nNext: $next."
    }

    // ===== Permission rows =====

    private fun addPermissionRows() {
        // POST_NOTIFICATIONS (Android 13+): needed for the fallback full-screen
        // notification when the overlay permission is missing.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            content.addView(
                permissionRow(
                    "Notifications",
                    "Lets the reminder show even when the form can't be forced open.",
                    granted
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_POST_NOTIFICATIONS
                    )
                }
            )
        }

        // SYSTEM_ALERT_WINDOW / "Display over other apps": lets the alarm force
        // the form to the foreground even while the phone is unlocked.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            content.addView(
                permissionRow(
                    "Display over other apps",
                    "Lets the reminder pop the form to the front while you're using the phone.",
                    Settings.canDrawOverlays(this)
                ) {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
            )
        }

        // Battery optimization exemption: stops aggressive OEM battery managers
        // from killing the daily alarm.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            content.addView(
                permissionRow(
                    "Ignore battery optimization",
                    "Stops the system from killing the scheduled reminder to save power.",
                    pm.isIgnoringBatteryOptimizations(packageName)
                ) {
                    startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
            )
        }
    }

    /**
     * A labelled permission card showing granted/not-granted status, with a
     * "Grant" button shown only when it's missing.
     */
    private fun permissionRow(
        name: String,
        explanation: String,
        granted: Boolean,
        onGrant: () -> Unit
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(12)
            setPadding(p, p, p, p)
            setBackgroundColor(Color.parseColor("#1F000000"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }

        row.addView(TextView(this).apply {
            text = name
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        })
        row.addView(TextView(this).apply {
            text = explanation
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            alpha = 0.7f
        })
        row.addView(TextView(this).apply {
            text = if (granted) "✓ Granted" else "✗ Not granted"
            setTextColor(if (granted) Color.parseColor("#2E7D32") else Color.parseColor("#C62828"))
            setTypeface(typeface, Typeface.BOLD)
            val t = dp(6)
            setPadding(0, t, 0, 0)
        })

        if (!granted) {
            row.addView(actionButton("Grant", onGrant).apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dp(8)
            })
        }

        return row
    }

    // ===== Small view helpers =====

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

    private fun body(text: String) = TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
