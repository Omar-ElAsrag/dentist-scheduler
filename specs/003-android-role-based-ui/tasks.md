# Tasks: Android Role-Based UI Enforcement

**Input**: Design documents from `/specs/003-android-role-based-ui/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, quickstart.md

**Tests**: Not requested in the feature specification. Test tasks are omitted.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Android UI components**: `app/src/main/java/com/example/ui/components/`
- **Android screens**: `app/src/main/java/com/example/ui/screens/`
- **Android auth**: `app/src/main/java/com/example/ui/auth/`
- **Android data**: `app/src/main/java/com/example/data/`
- **Android root**: `app/src/main/java/com/example/`

---

## Phase 1: Setup

**Purpose**: Verify project readiness. No new dependencies or scaffolding required — this feature adds only UI gating to existing screens.

- [X] T001 Verify app builds without errors with current Supabase foundation codebase (`./gradlew assembleDebug`)
- [X] T002 Confirm `SessionManager.role` StateFlow is populated after authentication and accessible from composables

**Checkpoint**: App builds. `SessionManager.role` is available for role checks.

---

## Phase 2: Foundational (RoleGate Composable)

**Purpose**: Create the reusable `RoleGate` composable that all user stories depend on.

**CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 Create `app/src/main/java/com/example/ui/components/RoleGate.kt` — a composable with signature `RoleGate(allowedRoles: Set<String>, fallbackContent: @Composable () -> Unit = {}, content: @Composable () -> Unit)` that reads `SessionManager.role` via `collectAsState()` and conditionally renders `content` or `fallbackContent`

**Checkpoint**: `RoleGate` composable exists and is importable by screens.

---

## Phase 3: User Story 1 - Admin Sees All Features (Priority: P1)

**Goal**: Apply role gating to navigation and admin-only features. Verify admin sees all tabs and controls while non-admin users are restricted. This is the foundational gating layer — it hides the Analytics tab and admin-only panels.

**Independent Test**: Log in as admin and verify all 4 tabs visible and all controls functional. Log in as non-admin and verify Analytics tab hidden.

### Implementation for User Story 1

- [X] T004 [US1] Add role-aware navigation tab filtering in `app/src/main/java/com/example/MainActivity.kt` — filter `navItems` list to exclude `NavigationItem.Analytics` when `SessionManager.role` is not "admin"
- [X] T005 [US1] Add `RoleGate` guard on the Analytics route composable in `app/src/main/java/com/example/MainActivity.kt` to prevent direct navigation (deep-link) to Analytics for non-admin roles
- [X] T006 [US1] Hide clinics management controls (add FAB, edit/delete buttons) in `app/src/main/java/com/example/ui/screens/ClinicsScreen.kt` using `RoleGate` with `allowedRoles = setOf("admin")`
- [X] T007 [US1] Hide procedure template create/edit/delete controls in `app/src/main/java/com/example/ui/screens/ProceduresLibraryDialog.kt` using `RoleGate` with `allowedRoles = setOf("admin")`
- [X] T008 [US1] Verify admin login: all 4 navigation tabs visible, Analytics accessible, clinics CRUD visible, procedures library editing visible

**Checkpoint**: Admin has full access. Non-admin users cannot see Analytics tab, clinics management, or procedures library editing.

---

## Phase 4: User Story 2 - Dentist Has Limited Access (Priority: P2)

**Goal**: Apply dentist-specific restrictions: hide appointment booking controls and restrict analytics/clinics access (already covered in US1). Dentist retains patient and procedure card access.

**Independent Test**: Log in as dentist, verify no booking FAB on Schedule, no Analytics tab, no clinics management. Verify patient access and procedure card creation still work.

### Implementation for User Story 2

- [X] T009 [US2] Hide appointment booking FAB and any edit/cancel controls on the Schedule screen in `app/src/main/java/com/example/ui/screens/ScheduleScreen.kt` using `RoleGate` with `allowedRoles = setOf("admin", "receptionist")`
- [X] T010 [US2] Verify dentist login: Schedule FAB hidden, Analytics tab absent, Clinics controls hidden, patients viewable, procedure cards creatable
- [X] T011 [US2] Verify dentist CAN still view and edit medical history fields and create procedure cards in patient detail

**Checkpoint**: Dentist sees 3 navigation tabs, no booking FAB, no admin controls. Clinical access preserved.

---

## Phase 5: User Story 3 - Receptionist Has Most Restricted Access (Priority: P2)

**Goal**: Apply receptionist-specific restrictions: hide medical files, make medical history read-only, hide procedure card creation. Receptionist retains patient management and payment registration.

**Independent Test**: Log in as receptionist, verify medical files hidden, medical history read-only, create procedure card hidden, payments functional.

### Implementation for User Story 3

- [X] T012 [US3] Hide Medical Files section in `app/src/main/java/com/example/ui/screens/PatientPortalDialog.kt` using `RoleGate` with `allowedRoles = setOf("admin", "dentist")`
- [X] T013 [US3] Set medical history fields (systemic conditions, allergies, past dental treatments) to read-only in `app/src/main/java/com/example/ui/screens/PatientPortalDialog.kt` when role is "receptionist" using Compose `enabled = role != "receptionist"` or `RoleGate`-based gating
- [X] T014 [US3] Hide "Create Procedure Card" button in patient detail view in `app/src/main/java/com/example/ui/screens/PatientPortalDialog.kt` and/or `app/src/main/java/com/example/ui/screens/PatientsScreen.kt` using `RoleGate` with `allowedRoles = setOf("admin", "dentist")`
- [X] T015 [US3] Hide Settings Users management panel in `app/src/main/java/com/example/ui/screens/SettingsScreen.kt` using `RoleGate` with `allowedRoles = setOf("admin")`
- [X] T016 [US3] Verify receptionist login: Analytics tab absent, medical files hidden, medical history read-only, create procedure card hidden, payments registerable
- [X] T017 [US3] Verify receptionist CAN still view schedules, manage patients (create/edit), register payments

**Checkpoint**: Receptionist has no access to clinical/medical data or admin features. Patient intake and payments functional.

---

## Phase 6: User Story 4 - Unauthorized Action Feedback (Priority: P3)

**Goal**: Display clear permission-denied messages when a user attempts a restricted action via UI or direct navigation.

**Independent Test**: Attempt to deep-link to Analytics as dentist/receptionist, verify permission-denied message.

### Implementation for User Story 4

- [X] T018 [US4] Add inline permission-denied text when "Create Procedure Card" button is hidden in `app/src/main/java/com/example/ui/screens/PatientPortalDialog.kt` — show "Requires dentist or admin access" or similar message via `RoleGate` `fallbackContent`
- [X] T019 [US4] Add inline permission-denied text when Medical Files section is hidden in `app/src/main/java/com/example/ui/screens/PatientPortalDialog.kt` — show "You don't have permission to access medical files" via `RoleGate` `fallbackContent`
- [X] T020 [US4] Verify deep-link to Analytics as dentist or receptionist shows permission-denied message (via `RoleGate` guard from T005)
- [X] T021 [US4] Verify all `RoleGate` usages with restricted access provide a clear `fallbackContent` message

**Checkpoint**: All restricted actions show a clear permission-denied message. No silent failures.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, cleanup, and edge case handling.

- [X] T022 Handle null/loading role state: In `RoleGate.kt`, when `SessionManager.role` is null, show a loading indicator or minimal UI with "Contact admin" message
- [X] T023 Verify edge case: user with no profile (bootstrap, `tenant_id = NULL`) sees a minimal UI directing to contact admin
- [X] T024 [P] Run quickstart scenario 1: verify admin full access (4 tabs, all controls)
- [X] T025 [P] Run quickstart scenario 2: verify dentist restrictions (3 tabs, no booking FAB)
- [X] T026 [P] Run quickstart scenario 3: verify receptionist restrictions (3 tabs, no medical files, read-only history)
- [X] T027 Run quickstart scenario 4: verify null/unrecognized role shows contact-admin message
- [X] T028 Run quickstart scenario 5: role hardcoding audit — search for "admin", "dentist", "receptionist" string literals in screen files, verify all gating goes through `RoleGate`
- [X] T029 Final audit: verify SC-004 compliance — no hardcoded role strings in composable code outside `RoleGate.kt` and `SessionManager.kt`

---

## Phase 8: Review Fixes (Post-Review)

**Purpose**: Address three constitution-mandated gates missed in initial implementation.

- [X] T030 Hide "Schedule Next" appointment button from dentist in `app/src/main/java/com/example/ui/screens/PatientPortalDialog.kt` using `RoleGate` with `allowedRoles = setOf("admin", "receptionist")`
- [X] T031 Hide both Delete Patient Record buttons from dentist and receptionist in `app/src/main/java/com/example/ui/screens/PatientPortalDialog.kt` using `RoleGate` with `allowedRoles = setOf("admin")`
- [X] T032 Set financial settings (treatment fee, lab fees, percentage, deduct lab fees) to read-only for dentist and receptionist in `ProcedureCardSection` of `app/src/main/java/com/example/ui/screens/PatientPortalDialog.kt` using `enabled = canEditFinancials` where `canEditFinancials = role == "admin"`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — verify existing build
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **US1 Admin Full Access (Phase 3)**: Depends on Foundational (RoleGate composable)
- **US2 Dentist Restrictions (Phase 4)**: Depends on Foundational — independent of US1
- **US3 Receptionist Restrictions (Phase 5)**: Depends on Foundational — independent of US1/US2
- **US4 Unauthorized Feedback (Phase 6)**: Depends on US1/US2/US3 (gating points must exist before adding messages)
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational — no dependency on other stories
- **US2 (P2)**: Can start after Foundational — independent of US1
- **US3 (P2)**: Can start after Foundational — independent of US1/US2
- **US4 (P3)**: Depends on US1, US2, US3 (gating points must exist to add messages)

### Within Each User Story

- Apply RoleGate to screen files
- Verify with the target role
- No test-before-code (manual verification only)

### Parallel Opportunities

- Phase 1: T001 and T002 can run in parallel
- Phase 3–5: US1, US2, US3 screen gating tasks can be worked in parallel (different screen files)
- Phase 7: T024, T025, T026 can run in parallel (different verification scenarios)

---

## Parallel Example: User Story Screen Gating

```bash
# All screen gating tasks across US1/US2/US3 can run in parallel:
Task: "T006 [US1] Hide clinics management controls in ClinicsScreen.kt"
Task: "T007 [US1] Hide procedures library editing in ProceduresLibraryDialog.kt"
Task: "T009 [US2] Hide appointment booking FAB in ScheduleScreen.kt"
Task: "T012 [US3] Hide Medical Files in PatientPortalDialog.kt"
Task: "T013 [US3] Set medical history fields read-only in PatientPortalDialog.kt"
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (RoleGate composable)
3. Complete Phase 3: US1 (Admin full access + navigation gating)
4. **STOP and VALIDATE**: Admin sees everything, non-admin cannot access Analytics
5. Basic role gating is operational

### Incremental Delivery

1. Complete Setup + Foundational → RoleGate ready
2. Add US1 (Navigation + Admin-Only) → Test → Navigation gating works
3. Add US2 (Dentist Restrictions) → Test → Dentist experience complete
4. Add US3 (Receptionist Restrictions) → Test → Receptionist experience complete
5. Add US4 (Permission Feedback) → Test → Graceful denial messages
6. Polish → Audit → All scenarios validated

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each phase or logical group
- Stop at any checkpoint to validate story independently
- The `RoleGate` composable is the single point of role-checking logic — all screen gating must go through it
- No new dependencies, no new data model, no new API calls — this feature is purely UI-layer
- Role constants ("admin", "dentist", "receptionist") should only appear in `RoleGate.kt`, `SessionManager.kt`, and test verification code
