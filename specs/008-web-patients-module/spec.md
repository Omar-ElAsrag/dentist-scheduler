# Feature Specification: Web Patients Module

**Feature Branch**: `kit-09-web-patients`

**Created**: 2026-06-17

**Status**: Draft

**Input**: KIT-09 from `docs/implementation-plan.md` — "Build the patients list and patient detail pages on the web app."

## User Scenarios & Testing *(mandatory)*

### User Story 1 — View and Search the Patient List (Priority: P1)

A user logs in to the web app and navigates to the Patients section. They see a list of all patients belonging to their tenant, with each row showing the patient's name, clinic, assigned dentist, and status. They can search by patient name and filter the list by clinic, status (active/inactive), and assigned dentist.

**Why this priority**: The patient list is the entry point for all patient-related workflows. Without it, users cannot find or access any patient record.

**Independent Test**: Log in as any role, navigate to Patients, verify the list loads with tenant-scoped patients, type a search query and confirm results filter, apply clinic/dentist/status filters and verify results narrow correctly.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they navigate to the Patients page, **Then** the patient list loads displaying all patients in their tenant with name, clinic, assigned dentist, and status columns.
2. **Given** the patient list is displayed, **When** the user types a name in the search field, **Then** the list filters to show only patients whose names match the search text.
3. **Given** the patient list is displayed, **When** the user selects a filter (clinic, status, or dentist), **Then** the list updates to show only matching patients.
4. **Given** a receptionist who can see all patients in the tenant, **When** they view the patient list, **Then** they see all patients across all clinics and dentists (not limited to a single clinic).
5. **Given** a dentist, **When** they view the patient list, **Then** they see only patients linked to their assigned clinic(s).
6. **Given** no patients exist in the tenant, **When** the user navigates to the Patients page, **Then** an empty state message is displayed in the current language (AR/EN).

---

### User Story 2 — View Patient Detail (Priority: P2)

A user clicks on a patient row in the patient list and navigates to the patient detail page. The page displays the patient's full profile organized into sections: demographics (name, phone, date of birth, gender, clinic, assigned dentist), medical history (systemic conditions, allergies, past dental treatments, general notes), and a tabbed area with Procedure Cards, Appointments, Payments, and Medical Files sub-views. The user can switch between these tabs to see related records for this patient.

**Why this priority**: Patient detail is the core view for understanding a patient's clinical and administrative history. Without it, users have no context for the patient beyond the list row.

**Independent Test**: Click any patient row, verify the detail page loads with demographics, medical history, and tabbed sections. Switch between tabs and verify content loads for each.

**Acceptance Scenarios**:

1. **Given** a user viewing the patient list, **When** they click a patient row, **Then** they navigate to the patient detail page showing the full patient profile.
2. **Given** the patient detail page, **When** the page loads, **Then** the demographics section displays: name, phone, date of birth, gender, clinic name, and assigned dentist name.
3. **Given** the patient detail page, **When** the page loads, **Then** the medical history section displays: systemic conditions, allergies, past dental treatments, and general notes.
4. **Given** the patient detail page, **When** the user clicks the "Procedure Cards" tab, **Then** a list of the patient's procedure cards is displayed.
5. **Given** the patient detail page, **When** the user clicks the "Appointments" tab, **Then** the patient's appointment history is displayed.
6. **Given** the patient detail page, **When** the user clicks the "Payments" tab, **Then** a list of payments across all procedure cards for this patient is displayed.
7. **Given** the patient detail page, **When** the user clicks the "Medical Files" tab, **Then** uploaded files (notes, X-rays, images, documents) are displayed.
8. **Given** a user navigates to a patient ID that does not exist or belongs to another tenant, **When** the page tries to load, **Then** a "Patient not found" message is displayed.

---

### User Story 3 — Create a New Patient (Priority: P3)

