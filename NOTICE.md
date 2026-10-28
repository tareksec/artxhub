# NOTICE

**CyberHome** is a modified version of **Lawnchair** (branch `15-dev`).

- Lawnchair — Copyright (c) Lawnchair Launcher contributors
  https://github.com/LawnchairLauncher/lawnchair
- Launcher3 — Copyright (c) The Android Open Source Project (AOSP)
  https://android.googlesource.com/platform/packages/apps/Launcher3

## Licensing

- **Upstream code** (Lawnchair, Launcher3) is licensed under the Apache License,
  Version 2.0 — see [`LICENSE.txt`](LICENSE.txt). A few Lawnchair source files carry GPL-3.0
  headers instead; they keep them. All upstream copyright headers are kept unchanged.
- **CyberHome's own code and changes** are licensed under the GNU General Public License,
  version 3 or (at your option) any later version — see [`COPYING`](COPYING). Our files carry
  `SPDX-License-Identifier: GPL-3.0-or-later`.
- **CyberHome as a whole** (the combined work, including the APK we distribute) is therefore
  distributed under GPL-3.0-or-later. Apache 2.0 code can be included in a GPLv3 work.
- Our own art/brand assets (once added) are not covered by the GPL; they live in
  `assets-proprietary/` with their own license.

## Our changes

Every upstream file we modified is listed in
[`docs/UPSTREAM_CHANGES.md`](docs/UPSTREAM_CHANGES.md) and carries a
"Modified by CyberHome" comment. Our own code lives in the `cyber/` folder and in the
`com.techvrs.cyberhome.cyber` package; our own resources use the `cyber_` prefix.

"Lawnchair" is the name of the upstream project. CyberHome is not affiliated with or
endorsed by the Lawnchair team.

## Third-party components

Libraries bundled by Lawnchair keep their own licenses; the build generates a
report of them (`licenses.json`, via the licensee plugin).

Fonts added by CyberHome (SIL Open Font License 1.1; static Latin-subset instances as
distributed by Fontsource, glyphs unmodified):

| Font | Copyright | Files | License |
|---|---|---|---|
| Nunito (Regular, Bold, ExtraBold, Black) | Copyright 2014 The Nunito Project Authors (https://github.com/googlefonts/nunito) | `cyber/res/font/cyber_nunito_*.ttf` | [`docs/licenses/OFL-Nunito.txt`](docs/licenses/OFL-Nunito.txt) |

Character art and wallpapers (`cyber/res/drawable-nodpi/cyber_char_*.png`, `cyber_wall_*.jpg`,
originals in `assets-proprietary/`) were made by the CyberHome owner: **all rights reserved**, not
covered by the GPL (see [`assets-proprietary/LICENSE`](assets-proprietary/LICENSE)). Icons and
placeholder card art (`cyber/res/drawable/cyber_ic_*.xml`, `cyber_card_art_*.xml`) are original
vector drawings.

Weather data by [Open-Meteo.com](https://open-meteo.com/) (CC BY 4.0).

Libraries added by CyberHome: AndroidX WorkManager (Apache 2.0).

## Source code

Source repository: TODO (public repo URL, to be added by the maintainer)
