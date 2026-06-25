# Tasks: Web Patients Module

**Input**: Design documents from `specs/008-web-patients-module/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, quickstart.md

**Tests**: Not requested — manual verification via quickstart.md scenarios.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- All source files under `web/` (React + TypeScript web app)
- Feature components: `web/src/features/patients/`
- Shared hook: `web/src/hooks/usePatients.ts`
- Translations: `web/src/locales/en/translation.json`, `web/src/locales/ar/translation.json`
- Routing: `web/src/App.tsx` (or router config file)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the patients feature directory and ensure routing scaffold is ready

- [x] T001 Create feature directory `web/src/features/patients/` if not already present
- [x] T002 Add patients routes to React Router (`/patients` for list, `/patients/:id` for detail) in `web/src/App.tsx`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core hook and barrel export that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 Create `usePatients` hook with list, getById, create, update, search, and filter functions using React Query and Supabase in `web/src/hooks/usePatients.ts`
- [x] T004 [P] Create barrel export file `web/src/features/patients/index.ts` exporting all components

**Checkpoint**: Foundation ready — user story implementation can now begin in parallel

---

## Phase 3: User Story 1 — View and Search the Patient List (Priority: P1) 🎯 MVP

**Goal**: A user can navigate to the Patients section, see a paginated list of tenant-scoped patients, and search/filter by name, clinic, status, and dentist.

**Independent Test**: Log in as any role, navigate to Patients, verify list loads with correct columns, search by name, apply filters, verify pagination works.

### Implementation for User Story 1

- [x] T005 [P] [US1] Create `PatientRow` component rendering a single patient row (name, clinic, dentist, status) in `web/src/features/patients/PatientRow.tsx`
- [x] T006 [P] [US1] Create `PatientsFilters` component with search input and clinic/status/dentist dropdown filters in `web/src/features/patients/PatientsFilters.tsx`
- [x] T007 [US1] Create `PatientsPage` component combining PatientsFilters, patient list table with pagination, and "New Patient" button placeholder in `web/src/features/patients/PatientsPage.tsx`
- [x] T008 [US1] Wire PatientsPage into router at `/patients` route with auth guard in `web/src/App.tsx`
- [x] T009 [US1] Add AR/EN translation keys for patient list (column headers, search placeholder, filter labels, empty state) in `web/src/locales/en/translation.json` and `web/src/locales/ar/translation.json`

**Checkpoint**: Patient list fully functional — search, filter, paginate, and click a row placeholder

---

## Phase 4: User Story 2 — View Patient Detail (Priority: P2)

**Goal**: Clicking a patient row navigates to the detail page showing demographics, medical history, and tabbed sub-views (Procedure Cards, Appointments, Payments, Medical Files).

**Independent Test**: Click any patient row, verify detail page loads with demographics, medical history, and 4 tabs. Switch tabs, verify content loads. Navigate to invalid ID, verify "Patient not found".

### Implementation for User Story 2

- [x] T010 [P] [US2] Create `PatientTabs` component with tab state management and lazy-loaded tab panels in `web/src/features/patients/PatientTabs.tsx`
- [x] T011 [US2] Create `PatientDetailPage` component displaying demographics section, medical history section, and PatientTabs in `web/src/features/patients/PatientDetailPage.tsx`
- [x] T012 [US2] Wire PatientDetailPage into router at `/patients/:id` route with auth guard in `web/src/App.tsx`
- [x] T013 [US2] Wire row click in PatientsPage to navigate to `/patients/:id` (update `web/src/features/patients/PatientsPage.tsx` and `web/src/features/patients/PatientRow.tsx`)
- [x] T014 [US2] Add AR/EN translation keys for patient detail (section headings, tab labels, field labels, "Patient not found" message) in `web/src/locales/en/translation.json` and `web/src/locales/ar/translation.json`

**Checkpoint**: Patient detail fully functional — navigate from list, view all sections and tabs

---

## Phase 5: User Story 3 — Create a New Patient (Priority: P3)

**Goal**: Admin, dentist, or receptionist can create a new patient with basic fields. Receptionist form excludes medical history fields. New patient syncs to Android.

**Independent Test**: Click "New Patient", fill required fields, submit. Verify patient appears in list. Verify on Android.

### Implementation for User Story 3

- [x] T015 [US3] Create `NewPatientModal` component with role-gated form (receptionist: name, phone, DOB, gender, clinic, dentist only; admin/dentist: all fields) in `web/src/features/patients/NewPatientModal.tsx`
- [x] T016 [US3] Integrate NewPatientModal into PatientsPage ("New Patient" button opens modal, on success refreshes list) in `web/src/features/patients/PatientsPage.tsx`
- [x] T017 [US3] Add AR/EN translation keys for new patient form (field labels, validation messages, submit button, success message) in `web/src/locales/en/translation.json` and `web/src/locales/ar/translation.json`

**Checkpoint**: Patient creation functional — all roles can create, receptionist gets basic-only form

---

## Phase 6: User Story 4 — Edit Patient Information (Priority: P4)

**Goal**: Admin and dentist can edit both demographics and medical history. Receptionist can edit demographics only — medical history fields are read-only. Changes persist and sync to Android.

**Independent Test**: As admin/dentist, edit all fields and save. As receptionist, verify medical fields are read-only and demographics are editable. Verify changes appear on Android.

### Implementation for User Story 4

- [x] T018 [P] [US4] Create `EditDemographicsForm` component (editable by all roles) in `web/src/features/patients/EditDemographicsForm.tsx`
- [x] T019 [P] [US4] Create `EditMedicalHistoryForm` component (editable by admin/dentist, read-only for receptionist) in `web/src/features/patients/EditMedicalHistoryForm.tsx`
- [x] T020 [US4] Integrate edit forms into PatientDetailPage (inline or toggle between view/edit mode) in `web/src/features/patients/PatientDetailPage.tsx`
- [x] T021 [US4] Add AR/EN translation keys for edit forms (edit/save/cancel buttons, field labels, permission denied message) in `web/src/locales/en/translation.json` and `web/src/locales/ar/translation.json`

**Checkpoint**: Patient editing functional — role-gated editing enforced at UI level, RLS enforced at DB level

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Edge cases, error handling, loading states, and final validation

- [x] T022 [P] Add loading skeletons/spinners for patient list and detail page in `web/src/features/patients/PatientsPage.tsx` and `web/src/features/patients/PatientDetailPage.tsx`
- [x] T023 [P] Add error state UI with retry button for network failures in `web/src/hooks/usePatients.ts` (error propagation to components)
- [x] T024 [P] Add empty state messages (no patients, no search results) in relevant translation files and components
- [x] T025 [P] Handle soft-deleted patients (exclude from list, show "not found" on detail) in `web/src/hooks/usePatients.ts`
- [x] T026 Run quickstart.md validation scenarios (all 7 scenarios) and fix any issues

---

## Phase 8: Follow-Up Fixes

**Purpose**: Delete patient (admin only) with confirmation dialog, and sort patients newest-first

- [x] T027 [US2] Add soft delete patient capability — `useSoftDeletePatient` mutation in `web/src/hooks/usePatients.ts`, delete button with shadcn `AlertDialog` confirmation on `web/src/features/patients/PatientDetailPage.tsx`, shadcn `Button` and `AlertDialog` components in `web/src/components/ui/`, `cn` utility in `web/src/lib/utils.ts`, and AR/EN translation keys for delete confirmation
- [x] T028 [US1] Change patient list sort from alphabetical to newest-first by replacing `.order('name', { ascending: true })` with `.order('created_at', { ascending: false })` in `web/src/hooks/usePatients.ts`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) completion
- **User Story 2 (Phase 4)**: Depends on US1 (Phase 3) — needs list page with row click navigation
- **User Story 3 (Phase 5)**: Depends on US1 (Phase 3) — modal opens from list page
- **User Story 4 (Phase 6)**: Depends on US2 (Phase 4) — edit forms integrate into detail page
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational. Builds the list page — the entry point for all other stories.
- **US2 (P2)**: Depends on US1 (click row → navigate to detail). Adds detail page with tabs.
- **US3 (P3)**: Depends on US1 (modal opens from PatientsPage). Adds create patient flow.
- **US4 (P4)**: Depends on US2 (edit forms integrate into PatientDetailPage). Adds role-gated editing.

### Within Each User Story

- [P] components before page assembly
- Page assembly before route wiring
- Translation keys after all UI text is finalized
- Verify checkpoint before moving to next story

### Parallel Opportunities

- **Phase 2**: T004 can run in parallel with T003 tail end
- **Phase 3**: T005 and T006 can run in parallel (PatientRow + PatientsFilters are independent)
- **Phase 4**: T010 can run in parallel with T011 start (PatientTabs before PatientDetailPage integration)
- **Phase 6**: T018 and T019 can run in parallel (EditDemographicsForm + EditMedicalHistoryForm)
- **Phase 7**: T022, T023, T024, T025 can all run in parallel (independent edge case handling)

---

## Parallel Example: User Story 1

```bash
# Launch independent components together:
Task: "Create PatientRow component in web/src/features/patients/PatientRow.tsx"
Task: "Create PatientsFilters component in web/src/features/patients/PatientsFilters.tsx"