An admin, dentist, or receptionist needs to add a new patient to the system. From the patient list page, they click a "New Patient" button which opens a form. They fill in the patient's basic information — name, phone, date of birth, gender, clinic, and assigned dentist — and submit the form. The new patient appears in the patient list immediately.

**Why this priority**: Adding patients is a frequent receptionist task and a prerequisite for scheduling appointments and creating procedure cards.

**Independent Test**: Click "New Patient", fill in required fields, submit. Verify the patient appears in the list. Log in on Android and verify the new patient is visible there too.

**Acceptance Scenarios**:

1. **Given** an admin, dentist, or receptionist on the patient list page, **When** they click the "New Patient" button, **Then** a patient creation form opens (as a modal or dedicated page).
2. **Given** the new patient form is open, **When** the user fills in all required fields (name, phone, clinic) and submits, **Then** the patient is created and appears in the patient list.
3. **Given** the new patient form is open, **When** the user submits with missing required fields, **Then** validation errors are shown and the patient is not created.
4. **Given** a newly created patient on the web app, **When** the Android app refreshes its patient list, **Then** the new patient is visible on Android.
5. **Given** the new patient form, **When** a receptionist opens it, **Then** only basic fields are available (name, phone, date of birth, gender, clinic, dentist) — medical history fields are not shown.

---

### User Story 4 — Edit Patient Information (Priority: P4)

An admin or dentist needs to update a patient's information. From the patient detail page, they edit demographic fields or add medical history details (systemic conditions, allergies, past treatments, notes). A receptionist can edit only demographic fields but not medical history fields. Changes are saved to Supabase and visible to all users of the same tenant.

**Why this priority**: Editing maintains the accuracy of patient records. Role-gated editing ensures clinical data is only modified by qualified personnel.

**Independent Test**: As admin/dentist, edit demographics and medical fields, save, refresh, verify changes persisted. As receptionist, verify medical fields are not editable.

**Acceptance Scenarios**:

1. **Given** an admin or dentist on the patient detail page, **When** they edit demographic fields (name, phone, date of birth, gender, clinic, dentist) and save, **Then** the changes are persisted and visible on reload.
2. **Given** an admin or dentist on the patient detail page, **When** they edit medical history fields (systemic conditions, allergies, past treatments, notes) and save, **Then** the changes are persisted.
3. **Given** a receptionist on the patient detail page, **When** they view the demographics section, **Then** the fields are editable.
4. **Given** a receptionist on the patient detail page, **When** they view the medical history section, **Then** the fields are read-only (disabled or non-editable).
5. **Given** a patient edit is saved on the web app, **When** the Android app views the same patient, **Then** the updated information is visible.
6. **Given** a user editing a patient, **When** a network error occurs during save, **Then** a user-friendly error message is displayed and the unsaved changes are preserved.

---

### Edge Cases

