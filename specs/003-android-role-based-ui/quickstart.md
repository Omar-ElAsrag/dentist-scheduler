# Quickstart: Android Role-Based UI Enforcement

**Feature**: 003-android-role-based-ui
**Date**: 2026-06-12

Validation scenarios that prove role-based UI enforcement works end-to-end. Run these after completing all tasks in `tasks.md`.

## Prerequisites

- Android app build succeeds with `./gradlew assembleDebug`
- Supabase project has at least 3 test users, one per role (admin, dentist, receptionist), all in the same tenant
- Each test user has a matching `profiles` row with the correct `role` value

## Scenario 1: Admin Full Access

**Purpose**: Confirm admin sees all navigation tabs and all controls.

1. Log in as admin
2. Verify bottom nav shows 4 tabs: Schedule, Patients, Analytics, Clinics
3. Open the Schedule screen — verify the FAB for booking is visible
4. Open the Patients screen — select any patient, verify medical history fields are editable, Medical Files section is visible, Create Procedure Card button is visible
5. Open the Clinics screen — verify the add FAB is visible, edit/delete controls are visible on each clinic item
6. Open the Procedures Library (from top bar icon) — verify create, edit, delete controls are visible
7. Open Settings (from nav drawer) — verify Users management panel is accessible

**Expected**: All controls visible and functional. No restrictions.

## Scenario 2: Dentist Restrictions

**Purpose**: Confirm dentist cannot book appointments, access analytics, or manage clinics.

1. Log in as dentist
2. Verify bottom nav shows exactly 3 tabs: Schedule, Patients, Clinics (no Analytics)
3. Open the Schedule screen — verify the booking FAB is NOT visible
4. Open the Patients screen — select any patient:
   - Verify medical history fields ARE editable
   - Verify Create Procedure Card button IS visible
   - Verify Medical Files section IS visible
5. Open the Clinics screen — verify the add FAB is NOT visible
6. Open the Procedures Library — verify create/edit/delete controls are NOT visible (read-only)
7. Attempt direct navigation to Analytics (if possible) — verify permission-denied message

**Expected**: Analytics and booking controls hidden. Clinical access preserved.

## Scenario 3: Receptionist Restrictions

**Purpose**: Confirm receptionist cannot access medical files, analytics, or create procedure cards.

1. Log in as receptionist
2. Verify bottom nav shows exactly 3 tabs (no Analytics)
3. Open the Patients screen — select any patient:
   - Verify medical history fields are visible but READ-ONLY (cannot edit)
   - Verify Medical Files section is NOT visible
   - Verify Create Procedure Card button is NOT visible
4. Open the Clinics screen — verify add FAB is hidden
5. Open a procedure card with payments — verify Register Payment IS functional
6. Open the Procedures Library — verify read-only (no create/edit/delete)
7. Verify you CAN create a new patient (receptionist can do patient intake)

**Expected**: Medical/clinical features hidden. Patient management and payments functional.

## Scenario 4: Null Role / Unrecognized Role

**Purpose**: Confirm graceful handling when role is not available.

1. Force `SessionManager.role` to null (or use a user with no profile row)
2. Launch the app — verify a minimal UI appears with a message directing the user to contact admin
3. Verify no crash, no blank screen

**Expected**: Graceful fallback with contact-admin message.

## Scenario 5: Role Hardcoding Audit

**Purpose**: Confirm no hardcoded role strings in composable code.

1. Search `app/src/main/java/com/example/ui/` for the string literals `"admin"`, `"dentist"`, `"receptionist"` outside of `RoleGate.kt` and `SessionManager.kt`
2. Verify that all role references use the `RoleGate` composable (or constants defined by `RoleGate`)

**Expected**: Zero hardcoded role string literals in screen files. All gating goes through `RoleGate`.
