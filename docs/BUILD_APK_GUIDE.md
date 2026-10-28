# APK বিল্ড গাইড (নিজের ফোনে টেস্ট করার জন্য)

লক্ষ্য: Lawnchair fork থেকে নিজের launcher-এর `.apk` বানিয়ে ফোনে ইনস্টল করা।
Play Store এখন নয়। আগে নিজে ও বন্ধুরা টেস্ট করবেন।

---

## ১. যা যা লাগবে

| জিনিস | কেন | টিপস |
|---|---|---|
| PC/Laptop (Windows, Linux বা Mac) | Android build ভারী কাজ | RAM ১৬ GB হলে ভালো (৮ GB-এ ধীর), খালি জায়গা কমপক্ষে ৩০-৫০ GB |
| **JDK 17** (বা নতুন) | Gradle build চালাতে | Android Studio-র সাথে আসা JDK-ও চলে |
| **Android Studio** (সর্বশেষ stable) | SDK ও Gradle সহজে ম্যানেজ করতে | SDK Manager থেকে প্রজেক্ট যে `compileSdk` চায় সেই Platform নামান (Claude Code জানাবে, Lawnchair 15 এর জন্য সম্ভবত API 35) |
| **Git** | repo clone করতে | |
| **adb** (Platform-Tools) | ফোনে সরাসরি ইনস্টল ও debug | Android Studio-র সাথে আসে |
| **Claude Code** + Opus model | কোড লিখতে | আপনার প্ল্যান অনুযায়ী |
| একটা Android ফোন (Android 10+) | টেস্ট | USB cable সহ |
| ভালো ইন্টারনেট | Gradle dependency নামাতে (প্রথমবার কয়েক GB) | |

প্রথম বিল্ড ২০-৪০ মিনিট লাগতে পারে। এটা স্বাভাবিক।

---

## ২. একবারের সেটআপ

1. **Android Studio** ইনস্টল করুন, খুলে **SDK Manager** থেকে নামান:
   - Android SDK Platform (Claude Code যে API বলবে)
   - Android SDK Build-Tools
   - Android SDK Platform-Tools
2. JDK 17 আছে কিনা দেখুন:
   ```
   java -version
   ```
3. Git আছে কিনা দেখুন:
   ```
   git --version
   ```
4. ফোনে **Developer options** চালু করুন:
   - Settings → About phone → **Build number** ৭ বার ট্যাপ
   - Settings → Developer options → **USB debugging** চালু
5. ফোন USB দিয়ে লাগিয়ে PC-তে চেক করুন:
   ```
   adb devices
   ```
   ফোনে "Allow USB debugging" আসলে Allow দিন।

---

## ৩. প্রজেক্ট ফোল্ডার

1. একটা ফোল্ডার বানান, যেমন `cyberhome-project`।
2. এই ডকস ফোল্ডারের ফাইলগুলো (`CLAUDE.md`, `PROJECT_SPEC.md`, `TASKS.md`, `ASSETS.md`) সেখানে রাখুন।
3. ফোল্ডারে terminal খুলে Claude Code চালু করুন:
   ```
   claude
   ```
4. `TASKS.md`-এর শুরুর prompt দিন, তারপর **একটা একটা করে Task** দিন (Task 0 থেকে)।

Task 0-তেই Claude Code Lawnchair clone করবে। নিজে করতে চাইলে:
```
git clone --recurse-submodules -b 15-dev https://github.com/LawnchairLauncher/lawnchair.git cyberhome
cd cyberhome
```

---

## ৪. APK বিল্ড করা

প্রজেক্টের ভেতরে (Claude Code ঠিক variant নাম জানাবে):

**Debug APK (নিজের টেস্টের জন্য, সবচেয়ে সহজ):**
```
./gradlew assembleDebug
```
Windows-এ `gradlew.bat assembleDebug`। APK থাকে `build/outputs/apk/...` ফোল্ডারে (Claude Code সঠিক পাথ বলে দেবে)।

**ফোনে সরাসরি ইনস্টল (USB):**
```
adb install -r path/to/file.apk
```

