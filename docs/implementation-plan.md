# DentSched — Implementation Plan
**Version:** 1.0  
**Last Updated:** 2026-06-11  
**Developer:** Solo  
**Package Manager:** npm  
**Constitution Version:** 1.2

Each section below is a self-contained **Spic-Kit**. Feed them to OpenCode one at a time in order. Do not start a kit until the previous one is marked ✅ done.

---

## PHASE 1 — Foundation

---

### KIT-01 · Repository & Project Structure

**Goal:** Set up the monorepo structure, gitignore, and folder conventions before any code is written.

**Tasks:**
- [ ] Create root monorepo with `/app` (Android) and `/web` (React) folders
- [ ] Move existing Android project into `/app`
- [ ] Place `.gitignore` at repo root (ignore `yarn.lock`, `pnpm-lock.yaml`, keep `package-lock.json`)
- [ ] Create `/supabase` folder with placeholder `migrations/` and `seed.sql`
- [ ] Create `README.md` at root with project overview and setup instructions
- [ ] Create `.env.example` at root and inside `/web` documenting required variables

**Acceptance Criteria:**
- Repo structure matches: `/app`, `/web`, `/supabase`, `/docs`
- `.gitignore` correctly ignores secrets, build outputs, and OS files
- `.env.example` files exist and list all required keys with no real values

---

### KIT-02 · Supabase Project Setup & Schema

**Goal:** Create the Supabase project and apply the full database schema with all tables, indexes, and triggers.

**Tasks:**
- [ ] Create Supabase project via dashboard
- [ ] Write and apply migration `001_initial_schema.sql` covering all tables:
  - `tenants`, `profiles`, `clinics`, `dentist_clinics`
  - `patients`, `appointments`
  - `clinical_procedures`, `procedure_types`, `clinical_procedure_steps`
  - `procedure_cards`, `procedure_card_steps`, `procedure_payments`
  - `medical_files`
- [ ] Add `created_at` and `updated_at` columns to all tables
- [ ] Add `updated_at` auto-update trigger (function + trigger on every table)
- [ ] Add `deleted_at` soft-delete column to `patients` and `procedure_cards`
- [ ] Add all foreign key constraints and indexes as per constitution schema
- [ ] Write `seed.sql` with the 9 system-wide clinical procedures and their types/steps/fees

**Acceptance Criteria:**
- All tables exist in Supabase with correct columns and constraints
- `updated_at` updates automatically on any row change
- Seed data loads without errors and produces the 9 base procedures

---

### KIT-03 · Supabase Auth & RLS Policies

**Goal:** Enable authentication and lock down every table with Row Level Security so tenants are fully isolated and roles are enforced at the database level.

**Tasks:**
- [ ] Enable Supabase Auth with email/password provider
- [ ] Create `profiles` table trigger: auto-insert a profile row when a new auth user is created
- [ ] Enable RLS on every table
- [ ] Write RLS policies for `tenants`:
  - Users can only read their own tenant
- [ ] Write RLS policies for `profiles`:
  - Users can read profiles within their tenant
  - Only admin can insert/update/delete profiles
- [ ] Write RLS policies for `clinics`, `dentist_clinics`:
  - All roles can read within tenant
  - Only admin can write
- [ ] Write RLS policies for `patients`:
  - Admin + Receptionist: read/write all patients in tenant
  - Dentist: read/write only patients linked to their clinic(s)
  - Receptionist: insert allowed, but cannot update medical fields (`systemic_conditions`, `allergies`, `past_dental_treatments`)
- [ ] Write RLS policies for `appointments`:
  - Admin + Receptionist: full read/write
  - Dentist: read-only, filtered to their own appointments
- [ ] Write RLS policies for `procedure_cards` and `procedure_card_steps`:
  - Admin: full access
  - Dentist: read/write own cards only
  - Receptionist: read-only
- [ ] Write RLS policies for `procedure_payments`:
  - Admin: full access
  - Dentist: read/write payments on own cards
  - Receptionist: read + insert only (no update/delete)
