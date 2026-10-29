# CyberHome — Next-gen Upgrade প্ল্যান (AI-এর জন্য নির্দেশনা)

> অবস্থা: **শুধু নির্দেশনা ও spec।** এই folder-এ কোনো code নেই। পরের AI এগুলো পড়ে ধাপে ধাপে implement করবে।
> তারিখ: 2026-10-29 · Test phone: Nokia G10, Android 13 (API 33)

## আপনি যা চেয়েছেন (৪টা লক্ষ্য + Mika lock screen)

| # | লক্ষ্য | কোন ফাইলে |
|---|---|---|
| 1 | Phone-এর সাথে ভালোভাবে মিশে যাবে, কোনো security issue থাকবে না | [03-security-hardening.md](03-security-hardening.md) |
| 2 | Smooth আর fast | [04-performance-smoothness.md](04-performance-smoothness.md) |
| 3 | Dynamic, modern, next-generation UI | [05-next-gen-ui-design-system.md](05-next-gen-ui-design-system.md) |
| 4 | Theme শুধু home আর icon-এ নয়: status bar, navigation bar, quick settings (data on/off-এর panel), lock screen — সব জায়গায় | [02-android-limits.md](02-android-limits.md), [06-system-wide-theme-engine.md](06-system-wide-theme-engine.md) |
| 5 | Mika theme-এর lock screen পুরো production-grade, হুবহু reference-এর মতো, কিন্তু **screenshot বসানো নয়** | [07-mika-lockscreen-spec.md](07-mika-lockscreen-spec.md), [08-mika-lockscreen-implementation.md](08-mika-lockscreen-implementation.md) |

## ফাইলগুলো (এই ক্রমে পড়বে)

| ফাইল | কী আছে |
|---|---|
| [00-AI-MASTER-PROMPT.md](00-AI-MASTER-PROMPT.md) | **পরের AI-কে এটাই প্রথমে দিন।** সব নিয়ম, পড়ার ক্রম, কাজের ধরন |
| [01-current-state-audit.md](01-current-state-audit.md) | এখনকার code-এ কী আছে, কী ভুল (file:line সহ) |
| [02-android-limits.md](02-android-limits.md) | Android-এ launcher কী পারে আর কী পারে না (খুব জরুরি) |
| [03-security-hardening.md](03-security-hardening.md) | Security ও privacy কাজ |
| [04-performance-smoothness.md](04-performance-smoothness.md) | গতি, smoothness, APK size |
| [05-next-gen-ui-design-system.md](05-next-gen-ui-design-system.md) | নতুন design system, motion, glass, shader |
| [06-system-wide-theme-engine.md](06-system-wide-theme-engine.md) | এক theme → সব জায়গায় (status bar, nav bar, Control Center, system palette, lock) |
| [07-mika-lockscreen-spec.md](07-mika-lockscreen-spec.md) | Mika lock screen-এর pixel-level নকশা |
| [08-mika-lockscreen-implementation.md](08-mika-lockscreen-implementation.md) | Lock screen কীভাবে বানাবে (architecture, edge case) |
| [09-task-breakdown.md](09-task-breakdown.md) | ধাপে ধাপে task, প্রতিটার শেষ হওয়ার শর্ত, প্রতিটার জন্য আলাদা prompt |
| [10-testing-qa-checklist.md](10-testing-qa-checklist.md) | Test-এর তালিকা |

## সবচেয়ে জরুরি সত্য কথা (পড়ে নিন)

