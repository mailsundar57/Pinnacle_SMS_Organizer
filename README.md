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

Pinnacle targets modern Android (**Android 13 / API 33 through Android 16 / API 36**) and supports both modes:
- **Default SMS App (`RoleManager.ROLE_SMS`) — recommended primary flow**: Full SMS inbox reading, incoming SMS handling (`SMS_DELIVER`), composing/sending SMS, and syncing read status with Android's Telephony provider.
- **Read-Only SMS Mode (`READ_SMS` + `RECEIVE_SMS`)**: Organizes and reads existing/incoming SMS without replacing your default messaging app, using local Room tracking for read/unread state, pins, and category overrides, with a banner to optionally upgrade to Default SMS App.

Open **More → SMS handler diagnostics** to inspect `RoleManager.ROLE_SMS` status, runtime permission grants, installer provenance, and resolution of all 4 required default-SMS manifest components. The report omits message bodies, senders, and message identifiers.

### Android 15 / 16 Restricted Settings (Local APK Testing vs. Google Play)
- **Local / sideloaded APK testing**: Android 15 and 16 enforce Enhanced Confirmation Mode (ECM) on sideloaded APKs, blocking both `ROLE_SMS` and `READ_SMS` until manually unlocked. Open **App info → Permissions → SMS** (tap it once if greyed out to trigger Android's Restricted setting notice), return to **App info → ⋮ → Allow restricted settings**, authenticate, and retry. Installing via `adb install` or Google Play does not trigger sideload ECM restrictions.
- **Google Play distribution**: Google Play's SMS/Call Log policy requires apps declaring `READ_SMS`/`RECEIVE_SMS`/`SEND_SMS` to request the Default SMS Handler role (`ROLE_SMS`) as their primary flow, submit the Play Console Permissions Declaration Form, host a public privacy policy URL, and target API 36. Release builds use the standard `bundleRelease` AAB signed with your private keystore configured via `PINNACLE_STORE_FILE`, `PINNACLE_STORE_PASSWORD`, `PINNACLE_KEY_ALIAS`, and `PINNACLE_KEY_PASSWORD` in `~/.gradle/gradle.properties` or CI secrets.
- **Current implementation scope**: SMS inbox reads, incoming SMS provider insertion/notifications (`SMS_DELIVER`), live read-only inbox refresh (`SMS_RECEIVED`), basic text composition, and send-result recording are implemented. The app is SMS-only and does not process or display MMS (the inert `WAP_PUSH_DELIVER` receiver and MMS URI routes remain in the manifest for `ROLE_SMS` eligibility).

---

## 🛠️ Tech Stack

- **Language**: Kotlin 1.9.23
- **Minimum SDK**: API 33 (Android 13 Tiramisu)
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
