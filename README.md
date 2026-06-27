# School Management App — Setup Guide

A full offline-first school management system: Node/Express/Postgres backend +
Android (Kotlin/Compose/Room) app with local-first sync.

## What's in this zip

```
school-mgmt/
├── backend/     Node.js + Express + Prisma + PostgreSQL API
└── android/     Kotlin + Jetpack Compose + Room + Retrofit + Hilt app
```

`node_modules/` and `.env` were deliberately excluded (regenerable / secret-ish —
see Step 1 below). Android `build/`, `.gradle/`, and `.idea/` were excluded too,
since Android Studio regenerates these automatically.

---

## Part 1 — Backend Setup

### 1. Install dependencies
```bash
cd backend
npm install
```

### 2. Set up the database

**Option A — Local Postgres**: install Postgres, create a database named
`school_mgmt`.

**Option B — Free hosted Postgres (recommended)**: create a free database at
[neon.tech](https://neon.tech) — it gives you a `DATABASE_URL` connection
string directly, no local Postgres install needed. See the "Database" question
earlier in this build's history for why Neon specifically was recommended
(generous free tier, auto-suspend, fast resume).

### 3. Configure environment variables
```bash
cp .env.example .env
```
Edit `.env` and set `DATABASE_URL` to your real connection string (local or
Neon). Also change `JWT_SECRET` to a long random string — the placeholder
value is not safe to use even for local testing long-term.

### 4. Run database migrations and seed data
```bash
npx prisma generate
npx prisma migrate dev --name init
node prisma/seed.js
```
The seed script creates the class list (Play, Nursery, LKG, UKG, Class 1–5),
the academic year, book variants per class, and 3 uniform categories ×
8 sizes (20–34). It's safe to re-run.

### 5. Start the server
```bash
npm run dev
```
Server runs at `http://localhost:4000`. Check `http://localhost:4000/health`
in a browser to confirm it's running.

### 6. Create your first admin account
The database starts with no users. Use any HTTP client (curl, Postman) to
call the one-time bootstrap route:
```bash
curl -X POST http://localhost:4000/api/auth/bootstrap-admin \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "yourpassword"}'
```
This only works once — it refuses if an admin already exists. Save the
returned token, or just log in normally afterward via `/api/auth/login`.

### 7. (Optional) Set up recurring/staff/expense reference data
The Staff, Expense Category, and Recurring Expense endpoints all start
empty — add your school's actual categories (Rent, Office Supplies, etc.)
and staff members through the app once you're logged in, or via direct API
calls if you want to pre-populate before first use.

---

## Part 2 — Android Setup

### 1. Open in Android Studio
Open the `android/` folder as a project in Android Studio (use a recent
stable version — this project uses Kotlin 1.9.24, Compose BOM 2024.06.00,
and KSP). Let Gradle sync.

**If Gradle sync complains about the wrapper**: this zip didn't include
`gradle/wrapper/gradle-wrapper.jar` (binary file, regenerated automatically).
If Android Studio doesn't offer to regenerate it, run:
```bash
cd android
gradle wrapper --gradle-version 8.7
```
(requires a system-wide Gradle install just for this one command), or simply
let Android Studio's "Sync Project with Gradle Files" handle it — it usually
downloads what's missing automatically.

### 2. Point the app at your backend
Edit `android/app/build.gradle.kts`, find `BASE_URL` under `buildTypes { debug { ... } }`:

- **Android Emulator** talking to a backend running on your own machine:
  keep `http://10.0.2.2:4000/` (this is a special alias the emulator uses
  to reach your computer's localhost — not a real IP).
- **Physical phone** on the same WiFi as your computer: replace it with
  your computer's actual LAN IP, e.g. `http://192.168.1.50:4000/`.
- **Deployed backend** (Neon + a real host like Render/Railway): use that
  public URL, and switch to `https://` once you have one — and remove
  `android:usesCleartextTraffic="true"` from `AndroidManifest.xml` once
  you're no longer using plain `http://`.

### 3. Build and run
Run the app on an emulator or physical device from Android Studio (the green
Run button, or Shift+F10). First build will take a while — Room, Hilt, and
Moshi all generate code via KSP.

### 4. Expect some first-build friction
This is a large, from-scratch project (21 Room entities, 22 DAOs, 13
repositories, 20+ Compose screens) built without ever running a real Android
compiler — every file was carefully hand-verified for syntax and
cross-references, but Gradle/Kotlin's compiler will catch things a text-based
review can't. If you hit errors on first build:
- Read the actual error message/file/line Android Studio gives you — it's
  almost always pinpoint-accurate for Kotlin compile errors.
- Common categories to expect: a version mismatch between a library and its
  KSP processor, a missing import, or a small type mismatch.
- Paste the exact error back if you want help resolving it — much faster to
  fix with the real compiler output in hand than to guess blindly.

### 5. Log in
Use the admin username/password you created via `bootstrap-admin` in Part 1,
Step 6.

---

## Project feature map (for your own reference)

| Feature | Backend | Android |
|---|---|---|
| Students + withdrawal tracking | `studentController.js` | `StudentRepository`, `StudentDetailScreen` |
| Teachers | `teacherController.js` | `TeacherRepository`, `TeacherListScreen` |
| Classes/Sections | `academicController.js` | `AcademicRepository` |
| Fee structures + bulk-assign | `feeController.js` | `FeeStructureRepository`, `FeeStructuresScreen` |
| Payments + partial/dues | `paymentController.js` | `FeeRepository`, `RecordPaymentDialog` |
| Books/uniforms + inventory | `itemController.js` | `InventoryRepository`, `PurchaseRepository` |
| Staff + salaries | `staffController.js`, `salaryController.js` | `StaffRepository`, `StaffDetailScreen` |
| Expenses (one-off + recurring) | `expenseController.js` | `ExpenseRepository`, `ExpensesScreen` |
| Monthly financial report | `reportController.js` | `ReportRepository`, `MonthlyReportScreen` |
| Offline sync | `syncService.js`, `syncRegistry.js` | `SyncRepository`, `SyncWorker`, `sync/` package |

## A note on the "Net for the month" figure
This is a simple cash-basis calculation (money collected minus expenses minus
salaries, within the calendar month). It's deliberately not labeled "profit"
anywhere in the app — it isn't an accounting or tax-grade figure, just a
quick at-a-glance number for the school office.
