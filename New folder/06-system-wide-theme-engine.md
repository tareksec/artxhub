# 06 — System-wide theme engine (theme everywhere, not only home + icons)

> বাংলা সারাংশ: একবার theme বেছে নিলে সেটা home, icon, drawer, folder, status bar/nav bar-এর রং, আমাদের নিজস্ব Control Center (data/Wi-Fi/flash-এর panel), Mika Lock, আর (ADB mode চালু থাকলে) পুরো phone-এর system রঙে লাগবে। আর theme বদলালে সব একসাথে বদলাবে, সব একসাথে ফেরত যাবে।

Depends on `05` (theme model) and the limits in `02`.

## 1. ThemeApplier — one entry point
`ThemeApplier.apply(spec, options)` runs these steps in order on a background executor, reports progress, and is fully reversible:

| Step | What | Reversible by |
|---|---|---|
| 1 | Snapshot current state (favorites rows for screen 0 + hotseat, wallpapers' IDs, previous system palette JSON, previous theme id) — see `03` S1 | `ThemeApplier.revert()` |
| 2 | Persist `spec.id` in `ThemeRepository` → every observing surface recomposes/redraws | previous id |
| 3 | Board: place/replace the theme's board widget without deleting user items | snapshot |
| 4 | Icons: switch icon transformer, clear icon cache, `reloadIcons()` | previous transformer |
| 5 | Home wallpaper (`FLAG_SYSTEM`) — correctly sized | `WallpaperManager` can't "undo"; we store the previous built-in id or user file |
| 6 | Lock wallpaper (`FLAG_LOCK`) — **clean art only** (no clock/text) | same |
| 7 | Mika Lock / theme lock overlay enabled if `spec.lock != null` and user opted in | toggle |
| 8 | Control Center skin | automatic (observes theme) |
| 9 | System palette (only if ADB permission granted and user enabled it) | saved JSON |

UI: a bottom sheet with step checkmarks; on failure, show which step failed and offer "Revert".

## 2. Status bar & navigation bar (inside CyberHome windows)
- A single helper `SystemBarStyler.apply(window, spec.systemBars, isDark)` called from: Launcher activity (on theme change and on state change — workspace vs all-apps vs overlay), Control Center, Mika Lock, all CyberHome settings activities.
- Uses `WindowCompat.setDecorFitsSystemWindows(window, false)`, transparent bars, `isAppearanceLightStatusBars/NavigationBars` from the theme (Mika light → dark icons; Mika dark/Cyber → light icons), `isNavigationBarContrastEnforced = false`.
- Auto-contrast option: sample the wallpaper's top 8% / bottom 8% luminance (`WallpaperColors.colorHints` + a downscaled sample) and pick icon colour per area.
- Optional theme scrims: top gradient (status bar) and bottom glow (nav bar) drawn by our root view — e.g. Mika: cream → transparent, 64 dp, alpha 0.5.
- Upstream touch point: Launcher3 sets bar appearance in `SystemUiController` (`src/com/android/launcher3/util/SystemUiController.java`) — hook there with a minimal override flag; record it in `UPSTREAM_CHANGES.md`.

## 3. CyberHome Control Center (replaces the system shade *from home*)
Trigger: swipe down on the workspace (Lawnchair gesture setting "Swipe down" → new action `OpenControlCenter`, default for themed setups; keep "Open notifications" as an option). Also: a Quick Settings tile "CyberHome Center" (`TileService`) so it can be opened from anywhere, and a long-press action.

Layout (themed by tokens; Mika variant uses glass cream cards, mocha ink, pink accents, paw ornaments):
1. **Header**: big time + date, battery %, a small theme character/ornament.
2. **Connectivity row** (2×2 large tiles): Internet (state from `NetworkCallback`; tap → `Settings.Panel.ACTION_INTERNET_CONNECTIVITY`), Bluetooth (state; tap → settings), Flashlight (real toggle), Do-not-disturb (real toggle if policy access, else opens the grant screen).
3. **Small tiles** (grid, user-reorderable): auto-rotate, ringer mode, location (opens settings), hotspot (opens settings), screen record/cast (open system intents), calculator, camera, alarm.
4. **Sliders**: brightness (real if `WRITE_SETTINGS`), media volume.
5. **Media card** (from `MediaSessionTracker`) — same component as Mika Lock's music card.
6. **Notifications** (from the notification listener): grouped by app, swipe to dismiss, tap to open, inline reply for `RemoteInput` (optional, later).
7. Footer: "System panel" button → `StatusBarManager`/`expandNotificationsPanel` via existing `EXPAND_STATUS_BAR` permission, and "Edit".

Implementation: Compose (Lawnchair already uses Compose) hosted in a launcher overlay window/`AbstractFloatingView`, so it opens instantly without an activity launch. Drag-to-open with the finger, spring settle, scrim + blur of the workspace behind.

Every tile honestly reflects what it can do: if an action only opens a system panel, the tile shows a small "↗" affordance.

## 4. Lock screen per theme
- `spec.lock` describes a layout (Mika's is fully specified in `07`). Cyber gets a simpler lock later; until then Cyber uses system keyguard + clean wallpaper.
- The overlay host (`08`) is theme-agnostic: it renders `spec.lock` through a `LockLayoutRenderer` interface, so future themes plug in.

## 5. System palette (ADB mode) — see `02`
- `SystemPaletteController`: `isAvailable()` (API ≥ 31 && permission granted), `apply(seed, style)`, `restore()`.
- Per theme seed: Mika `F4A3B4` (pink) style `TONAL_SPOT`; Cyber: its accent with `VIBRANT`.
- After applying, verify by reading back; show "Your phone's brand may ignore this" if unchanged.

## 6. Theme pack format v2 (extend `docs/THEME_PACKS.md`)
```json
{
  "format": 2,
  "name": "Mika Cozy",
  "theme": {
    "colors": { "light": { "bg": "#FBF6EF", "ink": "#4A3728", "accent": "#F4A3B4" }, "dark": { } },
    "type": { "display": "builtin:nunito_black" },
    "systemBars": { "lightIcons": false, "topScrim": "#80FBF6EF" },
    "systemPalette": { "seed": "F4A3B4", "style": "TONAL_SPOT" },
    "lock": { "layout": "mika", "art": "lock/background.webp", "noteText": "Small steps, big dreams" }
  },
  "wallpaper": { "image": "wallpaper.webp" }
}
```
- Format 1 packs must still import. Validate every field; unknown fields ignored; colours parsed strictly.

## Acceptance
- Switching Mika ↔ Cyber changes home, icons, drawer, folders, bar icon colours, Control Center, lock overlay and (if enabled) system palette — and reverting restores everything, including the user's dock.
- Swiping down on home opens Control Center in < 100 ms; flashlight toggles; internet tile opens the system internet panel; notifications listed match the system shade.
