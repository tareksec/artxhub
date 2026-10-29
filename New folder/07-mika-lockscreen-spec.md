# 07 — Mika Lock: exact visual spec

> বাংলা সারাংশ: Reference ছবির (`cyberhome/mika/res/drawable-nodpi/mika_lockscreen_art.jpg`, 720×1600) প্রতিটা অংশের মাপ, রং, font আর আচরণ। ঘড়ি, তারিখ, আবহাওয়া, battery, গান, নোট — সব live। ছবিতে থাকবে শুধু art (জানালা, গাছ-মগ, বই, sticky note-এর কাগজ, ঘুমন্ত বিড়াল)। আপনার Nokia G10-এর screen-ও 720×1600, তাই reference-এর সাথে pixel মিলিয়ে দেখা যাবে।

## 0. Source of truth
- **Visual reference:** `cyberhome/mika/res/drawable-nodpi/mika_lockscreen_art.jpg` (also copy it to `cyberhome/mika/design/reference/mika_lock_reference.jpg`; never ship it in the APK — move it out of `res/`).
- **What's wrong today:** `mika_lockscreen_live.png` (repo root) — two clocks, fake data.
- All positions below are **fractions of the screen** (x of width W, y of height H) measured on the 720×1600 reference, plus px at 720×1600 for the overlay test. Text sizes are given as fraction of W so they scale; clamp with font-scale rules in §6.
- Top 0–4% of the reference is a fake iOS status bar → **not drawn**. The real Android status bar stays visible (transparent, dark icons).

## 1. Layer stack (bottom → top)
| Layer | Content | Source | Notes |
|---|---|---|---|
| L0 | Background: warm cream wall, window light rays | `mika_lock_bg.webp` (clean art) | Fills screen, `centerCrop`, anchored bottom. |
| L1a | Plant in cat mug | `mika_lock_plant.webp` (transparent) | parallax 4 dp |
| L1b | Book stack ("Good Vibes / Happy Days / Sweet Home" spines are part of the art — they are decorative art text, acceptable) | `mika_lock_books.webp` | parallax 3 dp |
| L1c | Sticky note paper + pink washi tape + paw sticker, **without text** | `mika_lock_note.webp` | rotation baked in art; text drawn live on top (L2) |
| L1d | Sleeping tabby cat on gingham blanket with fish plush | `mika_lock_cat.webp` | parallax 6 dp; optional idle "breathing" scale 1.000↔1.006, 4 s |
| L2 | Live content: date/weather line, clock, battery line, progress bar, music-app circle, music card, note text | drawn | §3 |
| L3 | Doodles: hearts, paws, cat-face outline | vector drawables `mika_lock_doodle_*.xml` | §4 |
| L4 | Notifications chip row / sheet, unlock hint, emergency button | drawn | §5 |

**Art delivery:** if the owner provides only one flattened clean plate (no text/UI), L1a–L1d are merged into L0 except the note *text*, which must still be blank on the paper. Minimum 1080×2400, WebP. Until the owner delivers art, the AI may create a *temporary* clean plate by removing the baked UI from the reference (inpaint) — mark it `TEMP` in `ASSETS.md`.

## 2. Colour tokens (Mika lock, light)
| Token | Value | Use |
|---|---|---|
| `lock.ink` | `#6B4C43` | clock digits, paw, music-app circle, progress fill, control icons |
| `lock.inkSoft` | `#5E4A41` | date/weather line, battery text, name line |
| `lock.inkNote` | `#7A5E52` | handwritten note text |
| `lock.inkDim` | `#9C8778` | artist line |
| `lock.track` | `#D8BAA9` | progress track |
| `lock.card` | `#FBE6D8` @ 82% alpha | music card fill (+ blur 20 dp on API 31+) |
| `lock.cardBorder` | `#FFF7F0` @ 80% (top-left highlight) → `#E8D2C1` @ 60% | 1 dp gradient border |
| `lock.cardShadow` | `#6B4C43` @ 8%, y 6 dp, blur 18 dp | card shadow |
| `lock.heart` | `#F6AFA6` | floating hearts |
| `lock.heartControl` | `#A89184` (filled when liked: `#E98F95`) | heart in music card |
| `lock.circleGlyph` | `#FBEFE6` | glyph inside music-app circle |

Dark variant ("night café"): bg art darkened via 35% `#2A211D` scrim, ink → `#F6E7DA`, card → `#3A2E28` @ 80%. Only when system dark mode is on and the user enabled "Mika dark lock".

