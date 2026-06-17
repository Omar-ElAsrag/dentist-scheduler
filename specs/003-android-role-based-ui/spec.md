# Feature Specification: Android Role-Based UI Enforcement

**Feature Branch**: `003-android-role-based-ui`

**Created**: 2026-06-12

**Status**: Draft

**Input**: User description: "KIT-07 · Android — Role-Based UI Enforcement: Hide or disable UI elements based on the logged-in user's role (admin, dentist, receptionist), using the role stored in SessionManager."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Admin Sees All Features (Priority: P1)

An admin logs into the app and can access every screen, every navigation tab, every create/edit/delete control, and every admin-only panel without restriction.

**Why this priority**: Admin is the practice owner; they need full visibility to manage the clinic, finances, team, and procedures library. Without admin access, the practice cannot be managed.

**Independent Test**: Log in as a user with role "admin", navigate through all four tabs (Schedule, Patients, Analytics, Clinics), open the navigation drawer, and verify every control is visible and active. This story alone proves the gating system does not over-restrict admin users.

**Acceptance Scenarios**:

1. **Given** an admin is logged in, **When** they view the bottom navigation bar, **Then** all four tabs (Schedule, Patients, Analytics, Clinics) are visible.
2. **Given** an admin is logged in, **When** they open the navigation drawer, **Then** Profile and Settings options are visible, and within Settings the Users management panel is accessible.
3. **Given** an admin is logged in, **When** they open the Procedures Library dialog, **Then** they can create, edit, and delete procedure templates.
4. **Given** an admin is logged in, **When** they view the Clinics screen, **Then** the add/edit/delete clinic controls are visible and functional.

---

### User Story 2 - Dentist Has Limited Access (Priority: P2)

A dentist logs in and can view their own schedule, patients, and procedure cards, but cannot book new appointments, access analytics, or manage clinics.

**Why this priority**: Dentists are the primary clinical users and need access to patient data and procedure tracking, but should not perform administrative functions like booking appointments or viewing financial analytics.

**Independent Test**: Log in as a user with role "dentist", navigate through the app, and verify that the Analytics tab is hidden, appointment booking controls are absent from the Schedule screen, and clinics management controls are not visible. Verify the dentist CAN still view patients and create procedure cards.

**Acceptance Scenarios**:

1. **Given** a dentist is logged in, **When** they view the bottom navigation bar, **Then** the Analytics tab is not visible (only Schedule, Patients, Clinics tabs appear).
2. **Given** a dentist is logged in, **When** they view the Schedule screen, **Then** the floating action button for booking new appointments and any edit/cancel controls on existing appointments are hidden.
3. **Given** a dentist is logged in, **When** they view the Clinics screen, **Then** the add/edit/delete clinic controls are hidden — the screen shows a read-only list.
4. **Given** a dentist is logged in, **When** they open the Procedures Library dialog, **Then** they can view procedures but cannot create, edit, or delete them.
5. **Given** a dentist is logged in, **When** they open a patient's detail view, **Then** they CAN view and edit medical history fields (systemic conditions, allergies, past dental treatments) and CAN create procedure cards.

---

### User Story 3 - Receptionist Has Most Restricted Access (Priority: P2)

A receptionist logs in and can manage patient records, register payments, and view schedules, but cannot access medical files, medical history, analytics, procedure card creation, or administrative features.

**Why this priority**: Receptionists handle front-desk duties (patient intake, payments, scheduling) but must not access clinical data (medical files, medical history) or financial analytics. This is a legal/regulatory requirement in a medical practice context.

**Independent Test**: Log in as a user with role "receptionist", navigate through the app, and verify that the Analytics tab is hidden, the Medical Files section is absent from patient detail, medical history fields are read-only, procedure card creation is hidden, and clinics management is read-only. Verify the receptionist CAN still register payments and view schedules.

**Acceptance Scenarios**:

1. **Given** a receptionist is logged in, **When** they view the bottom navigation bar, **Then** the Analytics tab is not visible.
2. **Given** a receptionist is logged in, **When** they open a patient's detail view, **Then** the Medical Files tab/section is either hidden or displays a "no access" message.
3. **Given** a receptionist is logged in, **When** they view a patient's medical history fields (systemic conditions, allergies, past dental treatments), **Then** those fields are read-only and cannot be edited.
4. **Given** a receptionist is logged in, **When** they view the patient detail screen, **Then** the "Create Procedure Card" button is hidden.
5. **Given** a receptionist is logged in, **When** they register a payment on a procedure card, **Then** the payment is saved successfully (receptionist CAN register payments).
6. **Given** a receptionist is logged in, **When** they open the Procedures Library dialog, **Then** they can view procedures but cannot create, edit, or delete them.
7. **Given** a receptionist is logged in, **When** they view the Clinics screen, **Then** the add/edit/delete clinic controls are hidden.

