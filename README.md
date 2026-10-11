# Budget Buddy — Android

A native Android companion to the **Budget Buddy** web app. Sign in with your existing
account, view your monthly budget and recent transactions, and log a new expense — all
synced live with the same backend (and database) as the web app.

Built over a weekend as a focused, production-shaped **vertical slice** — not a full clone
of the web app. See **Scope & limitations** below.

- **Web app:** https://budget-buddy-frontend-pi.vercel.app
- **Backend API:** https://backend-production-6e92.up.railway.app

---

## Features

- **Email/password sign-in** against the existing backend (JWT).
- **Home dashboard:** monthly income, per-category budgets with progress bars (red when
  over budget), and recent transactions.
- **Add an expense:** writes to the backend; the category's spent total and the
  transaction list update immediately.
- **Robust UI states:** loading, empty results, incorrect credentials, expired session
  (returns to sign-in), and network failure (with a Retry action).
- **Sign out:** clears the in-memory session.

---

## Tech stack & architecture

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Networking:** Retrofit 2 + Gson converter; OkHttp with two interceptors
  (auth header + request logging)
- **Async:** Kotlin coroutines (`suspend` functions, `viewModelScope`)
- **State:** Android `ViewModel` + Compose state
- **minSdk:** 24 · **Gradle:** Kotlin DSL

**Shape — unidirectional and layered:**

```
Compose screens (MainActivity.kt)
      → ViewModels (LoginViewModel, HomeViewModel)
            → Repositories (AuthRepository, HomeRepository)
                  → ApiService (Retrofit)
                        → backend (Node/Express + Postgres on Railway)
```

**Files:**
- `Network.kt` — request/response models, `ApiService` endpoints, Retrofit client + interceptors
- `Auth.kt` — `SessionManager`, `AuthRepository`, `LoginViewModel`
- `Home.kt` — `HomeData`/`HomeUiState`, `HomeRepository`, `HomeViewModel`, money/date helpers
- `MainActivity.kt` — all Compose screens (Login, Home, Add-expense dialog)

---

## Authentication & security

- On login the backend returns a **JWT**; it's held in `SessionManager` **in memory only**.
- An **OkHttp interceptor** attaches `Authorization: Bearer <token>` to every protected
  request automatically — the screens never handle tokens directly.
- **Error semantics:** `401` on `/auth/login` = wrong credentials; `403` on a protected
  route = missing/expired token → the app routes back to sign-in.
- **Security choices:**
  - Password field is masked.
  - HTTP logging is set to `BASIC`, so credentials and tokens never appear in Logcat.
  - HTTPS only.
  - No secrets in source control.
  - Session is in-memory, so a full app restart requires signing in again — a deliberate
    v1 trade-off (see Next steps for secure persistence).

---

## Configuration

The backend base URL lives in `RetrofitClient.BASE_URL` in `Network.kt`. It's
**configuration, not a secret** — point it at a different environment as needed.

```
https://backend-production-6e92.up.railway.app/
```

---

## Running it

How the pieces differ:

| Thing | What it means |
|---|---|
| **Hosting the backend** | The API + database run on Railway (always on). |
| **Running in the emulator** | A virtual phone on your Mac, via Android Studio. |
| **Installing on a device** | Putting the app on a real Android phone. |
| **Distributing an APK** | Sharing an installable file directly. |
| **Publishing to Play Store** | Google distributes it (from an Android App Bundle). |

**To run:**
1. Open the project in Android Studio.
2. Let Gradle sync.
3. Choose an emulator (or a connected device) and press **Run**.

---

## Testing

**Unit tests** (JVM, in `app/src/test`):
- `MoneyTest` — money parsing/formatting, incl. null and bad input (4 tests)
- `LoginValidationTest` — blank-field validation + error-clears-on-typing (2 tests)
- `HomeRepositoryTest` — repository maps a **fake** `ApiService`'s responses into
  `HomeData`, no network (1 test, uses `kotlinx-coroutines-test`)

Run: right-click `com.budgetbuddy.android (test)` → **Run**, or `./gradlew test`.
**All 7 pass locally.**

**Manual checklist** (performed by hand on the emulator):
- [x] Successful sign-in
- [x] Incorrect credentials → error message
- [x] Empty fields → validation message
- [x] Home loads real income / budgets / transactions
- [x] Empty transactions → empty state
- [x] Add expense → appears, budget updates, and shows on the web app (same DB)
- [x] Sign out → returns to login
- [x] Screen rotation preserves typed input (ViewModel)
- [x] Network failure (airplane mode) → error + Retry
- [ ] Expired session → returns to sign-in (logic verified in code; hard to force live)

---

## Build & distribution

**Debug APK (for a personal demo):**
1. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. When it finishes, click **locate** in the notification.
3. File: `app/build/outputs/apk/debug/app-debug.apk`
4. Install by dragging the file onto a running emulator, or copy it to a phone (enable
   "install unknown apps" for your file manager/browser).

**Signed release APK (for sharing more widely):**
1. **Build → Generate Signed Bundle / APK → APK**
2. Create a new **keystore** (`.jks`), set a key alias + passwords, choose **release**.
3. Output: `app/build/outputs/apk/release/app-release.apk`
4. **Keystore handling:** keep the `.jks` file and its passwords private and **out of Git**.
   If you ever publish to Play, losing the keystore means you can't ship updates.

**APK vs AAB:** an **APK** installs directly on a device. An **Android App Bundle (.aab)**
is what you upload to Google Play; Google generates optimized per-device APKs from it.
For direct sharing/demo, use an APK. Play publication is optional and not required here.

---

## Scope & limitations (and next steps)

This mobile app is a **deliberately focused slice**, not full parity with the web app.

**Not in v1 (by design):** editing/adding/deleting categories, budget scaling, themes,
weekly budgets, charts/history, extra income, registration, Google sign-in.

**Next steps, roughly in priority order:**
1. **Edit a category's budget** — the backend endpoint (`PUT /api/categories/:id`) already exists.
2. **Persist the session securely** (encrypted storage) so you stay signed in across restarts.
3. **Pull-to-refresh** on the home screen.
4. Category **add/delete**; theme selection.
5. Charts / monthly history parity with the web app.

---

## How this was built

Built over a weekend as a learning project, with substantial help from an AI assistant
(Claude) for guidance, code, and debugging. The architecture, API integration, and tests
reflect real decisions made and verified during the build; the scope was kept intentionally
small in order to ship something reliable and well understood.
