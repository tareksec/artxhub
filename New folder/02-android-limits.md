# 02 — What Android allows a launcher to theme (read before promising anything)

> বাংলা সারাংশ: Root ছাড়া কোনো app phone-এর আসল lock screen, notification shade বা quick settings-এর ভেতরটা আঁকতে পারে না। তাই নিচের টেবিলে প্রতিটা জায়গার জন্য "সবচেয়ে কাছের বৈধ পথ" দেওয়া আছে। AI এর বাইরে কিছু চেষ্টা করবে না।

CyberHome is a normal (non-system, non-root) app installed by APK. It can draw only inside its own
windows. SystemUI (status bar, notification shade, quick settings, keyguard, navigation bar) belongs
to the system process and its signature.

## Surface-by-surface

| Surface the owner wants themed | Can we draw it directly? | Closest legitimate approach (what we build) |
|---|---|---|
| Home, icons, all-apps, folders, widgets, search, settings | Yes | Full theming from one theme model (`06`). |
| **Status bar** (top) | Only its *appearance while our window is in front*: transparent background, light/dark icons, show/hide. Cannot change icons, font, layout. | `WindowInsetsControllerCompat.isAppearanceLightStatusBars` driven by the theme token `systemBars.lightIcons` on every CyberHome window (home, drawer, Control Center, Mika Lock, settings). Optional: a themed *scrim* gradient behind it. |
| **Navigation bar** (bottom, gesture pill or 3 buttons) | Same as above: colour/transparency and light/dark only, while our window is in front. | Transparent + theme-driven icon contrast; `isNavigationBarContrastEnforced = false`; draw a themed bottom glow/dock behind it. |
| **Quick Settings / notification shade** (the panel where mobile data / Wi-Fi are switched) | **No.** | **(A) CyberHome Control Center** — swiping down on home opens *our* panel instead of the system shade (Lawnchair already maps "swipe down" gestures; replace the action). Contents in `06`. **(B) Optional system palette (ADB mode)** recolours the *real* shade, see below. A small "Open system panel" button stays in our Control Center. |
| Toggling mobile data | **No** (needs `MODIFY_PHONE_STATE`, system only). | Open `Settings.Panel.ACTION_INTERNET_CONNECTIVITY` (Android 10+ bottom sheet, shows Wi-Fi + mobile data). |
| Toggling Wi-Fi | No on targetSdk ≥ 29 (`setWifiEnabled` is deprecated/ignored). | Same internet panel, or `Settings.Panel.ACTION_WIFI`. Show live state via `ConnectivityManager.NetworkCallback`. |
| Bluetooth | No direct toggle on API 33+ without `BLUETOOTH_CONNECT` + it still opens a system dialog. | `Intent(Settings.ACTION_BLUETOOTH_SETTINGS)`; show state read-only. |
| Flashlight | **Yes** | `CameraManager.setTorchMode` + `TorchCallback`. No camera permission needed. |
| Brightness | **Yes**, with the user granting "Modify system settings" (`WRITE_SETTINGS`, special app access). | Slider writing `Settings.System.SCREEN_BRIGHTNESS`; without the grant, use window brightness only while our panel is open and offer the grant screen. |
| Volume / ringer | Yes (volume); ringer → silent needs DND access on some versions. | `AudioManager.adjustStreamVolume`; ringer modes; DND toggle only if `isNotificationPolicyAccessGranted`. |
| Auto-rotate | Yes with `WRITE_SETTINGS`. | `Settings.System.ACCELEROMETER_ROTATION`. |
| Notifications list | Yes, read-only, via a `NotificationListenerService` the user enables. | Reuse/extend Lawnchair's `NotificationListener`. Tap → `contentIntent.send()`, swipe → `cancelNotification(key)`. |
| **Lock screen (keyguard)** | **No.** We cannot replace or restyle the system keyguard, its clock, or its bouncer. Android 14+ clock faces are only selectable through the system wallpaper picker. | **Mika Lock overlay** (`08`): our own activity shown *over* the keyguard (`setShowWhenLocked(true)`) when the screen turns off. User swipes up → `requestDismissKeyguard` → the **system** PIN/biometric bouncer appears → device unlocks. Plus a **clean** lock wallpaper (art only, no UI) on `FLAG_LOCK` so the system keyguard behind it also looks on-theme. |
| Always-on display | No. | Out of scope. |
| Before first unlock after reboot (Direct Boot) | Our process can't run UI until the user unlocks once (unless direct-boot-aware). | Accept: first lock after reboot shows the system keyguard with our clean Mika wallpaper. Document it. |
| Settings app, keyboard, other apps' colours | Not directly. | Only via the optional system palette (ADB mode) on Android 12+. |

## Optional "Advanced mode": recolour the real system UI (Android 12+, no root)

Android 12+ (Material You) generates the system palette (quick settings, notification shade,
settings, keyboard, many apps) from `Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES`.
Writing it needs `android.permission.WRITE_SECURE_SETTINGS`, which a normal app cannot get — **but
the owner can grant it once from a PC**:

```
adb shell pm grant com.techvrs.cyberhome android.permission.WRITE_SECURE_SETTINGS
```

(Use `com.techvrs.cyberhome.debug` for the debug build.) Then the theme engine writes JSON such as:

```json
{
  "android.theme.customization.color_source": "preset",
  "android.theme.customization.system_palette": "F4A3B4",
  "android.theme.customization.accent_color": "F4A3B4",
  "android.theme.customization.theme_style": "TONAL_SPOT",
  "_applied_timestamp": 1761700000000
}
```

Rules:
- Read and **save the previous value** before writing; "Restore system colours" button writes it back. Also restore on theme switch-off and in the uninstall-safe path (document that uninstalling does not auto-restore; restore first).
- Check permission with `checkSelfPermission`; if missing, the UI shows the adb command with a copy button and a plain explanation. Never ask for it at runtime (impossible).
- Works on near-stock Android (Nokia G10 ✓, Pixel ✓). Samsung One UI / MIUI / ColorOS may ignore or partly apply it — detect by reading back and say so in the UI.
- Supported `theme_style` values differ by version (`TONAL_SPOT`, `VIBRANT`, `EXPRESSIVE`, `SPRITZ`, `RAINBOW`, `FRUIT_SALAD`, `MONOCHROMATIC` on 13+). Default to `TONAL_SPOT`.
- Without ADB mode: setting the theme's wallpaper already makes Android 12+ derive a palette from it (if the user's system colour source is "wallpaper"). Provide `WallpaperColors` hints by choosing art whose dominant colour matches the theme accent.

## Things we will NOT do (unsafe or abusive)
- Accessibility-service overlays that draw a fake status bar/shade over every app. (Fragile, battery-heavy, a privacy red flag, and it can intercept input — rejected.)
- Disabling the keyguard, storing the user's PIN, or any "swipe to unlock without PIN" on a secure device.
- `SYSTEM_ALERT_WINDOW` overlays that cover other apps' content outside the lock flow.
- Root/Magisk/Substratum overlays.
