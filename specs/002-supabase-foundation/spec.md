# Feature Specification: Supabase Foundation & Data Layer Migration

**Feature Branch**: `002-supabase-foundation`

**Created**: 2026-06-11

**Status**: Draft

**Input**: User description: "Implement the Supabase backend foundation for DentSched: database schema (13 tables), authentication with Row Level Security policies, Android SDK integration, login screens, and migration from Room to Supabase — covering KIT-02 through KIT-06 of the implementation plan."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Complete Database Schema (Priority: P1)

As a system administrator setting up the DentSched platform, I want the full database schema applied to Supabase so that all 13 tables exist with correct columns, constraints, indexes, and auto-updating timestamps — forming the foundation for every feature that follows.

**Why this priority**: Without the schema, nothing else can be built. Every subsequent kit (auth, RLS, SDK, data migration) depends on the tables existing.

**Independent Test**: Connect to the Supabase project and verify all 13 tables exist with correct columns, foreign keys, and that updating any row automatically updates its `updated_at` timestamp.

**Acceptance Scenarios**:

1. **Given** a fresh Supabase project, **When** the migration `001_initial_schema.sql` is applied, **Then** all 13 tables (`tenants`, `profiles`, `clinics`, `dentist_clinics`, `patients`, `appointments`, `clinical_procedures`, `procedure_types`, `clinical_procedure_steps`, `procedure_cards`, `procedure_card_steps`, `procedure_payments`, `medical_files`) exist with correct columns and constraints.
2. **Given** the schema is applied, **When** a row is updated in any table, **Then** the `updated_at` column is automatically set to the current timestamp.
3. **Given** the schema is applied, **When** `seed.sql` is executed, **Then** 9 system-wide clinical procedures with their types, steps, and default fees are inserted without errors.
4. **Given** the schema is applied, **When** a patient or procedure card row is soft-deleted, **Then** the `deleted_at` column is populated and the row remains in the table.

---

### User Story 2 - Authentication & Tenant Isolation (Priority: P2)

As a dental practice administrator, I want to log in with my email and password and be certain that no user from another practice can see any of my practice's data — patients, appointments, financials, or clinical records.

**Why this priority**: Multi-tenant isolation is the single most critical security requirement. A cross-tenant data leak would be catastrophic for a medical SaaS platform. RLS is the server-enforced guarantee.

**Independent Test**: Create two tenants with different users. Using a dentist JWT from tenant A, attempt to read rows belonging to tenant B — all queries must return zero rows.

**Acceptance Scenarios**:

1. **Given** a user authenticates with valid email and password, **When** the auth flow completes, **Then** a `profiles` row is automatically created (or retrieved) linked to the user's tenant and role.
2. **Given** a dentist from tenant A is authenticated, **When** they query `patients`, **Then** only patients belonging to tenant A and linked to their clinic(s) are returned.
3. **Given** a user from tenant A is authenticated, **When** they attempt to read any row from tenant B, **Then** the query returns zero rows — RLS blocks the access.
4. **Given** a receptionist is authenticated, **When** they attempt to read `medical_files`, **Then** the query returns zero rows — receptionists have no access to medical files.
5. **Given** an admin is authenticated, **When** they create, update, or delete a profile, **Then** the operation succeeds. When a non-admin attempts the same, it is blocked by RLS.

---

### User Story 3 - Android App Connects to Supabase (Priority: P3)

As a developer working on the Android app, I want the Supabase Kotlin SDK integrated so the app can communicate with the backend — authenticating users and querying data — replacing the local Room database entirely.

**Why this priority**: The SDK integration is the bridge between the Android client and the Supabase backend. Without it, the app cannot authenticate or read/write any data.

**Independent Test**: Launch the Android app, verify the Supabase client initialises without errors, and confirm the session manager correctly reports no active session on first launch.

**Acceptance Scenarios**:

1. **Given** the Android app is built with Supabase SDK dependencies added, **When** the app launches, **Then** the Supabase client initialises successfully and can reach the project.
2. **Given** the app launches for the first time, **When** the session manager is queried, **Then** it emits a null session (no user logged in).
3. **Given** a user enters valid credentials on the login screen, **When** they tap sign in, **Then** the app authenticates via Supabase Auth, loads their profile (role + tenant_id), and navigates to the main app.
4. **Given** a user enters invalid credentials, **When** they tap sign in, **Then** an error message is displayed without crashing.
5. **Given** a user is logged in, **When** they tap sign out, **Then** the session is cleared and the app returns to the login screen.

