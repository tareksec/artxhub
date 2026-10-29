# 08 — Mika Lock: architecture & implementation guide

> বাংলা সারাংশ: Screen বন্ধ হলেই আমাদের Mika Lock তৈরি হয়ে system lock-এর ওপরে বসে থাকে। Screen চালু করলে প্রথমে Mika Lock দেখা যায়। উপরে swipe করলে phone-এর নিজস্ব PIN/fingerprint আসে — আমরা নিরাপত্তায় হাত দিই না। Call, alarm, camera চলার সময় Mika Lock আসবে না।

## 1. Components (package `com.techvrs.cyberhome.mika.lock` + shared host in `com.techvrs.cyberhome.cyber.lock`)
| Class | Role |
|---|---|
| `cyber.lock.LockOverlayController` | Theme-agnostic. Registers a **dynamic** receiver for `ACTION_SCREEN_OFF`, `ACTION_SCREEN_ON`, `ACTION_USER_PRESENT` (these can't be manifest-declared). Decides whether to show the overlay; launches/finishes `LockHostActivity`. Lives in the launcher process, started from `CyberInitProvider`/`LawnchairApp` init when the user enabled "Theme lock screen". |
| `cyber.lock.LockHostActivity` | `showWhenLocked=true`, `turnScreenOn=false`, `excludeFromRecents=true`, `launchMode="singleInstance"`, own `taskAffinity` `${applicationId}.lock`, `exported=false`, `screenOrientation="portrait"`, `noHistory=false`, `resizeableActivity=false`, theme `Cyber.Lock` (transparent bars, no animation, `windowShowWallpaper=false`). Hosts the renderer returned by the active theme. Applies `FLAG_SECURE`. |
| `cyber.lock.LockLayoutRenderer` | Interface: `createView(context, deps): View`, `onScreenOn()`, `onScreenOff()`, `onDataChanged(LockState)`. |
| `cyber.lock.LockStateRepository` | Combines flows: time, battery, weather (existing Open-Meteo cache), media (`MediaSessionTracker`), notifications (listener), user settings → `StateFlow<LockState>`. |
| `cyber.lock.KeepAliveService` (optional) | Foreground service, type `specialUse` (manifest `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="Custom lock screen must react to screen-off"/>`), only if user turns on "Make lock screen more reliable". Shows a minimal, silent notification. |
| `mika.lock.MikaLockRenderer` | Implements `LockLayoutRenderer`: a `FrameLayout` with an art layer (`ImageView`s or one `MikaLockArtView`), `MikaLockContentView` (custom Canvas view for E1–E13 with precomputed layouts), notifications pill/sheet, unlock gesture handler. Replaces `MikaLockScreenView` drawing logic; keep `MikaLockScreenWidget` as a home widget that reuses the same content view (home variant shows no unlock hint/notifications). |
| `mika.lock.MikaLockSettingsActivity` (Compose) | Name, quote, progress source (battery/music), dark lock, doodle animations, notification display, test preview. |

## 2. Show / hide flow
```
SCREEN_OFF ─► LockOverlayController.shouldShow()?
               ├─ theme lock enabled && overlay permission path OK
               ├─ not in call (AudioManager.mode != IN_CALL/IN_COMMUNICATION/RINGTONE)
               ├─ no alarm/timer full-screen activity on top (best effort)
               └─ not already showing
            ─► startActivity(LockHostActivity) while the screen is off
               (pre-draw so SCREEN_ON shows it instantly)
SCREEN_ON  ─► renderer.onScreenOn() (resume animations, refresh time)
Swipe up   ─► if keyguardManager.isKeyguardLocked:
                  requestDismissKeyguard(activity, callback)
                    onDismissSucceeded → finish() (+ run pending action, e.g. open notification)
                    onDismissCancelled/Error → animate overlay back in
              else finish()
USER_PRESENT (unlocked some other way, e.g. fingerprint on the power key, Smart Lock) ─► finish()
```
- **Background activity start (Android 10+ restriction):** verify on the Nokia whether starting `LockHostActivity` from `SCREEN_OFF` works when another app was in the foreground. Known exemptions: our launcher is visible (home was in front), or the app holds `SYSTEM_ALERT_WINDOW` (user-granted "Display over other apps"). Plan: try without; if blocked (logcat: `Background activity start ... blocked`), guide the user to grant overlay permission in onboarding (opt-in). Fallback: full-screen-intent notification (`USE_FULL_SCREEN_INTENT`; on Android 14+ the user must allow it in settings). Document the result in `PROGRESS.md`.
- **Fingerprint unlock while Mika Lock is visible:** the system unlocks the keyguard; we receive `USER_PRESENT` (and `onDismissSucceeded` is not called) → finish immediately with a quick fade.
- **Face unlock / Smart Lock "trusted"**: same as above; if `isKeyguardLocked == false` on screen-on, Mika Lock still shows but swipe just finishes (no bouncer).
- Never call `finish()` on SCREEN_OFF; keep the instance for the next screen-on.

## 3. Edge cases (each needs a manual test, see `10`)
| Case | Behaviour |
|---|---|
| Incoming call while locked | Phone app's call UI (`showWhenLocked`) must be on top. On ringing (`AudioManager.MODE_RINGTONE` / `TelephonyCallback` only if permission is already granted) → finish Mika Lock; do not re-show until the next SCREEN_OFF after the call ends. |
| Alarm firing | Clock app's alarm activity is shown over keyguard; we must not cover it. Don't show if `AlarmManager.nextAlarmClock` triggers within ±1 min of now; also detect our activity being covered (`onPause` while screen on). |
| Double-press power → camera | System launches secure camera; our activity gets `onPause`/`onStop` — do not relaunch. |
| Media with its own lock UI | Allowed; our music card is just another controller. |
| Rotation | Lock is portrait-only. |
| Multi-user / work profile | Show only for the current user; never list work-profile notifications on lock if the work profile is locked. |
| Battery saver | Disable parallax/blur/animations; static render only. |
| Launcher process killed | Next start re-registers receivers; with KeepAliveService off, the first lock after the kill shows the system keyguard (clean Mika wallpaper) — acceptable, documented. |
| Reboot (Direct Boot) | System keyguard + clean Mika lock wallpaper until first unlock. |
| Theme switched / Mika Lock turned off | Controller unregisters; any visible instance finishes. |
| Accessibility (TalkBack) | Swipe-up replaced by a double-tap "Unlock" action; all controls have virtual nodes. |

## 4. Rendering details
- `MikaLockContentView`: compute every rect in `onSizeChanged` from the fractions in `07`, using the **full screen size** (not insets-adjusted), then nudge E1 down only if it would collide with the real status bar inset (`WindowInsets.statusBars().top`) — on the Nokia it shouldn't.
- Precreate `StaticLayout`s for title/artist/note; rebuild only on data change.
- Clock: measure with `Paint.getTextBounds` for the cap height so digits sit exactly between y 0.161–0.269 regardless of font metrics.
- Album art decoded off-main to the exact art box size; keep last 3 in an `LruCache`.
- Art layer: decode the WebPs at screen size once; keep as `HARDWARE` bitmaps; set via `ImageView` so the RenderThread caches them.
- Blur behind the music card on API 31+: `RenderEffect` on a dedicated child view that draws the art region under the card (snapshot once per art change), not live.
- Accessibility: `ExploreByTouchHelper` with nodes for clock, date/weather, battery, each media button, note, notifications pill, unlock.

## 5. Wallpapers
- `FLAG_LOCK` wallpaper = the **clean** composed art (L0 + L1, no text) so whenever the system keyguard/bouncer is visible it matches Mika and never shows a second clock.
- Remove `mika_lockscreen_art.jpg` from `res/` (it's the reference, with baked UI). Move it to `mika/design/reference/`.
- On theme revert, restore the previous lock wallpaper (store the user's original via `WallpaperManager.getWallpaperFile(FLAG_LOCK)` copy before first apply, if readable; otherwise fall back to the Cyber wallpaper and tell the user).

## 6. Manifest additions (mika/cyber manifests, all `exported=false` except where noted)
```xml
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<activity
    android:name="com.techvrs.cyberhome.cyber.lock.LockHostActivity"
    android:exported="false"
    android:showWhenLocked="true"
    android:turnScreenOn="false"
    android:excludeFromRecents="true"
    android:launchMode="singleInstance"
    android:taskAffinity="${applicationId}.lock"
    android:screenOrientation="portrait"
    android:resizeableActivity="false"
    android:theme="@style/Cyber.Lock" />
```
Optional (only when the user opts in): `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `USE_FULL_SCREEN_INTENT`. Document each in `docs/PERMISSIONS.md`.

## 7. Testing hooks (debug build only)
- `adb shell am broadcast -a com.techvrs.cyberhome.debug.LOCK_PREVIEW --es scenario reference` → shows `LockHostActivity` unlocked in "reference" mode (frozen 13:23, 3 %, fake media) for the fidelity diff in `07` §6.
- Scenarios: `reference`, `charging`, `no_media`, `many_notifications`, `low_battery`, `dark`, `bengali`.
- Receiver must exist only in the `debug` source set.
