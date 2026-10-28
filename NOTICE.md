# NOTICE

**CyberHome** is a modified version of **Lawnchair** (branch `15-dev`).

- Lawnchair — Copyright (c) Lawnchair Launcher contributors
  https://github.com/LawnchairLauncher/lawnchair
- Launcher3 — Copyright (c) The Android Open Source Project (AOSP)
  https://android.googlesource.com/platform/packages/apps/Launcher3

## Licensing

- **Upstream code** (Lawnchair, Launcher3) is licensed under the Apache License,
  Version 2.0 — see [`LICENSE.txt`](LICENSE.txt). All upstream copyright headers are kept
  unchanged, and that code stays available under Apache 2.0.
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

## Source code

Source repository: TODO (public repo URL, to be added by the maintainer)