- **Empty patient list**: When a tenant has no patients, the list shows an empty state with a message in the current language (AR/EN) and a prompt to create the first patient.
- **Patient not found / wrong tenant**: Navigating to a patient ID that does not exist or belongs to another tenant shows a "Patient not found" message (not an error page or stack trace).
- **Concurrent edits**: If two users edit the same patient simultaneously, the last write wins. The system does not need merge conflict resolution at this stage.
- **Network failure on load**: If the patient data fails to load (network error, Supabase unavailable), a retry-able error message is displayed with a "Try Again" button.
- **Network failure on save**: If a save operation fails, the error is displayed to the user and the form data is preserved so the user can retry.
- **Soft-deleted patients**: Patients marked with `deleted_at` are excluded from the list by default. An explicit filter can optionally show them to admin users.
- **Long patient lists**: Patient lists exceeding 100 entries use pagination or infinite scroll to avoid loading all records at once.
- **Search with empty results**: When a search query returns no matching patients, a "No patients found" message is shown instead of an empty table.
- **Role changes mid-session**: If a user's role is changed by an admin while the user has the app open, the next data fetch enforces the new role's data scope via RLS.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The web app MUST display a paginated patient list filtered by the authenticated user's tenant.
- **FR-002**: The patient list MUST support text search by patient name.
- **FR-003**: The patient list MUST support filter controls by clinic, status (active/inactive), and assigned dentist.
- **FR-004**: Dentists MUST see only patients linked to their assigned clinic(s); receptionists and admins MUST see all patients in the tenant.
- **FR-005**: Clicking a patient row MUST navigate to the patient detail page with demographics, medical history, and tabbed sub-views.
- **FR-006**: The patient detail page MUST include tabs for: Procedure Cards, Appointments, Payments, and Medical Files related to that patient.
- **FR-007**: An admin, dentist, or receptionist MUST be able to create a new patient with required fields: name, phone, and clinic assignment.
- **FR-008**: The new patient form for receptionists MUST NOT include medical history fields (systemic conditions, allergies, past dental treatments).
- **FR-009**: Admin and dentist users MUST be able to edit both demographic and medical history fields on a patient.
- **FR-010**: Receptionist users MUST be able to edit only demographic fields; medical history fields MUST be rendered as read-only.
- **FR-011**: All patient CRUD operations MUST filter by `tenant_id` and respect the user's role at the Supabase RLS level.
- **FR-012**: All data fetching mutations MUST use React Query for caching, loading states, and optimistic updates where appropriate.
- **FR-013**: All labels, buttons, error messages, and empty states in the patients module MUST be translated into Arabic and English.
- **FR-014**: Patient records created on the web app MUST be visible on the Android app (and vice versa) since both share the same Supabase backend.

### Key Entities

- **Patient**: Core entity representing a dental patient within a tenant. Attributes include name, phone number, date of birth, gender, systemic conditions, allergies, past dental treatments, general notes, active status, clinic assignment, assigned dentist, and soft-delete timestamp. Linked to appointments, procedure cards, and medical files.
- **Patient Filters**: User-applied constraints for the patient list view, including search text, clinic filter, status filter (active/inactive), and dentist filter. Determines which patients are displayed.
- **User Session (from KIT-08)**: Provides the tenant ID and role context that scopes all patient data access. Receptionist role restricts editing of medical fields.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The patient list loads and renders within 3 seconds for a tenant with up to 500 patients.
- **SC-002**: Searching and filtering the patient list returns updated results within 1 second of applying a filter.
- **SC-003**: Creating a new patient completes within 2 seconds from form submission to the patient appearing in the list.
- **SC-004**: A receptionist cannot edit or modify medical history fields — the fields are visually disabled or hidden and any direct mutation attempt is rejected by RLS.
- **SC-005**: All text in the patients module (labels, buttons, messages, empty states) displays correctly in both Arabic (RTL) and English (LTR) without layout breakage.
- **SC-006**: 100% of patient data queries are scoped to the user's tenant — no cross-tenant patient data is accessible.

## Assumptions

- The web app scaffold (KIT-08) is complete and provides: AuthContext with role and tenant ID, React Router with protected routes, i18next with AR/EN setup, TailwindCSS, and the folder structure under `src/features/patients/`.
- The `patients` table exists in Supabase with all columns defined in the constitution schema (§5.5) and RLS policies are already applied from KIT-03.
- The `profiles` table in Supabase provides `role`, `tenant_id`, and `full_name` fields for the authenticated user.
- The patient list uses server-side pagination (Supabase `range()` queries) to handle lists larger than 100 patients.
- The tabbed sub-views (Procedure Cards, Appointments, Payments, Medical Files) display read-only summary data in this kit. Full management of those sub-entities is handled by their respective kits (KIT-10, KIT-11, KIT-12, KIT-13).
- A receptionist can create a patient with basic fields only; medical history fields are populated later by an admin or dentist.
- Browser language detection is not required for the patients module — language is managed by the app-wide language toggle from KIT-08.
