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

## 📱 Android Restrictions & Default SMS Role

### Android SMS Access Permissions
- **`READ_SMS`**: Allows reading SMS messages from the system content provider (`content://sms` or `Telephony.Sms`).
- **`RECEIVE_SMS`**: Listens for incoming SMS messages to keep the local database synchronized.
- **`READ_CONTACTS`**: Resolves phone numbers into friendly contact names (optional).

### Default SMS App Role Limitations
Starting with Android 4.4 (API 19) and Android 10+ (`RoleManager.ROLE_SMS`):
- **When NOT the Default SMS App**:
  - The app operates in **Read-Only** mode with respect to the Android system SMS provider.
  - Attempting to update `Telephony.Sms.READ` column on the system provider is restricted by the operating system.
  - **Pinnacle's Solution**: The app maintains a **local read/unread override state** in its local Room database. Users can freely mark conversations as read or unread inside Pinnacle SMS Organizer regardless of whether it is the system default SMS app.
  - The app displays an informative, non-intrusive banner explaining the read-only mode and providing a one-tap button to open Android's default SMS app selector.
- **When Set as the Default SMS App**:
  - Full write support is unlocked: read status updates are synced directly to Android's `Telephony.Sms` provider.
  - The app provides all required manifest components:
    - `SmsReceiver`: handles `SMS_DELIVER_ACTION` (`android.provider.Telephony.SMS_DELIVER`).
    - `MmsReceiver`: handles `WAP_PUSH_DELIVER_ACTION` (`android.provider.Telephony.WAP_PUSH_DELIVER`).
    - `ComposeSmsActivity`: handles `ACTION_SEND` and `ACTION_SENDTO` intents (`sms:`, `smsto:`).
    - `HeadlessSmsSendService`: handles `RESPOND_VIA_MESSAGE` service calls.

---

## 🛠️ Tech Stack

- **Language**: Kotlin 1.9.23
- **Minimum SDK**: API 26 (Android 8.0 Oreo)
- **Target / Compile SDK**: API 34 (Android 14)
- **UI Framework**: Jetpack Compose with Material 3 (Compose BOM `2024.04.01`)
- **Architecture**: MVVM with Kotlin Coroutines and StateFlow
- **Database**: Room 2.6.1 with KSP code generation
- **Build System**: Gradle 8.5 with Gradle Kotlin DSL (`.gradle.kts`)
- **Testing**: JUnit 4 with Kotlin Coroutines Test

---

## 🚀 Building & Testing

### Prerequisites
- JDK 17 (e.g. Eclipse Adoptium Temurin 17 or OpenJDK 17)
- Android SDK 34 (`platforms;android-34`, `build-tools;34.0.0`, `platform-tools`)

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
