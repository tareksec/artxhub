# 10 — Testing & QA checklist (release build, Nokia G10, Android 13)

> বাংলা সারাংশ: APK বন্ধুদের দেওয়ার আগে এই তালিকার সব ঘর টিক হতে হবে।

## Security & data
- [ ] Apply Mika → Cyber → Mika: dock and screen-0 apps never lost.
- [ ] `dumpsys package` shows only expected exported components.
- [ ] Every permission in the merged manifest is documented in `PERMISSIONS.md`.
- [ ] App works with every optional permission denied (degraded but no crash).
- [ ] Only Open-Meteo network traffic (check with `adb shell dumpsys netstats` / a proxy).
- [ ] Theme pack with `../evil`, a 200 MB file, and a 30000×30000 PNG are all rejected cleanly.

## Mika Lock
- [ ] PIN set: screen off → on shows Mika Lock instantly (< 150 ms), no second clock anywhere.
- [ ] Swipe up → system PIN bouncer; wrong PIN stays locked; correct PIN unlocks to where the user was.
- [ ] Fingerprint (if available) / Smart Lock unlock closes Mika Lock immediately.
- [ ] No PIN set: swipe up unlocks.
- [ ] Back gesture on Mika Lock → system keyguard (still locked), never the home screen.
- [ ] Incoming call while locked: call UI shows, Mika Lock does not cover it.
- [ ] Alarm while locked: alarm UI shows, not covered.
- [ ] Double-press power → camera works.
- [ ] Media playing (YouTube Music, Spotify if installed, a local player): title, artist, art, play/pause state, prev/next all correct; music-app circle shows that app's icon.
- [ ] No media: idle card shown, layout identical.
- [ ] Notifications: secret hidden, private shows public version/app name only, public full; tap → bouncer → opens the right app.
- [ ] Charging, low battery, offline (no weather), dark mode, Bengali locale, font scale 1.3×, TalkBack.
- [ ] Fidelity diff vs reference passes (`07` §6); diff image saved.
- [ ] 20 lock/unlock cycles + 1 hour idle locked: no crash, no ANR, battery drain not noticeably higher than without Mika Lock (`dumpsys batterystats`).
- [ ] Reboot: before first unlock the system keyguard shows the clean Mika wallpaper (no baked UI).

## Theme everywhere
- [ ] Status/nav bar icon colours correct on home, all-apps, Control Center, lock, settings — for Mika light, Mika dark, Cyber.
- [ ] Swipe down on home opens Control Center (< 100 ms); flashlight toggles; internet tile opens system internet panel; brightness works after grant; notifications list matches system shade.
- [ ] ADB mode: system Quick Settings take Mika colours; restore returns originals; without the grant the UI explains the adb command.
- [ ] Theme switch restyles home, icons, drawer, folders, widgets live.

## Performance
- [ ] Cold start < 1.5 s; all-apps / folder / Control Center open with 0 janky frames in 10 tries.
- [ ] 0 StrictMode violations from `com.techvrs.cyberhome.*` in debug during a 5-minute session.
- [ ] Release APK ≤ 20 MB; numbers recorded in `PERFORMANCE.md`.

## Release
- [ ] License files, NOTICE, SPDX headers on new files, OFL font licenses, About screen with source URL.
- [ ] No copyrighted characters or trademark logos in the APK (`aapt2 dump resources` + visual check of `res/drawable*`).
- [ ] Owner tested on own phone and approved → then commit.
