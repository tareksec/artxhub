# 05 — Next-generation UI & design system

> বাংলা সারাংশ: একটাই design system — রং, font, কোণ, ছায়া, motion সব token থেকে আসবে। প্রতিটা theme (Cyber, Mika) শুধু token-এর মান বদলায়। Glass/blur, spring animation, haptic, parallax আর light/dark — সব জায়গায় একরকম।

## 1. One theme model (replaces `CyberTokens` + `MikaTheme` constants)
Create `com.techvrs.cyberhome.cyber.theme` (shared engine):

```kotlin
// SPDX-License-Identifier: GPL-3.0-or-later
data class CyberThemeSpec(
    val id: String,                 // "cyber", "mika", pack ids
    val colors: ThemeColors,        // light + dark variants
    val type: ThemeType,            // font families + scale
    val shape: ThemeShape,          // corner radii, icon shape
    val motion: ThemeMotion,        // spring stiffness/damping, durations
    val surfaces: ThemeSurfaces,    // glass alpha, blur radius, border, shadow
    val systemBars: SystemBarSpec,  // light icons?, scrim colors
    val iconStyle: IconStyleSpec,   // transformer id + params
    val wallpaper: WallpaperSpec,   // home + lock art resources
    val lock: LockSpec?,            // null = theme has no custom lock layout
    val controlCenter: ControlCenterSpec,
    val systemPalette: SystemPaletteSpec?, // seed color + style for ADB mode
)
```
- `ThemeRepository` (single source of truth): current spec as `StateFlow<CyberThemeSpec>`, persisted in DataStore/SharedPreferences, emits on change; every surface observes it.
- Views read tokens through a small `ThemeTokens` accessor; Compose screens get a `CompositionLocal`.
- `MikaTheme` constants become `MikaThemeSpec` (an instance), keeping the exact colours already tuned (cream `#FBF6EF`, mocha `#4A3728`, pink `#F4A3B4` …) plus the lock values in `07`.
- Theme packs (`CyberPack` zip format v2) can define a full `CyberThemeSpec` in JSON (schema in `06`).

## 2. Visual language (applies to every theme)
- **Layers**: wallpaper → ambient layer (parallax decor, optional) → glass surfaces → content → accents. Every card is one of three surface levels (`surface0` flat, `surface1` glass, `surface2` elevated glass).
- **Glass**: tinted translucent fill (`surfaces.glassAlpha` 0.72–0.86), 1 dp hairline border (`colors.border` @ 60%), soft shadow (y 6 dp, blur 18 dp, 8% ink), optional background blur 16–24 dp on API 31+.
- **Typography**: display (clock), title, body, caption — each with size, weight, letter-spacing, line-height tokens. Only OFL/Apache fonts. Keep Nunito; evaluate adding *Fredoka* or *Baloo 2* (OFL) for Mika's rounded display numerals (see `07`).
- **Iconography**: one stroke weight per theme (Mika: 1.75 dp rounded caps), vector drawables only, no emoji glyphs in UI.
- **Colour roles**: `bg, surface, surfaceGlass, border, ink, inkBody, inkDim, accent, accentSoft, success, warning, danger` + per-theme extras. Each has light & dark values; dark mode for Mika = "night café" (cocoa `#2A211D` bg, cream ink) — design it, don't invert.
- **Dynamic colour**: theme may set `followWallpaper = true` to derive accents from `WallpaperColors` (Monet-like) while keeping the theme's ink/shape/motion.

## 3. Motion system
| Token | Value (default) | Used for |
|---|---|---|
| `spring.snappy` | stiffness 800, damping 0.85 | buttons, toggles |
| `spring.smooth` | stiffness 380, damping 0.9 | panels, Control Center, cards |
| `spring.bouncy` | stiffness 500, damping 0.6 | playful accents (paw tap, heart) |
| `fade.short` | 120 ms | text swaps |
| `stagger` | 24 ms | list/grid entrance |
- Shared-element style transitions: widget → its settings screen; music card → expanded player.
- Gesture-driven: Control Center and Mika Lock unlock follow the finger (velocity-tracked), then spring.
- Micro-interactions: press scale 0.96, haptic `CLOCK_TICK`/`CONFIRM`/`REJECT` (API 30+) with fallbacks; heart → small burst; clock colon gently breathes (2 s, alpha 0.6–1) — disable with reduce-motion.
- Parallax: decor layers move 2–8 dp with workspace scroll and device tilt (`TYPE_GAME_ROTATION_VECTOR`, only while screen on and visible, low rate). Off when battery saver is on.

## 4. Screens to redesign with the system
1. Home (Cozy/Mika board) — token-driven, glass cards.
2. All apps — themed header, search pill, bottom art (already planned in PROGRESS.md).
3. Folders — glass sheet, theme corner radius.
4. **Control Center** (new, `06`).
5. **Mika Lock** (new, `07/08`).
6. Theme gallery / customizer — Compose, live preview of home + lock + Control Center side by side; "Apply" shows a progress sheet, never a frozen screen.
7. Onboarding — permission explanations with illustrations (notification access, display over apps, modify settings, optional ADB mode).
8. Settings & About (GPL source link).

## 5. Accessibility
- Touch targets ≥ 48 dp; contrast ≥ 4.5:1 for body text on glass (check over the actual wallpaper; add a local scrim when needed).
- All custom-drawn controls expose virtual views via `ExploreByTouchHelper` (clock, music buttons, notes).
- Font scale up to 1.3× must not clip (clock scales down to fit, never overflows).
- TalkBack labels localised (en + bn).