- [ ] Write RLS policies for `medical_files`:
  - Admin + Dentist: full access
  - Receptionist: no access
- [ ] Write RLS policies for `clinical_procedures`, `procedure_types`, `clinical_procedure_steps`:
  - All roles: read (system-wide + own tenant)
  - Admin only: write (for custom procedures)
- [ ] Test each policy with each role using Supabase SQL editor

**Acceptance Criteria:**
- A dentist JWT cannot read another dentist's procedure cards
- A receptionist JWT cannot read medical_files or financial analytics
- A user from tenant A cannot read any row from tenant B
- All policy tests pass in Supabase SQL editor

---

### KIT-04 · Android — Supabase Integration Setup

**Goal:** Add the Supabase Kotlin SDK to the Android app and wire up the client so it's ready to replace Room calls.

**Tasks:**
- [ ] Add Supabase Kotlin SDK dependencies to `app/build.gradle.kts`:
  - `io.github.jan-tennert.supabase:postgrest-kt`
  - `io.github.jan-tennert.supabase:auth-kt`
  - `io.github.jan-tennert.supabase:storage-kt`
  - `io.github.jan-tennert.supabase:realtime-kt`
  - `io.ktor:ktor-client-android`
- [ ] Add `SUPABASE_URL` and `SUPABASE_ANON_KEY` to `local.properties` (gitignored)
- [ ] Create `SupabaseClient.kt` singleton that reads keys from BuildConfig
- [ ] Update `build.gradle.kts` to inject `local.properties` values into `BuildConfig`
- [ ] Create `SessionManager.kt` to hold and expose the current auth session as a `StateFlow`
- [ ] Verify Supabase client initialises and can reach the project (simple health check)

**Acceptance Criteria:**
- App builds without errors with SDK added
- `SupabaseClient` initialises on app start
- `SessionManager` emits `null` session on first launch (no user logged in)

---

### KIT-05 · Android — Authentication Screens

**Goal:** Replace the SharedPreferences-based dentist profile with a real Supabase login flow.

**Tasks:**
- [ ] Create `AuthViewModel.kt` with:
  - `signIn(email, password)` → calls Supabase Auth
  - `signOut()`
  - `currentUser: StateFlow<UserSession?>`
  - `uiState: StateFlow<AuthUiState>` (loading, error, success)
- [ ] Create `LoginScreen.kt` (Compose):
  - Email + password fields
  - Sign in button
  - Error message display
  - Loading state
- [ ] Update `MainActivity.kt` to check session on launch:
  - If session exists → navigate to main app
  - If no session → navigate to LoginScreen
- [ ] Remove old SharedPreferences profile logic from `DentistViewModel`
- [ ] Load `profiles` row after login and store role + tenant_id in `SessionManager`
- [ ] Add sign out action to `SettingsScreen`

**Acceptance Criteria:**
- Cold launch with no session shows LoginScreen
- Successful login navigates to the app and stores role in SessionManager
- Failed login shows an error message
- Sign out clears session and returns to LoginScreen
- No hardcoded credentials anywhere in the codebase

---

### KIT-06 · Android — Replace Room with Supabase (Data Layer)

**Goal:** Swap the Room database for Supabase Kotlin SDK calls across the entire repository layer, keeping all existing UI screens working.

**Tasks:**
- [ ] Create `SupabaseRepository.kt` mirroring the interface of the existing `DentistRepository`
- [ ] Migrate each domain one by one (verify UI still works after each):
  - [ ] Clinics: `getClinics()`, `insertClinic()`, `updateClinic()`, `deleteClinic()`
  - [ ] Patients: `getPatients()`, `getPatientById()`, `insertPatient()`, `updatePatient()`, `deletePatient()`
  - [ ] Clinical Procedures + Types + Steps: all read operations + admin writes
  - [ ] Procedure Cards: full CRUD + `recalculateCard()` logic
  - [ ] Procedure Card Steps: full CRUD
  - [ ] Procedure Payments: full CRUD + recalculate trigger
  - [ ] Medical Files: insert + delete
