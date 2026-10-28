# Progress

## Task 0 — Environment and upstream build (2026-10-28)

- Machine: JDK 21 (Temurin), Git 2.55, Android SDK at `%LOCALAPPDATA%\Android\Sdk`, adb present.
  Gradle auto-installed SDK Platform 36.1 and Build-Tools 36.1.0.
- Cloned Lawnchair `15-dev` (commit `505dbc4`) with submodules into `cyberhome/`.
- Build variant: `lawnWithQuickstepGithubDebug`
  (`gradlew.bat assembleLawnWithQuickstepGithubDebug`).
- Unmodified upstream debug APK built OK (first build ~15 min).
- Issue hit: an interrupted first build left zero-filled files in the Gradle cache
  (71 POMs, `kotlin-compiler-embeddable-2.3.0.jar`). Fixed by deleting those cache entries
  and `~/.gradle/caches/9.3.0/transforms`.

## Task 1 — Rebrand and safe isolation (2026-10-28)

Changed:
- Package id `com.techvrs.cyberhome` (debug: `com.techvrs.cyberhome.debug`), so it installs
  side by side with Lawnchair (`app.lawnchair*`). Provider authorities and custom permissions
  all derive from `${applicationId}`, so there are no install conflicts.
- App name `CyberHome` / `CyberHome (Debug)`; APK named `CyberHome.<version>.github.debug.apk`.
- Placeholder adaptive launcher icon (dark, cyan HUD brackets, house glyph) incl. themed-icon layer,
  also used as the About-screen logo.
- 13 user-visible "Lawnchair" strings rebranded in English + 62 locales via generated overlay
  (`./gradlew cyberRebrandStrings`).
- `NOTICE.md`, `docs/UPSTREAM_CHANGES.md`, this file. `LICENSE.txt` kept.
- Secrets: none needed. Release signing already falls back to the debug key when
  `keystore.properties` is missing; all upstream CI steps that use secrets are guarded by
  `github.repository_owner == 'LawnchairLauncher'` and are skipped on our fork.

Known issues / left for later:
- **License:** upstream `15-dev` is **Apache 2.0**, not GPL v3 as CLAUDE.md assumed. Needs a
  decision on the license for our own additions (GPL v3 is still possible: Apache 2.0 code can
  be combined into a GPLv3 work).
- About screen still shows Lawnchair links/contributors and fetches data from the GitHub API
  (network call, conflicts with privacy rule 6) → replace in Task 10.
- `search_provider_sponsored_description` intentionally not rebranded (it claims a revenue
  share with Lawnchair); sponsored search providers should be removed later.
- Internal names (classes `app.lawnchair.*`, theme names, log tags) unchanged on purpose.
- `baseline-profile` module still targets package `app.lawnchair` (only used to generate
  baseline profiles; not part of the APK).
- Not yet tested on a device (no device/emulator connected).
