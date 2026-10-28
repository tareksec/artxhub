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
| `lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt` | Default `pref_workspaceRows` 5 → 8 (selects the `cyber_4_by_8` grid) | 4 |
| `lawnchair/res/xml/device_profiles.xml` | New grid option `cyber_4_by_8` (4×8, `defaultLayoutId=@xml/cyber_default_workspace`) appended | 4 |
| `lawnchair/src/app/lawnchair/LawnchairLauncher.kt` | `onCreate`: one call `CyberOnboarding.maybeShow(this)` (first-run onboarding) | 8 |
| `lawnchair/src/app/lawnchair/ui/preferences/destinations/PreferencesDashboard.kt` | "CyberHome" entry at the top; "About" opens our offline About; removed `SyncLiveInformation()` and `AnnouncementPreference()` (network) | 8, 10 |
| `lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt` | Default `pref_searchResultStartPageSuggestion` true → false (no web suggestions) | 10 |
| `src/com/android/launcher3/widget/custom/CustomWidgetManager.java` | Always registers `R.array.cyber_custom_widget_providers` (our in-launcher Cozy Board widget); loop moved into `addBuiltInProviders()`; clears donor parcel `initialLayout` / `initialKeyguardLayout` so host view doesn't inflate third-party layouts | Board |
| `src/com/android/launcher3/model/ModelDbController.java` | `loadDefaultFavoritesIfNecessary()`: on our grid skip app-restriction / auto-install / partner layouts (they replaced our layout on GMS phones), then call `CyberLayout.onDefaultLayoutLoaded()` to add the Cozy Board | Board |
| `src/com/android/launcher3/model/ModelDbController.java` | Same method: also loads the default layout when `CyberLayout.consumeResetRequest()` is true ("Apply look" / reset), so the reset happens inside one model load instead of wipe + second reload (BUG-2) | BUG-2 |
| `lawnchair/res/values/styles.xml` | `AppTheme`: `android:windowSplashScreenBackground` = `@color/cyber_bg` (cream / plum starting window instead of white, BUG-1) | BUG-1 |
| `src/com/android/launcher3/widget/LauncherWidgetHolder.java` | `attachViewToHostAndGetAttachedView`: do not recycle in-launcher custom widgets through `AppWidgetHost`; preserve view tag on recycled views | Board |
| `src/com/android/launcher3/Launcher.java` | `bindInflatedItems`: ensure view tag is restored on inflated widget views so `addInScreen` does not drop them | Board |
| `src/com/android/launcher3/widget/LauncherAppWidgetHostView.java` | `updateAppWidget`: early return for in-launcher custom widgets so remote view updates do not overwrite local views | Board |

## Overrides without editing upstream files

`cyber/cyber.gradle` adds `cyber/res` to every build-type source set. Build-type resources
win the resource merge, so these files replace upstream resources of the same name:

| Overlay file | Replaces |
|---|---|
| `cyber/res/drawable/ic_launcher_home.xml` | `res/drawable/ic_launcher_home.xml` (app icon) |
| `cyber/res/mipmap-anydpi-v26/ic_launcher_home*.xml` | `res/mipmap-anydpi-v26/ic_launcher_home*.xml` |
| `cyber/res/drawable/ic_launcher_home_comp.xml` | `lawnchair/res/drawable/ic_launcher_home_comp.png` (About logo) |
| `cyber/res/values*/cyber_rebrand_strings.xml` | Strings in `lawnchair/res/values*/strings.xml` that say "Lawnchair" |
| `cyber/res/values/cyber_config.xml` | `config_default_accent_color` (pink), `config_default_enable_smartspace` (false), `config_default_icon_shape` (squircle) in `lawnchair/res/values/config.xml` |

`cyber.gradle` also sets: `versionName` 0.2.0 (versionCode stays upstream's), `androidResources.localeFilters = ['en']`
(owner's choice: English only, for APK size) and release-only `abiFilters` arm64-v8a + armeabi-v7a.

The dock style is set at "Apply look" through Lawnchair's own preferences
(`CyberLauncherStyle`: dock background on, 82 % alpha, our surface color per light/dark), not by code changes.

Generated per variant (not in git): `res/xml/cyber_default_workspace.xml` from
`cyber/templates/cyber_default_workspace.xml`, with `@APP_ID@` replaced by the variant's
applicationId (task `cyberGenerate<Variant>LayoutTemplates`).

`cyber.gradle` also adds `cyber/src` (our Kotlin code) and `cyber/AndroidManifest.xml` (our
components, e.g. widgets) to every build type, and `cyber/debug/src` +
`cyber/debug/AndroidManifest.xml` to the `lawnWithQuickstepGithubDebug` variant only.

## After every upstream merge

1. `./gradlew cyberRebrandStrings` — regenerates the string overrides (new/changed strings).
   Run it on its own, not in the same command as an `assemble*` task (Gradle rejects that
   because it writes into a resource source folder).
2. Grep for new user-visible "Lawnchair" text in Kotlin/XML and new `ic_launcher_home*` resources.
3. Re-check this table.