- [ ] Replace all `Flow<List<T>>` Room flows with Supabase `select()` calls wrapped in `flow { }`
- [ ] Inject `tenant_id` from `SessionManager` into every insert and every query filter
- [ ] Keep `recalculateCard()` logic in the ViewModel (not DB trigger) for now
- [ ] Remove Room dependency from `build.gradle.kts` once all calls are migrated
- [ ] Delete `DentistDatabase.kt` and `DentistRepository.kt`

**Acceptance Criteria:**
- All existing screens (Schedule, Patients, Clinics, Analytics, Procedures, Profile) load data from Supabase
- Insert, update, and delete operations persist to Supabase and reflect in the UI
- No Room imports remain in the codebase
- `tenant_id` is present on every inserted row (verify in Supabase dashboard)

---

## PHASE 2 — Multi-Role & Web

---

### KIT-07 · Android — Role-Based UI Enforcement

**Goal:** Hide or disable UI elements based on the logged-in user's role, using the role stored in SessionManager.

**Tasks:**
- [ ] Add `role` and `tenantId` to `SessionManager` as `StateFlow` values
- [ ] Create a `RoleGuard` composable helper: wraps content and only shows it if role matches
- [ ] Apply role gates across all screens:
  - Schedule: hide booking/edit/cancel buttons for Dentist role
  - Patients: hide medical info edit fields for Receptionist
  - Procedure Cards: hide create card button for Receptionist
  - Payments: show register payment to Admin, Dentist, Receptionist; hide analytics to Receptionist
  - Analytics: entire screen hidden from Dentist and Receptionist
  - Settings → Users panel: only visible to Admin
  - Clinics management: only visible to Admin
  - Procedures Library edit/delete: only visible to Admin
- [ ] Show a "You don't have permission" message for any attempted restricted action

**Acceptance Criteria:**
- Logging in as Dentist: no appointment booking buttons visible
- Logging in as Receptionist: no procedure card creation, no analytics tab
- Logging in as Admin: all features accessible
- Role gates are driven by `SessionManager.role`, not hardcoded strings in composables

---

### KIT-08 · Web — Project Scaffold

**Goal:** Create the React web app scaffold with routing, Supabase client, auth, and i18n wired up.

**Tasks:**
- [ ] Scaffold React + TypeScript app in `/web` using Vite:
  ```
  npm create vite@latest web -- --template react-ts
  ```
- [ ] Install core dependencies:
  - `@supabase/supabase-js`
  - `@tanstack/react-query`
  - `react-router-dom`
  - `i18next` + `react-i18next`
  - `tailwindcss`
- [ ] Create `/web/.env.example` with `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY`
- [ ] Create `src/lib/supabase.ts` — Supabase client singleton
- [ ] Create `src/lib/i18n.ts` — i18next setup with AR and EN namespaces
- [ ] Add Arabic and English translation JSON files under `src/locales/ar/` and `src/locales/en/`
- [ ] Implement RTL: set `document.dir` to `rtl` when language is Arabic
- [ ] Create `src/context/AuthContext.tsx` — session + role + tenantId exposed via context
- [ ] Create `LoginPage.tsx` — email/password form using Supabase Auth
- [ ] Set up `react-router-dom` with protected routes (redirect to login if no session)
- [ ] Create shell layout with sidebar navigation (links vary by role)
- [ ] Set up folder structure:
  ```
  src/
    features/
      schedule/
      patients/
      procedures/
      finance/
      analytics/
      settings/
    components/
    hooks/
    lib/
    locales/
  ```

**Acceptance Criteria:**
- `npm run dev` in `/web` starts the app without errors
- Login works via Supabase Auth and stores session
- Language toggle switches between AR (RTL) and EN (LTR)
- Protected routes redirect unauthenticated users to login
- Sidebar links respect the logged-in user's role