## 3. Live elements — geometry & behaviour (720×1600 px → fractions)
| # | Element | Position / size | Type | Data & behaviour |
|---|---|---|---|---|
| E1 | Date + weather line, e.g. `Sat, 4 [rain icon] Heavy rain` | centered at x 0.50, baseline y 0.134 (215 px); total width ≈ 0.58 W | rounded sans, semibold, size 0.050 W (≈36 px), `lock.inkSoft` | `{EEE}, {d}` from device locale (pattern via `DateFormat.getBestDateTimePattern(locale, "EEEd")`) + weather icon (vector, 1.1× text height, from Open-Meteo WMO code) + localized condition text. No weather → date only, centered. Tap → opens Weather/clock app after unlock. |
| E2 | Floating heart | center (0.868, 0.138), 0.031 W | vector `lock.heart` | static; gentle float ±2 dp, 3 s |
| E3 | Clock `HH:mm` | centered x 0.50; digits span x 0.153–0.847 (≈0.694 W); cap top y 0.161, baseline y 0.269 (430 px); cap height 0.1075 H (172 px) | heavy rounded display font, `lock.ink`, tabular figures, letter-spacing −0.01 | 24 h or 12 h from `DateFormat.is24HourFormat`; 12 h hides AM/PM (shown small under date if needed). Updates on `TIME_TICK`/`TIME_SET`/`TIMEZONE_CHANGED`. Width auto-fits: if text would exceed 0.72 W (e.g. "10:58" in a wide font) scale down. Colon may "breathe" (alpha 0.6↔1, 2 s) unless reduce-motion. Tap → opens Clock/alarms after unlock. |
| E4 | Paw (filled) | center (0.889, 0.203), 0.044 W | vector, `lock.ink` | tap → bouncy scale + light haptic (easter egg) |
| E5 | Cat-face outline doodle | center (0.868, 0.294), width 0.13 W | vector outline 1.75 dp, `lock.ink` + pink cheeks `#F6B9B0` | static; blinks every ~8 s (eyes scaleY) unless reduce-motion |
| E6 | Small paw | center (0.701, 0.299), 0.042 W | vector `lock.ink` | static |
| E7 | Battery glyph + `%` | glyph at (0.111, 0.322), height 0.032 H; text starts x 0.160, baseline y 0.329 | glyph vector reflects level (5 steps) + bolt when charging; text size 0.039 W, `lock.inkSoft` | `ACTION_BATTERY_CHANGED`. Charging → `"45% · Charging"` (localized) and a soft shimmer on the progress fill. |
| E8 | Name line, e.g. `Fonseca 🦋` | x 0.097, baseline y 0.347 (555 px) | same font/size as E7 + butterfly vector `mika_lock_butterfly.xml` (no emoji) | User's display name from Mika settings (default: owner's name, editable, max 18 chars). Optional: hide. |
| E9 | Progress bar | x 0.097–0.581, center y 0.3725 (596 px), height 0.0075 H (12 px), fully rounded | track `lock.track`, fill `lock.ink` | Default = battery level. Setting: "Show music progress" → playback position from `PlaybackState` when media is active. Animate fill with `spring.smooth`. |
| E10 | Music-app circle | center (0.729, 0.346), diameter 0.117 W (84 px) | filled circle `lock.ink`, glyph `lock.circleGlyph` | Glyph = the **active media app's monochrome/adaptive icon** (tinted), *not* a Spotify logo. No session → generic music-note vector. Tap → toggles play/pause; long-press → open that app (after unlock). |
| E11 | Music card | x 0.081–0.933, y 0.3975–0.519 (636–830 px), corner radius 0.039 W (28 px) | glass card (§2) | Hidden? **No** — always shown to keep the exact composition; idle state described below. |
| E11a | Album art | x 0.118–0.294, y 0.416–0.502, square, radius 16 px | bitmap, `centerCrop` | `MediaMetadata.METADATA_KEY_ALBUM_ART`/`ART`/`DISPLAY_ICON`; fallback = soft sunset placeholder vector (theme art). Crossfade 200 ms on change. |
| E11b | Title | x 0.336, baseline y 0.434 | rounded sans semibold 0.039 W, `lock.ink`, 1 line ellipsize | `METADATA_KEY_TITLE`; idle: "Nothing playing" (localized) |
| E11c | Artist | x 0.336, baseline y 0.455 | regular 0.033 W, `lock.inkDim` | `METADATA_KEY_ARTIST` or app name; idle: "Tap ▶ to play" |
| E11d | Music-note glyph | center (0.878, 0.424), 0.044 W | vector `lock.ink` @ 80% | decorative |
| E11e | Controls | row center y 0.484 (775 px): prev x 0.424, play/pause x 0.563, next x 0.719, heart x 0.871; icon size 0.05 W; touch target 48 dp each | vectors `lock.ink`; heart `lock.heartControl` | `MediaController.transportControls`. Play/pause icon from `PlaybackState`, never a local flag. Heart: if the session exposes a custom action that looks like "like/thumbs up/favorite" or supports `setRating(HEART)` use it; otherwise hide the heart (keep spacing). Idle play → `MediaSessionManager` last-session resume via media button intent to last app, else open default music app. |
| E12 | Sticky note text `Small / steps / big / dreams ♡` | on note paper, text box x 0.590–0.790, y 0.605–0.695, rotated ≈ −8° (counter-clockwise; measure exactly against the paper edge in the art) around the note center (0.700, 0.645) | handwriting font, size 0.042 W, line height 1.18, `lock.inkNote`, small heart vector after last word | User-editable quote (Mika settings, ≤ 40 chars, auto-wraps to ≤ 4 lines, shrink-to-fit). Default "Small steps, big dreams". |
| E13 | Floating hearts & outline paw (lower) | hearts (0.507, 0.688) 0.03 W and (0.924, 0.778) 0.034 W; outline paw (0.474, 0.731) 0.04 W | vectors | gentle float |

