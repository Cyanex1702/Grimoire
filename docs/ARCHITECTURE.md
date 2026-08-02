# Architecture

## Runtime structure

The app uses a pragmatic MVVM architecture:

1. Compose screens emit user intents and render immutable `FinanceUiState`.
2. `MainViewModel` coordinates writes, settings changes, and lifecycle-safe state.
3. `FinanceRepository` exposes `StateFlow` streams and performs database work on `Dispatchers.IO`.
4. `FinanceDatabase` owns schema, indices, constraints, mapping, and transactions.
5. `SettingsRepository` persists preferences with DataStore.

Domain parsing and validation are plain Kotlin so they can be tested without Android. `FinanceInsights` is the shared analytics layer used by the dashboard, reports, and widget.

## Database schema

### `transactions`

| Column | Type | Notes |
|---|---|---|
| `id` | TEXT | UUID primary key |
| `type` | TEXT | `INCOME` or `EXPENSE` |
| `amount_cents` | INTEGER | Positive, minor currency units |
| `category` | TEXT | Built-in or custom label |
| `description` | TEXT | User-visible summary |
| `occurred_at` | INTEGER | Epoch milliseconds |
| `notes` | TEXT | Optional notes |
| `tags` | TEXT | Pipe-delimited values |
| `payment_method` | TEXT | Card, cash, transfer, wallet, other |
| `recurrence` | TEXT | One-time through yearly |
| `receipt_uri` | TEXT | Document-provider reference |
| `created_at` | INTEGER | Audit timestamp |
| `modified_at` | INTEGER | Audit timestamp |

Indices cover descending history and type/category filtering. A check constraint rejects non-positive amounts.

### `budgets`

Stores ID, name, limit in minor units, optional category, period, and alert preference. Overall monthly budget lives in settings so onboarding can configure it before a custom budget exists.

### `savings_goals`

Stores ID, name, target and saved values in minor units, optional target date, and presentation color.

## Error and state handling

- Financial inputs are validated before persistence and again by database constraints.
- Ambiguous voice commands always open the review form and highlight required missing details.
- Speech cancellation/unavailability produces a snackbar instead of losing navigation state.
- Deletes require confirmation and expose an immediate undo action.
- Empty searches, transaction lists, budgets, goals, and reports have explicit states.
- Repository I/O does not block Compose rendering.

## Extension seams

- Replace `FinanceDatabase` internals with Room without changing the repository or screens.
- Add opt-in sync behind `FinanceRepository`, using versioned records and `modifiedAt` conflict resolution.
- Add custom categories as a dedicated table while retaining category string snapshots on transactions for historical stability.
- Add SQLCipher at the database boundary with a Keystore-wrapped passphrase.
- Replace `RemoteViews` with Glance when Glance is part of the dependency baseline.