# Then assemble:
Task: "Create PatientsPage component in web/src/features/patients/PatientsPage.tsx"
```

## Parallel Example: User Story 4

```bash
# Launch both edit forms together:
Task: "Create EditDemographicsForm component in web/src/features/patients/EditDemographicsForm.tsx"
Task: "Create EditMedicalHistoryForm component in web/src/features/patients/EditMedicalHistoryForm.tsx"

# Then integrate:
Task: "Integrate edit forms into PatientDetailPage"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T002)
2. Complete Phase 2: Foundational (T003–T004)
3. Complete Phase 3: User Story 1 (T005–T009)
4. **STOP and VALIDATE**: Test patient list with all roles, search, filters, pagination
5. Deploy/demo if ready — this delivers a working patient list

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 → Patient list (MVP!)
3. Add US2 → Patient detail with tabs
4. Add US3 → Create new patient
5. Add US4 → Edit patient with role gating
6. Polish → Error states, loading states, validation

### Single Developer Strategy

Since this is a solo project, execute sequentially:
1. T001 → T002 → T003 → T004
2. T005 → T006 → T007 → T008 → T009 (US1)
3. T010 → T011 → T012 → T013 → T014 (US2)
4. T015 → T016 → T017 (US3)
5. T018 → T019 → T020 → T021 (US4)
6. T022 → T023 → T024 → T025 → T026 (Polish)

---

## Notes

- [P] tasks = different files, no dependencies — can be parallelized if multiple developers available
- [Story] label maps task to specific user story for traceability
- Each user story checkpoint should be independently verifiable
- Commit after each task or logical group (e.g., after completing a component + its tests)
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
