# Permissions

CyberHome adds **no new permissions** of its own. Everything below is either already declared by
Lawnchair / Launcher3 (a launcher needs them) or is requested at runtime only when the user turns on
an optional feature.

## Used by CyberHome features

| Permission | Declared by | Used for | When asked |
|---|---|---|---|
| `INTERNET` | Lawnchair | Weather widget only (Open-Meteo: city search + current weather, ~1×/hour) | Never (normal permission) |
| `SET_WALLPAPER` | Launcher3 | "Apply Look" sets the bundled dot-grid wallpaper (home screen only) | Never (normal permission) |
| `BIND_APPWIDGET` + system "Allow widget access" dialog | Launcher3 | Placing our widgets from the default layout | Once, in "Apply Look" (tick "Always allow"). If refused, each widget shows "Tap to set up". |
| `POST_NOTIFICATIONS` (Android 13+) | Launcher3 | Optional water reminders | Only when the user enables reminders |
| Notification access (Settings page, not a manifest permission) | Launcher3 `NotificationListener` | Optional song title in the music strip (also powers notification dots) | Only if the user opens "Music track info" and enables it |
| Default home role (`RoleManager.ROLE_HOME`) | — | Becoming the home app | In "Apply Look" (system dialog) |

## Not used

- **No location permission.** The weather city is typed in by the user.
- **No calendar permission.** The month calendar shows dates only (event dots are a later, opt-in idea).
- **No storage permission.** Card images come from the system photo picker, which needs none.
- No ads, analytics or crash reporting. Lawnchair's automatic network calls were turned off
  (settings "live information"/announcements download, GitHub-based About screen, web search
  suggestions in the app drawer); see `UPSTREAM_CHANGES.md`.

## Hardened Security Audit & Removals (Task A2)

The following permissions inherited from upstream were audited and explicitly removed via `tools:node="remove"`:
- `android.permission.CALL_PHONE` (Removed: dialing is handled safely via `Intent.ACTION_DIAL`)
- `android.permission.READ_EXTERNAL_STORAGE` (Removed: image selection uses system Photo Picker)

## Additional Features & Permissions

| Permission | Declared by | Used for | When asked |
|---|---|---|---|
| Camera / Flashlight | System | Hardware flashlight toggle in CyberHome Control Center | Normal feature, no dangerous camera permission required for torch mode |
| `WRITE_SECURE_SETTINGS` | Optional | Material You system palette customization (ADB mode) | Never requested at runtime; granted once by user via PC `adb shell pm grant` |
| `KeyguardManager.requestDismissKeyguard` | System API | Mika Lock screen unlocking flow | Authenticates via system PIN/fingerprint |
