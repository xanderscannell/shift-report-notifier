# Shift Report Notifier

A tiny single-user Android app that fires a daily, alarm-style full-screen
reminder to fill out a Google Form. Tapping the app icon (or the alarm
notification) opens the form pre-filled with today's date, the current time,
your name, and the time-of-day (Morning/Afternoon/Evening). The screen only
closes once the form is actually submitted.

## Setup

1. **Open in Android Studio** and let it Gradle-sync. (It uses its bundled
   JDK; `local.properties` with your `sdk.dir` is generated automatically.)

2. **Create your config.** The personal settings are kept out of git. Copy the
   template and fill in your own values:

   ```sh
   cp app/src/main/java/com/example/dailyform/FormConfig.kt.example \
      app/src/main/java/com/example/dailyform/FormConfig.kt
   ```

   In `FormConfig.kt` set:
   - `FORM_ID` — the long string in your form URL between `/d/e/` and `/viewform`.
   - The `*_ENTRY_ID` values — from your form's **Get pre-filled link** (fill a
     sample response, click *Get link*, and read the `entry.NNN` values out of
     the resulting URL).
   - `YOUR_NAME` and the Morning/Afternoon/Evening labels (must match your
     form's options exactly).

3. **Make the form public.** In the form's **Settings → Responses**, turn off
   *Collect email addresses* and *Limit to 1 response*. Otherwise the in-app
   WebView hits a Google sign-in wall instead of showing the form.

4. **Set your schedule** in `FormConfig.kt`: `REMINDER_HOUR` / `REMINDER_MINUTE`
   for the time, and `REMINDER_DAYS` for which days it fires (e.g. drop
   `SATURDAY`/`SUNDAY` for weekdays only).

5. **Build & run.** On Android 14+, grant the full-screen-intent permission for
   the app once so the reminder can pop over the lock screen.

## Notes

- `FormConfig.kt` and `local.properties` are gitignored.
- The back button is intentionally swallowed in `FormActivity` so the reminder
  can't be dismissed without submitting; remove `onBackPressed()` to change that.
