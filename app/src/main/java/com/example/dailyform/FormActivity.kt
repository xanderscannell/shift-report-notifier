package com.example.dailyform

import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FormActivity : AppCompatActivity() {

    // All per-user settings (form ID, entry IDs, name, labels) live in
    // FormConfig.kt, which is gitignored. Copy FormConfig.kt.example to
    // FormConfig.kt and fill in your own values.

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the screen appear over the lock screen and wake the display.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
            .requestDismissKeyguard(this, null)

        // Setup that used to live in MainActivity: make sure the daily alarm is
        // armed and we're allowed to post the notification that fires it.
        ensureSetup()

        val webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true   // Google Forms requires JS
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                // After a successful submit, Forms lands on a "formResponse"
                // URL. Closing only then = you can't dodge it by hitting back.
                if (url != null && url.contains("formResponse")) {
                    finish()
                }
            }
        }

        webView.loadUrl(buildFormUrl())
    }

    private fun ensureSetup() {
        AlarmScheduler.scheduleDaily(this)

        // On Android 13+ we need POST_NOTIFICATIONS for the alarm's full-screen
        // notification to show. Only prompts if it isn't already granted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
            )
        }
    }

    private fun buildFormUrl(): String {
        val now = LocalDateTime.now()
        val date = now.toLocalDate().toString()                       // YYYY-MM-DD
        val time = now.format(DateTimeFormatter.ofPattern("HH:mm"))   // 24-hour HH:mm

        // Derive Morning/Afternoon/Evening from the current hour, using the
        // thresholds and labels from FormConfig. Evening is the wrap-around
        // bucket (everything outside the morning/afternoon windows).
        val partOfDay = when (now.hour) {
            in FormConfig.MORNING_START_HOUR until FormConfig.AFTERNOON_START_HOUR ->
                FormConfig.MORNING_LABEL
            in FormConfig.AFTERNOON_START_HOUR until FormConfig.EVENING_START_HOUR ->
                FormConfig.AFTERNOON_LABEL
            else -> FormConfig.EVENING_LABEL
        }

        return Uri.parse("https://docs.google.com/forms/d/e/${FormConfig.FORM_ID}/viewform")
            .buildUpon()
            .appendQueryParameter(FormConfig.DATE_ENTRY_ID, date)
            .appendQueryParameter(FormConfig.TIME_ENTRY_ID, time)
            .appendQueryParameter(FormConfig.NAME_ENTRY_ID, FormConfig.YOUR_NAME)
            .appendQueryParameter(FormConfig.PART_OF_DAY_ENTRY_ID, partOfDay)
            .build()
            .toString()
    }

    // Swallow the back button so the only way out is submitting the form.
    // Delete this whole method if you'd rather be able to bail out early.
    @Deprecated("Old back API, but fine for a single-user personal app")
    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        // no-op
    }
}