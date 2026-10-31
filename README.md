# Pinnacle SMS Organizer

A production-quality native Android SMS Organizer inspired by Microsoft SMS Organizer, built with Kotlin, Jetpack Compose, Material 3, Room, Coroutines & Flow, and MVVM architecture.

---

## 🌟 Key Features

1. **Intelligent Local Classification**:
   - Automatically organizes messages into **Personal**, **Transactions**, **OTPs**, **Promotions**, and **Other**.
   - Fully deterministic, high-precision local rule engine with zero external API calls.
2. **Dedicated OTP View**:
   - Instant OTP detection from bank and service messages (Google, Uber, Amazon, banks, etc.).
   - Large monospace code display with freshness status (*Active* vs *Expired*) and expiry cues.
   - One-tap "Copy OTP" button with clipboard integration and haptic toast feedback.
   - **Privacy First**: OTP codes are parsed dynamically on-the-fly; extracted OTPs are never logged, stored in plaintext, or transmitted.
3. **Category Tabs & Badges**:
   - Category tabs with real-time unread count badges.
   - Color-coded category tags for every sender and message.
4. **User Category Overrides**:
   - Manually assign any sender or phone number to a custom category (e.g. reclassify a service sender as Personal or Promotions).
   - Category overrides are persisted locally in a Room database and take precedence over default rules.
5. **Fast Local Search**:
   - Real-time search filtering across senders, phone numbers, and message bodies.
6. **Chronological Conversation Screen**:
   - Chat bubbles styled for inbound and outbound messages.
   - Inline OTP card with one-tap copy button.
   - Inline financial transaction details (amount, debit/credit type, account ending, available balance).
   - Fast category change dialog accessible directly from the top app bar.
7. **Read / Unread Management**:
   - Mark individual conversations as read or unread.
   - Automatically marks conversations as read upon opening.
   - Pin important conversations to the top of your inbox.
8. **Material 3 Design & Dark Mode**:
   - Custom SMS Organizer theme (Navy Blue, Teal, and Category Accents).
   - Dynamic colors support on Android 12+ (API 31+) and full Dark Mode support.
9. **Interactive Demo Mode**:
   - Built-in preview mode with realistic simulated SMS threads (banks, food delivery, OTPs, personal chats).
   - Allows exploring all screens and features on emulators or devices without physical SIM cards or SMS permission.

---

## 🔒 Architecture & Privacy

Pinnacle SMS Organizer is engineered with a strict **Privacy-by-Design** philosophy:
- **100% On-Device**: Message content and classifications never leave your device.
- **No External Servers or AI APIs**: Classification runs purely locally using deterministic regex and keyword heuristics.
- **No Sensitive Logging**: Phone numbers, message texts, and OTP codes are strictly omitted from Android Logcat and analytics.
- **Room Persistence**: User-owned customizations (category overrides, local read states, pinned threads) are persisted locally in SQLite via Android Jetpack Room.

