# 04 — Performance, smoothness and size

> বাংলা সারাংশ: সব মাপ release build দিয়ে নেওয়া হবে, Nokia G10-এ। লক্ষ্য: দ্রুত খোলা, কোনো ঝাঁকুনি (jank) নেই, lock screen screen-on হওয়ার সাথে সাথেই তৈরি।

Device class: Nokia G10 = Unisoc T606, 4 GB RAM, 60 Hz, 720×1600. If it is smooth here it is smooth everywhere.

## Targets (release build, Nokia G10)
| Metric | Target | How to measure |
|---|---|---|
| Launcher cold start to first frame | < 1.5 s | `adb shell am start -W -S` on the home intent; Macrobenchmark `StartupBenchmark` in `baseline-profile/` |
| Home swipe / all-apps open / folder open | 0 janky frames in 10 runs (frame time < 16.6 ms) | `adb shell dumpsys gfxinfo com.techvrs.cyberhome framestats`, Macrobenchmark `FrameTimingMetric` |
| Screen-on → Mika Lock fully drawn | < 150 ms (activity pre-created on screen-off) | logcat timestamps + Perfetto trace |
| Control Center open | first frame < 100 ms, 60 fps animation | gfxinfo |
| Theme apply (Mika ↔ Cyber) | < 2 s with progress UI, no "Working…" freeze | stopwatch + trace |
| Memory (home idle) | PSS < 180 MB | `dumpsys meminfo` |
| Release APK | ≤ 20 MB | file size |

## Rules for implementers
1. **Main thread**: no disk, DB, network, or `BitmapFactory` work. Enable `StrictMode` (thread + VM policies, `penaltyLog`) in debug builds for our packages and fix every violation we cause.
2. **Bitmaps**: decode with `inJustDecodeBounds` → `inSampleSize` to the target view size; `inPreferredConfig = HARDWARE` for static art drawn on hardware canvases (API 26+); cache in an `LruCache` sized to 1/8 of `maxMemory`; recycle nothing manually with hardware bitmaps.
3. **onDraw**: zero allocations. Pre-create `Paint`, `RectF`, `Path`, `StaticLayout`; rebuild them in `onSizeChanged` or when data changes. Cache text layouts; only re-layout the clock when the minute changes.
4. **Invalidation**: invalidate only dirty regions (clock area on `TIME_TICK`, battery area on battery change). Use separate child views / `RenderNode`s for static art vs. live text so the art layer is never re-rasterized.
5. **Blur/glass**: `RenderEffect.createBlurEffect` (API 31+) on a *snapshot* layer, not per-frame on large areas; on API < 31 use a pre-blurred downscaled bitmap. Measure cost on the G10; cap radius at 24 dp.
6. **Shaders**: AGSL `RuntimeShader` (API 33+) only for small accents (glow, grain); provide static fallbacks.
7. **Animations**: `SpringAnimation`/Compose `spring()`; animate only `translation`, `alpha`, `scale` (render-thread friendly). Respect `Settings.Global.ANIMATOR_DURATION_SCALE` = 0 and "Remove animations" accessibility.
8. **Assets**: convert all PNG/JPG art to WebP (lossless for icons/cutouts, q≈85 for wallpapers); ship wallpapers at 1080×2400 max; icon art at ≤ 256 px. The four `mika_ic_*_3d.png` (~1.7 MB) must shrink to < 200 KB total.
9. **Baseline profile**: regenerate after every major UI change (`baseline-profile/` module) including flows: cold start, open all-apps, open Control Center, show Mika Lock.
10. **Background work**: weather via WorkManager (≥ 1 h interval, network constraint); no polling loops; listeners unregistered in `onDetachedFromWindow`/`onStop`.
11. **Theme apply**: run DB + wallpaper work on a background executor with a progress UI; apply the wallpaper with a correctly sized bitmap (`WallpaperManager.getDesiredMinimumWidth/Height`), not full-res decode.
12. **Process priority**: as the default home app our process is usually kept alive; do not add a permanent foreground service unless the user opts in (battery).

## Deliverables
- `docs/PERFORMANCE.md` with the measured table above (before/after) for each release.
- Macrobenchmark tests for: startup, all-apps open, Control Center open, Mika Lock show.
