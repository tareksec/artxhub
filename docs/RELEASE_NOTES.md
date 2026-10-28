# CyberHome — release notes

## 0.2.0 (test build, 2026-10-28, not yet tested on a device)

- Home page is the Cozy Board (one in-launcher view) with the owner's characters and wallpapers.
- "Apply look" reloads the home screen once and checks that the board is there (Retry if not).
- Cream / plum starting window instead of a white flash.
- Edit board: decoration on/off, swap sides, hide parts. Dock pill in the board's colors, rounded icons.
- Card images: crop screen. Theme packs and backup / restore of CyberHome settings (docs/THEME_PACKS.md).
- Clear messages when notifications are off or CyberHome is not the home app.
- Release APK 12.9 MB: WebP art, English only, ARM only.

## 0.1 (test build, 2026-10-28)

First test build for the owner and friends. Based on Lawnchair 15 (`15-dev`, commit `505dbc4`).

- Rebranded launcher "CyberHome" (`com.techvrs.cyberhome`), installs next to Lawnchair.
- Cozy anime design in two versions, Light "Milk tea" and Dark "Night cafe" (Light / Dark / Auto): pastel wallpapers, rounded cards, Nunito font, pink accent.
- One-tap onboarding "Apply look": default home, widget access, wallpaper, 4×8 layout.
- Widgets: Block clock + date, Month calendar, Battery strip, Weather (Open-Meteo, manual city),
  3 Character cards (own photos), Day counter, Drink water (+ optional reminders), Music strip,
  App dial, Settings/Messages tiles.
- Settings → CyberHome: reset look, edit widgets, weather city, water, music info, switch back,
  About (GPL notice, credits).
- Privacy: no ads, no analytics; only the weather widget uses the network.

Known limitations: see `docs/PROGRESS.md`.

## How to build a signed release APK (for friends)

1. Create a keystore once and keep it safe (back it up in two places; losing it means friends
   must uninstall to get updates):
   ```
   keytool -genkeypair -v -keystore cyberhome.jks -alias cyberhome -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Put `cyberhome.jks` in the project root (`cyberhome/`) and create `keystore.properties` there:
   ```
   storeFile=cyberhome.jks
   storePassword=<your store password>
   keyAlias=cyberhome
   keyPassword=<your key password>
   ```
   Both are ignored by git (`.gitignore` has `keystore.properties` and `*.jks`). Never commit them.
3. Build: `gradlew.bat assembleLawnWithQuickstepGithubRelease`
4. APK: `build/outputs/apk/lawnWithQuickstepGithub/release/CyberHome.<version>.github.release.apk`
5. Verify: `apksigner verify --print-certs <apk>` (apksigner is in the SDK build-tools folder).

Without `keystore.properties` the release build is signed with the debug key (fine for testing,
not for sharing long-term).