---

### KIT-09 · Web — Patients Module

**Goal:** Build the patients list and patient detail pages on the web app.

**Tasks:**
- [ ] Create `usePatients` hook — fetches patients filtered by tenant + role
- [ ] Build `PatientsPage.tsx`:
  - Searchable, filterable list (by clinic, status, dentist)
  - New patient button (Admin + Dentist + Receptionist)
  - Click row → navigate to patient detail
- [ ] Build `PatientDetailPage.tsx`:
  - Demographics section (editable by Admin + Dentist; read-only for Receptionist)
  - Medical history section (editable by Admin + Dentist only)
  - Tabs: Procedure Cards | Appointments | Payments | Medical Files
- [ ] Build `NewPatientModal.tsx` — create patient form
- [ ] Wire all mutations through React Query with optimistic updates
- [ ] Add AR/EN translations for all labels

**Acceptance Criteria:**
- Patient list loads and filters correctly
- Creating a patient from web appears in the Android app (and vice versa)
- Receptionist cannot edit medical info fields (fields are disabled or hidden)
- All text is translated in both languages

---

### KIT-10 · Web — Schedule Module

**Goal:** Build the appointment calendar on the web app.

**Tasks:**
- [ ] Install a calendar library (recommended: `@fullcalendar/react`)
- [ ] Create `useAppointments` hook — fetches appointments filtered by tenant + role
- [ ] Build `SchedulePage.tsx`:
  - Day / Week / Month view toggle
  - Appointments color-coded by dentist
  - Admin + Receptionist: click slot → open booking modal
  - Dentist: read-only view of own appointments
- [ ] Build `AppointmentModal.tsx`:
  - Fields: patient (search/select), dentist (select), clinic (select), date, time, duration, notes
  - Status selector: scheduled / completed / canceled / no_show
- [ ] Add AR/EN translations

**Acceptance Criteria:**
- Calendar renders appointments from Supabase
- Admin and Receptionist can create/edit/cancel appointments
- Dentist view shows only their appointments with no edit controls
- Appointment created on web appears on Android schedule screen

---

## PHASE 3 — New Features

---

### KIT-11 · Web — Procedure Cards Module

**Goal:** Build procedure card creation and management on the web app.

**Tasks:**
- [ ] Create `useProcedureCards` hook
- [ ] Build `ProcedureCardsPage.tsx` — list view with filters
- [ ] Build `ProcedureCardDetail.tsx`:
  - Header: procedure, type, material, tooth number, status, dentist, clinic, date
  - Steps list with completion checkboxes + timestamps
  - Add custom step button
  - Financial section: treatment fee, lab fees, payments log, associate cut, clinic share
- [ ] Build `NewProcedureCardModal.tsx`:
  - Select procedure → sub-type → steps (pre-filled from template, customisable)
  - Financial defaults auto-filled from procedure template
- [ ] Receptionist: read-only view (no create, no edit)
- [ ] Add AR/EN translations

**Acceptance Criteria:**
- Procedure cards created on web appear on Android
- Step completion syncs across devices in real time (or on refresh)
- Financial calculations match Android logic exactly

---

### KIT-12 · Web & Android — File Uploads (Supabase Storage)

**Goal:** Allow dentists to upload X-rays and images attached to a patient or procedure card step.

**Tasks:**
- [ ] Create Supabase Storage bucket `medical-files` with RLS (tenant-isolated)
- [ ] Web: build file upload component (drag-and-drop + file picker)
- [ ] Web: display uploaded files in patient's Medical Files tab
- [ ] Android: replace local `photoUris` string with Supabase Storage URLs
- [ ] Android: add image upload to procedure card step (camera + gallery picker)
- [ ] Android: display remote images in step detail view
- [ ] Apply file size limit (max 10MB per file)
- [ ] Store `file_url` in `medical_files` and `photo_urls[]` in `procedure_card_steps`

