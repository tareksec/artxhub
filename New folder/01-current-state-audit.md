# 01 — Current state audit (2026-10-29)

> বাংলা সারাংশ: এখনকার code পড়ে যা পাওয়া গেছে। সবচেয়ে বড় সমস্যা: Mika lock screen একটা ছবি, Mika apply করলে user-এর dock-এর app মুছে যায় (backup ছাড়া), আর lock-এর লেখাগুলো hard-coded।

Paths are relative to `cyberhome/`.

## 1. Modules
| Module | What it is |
|---|---|
| `cyber/` | CyberHome engine: Cozy Board (`board/`), design tokens (`design/CyberTokens.kt`, `CyberTheme.kt`, `CyberThemeMode.kt`), RemoteViews widgets (`widgets/`), settings activities, theme packs (`pack/CyberPack.kt`), onboarding. |
| `mika/` | Mika cozy-cat theme: `MikaBoardView` (home board, ~39 KB of Canvas code), `MikaLockScreenView` + `MikaLockScreenWidget` (a **home-screen widget**, not a lock screen), `MikaIconTransformer` (icon theming), `MikaLook` (apply/revert), `MikaStore` (prefs), `MikaPreviewActivity`. |
| `lawnchair/`, `src/`, `quickstep/` | Upstream. Touch only when unavoidable. |

## 2. Mika lock screen — why it is "fake"
| Where | Problem |
|---|---|
| `mika/res/drawable-nodpi/mika_lockscreen_art.jpg` (720×1600, 96 KB) | The image itself contains a fake iOS status bar ("12:59", signal, "80"), the clock "13:23", the date/weather line, battery "3%", "Fonseca", a Spotify logo, the whole music card, and the sticky note text. It is a screenshot of a design, not art. Also too low-res for a 1080p+ screen. |
| `mika/src/.../theme/MikaLook.kt:44-54` | Sets that image as the **system lock wallpaper** (`FLAG_LOCK`). Result on device (`mika_lockscreen_live.png` in repo root): the real system clock "12:26" draws on top of the baked "13:23" → two overlapping clocks, fake "3%" battery, a dead music card. |
| `mika/src/.../board/MikaLockScreenView.kt:103` | Hard-coded `"$dayName, $dayNum 🌧 Chuva forte ♡"` — weather is never real. |
| `MikaLockScreenView.kt:123` | Hard-coded `"Fonseca 🦋"`. |
| `MikaLockScreenView.kt:51,194,230` | `isPlaying` is a local fake flag, not read from a `MediaController`; title/artist are string resources, never real metadata; album art is an emoji. |
| `MikaLockScreenView.kt:142,170,191-201` | Uses emoji/Unicode glyphs ("♫", "🌅", "⏮", "⏸") as icons — renders differently per OEM font, not exact. |
| `MikaLockScreenView.kt:126,131,136,165,216` | Allocates `RectF` objects inside `onDraw` (GC churn). |
| `MikaLockScreenView.kt:225-252` | `onTouchEvent` always returns `true` (swallows all touches incl. workspace long-press/scroll). No ripple/pressed state, no accessibility nodes for the buttons. |
| `MikaLockScreenWidget.kt` | It is only a 4×4 home widget. Nothing runs on the actual lock screen. |

## 3. Data-loss / security findings
| Where | Severity | Problem |
|---|---|---|
| `mika/src/.../theme/MikaLook.kt:78-87` | **High (data loss)** | `placeMikaBoard` deletes *every* item on desktop screen 0 **and the whole hotseat** with no backup. `placeCyberBoard` (`:127-131`) never restores them. A user who tries Mika loses their dock. |
| `mika/AndroidManifest.xml:6-16` | Medium | `MikaPreviewActivity` is `exported="true"` with `APPLICATION_PREFERENCES`. Any app can launch it and trigger "Apply" flows. Lawnchair already handles `APPLICATION_PREFERENCES`; duplicate filter also confuses the system settings entry. |
| Inherited manifest permissions | Medium | `READ_CONTACTS`, `CALL_PHONE`, `READ_HISTORY_BOOKMARKS`, `WRITE_HISTORY_BOOKMARKS`, `READ_EXTERNAL_STORAGE`, `READ_MEDIA_AUDIO/VIDEO`, `PACKAGE_USAGE_STATS`, etc. Audit each: keep only those a shipped feature uses; document in `docs/PERMISSIONS.md`. |
| `MikaLook.kt` / many `runCatching {}` | Low | Failures are swallowed; the UI shows success even when the DB insert or wallpaper set failed. |
| `cyber/src/.../pack/CyberPack.kt:153-177` | OK | Zip import already guards `..`, absolute paths, entry count and size. Add: decoded-bitmap size cap (decompression-bomb images) and MIME sniffing. |

## 4. Performance findings
- `mika/res/drawable-nodpi/mika_ic_*_3d.png`: four PNGs of 285–497 KB each (~1.7 MB) — convert to lossless/near-lossless WebP at the size actually drawn (≤ 256 px).
- `MikaLook.applyWallpaper` decodes full bitmaps without `inSampleSize`/`inPreferredConfig`.
- `MikaBoardView.kt` / `MikaIconTransformer.kt`: ~60 `Paint(`/`RectF(`/`decode` sites — audit for per-frame allocations and main-thread decoding.
- Older measurements (`bug-fix/04-performance-size.md`): debug APK 46 MB and slow by design; release ~17 MB with R8 + baseline profile. Always measure on **release**.

## 5. Theming today
- Theme = wallpaper + Cozy/Mika board + icon transformer. Status bar / nav bar are transparent via upstream styles (`res/values/styles.xml:78-79`, `values-v31`, `values-v33`) but icon colour (light/dark) is not driven by the active theme.
- No Control Center, no lock screen, no system-palette integration.
- Two separate token sets (`CyberTokens` vs `MikaTheme` constants) — no single theme model.

## 6. Existing pieces we can reuse
- `cyber/src/.../widgets/music/MediaSessionTracker.kt` — active media session via `MediaSessionManager.getActiveSessions` using Lawnchair's `NotificationListener` (declared in `AndroidManifest-common.xml:103-110`).
- Weather (Open-Meteo) worker in `cyber/.../widgets/weather/` (`Weather.kt`, WorkManager init guard).
- `CyberWallpaper` helpers, `CyberPack` zip format (extend for lock + palette fields).
- Fonts: Nunito (OFL) Regular/Bold/ExtraBold/Black in `cyber/res/font/`.
