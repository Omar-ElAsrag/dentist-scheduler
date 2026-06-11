# Implementation Plan: Android Role-Based UI Enforcement

**Branch**: `003-android-role-based-ui` | **Date**: 2026-06-12 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-android-role-based-ui/spec.md`

## Summary

Add a reusable role-checking mechanism (`RoleGate`) to the Android app that conditionally renders UI elements based on the logged-in user's role (admin, dentist, receptionist). Apply role gates across all existing screens to hide navigation tabs, disable editing controls, and show permission-denied messages. The role is read from the existing `SessionManager.role` StateFlow. This feature provides client-side UI enforcement as a complementary defense-in-depth layer on top of the existing Supabase RLS policies.

## Technical Context

**Language/Version**: Kotlin 2.2.10

**Primary Dependencies**: Jetpack Compose, `SessionManager.role` (existing), `androidx.compose.runtime.*`

**Storage**: N/A (no new data model — reads from existing SessionManager)

**Testing**: Manual verification via Android emulator (log in as each of the 3 roles, verify visibility rules)

**Target Platform**: Android (Jetpack Compose UI)

**Project Type**: Mobile app (Android)

**Performance Goals**: Role check must not cause visible UI jank; gate resolution < 16ms (single StateFlow read, no I/O)

**Constraints**: Role checks must use `SessionManager.role` as the single source of truth; no hardcoded role strings in composable functions; must not duplicate role-gating logic across screens

**Scale/Scope**: 6 existing screens to gate (Schedule, Patients, Clinics, Analytics, Procedures Library, Settings), 3 roles, ~20 gating points total

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **I. Multi-Tenant Isolation**: N/A — This feature is purely UI-level gating. No new data queries are introduced. Tenant isolation is already enforced by existing RLS policies in Supabase.
- [x] **II. Supabase-First**: ✅ PASS — No local database introduced. Role is read from SessionManager (populated from Supabase profiles table during auth). No service role key exposure.
- [x] **III. Role-Gated Access**: ✅ PASS — This feature IS the client-side complement to role-gated access. It adds UI enforcement on top of existing RLS server-side enforcement. Meets the "UI MUST mirror RLS enforcement but MUST NOT be the sole gate" requirement.
- [x] **IV. Clinical Integrity**: N/A — No changes to clinical data handling. Existing soft-delete and financial calculation logic remains unchanged.
- [x] **V. Separation of Concerns**: ✅ PASS — Role gating is encapsulated in a reusable `RoleGate` composable in `ui/components/`, not spread across screens. Business logic remains in repositories/view models. MVVM pattern preserved.

**Result**: All gates pass. No violations.

## Project Structure

### Documentation (this feature)

```text
specs/003-android-role-based-ui/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (conceptual gate model)
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (not created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/example/
├── ui/
│   ├── components/
│   │   └── RoleGate.kt             # Reusable role-checking composable
│   ├── auth/
│   │   └── AuthViewModel.kt        # No changes
│   ├── screens/
│   │   ├── ScheduleScreen.kt       # Gated: FAB hidden for dentist
│   │   ├── PatientsScreen.kt        # Gated: medical fields read-only for receptionist, create card hidden
│   │   ├── ClinicsScreen.kt         # Gated: add/edit/delete hidden for non-admin
│   │   ├── AnalyticsScreen.kt       # Gated: entire tab hidden for non-admin
│   │   ├── ProceduresLibraryDialog.kt  # Gated: edit/delete hidden for non-admin
│   │   ├── SettingsScreen.kt        # Gated: users panel hidden for non-admin
│   │   └── PatientPortalDialog.kt   # Gated: medical files hidden, fields read-only for receptionist
│   └── MainActivity.kt             # Gated: Analytics tab visibility, navigation guard
└── data/
    └── SessionManager.kt            # No changes (role already exposed as StateFlow)
```

**Structure Decision**: No new directories. The `RoleGate` composable goes into the existing `ui/components/` directory alongside existing shared components. Screen changes are minimal additions of `RoleGate` wrappers and role-conditionals on existing code paths.

## Complexity Tracking

No violations to justify.