**USB ছাড়া:** APK ফোনে পাঠান (Telegram/Drive/কেবল), ফাইল ম্যানেজার থেকে খুলুন, "Install unknown apps" অনুমতি দিন।

---

## ৫. ফোনে প্রথমবার চালানো

1. app খুলুন, onboarding-এ **Apply Look** চাপুন।
2. "Home app" হিসেবে আপনার launcher নির্বাচন করুন।
3. না হলে: Settings → Apps → Default apps → **Home app** → আপনার launcher।

**নিরাপত্তা:** আগের launcher (Pixel Launcher বা ফোনের নিজের) ইনস্টল রেখে দিন। কিছু গোলমাল হলে Settings → Default apps → Home app থেকে ফিরে যেতে পারবেন। আপনার app uninstall করলেও ফোন আগের launcher-এ ফিরে যায়।

---

## ৬. বন্ধুদের জন্য Release APK (Task 10)

Debug APK বন্ধুদের দেওয়ার জন্য ভালো না (ধীর, debug চিহ্নযুক্ত)। Signed release APK লাগবে।

1. **Keystore** বানান (একবারই, খুব যত্নে রাখুন):
   ```
   keytool -genkeypair -v -keystore cyberhome.jks -alias cyberhome -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **Keystore ফাইল আর password হারালে আপডেট দেওয়া যাবে না।** একাধিক জায়গায় ব্যাকআপ রাখুন (Google Drive, pendrive)।
3. **কখনো** keystore বা password GitHub-এ তুলবেন না। Claude Code `keystore.properties` ফাইল `.gitignore`-এ রাখবে।
4. বিল্ড:
   ```
   ./gradlew assembleRelease
   ```
5. বন্ধুদের জানান: APK ইনস্টলের সময় "Unknown source" অনুমতি দিতে হবে, আর Google Play Protect একবার সতর্কতা দেখাতে পারে (অপরিচিত ডেভেলপারের কারণে)। এটা স্বাভাবিক।

---

## ৭. সাধারণ সমস্যা ও সমাধান

| সমস্যা | সমাধান |
|---|---|
| `SDK location not found` | প্রজেক্ট রুটে `local.properties`-এ `sdk.dir=আপনার SDK পাথ` দিন (Android Studio খুললে নিজে তৈরি হয়) |
| `Unsupported class file / JDK` error | JDK 17 ব্যবহার করুন, `JAVA_HOME` ঠিক করুন |
| Out of memory | `gradle.properties`-এ `org.gradle.jvmargs=-Xmx4g` দিন |
| Submodule খালি / ফাইল নেই | `git submodule update --init --recursive` |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | আগের ভিন্ন key-র একই package ইনস্টল আছে। আগেরটা uninstall করুন |
| `adb devices` এ ফোন নেই | অন্য cable, USB mode "File transfer", driver ইনস্টল (Windows) |
| Widget home screen-এ নিজে বসছে না | এটা জানা ঝুঁকি। Task 4-এ Claude Code বিকল্প পথ করবে। কিছুদিন manual widget picker থেকে যোগ করা যাবে |
| Build ধীর | Gradle daemon চালু রাখুন, দ্বিতীয়বার থেকে দ্রুত হবে |

---

## ৮. আপনার করণীয় তালিকা (Claude Code-কে দেওয়ার আগে)

- [ ] `APP_NAME` ও `APP_ID` ঠিক করুন (এখন `CyberHome` / `com.techvrs.cyberhome`)। `CLAUDE.md`-তে বদলে নিন।
- [ ] GitHub-এ একটা public repo খুলুন (GPL-এর জন্য)। নাম ঠিক করুন, URL `CLAUDE.md`-তে বসান।
- [ ] Android Studio, JDK 17, Git ইনস্টল।
- [ ] ফোনে USB debugging চালু।
- [ ] নিজের আঁকা/লাইসেন্স করা art আছে কিনা ঠিক করুন (না থাকলে placeholder দিয়েই শুরু হবে)।
- [ ] Debug APK টেস্ট হলে তবেই Task 10-এর keystore বানান।
