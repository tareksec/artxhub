# Mika Cozy Cat Theme (মিকা থিম)

This theme module provides the complete cozy cat and anime home screen aesthetic matching the provided design specifications.

## 1. Visual Components & Layout

1. **Top Header Card**:
   - Live battery badge pill (e.g., `4%` or actual battery status)
   - Localized day of week (e.g., `sábado`)
   - Localized date header (e.g., `4 DE JANEIRO`)
   - Coffee doodle icon
   - Cutout illustration of anime girl with cozy cat ears/hoodie and sleeping white kitten on head (`mika_char_girl_cat_head.png`)
   - Interactive: tap opens Calendar / Clock

2. **Section Header**:
   - `🐾 Calendar 🐾`

3. **Photo Frame Widget**:
   - Rounded frame with fluffy white kitten looking at smartphone (`mika_photo_cat_phone.jpg`)
   - Subtitle: `🐾 Photos 🐾`
   - Interactive: tap opens Gallery / Photos

4. **Pastel App Grid**:
   - WhatsApp (soft mint green)
   - Instagram (coral/rose pastel)
   - TikTok (dark charcoal)
   - Pinterest (rose pink)
   - Spotify (mint)
   - Uber (charcoal)
   - Flo (peach/coral)
   - Right: Anime girl lifting black cat illustration (`mika_char_girl_blackcat.png`)

5. **Sticky Note & Quote Widgets**:
   - Warm paper note with pink washi tape: *"Good Things Take Time ♡"*
   - Peeking white kitten in bottom right corner (`mika_char_cat_peeking.png`)

6. **Search Pill**:
   - `🔍 Buscar 🐾` pill button
   - Interactive: tap opens Launcher app search

7. **Dock & Sleeping Kitten**:
   - Translucent cream dock bar (`#D9FBF4EB`) with Camera, Browser, Gallery, Calculator
   - Cute kitten sleeping belly down on the floor (`mika_char_cat_belly.png`)

8. **Lockscreen / Music Widget (`MikaLockScreenWidget`)**:
   - Chunky cocoa brown clock (`13:23`) with cat paw and doodle
   - Weather line: `Sáb., 4 🌧 Chuva forte ♡`
   - Live battery line + progress bar + Spotify circular glyph
   - Lofi Music Player card: *"Better Days · Lofi Vibes"* with Prev, Play/Pause, Next, and Heart buttons

## 2. Architecture & Isolation
- **No changes to `cyber/`**: CyberHome launcher and its widgets are 100% untouched.
- **Isolated module**: All Mika assets, layouts, styles, and Kotlin classes live inside `mika/`.
- **Dynamic Registration**: Both `CozyBoardWidget` and `MikaBoardWidget` are registered via `cyber_custom_widgets.xml` overlay.
- **Git Sync Pipeline**: `tools/sync_remote.ps1` automatically syncs upstream commits from other agents without conflicts.
