# Cyan Budget

Cyan Budget is an offline-first Android personal finance app built with Kotlin, Jetpack Compose, and Material 3. It focuses on fast transaction capture, calm financial insight, privacy, accessibility, and one-handed use.

## Included in this build

- Five-tab navigation: Home, Income, Expenses, Reports, and Settings
- Dashboard with privacy mode, balance, income, expenses, daily/weekly spending, monthly budget, savings, recent activity, and category chart
- Create, edit, duplicate, delete, undo-delete, search, filter, and sort transactions
- Amount/category/date/payment method/notes/tags/receipt/recurrence fields with validation
- Natural-language voice entry with a confirmation step and separate speech-to-text for descriptions and notes
- Local SQLite persistence behind a repository boundary and DataStore preferences
- Monthly and custom/category budgets with threshold-aware progress indicators
- Savings goals and contributions
- Today/week/month/year/all-time reports with bar/donut comparisons and CSV/PDF export
- Resizable Android home-screen widget with balance, spending progress, quick add, voice entry, and privacy mode
- Biometric or device-credential app lock
- WorkManager-powered widget refresh and a notification-ready reminder worker
- Light, dark, and system themes; currency selection; compact/privacy/notification settings
- Guided onboarding and debug-only sample data
- Unit tests plus a Compose UI onboarding test

Cloud synchronization is intentionally not enabled: the default build has no account system and no financial data leaves the device. The repository boundary is the integration seam for an opt-in encrypted cloud sync implementation.

## Build

Requirements:

- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35 and Build Tools 35.0.0

From this directory:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

To run instrumentation tests with an emulator or device connected:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

The checked-in `local.properties` is machine-specific and should be removed before committing. Android Studio will recreate it with the correct SDK path.

## Privacy and security

- Android Auto Backup is disabled for financial data.
- Data is stored locally and never sent by the app.
- Speech is handled by the device's installed recognition service; Cyan Budget stores only the confirmed transaction, never an audio recording.
- Biometric lock supports strong biometrics or the device credential.
- Privacy mode masks monetary values in the app and widget.
- Receipt access uses Android's document picker instead of broad storage permission.

For a regulated or high-risk production deployment, add database-at-rest encryption (for example SQLCipher with a Keystore-wrapped key), a formal threat model, dependency scanning, and an external accessibility/security audit before release.

## Project map

```text
app/src/main/java/com/cyanbudget/app/
├── data/       SQLite database, repository, DataStore settings
├── domain/     voice parser and validation
├── model/      entities, categories, currency and analytics
├── ui/         Compose screens and reusable components
├── widget/     Android AppWidget provider
└── work/       periodic refresh and notification worker
```

See [architecture](docs/ARCHITECTURE.md) and [user flows](docs/USER_FLOWS.md) for the detailed design.
