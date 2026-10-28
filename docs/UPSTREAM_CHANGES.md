# Upstream files changed by CyberHome

Every file from Lawnchair (`15-dev`) that we edited. Keep each edit small and marked with a
"Modified by CyberHome" / "CyberHome:" comment so upstream merges stay easy.

| File | Change | Task |
|---|---|---|
| `build.gradle` | `applicationId` of the `github` / `nightly` / `play` flavors → `com.techvrs.cyberhome[.nightly/.play]` | 1 |
| `build.gradle` | `derived_app_name` → `CyberHome` / `CyberHome (Debug)` | 1 |
| `build.gradle` | APK file name prefix `Lawnchair.` → `CyberHome.` | 1 |
| `build.gradle` | `apply from: 'cyber/cyber.gradle'` + modification notice at top | 1 |
| `lawnchair/src/app/lawnchair/backup/LawnchairBackup.kt` | Backup file name `Lawnchair_Backup` → `CyberHome_Backup` | 1 |
| `lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt` | Default `pref_launcherTheme` `"system"` → `"dark"` | 2 |

## Overrides without editing upstream files

`cyber/cyber.gradle` adds `cyber/res` to every build-type source set. Build-type resources
win the resource merge, so these files replace upstream resources of the same name:

| Overlay file | Replaces |
|---|---|
| `cyber/res/drawable/ic_launcher_home.xml` | `res/drawable/ic_launcher_home.xml` (app icon) |
| `cyber/res/mipmap-anydpi-v26/ic_launcher_home*.xml` | `res/mipmap-anydpi-v26/ic_launcher_home*.xml` |
| `cyber/res/drawable/ic_launcher_home_comp.xml` | `lawnchair/res/drawable/ic_launcher_home_comp.png` (About logo) |
| `cyber/res/values*/cyber_rebrand_strings.xml` | Strings in `lawnchair/res/values*/strings.xml` that say "Lawnchair" |
| `cyber/res/values/cyber_config.xml` | `config_default_accent_color` (cyan), `config_default_app_drawer_bg_color`, `config_default_folder_color` in `lawnchair/res/values/config.xml` |

`cyber.gradle` also adds `cyber/src` (our Kotlin code) to every build type, and
`cyber/debug/src` + `cyber/debug/AndroidManifest.xml` to the debug build only.

## After every upstream merge

1. `./gradlew cyberRebrandStrings` — regenerates the string overrides (new/changed strings).
   Run it on its own, not in the same command as an `assemble*` task (Gradle rejects that
   because it writes into a resource source folder).
2. Grep for new user-visible "Lawnchair" text in Kotlin/XML and new `ic_launcher_home*` resources.
3. Re-check this table.