---

### User Story 4 - Existing Screens Work with Supabase Data (Priority: P4)

As a dentist using the Android app, I want all my existing screens (Schedule, Patients, Clinics, Procedures, Analytics, Profile) to load data from Supabase and persist changes back — without any visible difference in behaviour from the previous Room-based version.

**Why this priority**: This is the final migration step. Users should not notice any change in functionality — only the data source changes from local to remote. Every screen must work identically.

**Independent Test**: Open each existing screen, verify data loads from Supabase, perform a create/update/delete operation, and confirm it persists in the Supabase dashboard with `tenant_id` populated.

**Acceptance Scenarios**:

1. **Given** a dentist is logged in, **When** they open the Patients screen, **Then** the patient list loads from Supabase filtered by their tenant and clinic assignments.
2. **Given** a dentist creates a new procedure card, **When** they save it, **Then** the card appears in the Supabase `procedure_cards` table with `tenant_id` set to the dentist's tenant.
3. **Given** a dentist logs a payment against a procedure card, **When** the payment is saved, **Then** the associate cut and clinic share are recalculated and the values match the expected formula.
4. **Given** all screens are working with Supabase, **When** the codebase is searched for Room imports, **Then** zero Room imports remain — the local database is fully removed.
5. **Given** a dentist inserts any record, **When** the Supabase dashboard is checked, **Then** the `tenant_id` column on that row is populated (never null).

---

### User Story 5 - Role-Based Data Access on Android (Priority: P5)

As a practice with multiple roles (admin, dentist, receptionist), I want the Android app to respect each user's role so that dentists see only their own data, receptionists cannot access clinical records, and admins have full visibility.

**Why this priority**: Role enforcement is already handled at the RLS layer (US2), but the Android app must also reflect these constraints in its UI and queries to provide a coherent user experience.

**Independent Test**: Log in as each role and verify that the data returned matches the role's access level — dentist sees only their patients, receptionist cannot see medical files, admin sees everything.

**Acceptance Scenarios**:

1. **Given** a dentist is logged in, **When** they view the patient list, **Then** only patients linked to their assigned clinic(s) are shown.
2. **Given** a receptionist is logged in, **When** they attempt to access medical files, **Then** the feature is unavailable (no data, no UI entry point).
3. **Given** an admin is logged in, **When** they view any screen, **Then** all data within their tenant is visible.
4. **Given** a receptionist creates a new patient, **When** the patient is saved, **Then** basic info is stored but medical fields (systemic_conditions, allergies, past_dental_treatments) cannot be edited by the receptionist.

---

### Edge Cases

- What happens when the Supabase project is unreachable (network outage)? The app should display a clear error state and not crash. Retry logic should be in place.
- What happens when a user's profile row is missing after authentication? The app should handle this gracefully — either create a default profile or show an error directing them to contact their admin.
- What happens when the `seed.sql` is run twice? It should be idempotent — running it again should not create duplicate procedures or fail with constraint violations.
- What happens when a Room-to-Supabase migration encounters a data type mismatch (e.g., Room stored dates as strings, Supabase expects DATE)? The migration must handle type conversion correctly.
- What happens when two users from the same tenant edit the same patient record simultaneously? Supabase handles this via last-write-wins at the row level. No conflict resolution UI is needed in Phase 1.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a complete database schema with 13 tables covering tenants, profiles, clinics, dentist-clinic assignments, patients, appointments, clinical procedures (with types and steps), procedure cards (with steps), procedure payments, and medical files.
- **FR-002**: Every table MUST include `created_at` and `updated_at` timestamp columns, with `updated_at` automatically managed by a database trigger on every row modification.
- **FR-003**: The `patients` and `procedure_cards` tables MUST include a `deleted_at` column for soft deletion. Hard delete is permitted for `procedure_card_steps` and `procedure_payments`.
- **FR-004**: The system MUST support email/password authentication, with automatic profile creation on first login.
- **FR-005**: Row Level Security MUST be enabled on every table, with policies that enforce tenant isolation (no cross-tenant access) and role-based access (admin, dentist, receptionist).
- **FR-006**: A dentist MUST only see patients linked to their assigned clinic(s), and MUST only see their own procedure cards and payments.
- **FR-007**: A receptionist MUST be able to create patients (basic info only) and register payments, but MUST NOT access medical files, edit medical history fields, or view financial analytics.
- **FR-008**: An admin MUST have full read/write access to all data within their tenant, including user management.
- **FR-009**: The Android app MUST integrate the Supabase Kotlin SDK for authentication and data access, replacing the Room local database entirely.
- **FR-010**: The Android app MUST provide a login screen with email/password fields, error handling, and loading states.
- **FR-011**: The Android app MUST check for an existing session on launch and navigate to the login screen if no session exists.
- **FR-012**: Every data insert from the Android app MUST include the authenticated user's `tenant_id`. Every query MUST filter by `tenant_id`.
- **FR-013**: The system MUST seed 9 system-wide clinical procedures (with types, steps, and default fees) on initial setup.
- **FR-014**: Associate cut and clinic share MUST be recalculated on every payment using the formula: if `deduct_lab_fees` then `associate_cut = (paid - lab_fees) * percentage`, else `associate_cut = paid * percentage`.

