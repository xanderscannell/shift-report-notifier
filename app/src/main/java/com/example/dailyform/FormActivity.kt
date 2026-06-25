package com.example.dailyform

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FormActivity : AppCompatActivity() {

    // The form screen. Reached two ways: forced to the foreground by
    // AlarmReceiver at reminder time, or opened on demand from MainActivity's
    // "Open form now" button. Either way its only job is to show the prefilled
    // form and close once it's submitted. Alarm scheduling and permission
    // prompts live in MainActivity, not here — bouncing the user to a Settings
    // screen while the form is up would be jarring.
    //
    // All per-user settings (form ID, entry IDs, name, labels) live in
    // FormConfig.kt, which is gitignored. Copy FormConfig.kt.example to
    // FormConfig.kt and fill in your own values.

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw this activity on top of the lock screen and wake the display.
        // setShowWhenLocked is the key: it shows (and lets you interact with) the
        // form over the keyguard WITHOUT unlocking. We deliberately do NOT call
        // requestDismissKeyguard here — on a secure lock that forces a PIN/biometric
        // prompt, which is exactly the unlock step we want to avoid.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

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

    private fun buildFormUrl(): String {
        val now = LocalDateTime.now()
        val date = now.toLocalDate().toString()                       // YYYY-MM-DD
        val time = now.format(DateTimeFormatter.ofPattern("HH:mm"))   // 24-hour HH:mm

        // Derive Morning/Afternoon/Evening from the current hour, using the
        // thresholds and labels from Prefs. Evening is the wrap-around
        // bucket (everything outside the morning/afternoon windows).
        val partOfDay = when (now.hour) {
            in Prefs.morningStartHour until Prefs.afternoonStartHour ->
                Prefs.morningLabel
            in Prefs.afternoonStartHour until Prefs.eveningStartHour ->
                Prefs.afternoonLabel
            else -> Prefs.eveningLabel
        }

        return Uri.parse("https://docs.google.com/forms/d/e/${Prefs.formId}/viewform")
            .buildUpon()
            .appendQueryParameter(Prefs.dateEntryId, date)
            .appendQueryParameter(Prefs.timeEntryId, time)
            .appendQueryParameter(Prefs.nameEntryId, Prefs.yourName)
            .appendQueryParameter(Prefs.partOfDayEntryId, partOfDay)
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