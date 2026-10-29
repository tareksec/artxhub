# MASTER PROMPT — give this file to the implementing AI first

You are continuing work on **CyberHome**, an Android launcher forked from Lawnchair `15-dev`
(Launcher3, Kotlin + Java, some Compose). Project root: `cyberhome/`. Our code lives in
`cyberhome/cyber/` (package `com.techvrs.cyberhome.cyber`) and `cyberhome/mika/`
(package `com.techvrs.cyberhome.mika`). Test device: **Nokia G10, Android 13 (API 33)**,
connected over USB with debugging enabled. `targetSdk 35`, `minSdk 26`.

## Read before writing any code (in this order)
1. `cyberhome/CLAUDE.md` — hard rules (license, isolation, no copyrighted art, privacy, build green).
2. `upgrade-plan/README.md` — the owner's goals and open decisions.
3. `upgrade-plan/01-current-state-audit.md` — known defects with file paths.
4. `upgrade-plan/02-android-limits.md` — what Android allows. **Do not promise or attempt anything listed as impossible there.**
5. The spec file for the task you are doing (03 … 08), then `09-task-breakdown.md` for the exact task.
6. `bug-fix/*.md` (older plan) — still valid context about the Cozy Board architecture and performance.

## Mission (all five, in the order given by 09-task-breakdown.md)
1. **Security & integration** — no exported surfaces we don't need, no data loss, minimum permissions, privacy-safe lock screen.
2. **Smooth & fast** — cold start < 1.5 s on the Nokia G10 release build, 0 dropped frames in common gestures, no main-thread disk/bitmap work.
3. **Next-gen UI** — one design-token system, motion system, glass/blur, haptics, light/dark, dynamic theme.
4. **System-wide theme** — the active theme (Cyber, Mika, future packs) drives: home, icons, all-apps, folders, widgets, *our* status/nav bar appearance, our Control Center panel (replaces pulling the system shade from home), our lock screen, and — optional ADB mode — the Android 12+ system palette.
5. **Mika Lock** — a real, live, interactive lock screen that matches `07-mika-lockscreen-spec.md` exactly, built from live layers, never from a screenshot.

## Non-negotiable rules
- **Never bypass the system keyguard.** Unlocking always goes through `KeyguardManager.requestDismissKeyguard`. Never implement our own PIN/pattern. Never use `FLAG_DISMISS_KEYGUARD` / `disableKeyguard`.
- **No baked UI in images.** Clock, date, weather, battery, music, notes, notifications are drawn live. Images contain only art.
- **Respect notification privacy** on the lock screen: honour `Notification.visibility` (`SECRET` → hide, `PRIVATE` → show `publicVersion` or app name only) and the system setting `lock_screen_show_notifications` / `lock_screen_allow_private_notifications`.
- **No hard-coded demo strings** ("Chuva forte", "Fonseca", "13:23", "Better Days", "Sáb."). Everything comes from live data, the user's settings, or localized string resources (English + Bengali at minimum).
- **No trademarked logos** (Spotify, WhatsApp, etc.) drawn by us. Show the real app icon from `PackageManager`/`MediaController` instead.
- **No copyrighted characters.** Images in `bug-fix/lockscreen` and `bug-fix/wallaper` are mood references only; never ship them.
- **Isolation:** new code under `com.techvrs.cyberhome.cyber.*` (shared engine) or `com.techvrs.cyberhome.mika.*` (Mika-only). Resource prefixes `cyber_` / `mika_`. Every upstream file you touch goes into `cyberhome/docs/UPSTREAM_CHANGES.md` with a one-line reason. Add `SPDX-License-Identifier: GPL-3.0-or-later` to every new file.
- **Permissions:** each new permission is opt-in, explained in-app before the system prompt, and documented in `cyberhome/docs/PERMISSIONS.md`. Everything must still work (degraded) if the user says no.
- **Network:** only Open-Meteo (HTTPS). Nothing else.
- **Git:** do **not** commit until the owner has tested the APK on their phone and says so. Keep work in the working tree; summarise changes instead.
- **Build green after every task:** run the correct Gradle variant (verify with `./gradlew tasks`; release build is what we measure). Install on the device, open it, check `adb logcat` for crashes/StrictMode violations.
- If something in the spec turns out impossible or unsafe on a device, **stop that item, say so, propose the closest alternative** — never silently change scope.

## How to work
- One task from `09-task-breakdown.md` at a time. For each: read the files involved, make the smallest change that meets the acceptance criteria, build, install, verify on device (screenshot via `adb exec-out screencap -p`), append a note to `cyberhome/docs/PROGRESS.md` (what changed / what's left / known issues).
- Prefer custom `View` + `Canvas` (or Compose) inside the launcher process over RemoteViews for anything that must look exact.
- No allocations in `onDraw`; no `BitmapFactory.decode*` on the main thread; no `runCatching {}` that silently swallows real bugs — log with a tag and surface a user-visible error where it matters.
- Talk to the owner in Bengali (technical terms in English). Code, comments and docs for code in English.

## Definition of done (whole upgrade)
All acceptance criteria in `09-task-breakdown.md` pass and every item in `10-testing-qa-checklist.md` is checked on the Nokia G10 with a release build.
