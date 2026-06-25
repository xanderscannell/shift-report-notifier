# Shift Report Notifier

A tiny single-user Android app that fires a daily, alarm-style full-screen
reminder to fill out a Google Form. When the alarm fires (or you tap **Open
form now**), the form opens pre-filled with today's date, the current time,
your name, and the time-of-day (Morning/Afternoon/Evening). The screen only
closes once the form is actually submitted.

Tapping the app icon opens a home screen showing the next reminder and the
permissions the alarm relies on, plus a gear icon (top-right) that opens an
in-app **Settings** screen where you can edit everything at runtime — the
reminder time/days, your name, and (under *Advanced*) the form ID and field
mappings.

## Setup

1. **Open in Android Studio** and let it Gradle-sync. (It uses its bundled
   JDK; `local.properties` with your `sdk.dir` is generated automatically.)

2. **Create your config.** `FormConfig.kt` holds the *seed defaults* a fresh
   install starts with, and is kept out of git. Copy the template:

   ```sh
   cp app/src/main/java/com/example/dailyform/FormConfig.kt.example \
      app/src/main/java/com/example/dailyform/FormConfig.kt
   ```

   You can either fill in your values here or leave the placeholders and set
   everything up in the app's **Settings** screen after launch (those edits are
   stored in SharedPreferences and take precedence over `FormConfig.kt`). The
   values you'll need:
   - `FORM_ID` — the long string in your form URL between `/d/e/` and `/viewform`.
   - The `*_ENTRY_ID` values — from your form's **Get pre-filled link** (fill a
     sample response, click *Get link*, and read the `entry.NNN` values out of
     the resulting URL).
   - `YOUR_NAME` and the Morning/Afternoon/Evening labels (must match your
     form's options exactly).
   - `REMINDER_HOUR` / `REMINDER_MINUTE` for the time, and `REMINDER_DAYS` for
     which days it fires (e.g. drop `SATURDAY`/`SUNDAY` for weekdays only).

3. **Make the form public.** In the form's **Settings → Responses**, turn off
   *Collect email addresses* and *Limit to 1 response*. Otherwise the in-app
   WebView hits a Google sign-in wall instead of showing the form.

4. **Build & run.** Open the app once and grant the permissions listed on the
   home screen (full-screen / display-over-other-apps, notifications, and the
   battery-optimization exemption) so the reminder can pop over the lock screen
   and survive aggressive battery managers. After launch, you can adjust any of
   the config above from the gear → **Settings** screen without rebuilding.

## Notes

- `FormConfig.kt` and `local.properties` are gitignored. `FormConfig.kt` only
  seeds the initial values; runtime edits live in SharedPreferences (`Prefs`).
- The back button is intentionally swallowed in `FormActivity` so the reminder
  can't be dismissed without submitting; remove `onBackPressed()` to change that.
