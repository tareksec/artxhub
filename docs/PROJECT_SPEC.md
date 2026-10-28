# PROJECT_SPEC.md — CyberHome Launcher

## 1. Product in one sentence
A launcher (Lawnchair fork) that, after install and **one tap on "Set as default home"**,
shows a ready-made cozy anime widget home screen (light or dark): big block clock, date, calendar,
music, weather, character cards, reminders, shortcut tiles. No other app (no KWGT, no
icon pack app) is required.

## 2. Reference look (from the user's screenshot)
Dark UI, 1-page home screen on a tall phone. Everything is a tile on a loose grid.
Layout, top to bottom:

| Zone | Content |
|---|---|
| Top-left (large card) | Character card: blue frame, label "PHONE" + incoming-call style tag, big "05" number with "DESIGN / BLUE" caption. Acts as a Phone shortcut. |
| Top-right | Huge block-digit clock: hour on first line ("06"), minutes below ("00"), a thin diagonal slash line crossing the hour digits, thin divider between hour and minutes. Below: date + weekday (e.g. "Oct 09 · Wed"). |
| Row under top-left | Two small tiles: **Settings** (icon + "SET UP") and **Messages** (speech-bubble icon + "MESSAGE"). |
| Left-middle | Battery strip (lightning icon + segmented bar), then a **month calendar** card (Sun–Sat header, today's date highlighted in an accent color, other days white/dim). |
| Right-middle (large card) | Second character card: pink frame, corner brackets, label "SPECIAL GIRLS" style tag, and a **day counter** pill (e.g. "147" with a label like "Anniversary"/"Days"). |
| Right of calendar | **Weather** tile (cloud icon, temperature on tap/expanded). |
| Middle strip | **Music player** strip: prev / next buttons, equalizer bars animation, track title area; plus a round **dial button** with a blue ring (acts as app drawer button). |
| Bottom-right | Third character card: teal frame, "DRINK" + local-language caption, acts as a **water reminder** tile. |
| Bottom-left | 2x2 small icon tiles (4 favorite app shortcuts). |

**Design direction (revised 2026-10-28): cozy anime, not cyber/HUD.** The anime character art and
soft pastel colors are the heart of the look; the techy frames in the reference are only decoration.
Two versions: **Light "Milk tea"** and **Dark "Night cafe"** (System / Light / Dark switch).

Style tokens (tune after first render; values in `cyber/res/values{,-night}/cyber_colors.xml`):

| Token | Light | Dark |
|---|---|---|
| Background | cream `#FBF6EF` + soft pastel blobs | plum-charcoal `#1C1B24` + muted blobs |
| Surface / border | `#FFFFFF` / `#EDE4DA` | `#26252F` / `#34323F` |
| Text / dim | `#2B2A33` / `#8A8494` | `#F4EEF6` / `#A39DB0` |
| Blue (accent / soft / tint) | `#4A6FD6` / `#9DB8FF` / `#E3EDFF` | `#9DB8FF` / `#6F8DE8` / `#2A3350` |
| Pink | `#D9578A` / `#F28AAE` / `#FFE3EE` | `#F7A3C2` / `#E07FA6` / `#4A2A3A` |
| Teal | `#2E9C84` / `#8FD8C8` / `#DDF5EF` | `#8FD8C8` / `#4FB3A0` / `#203A35` |
| Peach | `#C98A2E` / `#F2C278` / `#FFF1DA` | `#F2C278` / `#D9A050` / `#3A3226` |

- Each card has its own pastel tint (blue phone card, pink special card, teal drink, peach settings).
- Corners: soft and round (18 dp cards, pills for tags), no chamfers, brackets or HUD lines.
- Typography: one rounded family (Nunito, OFL): chunky rounded clock digits, friendly sentence-case labels.
- Icons: rounded line icons with 2 dp strokes.
- All labels bilingual-ready (English default; Bengali optional via string resources).

## 3. Functional requirements

### 3.1 One-tap setup (core promise)
1. First launch opens an **Onboarding** screen: short preview image, one big button **"Apply Look"**.
2. "Apply Look" performs, in order:
   - Requests the default-home role (`RoleManager.ROLE_HOME`, fallback to the system Home settings intent).
   - Applies the bundled dark dot-grid wallpaper (`WallpaperManager`).
   - Ensures the default workspace layout (section 3.2) is loaded.
   - Requests only the permissions needed by widgets (e.g. notification access is NOT needed; calendar read is optional).
3. User can re-run "Apply Look" later from Settings → "Reset to default look".
4. Never lock the user in: Settings must contain a clear "Switch back to previous launcher" help entry.

### 3.2 Default workspace layout
- Ship a default layout so the workspace is pre-populated on first load. Launcher3 supports a
  default workspace XML (`res/xml/default_workspace_*.xml`, with `<appwidget>`, `<favorite>`, `<folder>` tags)
  wired through the grid options (`device_profiles` / `defaultLayoutId`). Investigate how this
  Lawnchair branch selects it and add our own grid (e.g. 4 columns x 8 rows) plus layout.
- Our widgets are normal Android `AppWidgetProvider`s **bundled in the same APK**, so they also appear in the
  widget picker for manual placement.
- RISK to verify early (Task 4): can the launcher bind widgets from its own package silently from the default
  layout on Android 12–15? If not, implement a first-run flow that binds them via
  `AppWidgetHost`/`AppWidgetManager` with the proper bind permission/prompt, or places them programmatically after onboarding.

### 3.3 Widgets (each is its own provider + configuration where useful)
| ID | Widget | Details |
|---|---|---|
| W1 | **Block Clock** | Two-line big digits (HH over MM), diagonal slash ornament, 12/24h follows system, updates every minute (use `TextClock` in RemoteViews where possible for battery efficiency). Tap opens the default clock app. |
| W2 | **Date line** | "Oct 09 · Wed" style, locale aware. Can be merged into W1. |
| W3 | **Month Calendar** | Current month grid, today highlighted, tap opens calendar app. No permissions required for the grid itself. Optional: dots for days with events (requires READ_CALENDAR, off by default). |
| W4 | **Battery strip** | Lightning icon + segmented bar showing battery %, color changes when charging/low. |
| W5 | **Music strip** | Prev / Play-Pause / Next + equalizer art + title. Use `MediaSessionManager` only if user grants Notification Listener access; otherwise show a static strip that sends media key events (`AudioManager.dispatchMediaKeyEvent`). Document the trade-off. |
| W6 | **Weather tile** | Cloud icon + temperature. Data: Open-Meteo (no key), location from the user's chosen city (manual) with optional coarse location permission. Cache results; refresh at most every 30–60 min via WorkManager. |
| W7 | **Character card (x3 styles: blue, pink, teal)** | Image + frame + corner brackets + caption. Image source: bundled placeholder art or user-picked image from gallery (config activity). Each card has an optional tap action (open phone / open app / none). |
| W8 | **Day counter** | Pill showing days since/until a chosen date + label; configurable. Can be overlaid on the pink card. |
| W9 | **Water reminder** | "DRINK" tile; tap logs a glass; optional scheduled notification reminders (WorkManager, user-set interval). |
| W10 | **Shortcut tiles** | Settings, Messages, 4 favorites. Implemented as normal launcher shortcuts/favorites in the default layout using HUD-style icons. |
| W11 | **Dial button** | Round button with blue ring that opens the app drawer (can be a custom workspace shortcut or gesture target). |

Implementation guidance for widgets:
- Prefer `RemoteViews` (+ `TextClock`, `ImageView`) for reliability across launchers. Jetpack Glance is acceptable
  where it does not hurt fidelity. For pixel-exact HUD frames, **render static frames/ornaments to Bitmap with
  Canvas** (or use drawable layer-lists / vector drawables) and keep dynamic text in views.
- Respect widget size classes; provide `minWidth/minHeight`, `targetCellWidth/Height`, `resizeMode`.
- Keep memory low: RemoteViews bitmaps have size limits; downscale images.
- Light and dark versions: widget colors are set from code using the chosen mode (Lawnchair's
  launcher theme setting: Light / Dark / System), never from `@color` in widget XML.

### 3.4 App drawer and the rest
Keep Lawnchair's existing drawer, search, gestures, and settings (do not remove features).
Drawer and folders follow Lawnchair's light/dark theme with our pink accent; full restyle is later.

### 3.5 Settings additions (Compose, in our package)
- "Reset to default look"
- "Edit cards" (pick images for character cards)
- "Day counter" date + label
- "Weather city"
- "About" (version, GPL notice, source repo link, credits for Lawnchair / Launcher3 / fonts)

## 4. Non-goals (now)
Ads, premium/billing, cloud sync, Play Store listing, closed-testing setup, multiple launcher variants.
(Design the code so a premium "theme packs" feature can be added later without rewrites:
keep card art and color tokens in swappable resource sets.)

## 5. Quality bar
- Cold start to visible home < 1.5 s on a mid-range phone.
- No ANR; no crash on rotation, on widget removal, on missing permissions.
- Works on Android 10–15 where Lawnchair 15 supports it; test on at least one Android 13+ real device.
- Battery: no foreground service; no wake locks; minute updates only through system clock mechanisms.

## 6. Acceptance test (manual)
1. Install APK on a clean-ish phone.
2. Open app → Onboarding → "Apply Look" → set as default home.
3. Home screen shows the pre-placed layout from section 2 with live clock, calendar, battery, weather.
4. Long-press → widgets list shows all CyberHome widgets; adding one works.
5. Reboot phone → look persists.
6. Uninstall → phone falls back to the stock launcher.