Fonts (OFL only — evaluate with the overlay test, pick the closest, record choice in `ASSETS.md`):
- Clock: candidates **Fredoka Bold**, Nunito Black (already bundled), Baloo 2 ExtraBold. Reference digits: rounded terminals, single-storey shapes, very heavy, round "3" and "2".
- Date / UI text: Nunito Bold/SemiBold (bundled) or Fredoka Medium.
- Note handwriting: candidates **Patrick Hand**, Gochi Hand, Itim.

## 4. Doodles (vector drawables, `mika/res/drawable/mika_lock_*.xml`)
Redraw as clean SVG-derived vectors: `paw_filled`, `paw_outline`, `cat_face_outline` (with pink cheeks), `heart_filled`, `butterfly`, `music_note`, `weather_*` (clear, partly cloudy, cloudy, fog, drizzle, rain, heavy rain, snow, thunder — rounded cartoon style, 1.75 dp stroke, ink colour), `battery_0..4`, `battery_charging`, `ic_prev`, `ic_next`, `ic_play`, `ic_pause`, `heart_control`. No emoji anywhere.

## 5. States that the reference does not show (design them in the same style)
| State | Design |
|---|---|
| Notifications present | A glass pill row under the music card at y 0.535 (left-aligned, x 0.081): up to 5 app icons (24 dp, monochrome-tinted `lock.ink`) + "+N". Tap → sheet slides up from the bottom (glass, 70% height, blur behind) listing notifications per `03` S5 privacy rules; swipe down to close. The sticky note and art stay visible when collapsed. |
| Many notifications / heads-up while locked | Newest one appears briefly (3 s) as a glass toast at y 0.535, then collapses into the pill row. |
| Unlock affordance | Bottom: small paw + "Swipe up to unlock" (localized), y 0.955, `lock.inkSoft` @ 70%, fades out after 3 s; reappears on tap. The whole screen follows the finger upward (translationY, alpha), release past 25% or fling → unlock flow. |
| Secure device, bouncer | After swipe: call `requestDismissKeyguard`; while the bouncer is up keep our window hidden. On cancel → animate back in. |
| Emergency call | Secure devices: small "Emergency" text button at bottom-left (y 0.955, x 0.08), opens emergency dialer. |
| Camera | Swipe from bottom-right corner → secure camera intent. |
| Charging | E7 text + bolt; E9 shimmer. |
| Low battery (< 15%) | Battery glyph + text turn `#D9776F`. |
| No weather / offline | E1 shows date only. |
| Long-press empty area | Opens Mika lock customizer (after unlock). |
| Font scale 1.3× | Clock never grows beyond its box (auto-fit); other text may grow ≤ 1.15× then ellipsize. |
| RTL locales | Mirror horizontal positions except the clock and art. |
| Bengali locale | Date/weather strings in Bengali (`values-bn`), Bengali digits only if the locale formats them so. |

## 6. Fidelity acceptance test ("pure exact")
1. On the Nokia G10 (720×1600), set the clock state to match the reference (screenshot test mode: debug-only intent that freezes time at 13:23, battery 3%, fake media "Better Days / Lofi Vibes", weather "heavy rain", locale pt-BR). **Debug build only** — compiled out of release.
2. Capture `adb exec-out screencap -p > mika_lock_actual.png`.
3. Run `tools/mika_lock_diff.py` (to be written): overlays actual vs reference at 50% opacity and computes per-element bounding boxes. Pass if every element in §3 is within **±4 px** position and **±3 %** size, and colours within ΔE ≤ 5.
4. Save the overlay image to `cyberhome/mika/design/reference/diff_<date>.png` and attach it in `PROGRESS.md`.
5. Then repeat on an emulator at 1080×2400 and 1440×3200 to confirm proportional scaling.