**Acceptance Criteria:**
- File uploaded on Android is visible on web (same patient)
- File uploaded on web is visible on Android
- Files are scoped to tenant — no cross-tenant access
- Files over 10MB are rejected with a clear error message

---

### KIT-13 · Web — Finance Module

**Goal:** Build per-dentist and per-clinic financial summaries on the web app.

**Tasks:**
- [ ] Create `useFinance` hook — aggregates payments, associate cuts, clinic shares
- [ ] Build `FinancePage.tsx` (Admin + Dentist):
  - Dentist view: own earnings, pending balance, lab fees, payment history
  - Admin view: all dentists summary table + per-clinic breakdown
- [ ] Build `PaymentsPanel.tsx` (Admin + Dentist + Receptionist):
  - List payments for a procedure card
  - Register new payment (amount, date, notes)
  - Delete payment (Admin only)
- [ ] Auto-recalculate associate cut and clinic share on payment insert/delete (Supabase DB function or edge function)
- [ ] Add AR/EN translations

**Acceptance Criteria:**
- Receptionist can register a payment but cannot see the analytics summary
- Dentist sees only their own financial data
- Admin sees all dentists and clinics
- Calculations match: associate_cut and clinic_share update correctly after every payment

---

### KIT-14 · Web — Business Analytics Dashboard (Admin Only)

**Goal:** Build the analytics dashboard with charts and KPIs for the practice admin.

**Tasks:**
- [ ] Install charting library (`recharts` recommended)
- [ ] Create `useAnalytics` hook — queries aggregated data from Supabase
- [ ] Build `AnalyticsPage.tsx` with:
  - Revenue by clinic (bar chart, monthly/quarterly/yearly toggle)
  - Revenue per dentist (bar chart)
  - Most performed procedures (horizontal bar or pie chart)
  - Patient growth over time (line chart)
  - Outstanding balances (table)
  - Appointment completion rate vs cancellations/no-shows (donut chart)
- [ ] Create Supabase views or RPC functions for aggregation queries
- [ ] Gate entire page to Admin role only (redirect others away)
- [ ] Add AR/EN translations and RTL chart layout support

**Acceptance Criteria:**
- All charts load real data from Supabase
- Date range toggle (monthly / quarterly / yearly) updates all charts
- Non-admin users cannot access the page (tested with Dentist and Receptionist JWT)
- Charts render correctly in both LTR and RTL layout

---

### KIT-15 · Android — Appointments Module Upgrade

**Goal:** Replace the current appointment fields on the Patient table with the new dedicated `appointments` table, and add a proper calendar view to Android.

**Tasks:**
- [ ] Update Android data model to use `appointments` table instead of patient fields
- [ ] Update `ScheduleScreen.kt` to fetch from `appointments` via Supabase
- [ ] Add calendar/agenda view to ScheduleScreen (day + week)
- [ ] Admin + Receptionist: add appointment booking UI on Android
- [ ] Dentist: read-only appointment view
- [ ] Migrate any existing appointment data from patient fields to `appointments` table
- [ ] Remove `next_appointment_date`, `next_appointment_time`, `next_appointment_notes` fields from patient model once migrated

**Acceptance Criteria:**
- Android schedule screen shows appointments from Supabase `appointments` table
- Appointments created on web appear on Android calendar
- Dentist cannot tap to edit/cancel on Android
- Old patient appointment fields are fully removed

---

## PHASE 4 — Polish & Scale

---

### KIT-16 · Realtime Sync

**Goal:** Subscribe to Supabase Realtime so schedule and patient changes reflect instantly across all devices without a manual refresh.

**Tasks:**
- [ ] Enable Realtime on Supabase for: `appointments`, `patients`, `procedure_cards`, `procedure_card_steps`
- [ ] Android: subscribe to `appointments` channel in `ScheduleViewModel` — update StateFlow on INSERT/UPDATE/DELETE
- [ ] Android: subscribe to `procedure_card_steps` — update steps list live during a procedure
- [ ] Web: use Supabase Realtime in `useAppointments` and `useProcedureCards` hooks
- [ ] Handle subscription cleanup on logout and screen exit