### Layered MVVM Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│    (Jetpack Compose, Material 3, ViewModels, UI States)     │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                       Domain Layer                          │
│  (Use Cases, Deterministic SmsClassifier, OtpExtractor,     │
│              TransactionExtractor, Models)                  │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                        Data Layer                           │
│   (SmsRepository, Room Database: CategoryOverrideDao,       │
│    ThreadStateDao, SmsProviderReader, ContactReader)        │
└─────────────────────────────────────────────────────────────┘
```

---

## 📱 SMS access and default handler status

The app asks the user to select Pinnacle as the default SMS app before requesting SMS runtime permissions. It then requests `READ_SMS`, `RECEIVE_SMS`, and `SEND_SMS`; notification permission is independent and can be denied. Contact lookup is optional and currently does not request `READ_CONTACTS`.

### Default SMS App Role Limitations
Android 10+ uses `RoleManager.ROLE_SMS`; older supported versions use the legacy default-SMS chooser. Android and Google Play may restrict SMS permissions to an eligible default handler. A sideloaded app can also be subject to device-specific installation restrictions. The current environment had no usable ADB connection, so the installed phone's exact permission state could not be inspected.
- **When NOT the Default SMS App**:
  - Pinnacle does not query the system SMS provider. The inbox offers demo messages until the user selects Pinnacle as default and grants the required permissions.
  - The app refreshes role state on resume and stops querying real messages if the user switches to another SMS app.
- **Current implementation scope**: SMS inbox reads, incoming SMS provider insertion/notifications, basic text composition, and send-result recording are implemented. MMS delivery, multipart result handling polish, and `RESPOND_VIA_MESSAGE` are placeholders/incomplete. Do not treat the app as a complete SMS replacement until those pieces and real-device behavior are verified.

### Store distribution

Google Play requires a qualifying core use case, default-handler flow before SMS permission requests, an SMS permissions declaration in Play Console, and a privacy policy describing sensitive SMS processing, use, retention, sharing, and deletion. Publish and link that policy before submission; this repository does not currently host a published policy URL. As of August 31, 2026, new Play submissions must target API 36. Review the live Play SMS policy and requirements before submission. F-Droid builds can be produced from source; dependencies are AndroidX, Kotlin, Room, and coroutines from public repositories, with no proprietary service SDKs. Classification remains offline and on-device.

The privacy policy draft is [PRIVACY_POLICY.md](PRIVACY_POLICY.md). Replace its publisher/contact placeholders, verify every statement against the final build, publish it at a stable public URL, and add that URL to the store listing before release. The manifest disables app-data backup.

Release builds use the standard Android release variant. Create a private keystore and put `PINNACLE_STORE_FILE`, `PINNACLE_STORE_PASSWORD`, `PINNACLE_KEY_ALIAS`, and `PINNACLE_KEY_PASSWORD` in `~/.gradle/gradle.properties` or CI secrets; never commit keystores or passwords. The repository ignores common keystore extensions. Signing does not guarantee Play Protect warnings or store approval will disappear.

See [DISTRIBUTION_CHECKLIST.md](DISTRIBUTION_CHECKLIST.md) for current release gates and store-console work.

---

## 🛠️ Tech Stack

- **Language**: Kotlin 1.9.23
- **Minimum SDK**: API 26 (Android 8.0 Oreo)
- **Target / Compile SDK**: API 36 (Android 16)
- **UI Framework**: Jetpack Compose with Material 3 (Compose BOM `2024.04.01`)
- **Architecture**: MVVM with Kotlin Coroutines and StateFlow
- **Database**: Room 2.6.1 with KSP code generation
- **Build System**: Gradle 8.11.1 with Android Gradle Plugin 8.9.1 and Gradle Kotlin DSL
- **Testing**: JUnit 4 with Kotlin Coroutines Test

---

## 🚀 Building & Testing

### Prerequisites
- JDK 17 (e.g. Eclipse Adoptium Temurin 17 or OpenJDK 17)
- Android SDK 36 (`platforms;android-36`, `build-tools;36.0.0`, `platform-tools`)

### Run Unit Tests
To run all unit tests for the classifier, extractors, and repository:
```bash
./gradlew testDebugUnitTest
```

### Build Debug APK
To assemble the debug APK:
```bash
./gradlew assembleDebug
```

For Google Play, configure release signing through the external properties above, then build an Android App Bundle with:
```bash
./gradlew bundleRelease
```
The AAB is written to `app/build/outputs/bundle/release/app-release.aab`. Keep the keystore and properties out of version control, and increment `versionCode` for each upload. An unsigned release artifact is not ready for distribution. `assembleRelease` builds an APK for direct testing; Play submissions should use the signed AAB.
The compiled APK will be output at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Install on Device or Emulator
Connect an Android device with USB debugging enabled, or start an Android emulator, then run:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
To launch the app directly:
```bash
adb shell am start -n com.pinnacle.smsorganizer/.MainActivity
```
