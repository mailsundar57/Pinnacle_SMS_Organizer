# Contributing to Pinnacle SMS Organizer

Thank you for your interest in contributing to Pinnacle SMS Organizer! We welcome community contributions to make this open-source SMS organizer even better.

## Development Guidelines

1. **Architecture & Principles**:
   - The app strictly adheres to **Clean Architecture** and **MVVM**.
   - Keep Presentation (`ui`), Domain (`domain`), Data (`data`), and Platform (`provider`, `receiver`) separated.
   - **Privacy First**: Under no circumstances should SMS content, phone numbers, or OTP codes be logged to logcat, sent to external networks, or transmitted off-device.

2. **Code Style**:
   - Follow official Kotlin coding conventions (`kotlin.code.style=official`).
   - Use Jetpack Compose and Material 3 design patterns.
   - All deterministic classifier rules must include corresponding unit tests.

3. **Submitting Changes**:
   1. Fork the repository and create your branch from `main`:
      ```bash
      git checkout -b feature/your-feature-name
      ```
   2. Make your changes and ensure all unit tests pass:
      ```bash
      ./gradlew testDebugUnitTest
      ```
   3. Ensure the project builds cleanly:
      ```bash
      ./gradlew assembleDebug
      ```
   4. Commit your changes with descriptive commit messages following the Conventional Commits specification (e.g. `feat: add UPI intent classifier rule`, `fix: balance parsing for EUR`).
   5. Open a Pull Request against `main`.
