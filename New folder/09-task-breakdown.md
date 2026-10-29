# 09 — Task breakdown (do in this order)

> বাংলা সারাংশ: প্রতিটা task ছোট, আলাদাভাবে যাচাই করা যায়। প্রতিটার নিচে একটা ready-made prompt আছে — পরের AI-কে শুধু `00-AI-MASTER-PROMPT.md` + ওই task-এর prompt দিলেই চলবে। আপনি phone-এ test করে "ঠিক আছে" না বলা পর্যন্ত কোনো commit হবে না।

Every task ends with: release build OK → install on Nokia G10 → screenshot → `docs/PROGRESS.md` note → owner tests → (owner approves) commit.

---

## Phase A — Safety first

### A1. Theme switch never loses user data
- Files: `mika/src/.../theme/MikaLook.kt`, new `cyber/src/.../theme/ThemeSnapshot.kt`.
- Accept: apply Mika → apply Cyber → dock + screen 0 identical; instrumented test passes; failures shown to user.
- Prompt: *"Follow 00-AI-MASTER-PROMPT.md. Do task A1 from upgrade-plan/09-task-breakdown.md using 03-security-hardening.md §S1. Snapshot favorites rows before any change, move conflicting items instead of deleting, restore on revert, wrap DB work in a transaction. Build, install, verify on the Nokia, add a PROGRESS note. Do not commit."*

### A2. Lock down exported components & permissions
- Files: `mika/AndroidManifest.xml`, `cyber/AndroidManifest.xml`, root manifests via `tools:node="remove"`, `docs/PERMISSIONS.md`, `docs/UPSTREAM_CHANGES.md`.
- Accept: `03` S2/S3 acceptance; every remaining permission has a feature + reason in PERMISSIONS.md; app still works (search, widgets, notification dots, weather).
- Prompt: *"Do task A2 (03 §S2, §S3). Audit every permission and exported component; remove what no shipped feature uses; make MikaPreviewActivity non-exported. Verify on device nothing broke."*

### A3. Data & network hygiene
- `network_security_config`, backup rules, pack image-bomb guard, PendingIntent flags (`03` §S6–S7).
- Prompt: *"Do task A3 (03 §S6, §S7). Then run lint on the release variant and fix Security/Correctness issues in cyber/ and mika/."*

## Phase B — Fast & smooth

### B1. Measure baseline
- Write Macrobenchmarks (startup, all-apps, Control Center placeholder later, lock later); create `docs/PERFORMANCE.md` with release numbers on the G10.
### B2. Asset diet
- WebP conversion, downsizing (`04` rule 8). Accept: release APK ≤ 20 MB, Mika 3D icons < 200 KB total, no visible quality loss.
### B3. Draw & decode fixes
- StrictMode on (debug), remove onDraw allocations in `MikaBoardView`, `MikaLockScreenView`, `CozyBoardView`, `MikaIconTransformer`; off-main bitmap decode with sampling.
- Accept: 0 StrictMode violations from our packages in a 5-minute session; janky frames ≤ 1% in gfxinfo.
### B4. Theme apply without freeze
- Background executor + progress sheet (`06` §1 UI). Accept: no "Working…" freeze; < 2 s.
- Prompt (B1–B4 each): *"Do task B<n> from 09 following 04-performance-smoothness.md. Measure before and after on the Nokia release build and record numbers in docs/PERFORMANCE.md."*

## Phase C — Design system

### C1. `CyberThemeSpec` + `ThemeRepository`
- Migrate `CyberTokens` and `MikaTheme` constants into specs; all existing views read from the repository. No visual change expected (screenshot before/after identical).
### C2. Motion, glass, haptics primitives
- Shared helpers: springs, press-scale, glass surface drawable/composable with blur fallback, haptic helper.
### C3. Restyle home/all-apps/folders with the tokens
- Accept: switching theme restyles all three live without restart.
- Prompt: *"Do task C<n> following 05-next-gen-ui-design-system.md. Keep upstream edits minimal and list them in UPSTREAM_CHANGES.md."*

## Phase D — Theme everywhere

### D1. `SystemBarStyler` + `ThemeApplier`
- `06` §1–§2. Hook into `src/com/android/launcher3/util/SystemUiController.java` minimally.
- Accept: bar icon colour correct on home, all-apps, settings, for Mika light/dark and Cyber.
### D2. Control Center
- `06` §3: Compose panel, swipe-down action, QS `TileService`, real toggles (flashlight, DND, brightness with grant), honest system-panel shortcuts, media card, notifications.
- Accept: `06` acceptance; works with every optional permission denied (tiles show "grant" state).
### D3. System palette (ADB mode)
- `02` advanced section + `06` §5. Accept on the Nokia: after `adb shell pm grant …`, applying Mika turns the real Quick Settings pink/cream; "Restore system colours" returns the exact previous value.
### D4. Theme pack format v2
- `06` §6. Format-1 packs still import; new fields validated.

## Phase E — Mika Lock (production)

### E0. Art
- Owner delivers clean layered art (README D3). Until then make a TEMP clean plate by removing baked UI from the reference. Move `mika_lockscreen_art.jpg` out of `res/` into `mika/design/reference/`.
### E1. Vectors & fonts
- All doodles/icons from `07` §4 as vector drawables; pick fonts via overlay test (`07` §3 fonts); add OFL license texts to `docs/licenses/`.
### E2. `MikaLockContentView` (render only)
- Implement E1–E13 exactly per `07` §3 with live data via `LockStateRepository`. Also used by the home widget `MikaLockScreenWidget` (replace old `MikaLockScreenView`).
- Accept: fidelity diff `07` §6 passes (±4 px, ±3 %, ΔE ≤ 5).
### E3. Lock host & flow
- `LockOverlayController`, `LockHostActivity`, unlock gesture, `requestDismissKeyguard`, USER_PRESENT handling (`08` §1–§2). Investigate background-start on the Nokia and implement the needed opt-in path.
- Accept: with PIN set, 20 lock/unlock cycles: Mika Lock appears every time, system PIN always required, no crash, no stuck black screen.
### E4. Notifications, media, privacy
- Pill row + sheet, privacy rules `03` §S5, open-after-unlock actions, media heart logic.
### E5. Edge cases
- Every row of `08` §3 handled and tested.
### E6. Settings & onboarding
- `MikaLockSettingsActivity`, onboarding permission screens (en + bn).
- Prompt (E-tasks): *"Do task E<n> from 09. Spec: 07-mika-lockscreen-spec.md, implementation: 08-mika-lockscreen-implementation.md, security: 03 §S4–S5. Nothing may be baked into images; never bypass the keyguard. Verify on the Nokia G10 with a PIN set and attach the fidelity diff image to PROGRESS.md."*

## Phase F — Release
- Full `10-testing-qa-checklist.md` on release build; update `RELEASE_NOTES.md`, `PERMISSIONS.md`, `PERFORMANCE.md`, About screen repo URL (needs owner's URL); signed release APK for friends.
