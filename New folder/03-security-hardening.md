# 03 — Security, privacy and integration hardening

> বাংলা সারাংশ: কোনো data হারাবে না, অপ্রয়োজনীয় permission থাকবে না, lock screen কখনো PIN এড়িয়ে যাবে না, notification-এর গোপন লেখা lock-এ দেখাবে না।

## S1. No data loss when switching themes (High)
- Before `MikaLook.placeMikaBoard` / `placeCyberBoard` change the `favorites` table, snapshot every row on `CONTAINER_DESKTOP` screen 0 and `CONTAINER_HOTSEAT` into a backup table or JSON file in `filesDir/theme_backups/<timestamp>.json`.
- Do not delete the hotseat at all unless the theme explicitly needs it; prefer moving conflicting items to a new screen (`screen = max+1`) instead of deleting.
- "Revert" restores the snapshot. Keep the last 3 snapshots.
- Do the DB work inside one transaction (`modelDbController` / `SQLiteDatabase.beginTransaction`) and only report success after commit + model reload.
- Unit test: apply Mika → apply Cyber → hotseat and screen-0 apps identical to the start.

## S2. Exported components
- Every activity/service/receiver/provider in `cyber/` and `mika/` manifests: `exported="false"` unless the system must start it.
- `MikaPreviewActivity`: set `exported="false"`, remove the `APPLICATION_PREFERENCES` filter (Lawnchair's settings already own it). Open it from CyberHome settings.
- New exported things allowed: the Mika Lock activity **is not exported**; the `NotificationListenerService` stays exported but protected by `BIND_NOTIFICATION_LISTENER_SERVICE`; Quick Settings `TileService` (if added) protected by `BIND_QUICK_SETTINGS_TILE`.
- Any `PendingIntent` we create: `FLAG_IMMUTABLE` + explicit component.
- Dynamic receivers: `RECEIVER_NOT_EXPORTED` (already done in `MikaLockScreenView`) except system broadcasts that require otherwise.

## S3. Permission audit
Make a table in `docs/PERMISSIONS.md` with columns: permission, who declares it (upstream/us), feature using it, keep/remove, runtime prompt text.
- Remove (via `tools:node="remove"` in our manifest overlay, recorded in `UPSTREAM_CHANGES.md`) any permission whose feature we do not ship. Candidates: `READ_CONTACTS`, `CALL_PHONE`, `READ/WRITE_HISTORY_BOOKMARKS`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`, `READ_EXTERNAL_STORAGE` (use the Photo Picker instead), `PACKAGE_USAGE_STATS` (verify no feature uses it first).
- New, all opt-in with an in-app explanation screen first:
  - Notification access (Control Center list, Mika Lock notifications, media) — already used.
  - `SYSTEM_ALERT_WINDOW` ("Display over other apps") — only to reliably show Mika Lock when another app was in front at screen-off. Verify on device whether it's needed (see `08`).
  - `WRITE_SETTINGS` (brightness/rotation in Control Center).
  - `FOREGROUND_SERVICE_SPECIAL_USE` only if the optional "keep Mika Lock reliable" service is enabled.
  - `WRITE_SECURE_SETTINGS` via adb only (system palette).
  - `ACCESS_NOTIFICATION_POLICY` (DND toggle) only if that tile ships.
- Never request `READ_PHONE_STATE`; detect calls via `AudioManager.mode` / `TelecomManager.isInCall` (needs `READ_PHONE_STATE` on some versions → fall back to `AudioManager`).

## S4. Lock-screen security model (must be followed exactly)
- Mika Lock never authenticates. It only **covers** the keyguard. Unlock path = `KeyguardManager.requestDismissKeyguard(activity, callback)`; system shows its bouncer for secure devices.
- If `KeyguardManager.isDeviceSecure` and the device is locked: notifications show only public content (see S5); media controls allowed (Android's own keyguard allows them); **no** app launching except camera via the secure camera intent (`MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE`) and "open notification" which first calls `requestDismissKeyguard` then sends the `PendingIntent` on success.
- Emergency call button visible on secure devices (`TelecomManager.createLaunchEmergencyDialerIntent` / `ACTION_EMERGENCY_DIAL` fallback).
- Do not show over: active phone call, ringing, alarm firing, an existing showWhenLocked activity (camera, maps navigation). See `08` §edge cases.
- No screenshots of the lock with private data: `FLAG_SECURE` on the Mika Lock window when any non-public notification text is shown (simplest: always `FLAG_SECURE`).
- Back button / gestures cannot "escape" into the device: Mika Lock simply finishes, revealing the system keyguard (which is still locked). That is the correct, safe behaviour.

## S5. Notification privacy on Mika Lock
- Read `Settings.Secure` `lock_screen_show_notifications` and `lock_screen_allow_private_notifications` (fall back to "show, hide sensitive" if unreadable).
- Per notification: `VISIBILITY_SECRET` → skip; `VISIBILITY_PRIVATE` (or channel `lockscreenVisibility`) → render `publicVersion` if present else "App name · new notification"; `VISIBILITY_PUBLIC` → full.
- Never persist notification content to disk or logs.

## S6. Data at rest and in transit
- `android:allowBackup` / `dataExtractionRules`: exclude `theme_backups`, notification caches, and anything personal from cloud backup.
- `network_security_config.xml`: cleartext disabled; only the Open-Meteo domain is contacted. Weather requests contain only rounded lat/long (2 decimals) or the city the user typed.
- Theme packs: keep zip-slip/size guards in `CyberPack.readZip`; add image decode bounds check (`inJustDecodeBounds` → reject > 4096 px per side or > 40 MP) and MIME sniff (PNG/JPEG/WebP magic bytes only).
- User-picked images: copy via Photo Picker URI into app-private storage; never keep persistable URI grants longer than needed.

## S7. Build & release hygiene
- Release: `minifyEnabled true`, `shrinkResources true`, `debuggable false`, R8 full mode; keep rules for `@Keep` custom widget plugins.
- Signing config read from `keystore.properties` (gitignored). Verify `.gitignore` covers `local.properties`, `*.jks`, `*.keystore`, `keystore.properties`.
- Run `./gradlew lint` for the release variant; fix all `Security`/`Correctness` errors in our modules.
- Optional: run the repo's `security-review` skill on our modules before sharing APKs.

## Acceptance
- Theme switch round-trip keeps all user items (instrumented test + manual).
- `adb shell dumpsys package com.techvrs.cyberhome | grep -i exported` shows only the components listed in S2.
- With a PIN set: Mika Lock → swipe → system PIN bouncer always appears; wrong PIN keeps the device locked; Mika Lock never shows private notification text.