**Acceptance Criteria:**
- Receptionist books appointment on web → appears on Android within 2 seconds (no refresh)
- Dentist marks a step complete on Android → web updates without page reload
- No duplicate events or memory leaks on subscription cleanup

---

### KIT-17 · Android — Push Notifications (Appointment Reminders)

**Goal:** Send push notifications to dentists for upcoming appointments.

**Tasks:**
- [ ] Set up Firebase Cloud Messaging (FCM) in the Android app
- [ ] Store FCM token in `profiles` table on login
- [ ] Create a Supabase Edge Function that runs on a schedule (pg_cron):
  - Queries appointments for the next 24 hours
  - Sends FCM push to the relevant dentist's token
- [ ] Handle notification tap → navigate to appointment detail
- [ ] Request notification permission on Android 13+

**Acceptance Criteria:**
- Dentist receives a push notification ~24 hours before an appointment
- Tapping the notification opens the correct appointment
- Notification permission is requested gracefully on first launch

---

### KIT-18 · Tenant Onboarding Flow

**Goal:** Build a self-serve signup flow so a new dental practice can create their account, set up their practice, and invite their team.

**Tasks:**
- [ ] Web: build `SignupPage.tsx` — practice name, admin email, password
- [ ] On signup: create `tenants` row + `profiles` row with role = admin
- [ ] Web: build `OnboardingWizard.tsx` (shown after first login):
  - Step 1: Add clinic(s) name, address, phone
  - Step 2: Invite team members (email + role selection)
  - Step 3: Confirm procedure library (use defaults or customise)
- [ ] Build `UsersPage.tsx` (Admin only): manage team members, deactivate accounts
- [ ] Invite flow: Supabase Auth sends invite email → user sets password → profile is pre-seeded with role + tenant

**Acceptance Criteria:**
- A brand new practice can sign up, add a clinic, and invite a dentist without any manual DB intervention
- Invited user logs in and sees only their tenant's data
- Admin can deactivate a user (sets `is_active = false`, session is invalidated)

---

## Kit Extraction Guide for OpenCode / Spic-Kit

When feeding a kit to OpenCode, include:
1. The kit block above (goal + tasks + acceptance criteria)
2. The relevant sections from `CONSTITUTION.md` (schema, roles, conventions)
3. Any output files from the previous kit that this kit depends on

**Kit dependency order:**
```
KIT-01 → KIT-02 → KIT-03 → KIT-04 → KIT-05 → KIT-06
                                                  ↓
                                              KIT-07
                                              KIT-08 → KIT-09 → KIT-10 → KIT-11
                                                                            ↓
                                                       KIT-12 → KIT-13 → KIT-14
                                                                   ↓
                                                               KIT-15
                                                                   ↓
                                                    KIT-16 → KIT-17 → KIT-18
```

**Current status:**
- [ ] KIT-01 · Repository & Project Structure
- [ ] KIT-02 · Supabase Schema
- [ ] KIT-03 · Auth & RLS Policies
- [ ] KIT-04 · Android Supabase Integration
- [ ] KIT-05 · Android Auth Screens
- [ ] KIT-06 · Android Data Layer Migration
- [ ] KIT-07 · Android Role-Based UI
- [ ] KIT-08 · Web Scaffold
- [ ] KIT-09 · Web Patients Module
- [ ] KIT-10 · Web Schedule Module
- [ ] KIT-11 · Web Procedure Cards Module
- [ ] KIT-12 · File Uploads
- [ ] KIT-13 · Web Finance Module
- [ ] KIT-14 · Analytics Dashboard
- [ ] KIT-15 · Android Appointments Upgrade
- [ ] KIT-16 · Realtime Sync
- [ ] KIT-17 · Push Notifications
- [ ] KIT-18 · Tenant Onboarding
