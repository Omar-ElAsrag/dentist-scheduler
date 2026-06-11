# Tasks: Supabase Foundation & Data Layer Migration

**Input**: Design documents from `/specs/002-supabase-foundation/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, quickstart.md

**Tests**: Not requested in the feature specification. Test tasks are omitted.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Supabase migrations**: `supabase/migrations/`
- **Supabase seed**: `supabase/seed.sql`
- **Android data layer**: `app/src/main/java/com/example/data/`
- **Android UI**: `app/src/main/java/com/example/ui/`
- **Android build**: `app/build.gradle.kts`

---

## Phase 1: Setup (Project Initialization)

**Purpose**: Create the Supabase project and scaffold migration files.

- [ ] T001 Create Supabase project via Supabase dashboard and note the project URL and anon key
- [X] T002 [P] Create `supabase/migrations/001_initial_schema.sql` as an empty migration file
- [X] T003 [P] Create `supabase/migrations/002_auth_and_rls.sql` as an empty migration file
- [X] T004 Update `.env.example` at repo root to include `SUPABASE_URL` and `SUPABASE_ANON_KEY` placeholders
- [X] T005 Add `SUPABASE_URL` and `SUPABASE_ANON_KEY` to `app/local.properties` with real dev values (gitignored)

**Checkpoint**: Supabase project exists. Migration files scaffoldaged. `.env.example` documents required keys.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create shared SQL helper functions and the `updated_at` trigger that all tables depend on.

**CRITICAL**: No user story work can begin until this phase is complete.

- [X] T006 Write the `set_updated_at()` trigger function in `supabase/migrations/001_initial_schema.sql` — a PL/pgSQL function that sets `NEW.updated_at = now()`
- [X] T007 [P] Write helper function `auth.user_tenant_id()` in `supabase/migrations/002_auth_and_rls.sql` — returns the `tenant_id` from the `profiles` row for the current `auth.uid()`
- [X] T008 [P] Write helper function `auth.user_role()` in `supabase/migrations/002_auth_and_rls.sql` — returns the `role` from the `profiles` row for the current `auth.uid()`

**Checkpoint**: Helper functions and trigger function exist. Ready to create tables.

---

## Phase 3: User Story 1 - Complete Database Schema (Priority: P1)

**Goal**: All 13 tables exist with correct columns, constraints, indexes, and auto-updating timestamps. Seed data loads 9 clinical procedures.

**Independent Test**: Run `001_initial_schema.sql` in Supabase SQL editor, verify 13 tables exist, update a row and confirm `updated_at` changes, run `seed.sql` and confirm 9 procedures.

### Implementation for User Story 1

- [X] T009 [US1] Create `tenants` table in `supabase/migrations/001_initial_schema.sql` with columns: id (UUID PK), name (TEXT NOT NULL), plan (TEXT DEFAULT 'trial'), is_active (BOOLEAN DEFAULT true), created_at, updated_at
- [X] T010 [US1] Create `profiles` table in `supabase/migrations/001_initial_schema.sql` with columns: id (UUID PK REFERENCES auth.users), tenant_id (UUID REFERENCES tenants), full_name, role (TEXT CHECK), avatar_url, is_active, created_at, updated_at
- [X] T011 [US1] Create `clinics` table in `supabase/migrations/001_initial_schema.sql` with columns: id (SERIAL PK), tenant_id, name, default_percentage, deduct_lab_fees, address, phone, created_at, updated_at
- [X] T012 [US1] Create `dentist_clinics` junction table in `supabase/migrations/001_initial_schema.sql` with columns: dentist_id, clinic_id, percentage_override, composite PK
- [X] T013 [US1] Create `patients` table in `supabase/migrations/001_initial_schema.sql` with all columns per data-model.md including `deleted_at` for soft delete
- [X] T014 [US1] Create `appointments` table in `supabase/migrations/001_initial_schema.sql` with all columns per data-model.md
- [X] T015 [US1] Create `clinical_procedures` table in `supabase/migrations/001_initial_schema.sql` with all columns per data-model.md
- [X] T016 [US1] Create `procedure_types` table in `supabase/migrations/001_initial_schema.sql` with all columns per data-model.md
- [X] T017 [US1] Create `clinical_procedure_steps` table in `supabase/migrations/001_initial_schema.sql` with all columns per data-model.md
- [X] T018 [US1] Create `procedure_cards` table in `supabase/migrations/001_initial_schema.sql` with all columns per data-model.md including `deleted_at` for soft delete
- [X] T019 [US1] Create `procedure_card_steps` table in `supabase/migrations/001_initial_schema.sql` with all columns per data-model.md
- [X] T020 [US1] Create `procedure_payments` table in `supabase/migrations/001_initial_schema.sql` with all columns per data-model.md
- [X] T021 [US1] Create `medical_files` table in `supabase/migrations/001_initial_schema.sql` with all columns per data-model.md
- [X] T022 [US1] Attach `set_updated_at` trigger to all 13 tables in `supabase/migrations/001_initial_schema.sql`
- [X] T023 [US1] Add indexes on `tenant_id` for all tenant-scoped tables in `supabase/migrations/001_initial_schema.sql`
- [X] T024 [US1] Add indexes on foreign key columns (patient_id, dentist_id, clinic_id, procedure_card_id) in `supabase/migrations/001_initial_schema.sql`
- [X] T025 [US1] Write `supabase/seed.sql` with 9 system-wide clinical procedures using `INSERT ... ON CONFLICT DO NOTHING` for idempotency (see research.md R6)
- [ ] T026 [US1] Apply `001_initial_schema.sql` to Supabase project and verify all 13 tables exist
- [ ] T027 [US1] Run `seed.sql` and verify 9 procedures with types, steps, and fees are created

**Checkpoint**: 13 tables with triggers and indexes. Seed data loaded. `updated_at` auto-updates on row modification.

---

## Phase 4: User Story 2 - Authentication & Tenant Isolation (Priority: P2)

**Goal**: Email/password auth works, profiles auto-created on login, RLS enabled on every table with tenant isolation and role-based access policies.

**Independent Test**: Create two tenants. Authenticate as tenant A's dentist. Query patients — only tenant A's data returned. Attempt tenant B's data — zero rows.

### Implementation for User Story 2

- [ ] T028 [US2] Enable Supabase Auth with email/password provider in Supabase dashboard
- [X] T029 [US2] Write `on_auth_user_created` trigger in `supabase/migrations/002_auth_and_rls.sql` that auto-inserts a `profiles` row with `role = 'dentist'` (least privilege default, see research.md R3)
- [X] T030 [US2] Enable RLS on `tenants` table — users can only SELECT their own tenant row
- [X] T031 [US2] Enable RLS on `profiles` table — users can SELECT profiles within their tenant; only admin can INSERT/UPDATE/DELETE
- [X] T032 [US2] Enable RLS on `clinics` and `dentist_clinics` tables — all roles can SELECT within tenant; only admin can write
- [X] T033 [US2] Enable RLS on `patients` table — admin + receptionist: read/write all in tenant; dentist: read/write only patients linked to their clinic(s) via `dentist_clinics`; receptionist: INSERT allowed but UPDATE restricted on medical fields
- [X] T034 [US2] Enable RLS on `appointments` table — admin + receptionist: full read/write; dentist: read-only filtered to their own appointments
- [X] T035 [US2] Enable RLS on `procedure_cards` and `procedure_card_steps` tables — admin: full access; dentist: read/write own cards only; receptionist: read-only
- [X] T036 [US2] Enable RLS on `procedure_payments` table — admin: full access; dentist: read/write on own cards; receptionist: read + INSERT only (no UPDATE/DELETE)
- [X] T037 [US2] Enable RLS on `medical_files` table — admin + dentist: full access; receptionist: no access (deny all)
- [X] T038 [US2] Enable RLS on `clinical_procedures`, `procedure_types`, `clinical_procedure_steps` tables — all roles: SELECT (system-wide where tenant_id IS NULL + own tenant); admin only: write
- [ ] T039 [US2] Test cross-tenant isolation: create two tenants, authenticate as tenant A, verify zero rows returned for tenant B data
- [ ] T040 [US2] Test role isolation: authenticate as receptionist, verify `medical_files` returns zero rows; authenticate as dentist, verify only own procedure cards returned

**Checkpoint**: RLS on all 13 tables. Cross-tenant isolation verified. Role-based access verified for all 3 roles.

---

## Phase 5: User Story 3 - Android App Connects to Supabase (Priority: P3)

**Goal**: Supabase Kotlin SDK integrated, client initialises on launch, session manager works, login screen authenticates users.

**Independent Test**: Build and run the app. Login screen appears. Enter valid credentials → navigates to main app. Enter invalid → error shown. Sign out → returns to login.

### Implementation for User Story 3

- [X] T041 [US3] Add Supabase Kotlin SDK dependencies to `app/build.gradle.kts`: `postgrest-kt`, `auth-kt`, `storage-kt`, `realtime-kt`, and `io.ktor:ktor-client-android`
- [X] T042 [US3] Update `app/build.gradle.kts` to inject `SUPABASE_URL` and `SUPABASE_ANON_KEY` from `local.properties` into `BuildConfig`
- [X] T043 [US3] Create `app/src/main/java/com/example/data/SupabaseClient.kt` — singleton that reads `SUPABASE_URL` and `SUPABASE_ANON_KEY` from BuildConfig and initialises the Supabase client with Postgrest, Auth, Storage, and Realtime modules
- [X] T044 [US3] Create `app/src/main/java/com/example/data/SessionManager.kt` — holds current auth session as `StateFlow<UserSession?>`, exposes `role` and `tenantId` as `StateFlow` values, provides `signIn()`, `signOut()`, and session restoration
- [ ] T045 [US3] Verify app builds without errors with SDK added and SupabaseClient initialises on launch (check Logcat)
- [X] T046 [US3] Create `app/src/main/java/com/example/ui/auth/AuthViewModel.kt` with `signIn(email, password)`, `signOut()`, `currentUser: StateFlow<UserSession?>`, `uiState: StateFlow<AuthUiState>` (loading, error, success)
- [X] T047 [US3] Create `app/src/main/java/com/example/ui/auth/LoginScreen.kt` (Compose) with email + password fields, sign in button, error message display, loading state
- [X] T048 [US3] Update `app/src/main/java/com/example/MainActivity.kt` to check session on launch: if session exists → navigate to main app; if no session → navigate to LoginScreen
- [X] T049 [US3] Load `profiles` row after login in `AuthViewModel.kt` and store `role` + `tenant_id` in `SessionManager`
- [X] T050 [US3] Add sign out action to `app/src/main/java/com/example/ui/SettingsScreen.kt` that calls `SessionManager.signOut()` and navigates to LoginScreen
- [X] T051 [US3] Remove old SharedPreferences profile logic from `app/src/main/java/com/example/ui/DentistViewModel.kt`

**Checkpoint**: App builds. Login screen appears on cold launch. Valid login navigates to main app with role loaded. Invalid login shows error. Sign out returns to login.

---

## Phase 6: User Story 4 - Existing Screens Work with Supabase Data (Priority: P4)

**Goal**: All existing Android screens load data from Supabase. Room is fully removed. Every insert includes `tenant_id`.

**Independent Test**: Open each screen, verify data loads, create/update/delete records, confirm they persist in Supabase dashboard with `tenant_id` populated. Zero Room imports in codebase.

### Implementation for User Story 4

- [X] T052 [US4] Create `app/src/main/java/com/example/data/SupabaseRepository.kt` mirroring the interface of the existing `DentistRepository` — methods for all 7 domains (Clinics, Patients, Procedures, Cards, Steps, Payments, Medical Files)
- [X] T053 [US4] Implement Clinics CRUD in `SupabaseRepository.kt`: `getClinics()`, `insertClinic()`, `updateClinic()`, `deleteClinic()` — all filtered by `tenant_id` from `SessionManager`
- [X] T054 [US4] Implement Patients CRUD in `SupabaseRepository.kt`: `getPatients()`, `getPatientById()`, `insertPatient()`, `updatePatient()`, `deletePatient()` (soft delete via `deleted_at`) — filtered by `tenant_id`
- [X] T055 [US4] Implement Clinical Procedures + Types + Steps read operations in `SupabaseRepository.kt` — read system-wide (tenant_id IS NULL) + own tenant; admin writes for custom procedures
- [X] T056 [US4] Implement Procedure Cards CRUD in `SupabaseRepository.kt`: full CRUD + `recalculateCard()` logic (see research.md R5 — calculation in ViewModel) — soft delete via `deleted_at`
- [X] T057 [US4] Implement Procedure Card Steps CRUD in `SupabaseRepository.kt`: full CRUD with completion timestamps
- [X] T058 [US4] Implement Procedure Payments CRUD in `SupabaseRepository.kt`: full CRUD + recalculate associate cut and clinic share on insert/delete (formula from data-model.md)
- [X] T059 [US4] Implement Medical Files insert + delete in `SupabaseRepository.kt`
- [X] T060 [US4] Replace all `Flow<List<T>>` Room flows with Supabase `select()` calls wrapped in `flow { }` across all ViewModels
- [X] T061 [US4] Inject `tenant_id` from `SessionManager` into every insert and every query filter in `SupabaseRepository.kt`
- [X] T062 [US4] Update `app/src/main/java/com/example/ui/DentistViewModel.kt` to use `SupabaseRepository` instead of `DentistRepository`
- [ ] T063 [US4] Verify all existing screens (Schedule, Patients, Clinics, Analytics, Procedures, Profile) load data from Supabase and persist changes
- [X] T064 [US4] Remove Room dependency from `app/build.gradle.kts`
- [X] T065 [US4] Delete `app/src/main/java/com/example/data/DentistDatabase.kt`
- [X] T066 [US4] Delete `app/src/main/java/com/example/data/DentistRepository.kt` (replaced by SupabaseRepository)
- [X] T067 [US4] Verify zero `import androidx.room` references remain in the codebase
- [ ] T068 [US4] Verify `tenant_id` is populated on every inserted row by checking Supabase dashboard

**Checkpoint**: All screens functional with Supabase. Room fully removed. `tenant_id` on every insert.

---

## Phase 7: User Story 5 - Role-Based Data Access on Android (Priority: P5)

**Goal**: Android app respects each user's role — dentists see only their data, receptionists cannot access clinical records, admins have full visibility.

**Independent Test**: Log in as each role. Dentist: only own patients/cards. Receptionist: no medical files, no analytics. Admin: all data visible.

### Implementation for User Story 5

- [ ] T069 [US5] Add role-aware query filtering in `SupabaseRepository.kt`: dentist queries for patients filtered by `dentist_clinics` junction (only patients in assigned clinics)
- [ ] T070 [US5] Add role-aware query filtering in `SupabaseRepository.kt`: dentist queries for procedure_cards and procedure_payments filtered by `dentist_id = current user`
- [ ] T071 [US5] Ensure receptionist role cannot access medical files UI — hide or disable the medical files entry point when `SessionManager.role == 'receptionist'`
- [ ] T072 [US5] Ensure receptionist role cannot edit medical history fields (`systemic_conditions`, `allergies`, `past_dental_treatments`) — disable fields in patient detail screen when role is receptionist
- [ ] T073 [US5] Ensure admin role has full visibility — no additional filtering beyond tenant scope for admin queries
- [ ] T074 [US5] Verify: log in as dentist → patient list shows only patients from assigned clinics
- [ ] T075 [US5] Verify: log in as receptionist → medical files feature unavailable, medical history fields read-only
- [ ] T076 [US5] Verify: log in as admin → all data within tenant visible on all screens

**Checkpoint**: Role-based access verified for all 3 roles on Android.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, cleanup, and error handling across all user stories.

- [ ] T077 Run quickstart scenario 1: verify all 13 tables exist with correct structure
- [ ] T078 Run quickstart scenario 2: verify `updated_at` trigger fires on row update
- [ ] T079 Run quickstart scenario 3: verify seed data produces 9 clinical procedures
- [ ] T080 Run quickstart scenario 4: verify cross-tenant isolation with two test tenants
- [ ] T081 Run quickstart scenario 5: verify role-based access for admin, dentist, receptionist
- [ ] T082 Run quickstart scenario 6: verify Android app launches with Supabase SDK initialised
- [ ] T083 Run quickstart scenario 7: verify full auth flow (login → session persist → sign out)
- [ ] T084 Run quickstart scenario 8: verify all screens load from Supabase with `tenant_id`
- [ ] T085 Run quickstart scenario 9: verify soft delete for patients/cards, hard delete for steps
- [ ] T086 Add network error handling: display clear error state when Supabase is unreachable (no crash)
- [ ] T087 Add graceful handling when user's `profiles` row is missing after auth (show error directing to admin)
- [ ] T088 Final codebase audit: zero Room imports, zero hardcoded credentials, service role key absent from client code

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **US1 Schema (Phase 3)**: Depends on Foundational — creates all tables
- **US2 Auth & RLS (Phase 4)**: Depends on US1 (tables must exist before RLS policies)
- **US3 SDK Integration (Phase 5)**: Depends on US2 (auth must be configured before SDK can authenticate)
- **US4 Data Migration (Phase 6)**: Depends on US3 (SDK must be wired before repository can query)
- **US5 Role-Based Access (Phase 7)**: Depends on US4 (repository must exist before role filtering)
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational (Phase 2)
- **US2 (P2)**: Depends on US1 (tables must exist)
- **US3 (P3)**: Depends on US2 (auth must be configured)
- **US4 (P4)**: Depends on US3 (SDK must be integrated)
- **US5 (P5)**: Depends on US4 (repository must exist)

### Within Each User Story

- SQL tables before triggers and indexes
- RLS policies after tables exist
- SDK dependencies before client initialisation
- Repository before ViewModel updates
- ViewModel updates before UI verification

### Parallel Opportunities

- Phase 1: T002 and T003 can run in parallel (different migration files)
- Phase 2: T007 and T008 can run in parallel (different helper functions)
- Phase 3: T009–T021 can be written in parallel (different CREATE TABLE statements in same file)
- Phase 5: T046 and T047 can run in parallel (AuthViewModel and LoginScreen are different files)

---

## Parallel Example: Phase 3 Schema

```bash
# All table creation tasks can be written in parallel:
Task: "T009 [US1] Create tenants table"
Task: "T010 [US1] Create profiles table"
Task: "T011 [US1] Create clinics table"
# ... through T021
```

---

## Implementation Strategy

### MVP First (US1 + US2 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: US1 (Database Schema)
4. Complete Phase 4: US2 (Auth & RLS)
5. **STOP and VALIDATE**: Run quickstart scenarios 1–5
6. Backend is ready — Android integration can follow

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add US1 (Schema) → Test independently → 13 tables verified
3. Add US2 (Auth & RLS) → Test independently → Isolation verified
4. Add US3 (SDK) → Test independently → App connects to Supabase
5. Add US4 (Migration) → Test independently → All screens work
6. Add US5 (Role Access) → Test independently → Roles enforced on Android
7. Each story adds value without breaking previous stories

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each phase or logical group
- Stop at any checkpoint to validate story independently
- The `001_initial_schema.sql` and `002_auth_and_rls.sql` migrations should be applied to Supabase in order
- Financial calculations (`recalculateCard`) stay in ViewModel during Phase 1 (see research.md R5)
- Seed data uses `ON CONFLICT DO NOTHING` for idempotency (see research.md R6)
