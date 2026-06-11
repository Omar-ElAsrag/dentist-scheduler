# Implementation Plan: Supabase Foundation & Data Layer Migration

**Branch**: `002-supabase-foundation` | **Date**: 2026-06-11 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-supabase-foundation/spec.md`

## Summary

Build the complete Supabase backend foundation for DentSched: a 13-table database schema with auto-updating timestamps and soft deletes, Row Level Security policies enforcing tenant isolation and role-based access (admin/dentist/receptionist), email/password authentication with automatic profile creation, Android Kotlin SDK integration replacing Room, login screens, and full data layer migration — covering KIT-02 through KIT-06. The approach is sequential: schema first, then RLS, then SDK wiring, then auth UI, then data migration — each kit building on the previous.

## Technical Context

**Language/Version**: Kotlin (Android), SQL (Supabase migrations)

**Primary Dependencies**: Supabase Kotlin SDK (postgrest-kt, auth-kt, storage-kt, realtime-kt by jan-tennert), Ktor client-android, Jetpack Compose, Supabase (PostgreSQL + Auth + Storage)

**Storage**: Supabase PostgreSQL (all application state), Supabase Storage (future: X-rays, images)

**Testing**: Supabase SQL editor for RLS policy validation, Android emulator for UI verification

**Target Platform**: Android (mobile app), Supabase (cloud backend)

**Project Type**: Mobile app + cloud backend (SaaS)

**Performance Goals**: Auth + profile load within 5 seconds; data queries return within 2 seconds on standard mobile networks

**Constraints**: Always-online (no offline mode in Phase 1); service role key must never appear in client code; all queries must filter by tenant_id

**Scale/Scope**: 13 database tables, 3 user roles, 9 seed procedures, 7 data domains to migrate (Clinics, Patients, Procedures, Cards, Steps, Payments, Medical Files), 6 existing Android screens to preserve

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **I. Multi-Tenant Isolation**: ✅ PASS — Every table includes `tenant_id` referencing `tenants(id)`. RLS policies enforce tenant-scoped access on every table. All application queries filter by `tenant_id`. Cross-tenant access is blocked at the database level.
- [x] **II. Supabase-First**: ✅ PASS — All state persisted in Supabase. Room local database fully removed. Supabase client SDKs are the sole communication layer. Service role key is never used on the client (only anon key via BuildConfig).
- [x] **III. Role-Gated Access**: ✅ PASS — RLS policies defined for each role (admin/dentist/receptionist) on every table. Access enforced at the database layer, not UI alone. The Role × Feature Access Matrix from the product constitution is the source of truth.
- [x] **IV. Clinical Integrity**: ✅ PASS — Soft delete (`deleted_at`) on `patients` and `procedure_cards`. Hard delete permitted for steps and payments. Associate cut and clinic share auto-calculated on every payment using the defined formula. Treatment steps record completion timestamps.
- [x] **V. Separation of Concerns**: ✅ PASS — Android follows MVVM: Composable → ViewModel → Repository → Supabase SDK. No business logic in Composables. One ViewModel per feature domain. `StateFlow` for UI state. Financial calculations in ViewModel, not UI.

**Result**: All 5 gates pass. No violations. Complexity Tracking section not required.

## Project Structure

### Documentation (this feature)

```text
specs/002-supabase-foundation/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (not created by /speckit.plan)
```

### Source Code (repository root)

```text
/
├── app/                              # Android app
│   ├── src/main/java/com/example/
│   │   ├── data/
│   │   │   ├── SupabaseClient.kt     # Supabase SDK singleton
│   │   │   ├── SessionManager.kt     # Auth session StateFlow
│   │   │   └── SupabaseRepository.kt # Data access layer (replaces DentistRepository)
│   │   ├── ui/
│   │   │   ├── auth/
│   │   │   │   ├── AuthViewModel.kt  # Login/logout logic
│   │   │   │   └── LoginScreen.kt    # Compose login UI
│   │   │   ├── DentistViewModel.kt   # Updated to use SupabaseRepository
│   │   │   └── SettingsScreen.kt     # Updated with sign out action
│   │   └── MainActivity.kt           # Updated with session check
│   └── build.gradle.kts              # Updated with Supabase SDK deps, Room removed
├── supabase/
│   ├── migrations/
│   │   ├── 001_initial_schema.sql    # 13 tables + triggers + indexes
│   │   └── 002_auth_and_rls.sql      # Auth trigger + RLS policies
│   └── seed.sql                      # 9 clinical procedures + types + steps
└── .env.example                      # SUPABASE_URL + SUPABASE_ANON_KEY
```

**Structure Decision**: Android app in `app/` with MVVM architecture. Supabase migrations in `supabase/migrations/` numbered sequentially. Data layer centralized in `SupabaseRepository.kt` replacing the existing `DentistRepository`. Auth UI in a dedicated `ui/auth/` package.

## Complexity Tracking

No violations to justify.