---

### User Story 4 - Unauthorized Action Feedback (Priority: P3)

When any role attempts an action they are not permitted to perform (via UI or direct navigation), the system provides a clear "permission denied" message.

**Why this priority**: Graceful handling of unauthorized actions improves user experience and prevents confusion when features are not accessible.

**Independent Test**: As a dentist or receptionist, attempt to navigate directly to a restricted screen or trigger a restricted action. Verify a clear message indicates the action is not permitted.

**Acceptance Scenarios**:

1. **Given** a dentist or receptionist is logged in, **When** they somehow attempt to access the Analytics screen (e.g., via deep link), **Then** a message is displayed indicating they do not have permission to access this feature.
2. **Given** a receptionist is logged in, **When** they attempt to access medical files (e.g., via direct navigation), **Then** a "You don't have permission to access medical files" message is displayed.

---

### Edge Cases

- What happens if `SessionManager.role` is null (role not yet loaded after login)? → The UI should show a loading state or default to the most restrictive view until the role is available.
- What happens if the user's role is changed by an admin while the app is open? → The UI should reflect the new role on the next screen navigation or data refresh (not necessarily real-time for Phase 2).
- What about users with no role assigned (bootstrap case, `tenant_id` is NULL)? → These users should see a minimal UI prompting them to contact an admin.
- What about deep links or direct navigation to restricted screens via the navigation controller? → Role gates should also protect the navigation graph itself, not just UI visibility.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST read the current user's role from `SessionManager.role` as the single source of truth for all UI visibility decisions.
- **FR-002**: System MUST provide a reusable role-checking mechanism that conditionally renders content based on the current user's role, avoiding repeated manual role checks across screens.
- **FR-003**: System MUST hide the Analytics navigation tab from all users whose role is not "admin".
- **FR-004**: System MUST hide appointment booking controls (FAB, edit, cancel) from users whose role is "dentist".
- **FR-005**: System MUST hide the Medical Files section from users whose role is "receptionist".
- **FR-006**: System MUST set medical history fields (systemic conditions, allergies, past dental treatments) to read-only for users whose role is "receptionist".
- **FR-007**: System MUST hide the "Create Procedure Card" button from users whose role is "receptionist".
- **FR-008**: System MUST hide clinics management controls (add, edit, delete) from users whose role is not "admin".
- **FR-009**: System MUST hide procedure template creation, editing, and deletion controls in the Procedures Library from users whose role is not "admin".
- **FR-010**: System MUST display a user-visible message indicating lack of permission when a user attempts to access a feature restricted from their role.
- **FR-011**: System MUST allow users with role "receptionist" to register payments on procedure cards.

### Key Entities *(include if feature involves data)*

- **User Role**: A string value ("admin", "dentist", "receptionist") stored in SessionManager that determines which UI elements are visible. This maps directly to the `role` field in the `profiles` table and is loaded after authentication.
- **RoleGate**: A conceptual gate that wraps any UI element and only renders it if the current user's role matches an allowed set of roles. Can be configured for visibility (show/hide) or editability (read-only/editable).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An admin user sees all four navigation tabs (Schedule, Patients, Analytics, Clinics) and all CRUD controls across every screen.
- **SC-002**: A dentist user sees exactly three navigation tabs (Schedule, Patients, Clinics) — the Analytics tab is absent. The Schedule screen's appointment booking FAB is hidden.
- **SC-003**: A receptionist user sees exactly three navigation tabs (Schedule, Patients, Clinics). Within Patient detail, the Medical Files section is absent and medical history fields are non-editable.
- **SC-004**: No role string literal ("admin", "dentist", "receptionist") appears more than once in UI component code — all role checks reference a centralized role source via the reusable mechanism from FR-002.
- **SC-005**: A user whose role is null or unrecognized sees a minimal UI with a message directing them to contact their practice admin.
- **SC-006**: Switching from one role's login to another role's login updates all visible UI elements without requiring an app restart.

## Assumptions

- The `SessionManager.role` StateFlow is already populated after successful authentication (implemented in KIT-02 through KIT-06).
- The three roles (admin, dentist, receptionist) are the only roles in the system; no additional roles are introduced in this phase.
- Role changes by an admin do not require real-time UI updates during the current session — the user must sign out and sign back in to see updated role-based UI changes.
- The existing screen architecture (Composables, ViewModels, navigation) remains unchanged; this feature only adds conditional rendering on top of existing screens.
- The role-based RLS policies in Supabase already enforce server-side access control; this feature provides client-side UI enforcement as a complementary layer, consistent with the constitution's defense-in-depth principle.
- The Permissions Denied message is a simple text dialog or inline message; no complex error recovery flow is required in this phase.