1. **এখনকার Mika lock screen আসলে একটা ছবি।** `mika_lockscreen_art.jpg`-এর ভেতরেই "13:23", "Better Days" music card, iPhone-এর status bar আঁকা আছে। সেটা system lock wallpaper হিসেবে বসানো হয়েছে, তাই তার ওপর phone-এর আসল ঘড়ি বসে দুটো ঘড়ি দেখা যায় (`mika_lockscreen_live.png`-এ স্পষ্ট)। এটা পুরো বাদ যাবে।
2. **একটা সাধারণ app (root ছাড়া) phone-এর আসল lock screen, notification shade বা quick settings panel-এর ভেতরটা বদলাতে পারে না।** এটা Android-এর security নিয়ম। তাই আমরা যা করব:
   - **Lock screen:** নিজস্ব "Mika Lock" screen, যেটা phone লক হলে system lock-এর **ওপরে** আসবে। Unlock করার সময় PIN/fingerprint সবসময় phone-এর নিজস্ব system দিয়েই হবে (আমরা কখনো নিজের PIN বানাব না — এটাই নিরাপদ)।
   - **Quick settings / data on-off panel:** home screen-এ নিচে টানলে আমাদের নিজস্ব themed **"Control Center"** খুলবে (Wi-Fi, data, flashlight, brightness, sound, notification)। Android-এর নিয়মে কিছু toggle (যেমন mobile data, Wi-Fi) সরাসরি বন্ধ-চালু করা যায় না, তখন system-এর ছোট panel খুলবে।
   - **System-এর নিজস্ব panel-এর রং:** Android 12+ এ (আপনার Nokia-তে চলবে) একটা ঐচ্ছিক "Advanced" mode থাকবে — একবার PC থেকে একটা `adb` command দিলে app পুরো phone-এর system রং (quick settings, settings, keyboard) Mika-র pastel রঙে বদলে দিতে পারবে।
   - **Status bar ও navigation bar:** আমাদের app-এর ভেতরে (home, drawer, lock, control center) পুরো themed — transparent, সঠিক icon রং।
   - বিস্তারিত: [02-android-limits.md](02-android-limits.md)।

## কাজ শুরুর আগে আপনার সিদ্ধান্ত লাগবে

| # | প্রশ্ন | আমার সুপারিশ |
|---|---|---|
| D1 | Mika Lock চালাতে "Display over other apps" permission দেবেন? (না দিলে অন্য app খোলা অবস্থায় lock করলে Mika Lock নাও আসতে পারে) | হ্যাঁ, ঐচ্ছিক হিসেবে, onboarding-এ ব্যাখ্যা সহ |
| D2 | System palette-এর জন্য ADB "Advanced mode" রাখব? | হ্যাঁ, ঐচ্ছিক। না দিলে সব কিছু তবুও চলবে |
| D3 | Lock screen-এর পরিষ্কার art (ঘড়ি/লেখা ছাড়া background: জানালা, গাছ-মগ, বই, sticky note, ঘুমন্ত বিড়াল) কে দেবে? | আপনি দেবেন (নিজের বা commercial-license সহ AI art), ≥1080×2400, আলাদা layer হলে সবচেয়ে ভালো। না পেলে AI প্রথমে বর্তমান ছবি থেকে লেখা মুছে একটা অস্থায়ী version বানাবে |
| D4 | `bug-fix/lockscreen` আর `bug-fix/wallaper`-এর anime ছবি (Zero Two, Rem ইত্যাদি) | এগুলো **শুধু mood reference**। এগুলো copyrighted character, APK-তে ঢোকানো যাবে না (CLAUDE.md rule 5)। Personal phone-এ নিজে wallpaper হিসেবে বসাতে চাইলে app-এর "নিজের ছবি বেছে নিন" দিয়ে বসাবেন |
| D5 | Lock-এর "Fonseca 🦋" আর "Small steps big dreams" লেখা | এগুলো আপনার নিজের নাম/লেখা দিয়ে বদলানো যাবে (settings থেকে)। Default: আপনার নাম, আর "Small steps, big dreams" |
| D6 | Public source repo URL | এখনো `TODO`। GPL মানতে বন্ধুদের APK দেওয়ার আগে লাগবে |

## মনে রাখার নিয়ম

- আপনি phone-এ APK test না করা পর্যন্ত AI কোনো git commit করবে না (আপনার আগের নির্দেশ)।
- প্রতিটা task-এর পর build সবুজ থাকতে হবে, আর `docs/PROGRESS.md`-এ note যাবে।