### Key Entities

- **Tenant**: Represents a dental practice. All data is scoped to a tenant. Attributes: name, plan, is_active.
- **Profile**: Extends the auth user. Links a user to a tenant and assigns a role (admin/dentist/receptionist). Attributes: full_name, role, avatar_url, is_active.
- **Clinic**: A physical location within a tenant. Attributes: name, default_percentage, deduct_lab_fees, address, phone.
- **Dentist-Clinic**: Many-to-many junction linking dentists to clinics, with optional percentage override.
- **Patient**: A person receiving dental care. Attributes: demographics, medical history, allergies, assigned dentist/clinic, soft delete.
- **Appointment**: A scheduled visit. Attributes: patient, dentist, clinic, date, time, duration, status.
- **Clinical Procedure**: A template for a type of dental treatment. Attributes: name, has_types, display_order, default_fee.
- **Procedure Type**: A sub-type of a clinical procedure. Attributes: name, materials, default_fee.
- **Clinical Procedure Step**: A predefined step within a procedure type. Attributes: step_name, display_order.
- **Procedure Card**: An instance of a treatment for a specific patient. Attributes: procedure, type, material, tooth, status, financial fields, soft delete.
- **Procedure Card Step**: A step within a specific procedure card. Attributes: step_name, is_completed, completed_at, photo_urls.
- **Procedure Payment**: A payment logged against a procedure card. Attributes: amount, payment_at, notes.
- **Medical File**: An attachment (note, X-ray, image, document) linked to a patient. Attributes: title, content, file_url, file_type.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All 13 tables exist with correct columns, constraints, and auto-updating timestamps after applying the migration.
- **SC-002**: A user from tenant A cannot read a single row from tenant B — verified by attempting cross-tenant queries with each role.
- **SC-003**: A dentist cannot read another dentist's procedure cards — verified by querying with a dentist JWT from a different clinic.
- **SC-004**: A receptionist cannot access medical files or financial analytics — verified by querying with a receptionist JWT.
- **SC-005**: The Android app authenticates a user and loads their profile (role + tenant) within 5 seconds of tapping sign in.
- **SC-006**: All existing Android screens (Schedule, Patients, Clinics, Analytics, Procedures, Profile) load data from Supabase with no visible regression in functionality.
- **SC-007**: Every row inserted by the Android app has `tenant_id` populated — zero null `tenant_id` values in any table.
- **SC-008**: Zero Room imports remain in the Android codebase after migration.
- **SC-009**: The seed data produces exactly 9 clinical procedures with their associated types, steps, and fees.

## Assumptions

- A Supabase project has been created (or will be created as the first task) and is accessible via URL and anon key.
- The existing Android app uses Room for local storage and SharedPreferences for the dentist profile — both will be fully replaced.
- The Supabase Kotlin SDK by jan-tennert is the chosen client library for Android (postgrest-kt, auth-kt, storage-kt, realtime-kt).
- Email/password is the only authentication method in Phase 1. Social login and magic links are out of scope.
- The 9 system-wide clinical procedures and their types/steps/fees are defined in the product constitution and will be seeded verbatim.
- Network connectivity is always available (no offline mode in Phase 1).
- The existing Android UI screens will not be redesigned in this feature — only the data source changes.
- Financial calculations (associate cut, clinic share) are performed in the Android ViewModel, not as database triggers, during Phase 1.
