# CLAUDE.md — CyberHome Launcher (working codename)

You are working on an Android launcher app: a **fork of Lawnchair** with a custom
"cyber-anime widget" home screen look. Read this file fully, then `PROJECT_SPEC.md`
(what to build) and `TASKS.md` (the order to build it in) before writing code.

## Goal (current phase)
Produce a **debug/release APK for personal testing and sharing with friends**.
Play Store publishing is NOT in scope now. Do not add Play Store-only work.

## Base project
- Upstream: https://github.com/LawnchairLauncher/lawnchair (Apache 2.0 on `15-dev`, Kotlin + Jetpack Compose, built on Launcher3).
  Our own code and the combined app are GPL-3.0-or-later (`COPYING`); upstream `LICENSE.txt` stays. See `NOTICE.md`.
- Use branch **`15-dev`** (Lawnchair 15, based on Android 15 Launcher3). It is the more stable line.
  Do NOT use `16-dev`: upstream says it has major breaking changes and is unfriendly to new contributors.
  If `15-dev` no longer exists, stop and ask the user which branch to use.
- Requires JDK 17+, Android SDK (check `compileSdk` in the Gradle files and install that platform).
- Always read the repo's own README / CONTRIBUTING / docs for build instructions first.
  Find the exact Gradle task with `./gradlew tasks` / `./gradlew assembleDebug`. Older docs
  mention `assembleLawnWithQuickstepPlayDebug`; verify the variant name for this branch, do not assume.
- The repo may use git submodules: always `git clone --recurse-submodules` and run
  `git submodule update --init --recursive`.

## Hard rules
1. **Rebrand first.** Change app name, `applicationId` / package id, launcher icon, and any
   strings that say "Lawnchair" in user-visible places. The upstream name and package
   (`app.lawnchair`) must not be used by our app. Package id comes from `APP_ID` below.
2. **License compliance (GPL v3 for our code; upstream Apache 2.0).** Keep `LICENSE.txt` and `COPYING`,
   add `SPDX-License-Identifier: GPL-3.0-or-later` to new files, mark modified upstream files, keep all upstream copyright headers,
   add a `NOTICE.md` crediting Lawnchair and Launcher3 (AOSP), and add an About screen
   with a link to our public source repo. Our own art/brand assets are "All rights reserved"
   and live in a clearly separated folder (`assets-proprietary/`) with their own LICENSE file.
3. **Isolate our code.** Put new code in its own package (`<APP_ID>.cyber.*`) and new resources
   with a `cyber_` prefix. Touch upstream files only when unavoidable, and keep those edits
   small so future upstream merges stay easy. List every upstream file you edited in `docs/UPSTREAM_CHANGES.md`.
4. **No secrets in git.** Keystores, passwords, `keystore.properties`, `local.properties` go in `.gitignore`.
5. **No copyrighted art.** Never copy anime characters, logos or fonts from the reference screenshot
   or the web. Use placeholder/original art only (see `ASSETS.md`). Use only fonts with an open license (OFL/Apache).
6. **Privacy-friendly.** No analytics, no ads, no network calls except the weather widget
   (Open-Meteo, no API key). Request the minimum permissions and explain each in `docs/PERMISSIONS.md`.
7. **Build must stay green.** After every task run the build and fix errors before moving on.
   Commit once per finished task with a clear message.
8. Prefer small, verifiable steps. If a requirement is impossible or risky on Android,
   say so and propose the closest alternative instead of silently changing scope.

## Placeholders (user will replace)
- `APP_NAME` = CyberHome
- `APP_ID` = com.techvrs.cyberhome
- Source repo URL = (ask the user; leave a `TODO` constant)

## Definition of done for each task
- Compiles; `./gradlew assembleDebug` (correct variant) succeeds.
- Installs and opens on a real device or emulator without crashing.
- Behavior matches `PROJECT_SPEC.md`.
- Short note appended to `docs/PROGRESS.md` (what changed, what is left, known issues).

## Communication
Reply to the user in Bengali (বাংলা), keeping technical terms in English
(e.g. launcher, widget, APK, Gradle, build). Code, comments, commit messages in English.
