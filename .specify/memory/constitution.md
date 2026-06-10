<!--
  Sync Impact Report
  Version Change: N/A → 1.0.0
  Modified Principles: N/A (initial fill)
  Added Sections: Core Principles (I–V), Technical Architecture & Conventions,
    Build Phases & Feature Roadmap, Governance
  Removed Sections: N/A
  Templates Requiring Updates:
    - .specify/templates/plan-template.md ✅ updated (Constitution Check gates)
    - .specify/templates/spec-template.md ✅ no changes needed
    - .specify/templates/tasks-template.md ✅ no changes needed
  Follow-up TODOs: None
-->

# DentSched Constitution

## Core Principles

### I. Multi-Tenant Data Isolation
Every table MUST include a `tenant_id` column referencing `tenants(id)`.
All queries MUST filter by `tenant_id` at the application level.
Supabase RLS policies MUST enforce tenant isolation on every data access
operation — tenants are fully isolated from one another. Cross-tenant data
leakage is unacceptable.

Rationale: Dental practices are sensitive medical businesses. A data leak
between tenants would be catastrophic. RLS + tenant_id filtering provides
defense in depth.

### II. Supabase-First Backend
All application state MUST be persisted in Supabase (PostgreSQL, Auth,
Storage, Realtime). No local database is used; the system is always-online
in Phase 1. Supabase client SDKs are the sole communication layer between
the app and the database. The Supabase service role key MUST NEVER be used
on the client.

Rationale: Supabase unifies auth, database, storage, and realtime into one
service, eliminating integration overhead and keeping the tech stack lean.

### III. Role-Gated Access Control
Three roles exist per tenant: `admin`, `dentist`, `receptionist`. Access
MUST be enforced at the Supabase RLS layer — never at the UI alone. The
Role × Feature Access Matrix (Section 7 of the product constitution) is
the source of truth for what each role can do. The UI MUST mirror RLS
enforcement but MUST NOT be the sole gate.

Rationale: RLS is server-enforced and cannot be bypassed by a modified
client. UI-only gating is security theatre for a SaaS medical platform.

### IV. Clinical Record Lifecycle Integrity
Procedure cards track the full treatment lifecycle: `In Progress` →
`Completed` or `Canceled`. Soft delete (`deleted_at`) MUST be used for
patients and procedure cards; hard delete is permitted for steps and
payments. Associate cut and clinic share MUST be auto-calculated on every
payment using the formula defined in §6.6. Treatment steps MUST record
completion timestamps and may include photo evidence.

Rationale: Clinical records have legal and financial implications.
Immutability and traceability are non-negotiable in a medical context.

### V. Platform Separation of Concerns
- Android: MVVM pattern — Composable → ViewModel → Repository → Supabase
  SDK. No business logic in Composables. One ViewModel per feature domain.
  `StateFlow` for UI state, `viewModelScope` for coroutines.
- Web: React Query for all data fetching and caching. One custom hook per
  domain (`usePatients`, `useSchedule`, etc.). TypeScript strict mode.
  i18next for Arabic/English with RTL layout switching on `dir="rtl"`.
- Business logic MUST live in services/repositories, never in UI components.

Rationale: Separating concerns keeps each platform testable, maintainable,
and enables parallel development by different team members.

## Technical Architecture & Conventions

### Tech Stack
| Layer | Technology |
|---|---|
| Android App | Kotlin + Jetpack Compose |
| Web App | React (TypeScript) |
| Backend / Database | Supabase (PostgreSQL + Auth + Storage + Realtime) |
| State Management (Android) | ViewModel + StateFlow + Supabase Kotlin SDK |
| State Management (Web) | React Query + Supabase JS SDK |
| Internationalisation | Android: LocalizationUtils; Web: i18next (AR + EN, RTL) |

### Database Conventions
- All IDs: SERIAL integers for domain tables (patients, clinics, etc.);
  UUID for `profiles` and `tenants`
- All timestamps: `TIMESTAMPTZ` (stored UTC, displayed in local time)
- Soft delete (`deleted_at`) on patients and procedure cards; hard delete
  on steps and payments
- Every table includes `created_at` and `updated_at` (managed by trigger)

### Folder Structure (Web)
- `/features/{domain}/` — one directory per feature module (patients,
  schedule, finance, procedures, etc.)
- Shared UI components in `/components/`
- Shared hooks in `/hooks/`

## Build Phases & Feature Roadmap

### Phase 1 — Foundation
1. Create Supabase project: schema, RLS policies, seed data, Auth
2. Build login/auth screens on Android
3. Replace Room DB with Supabase Kotlin SDK (keep existing UI)
4. Migrate existing local data model to Supabase schema

### Phase 2 — Multi-role & Web
5. Enforce role-based UI on Android
6. Build React web app (same Supabase backend)
7. Arabic/RTL support on web (i18next)
8. Receptionist-specific flows on both platforms

### Phase 3 — New Features
9. Appointments module (calendar view, flexible duration)
10. File uploads via Supabase Storage (X-rays, images)
11. Multi-dentist financial reporting
12. Business analytics dashboard (Admin only)

### Phase 4 — Polish & Scale
13. Supabase Realtime subscriptions (live schedule updates)
14. Push notifications for appointment reminders (Android)
15. Tenant onboarding flow (signup → create practice → invite users)
16. Billing / subscription management

### Feature Modules
Authentication & User Management, Scheduling, Patient Management,
Procedure Cards, Procedures Library, Payments & Finances,
Business Analytics (Admin only), Medical Files & Attachments.

## Governance

This constitution is the single source of truth for DentSched development
decisions. All other practices derive from it.

### Amendment Procedure
1. Propose the change with rationale and impact assessment
2. Update this document with the new or revised principle/section
3. Increment the version number per the versioning policy below
4. Propagate changes to dependent templates and guidance files
5. All PRs and reviews MUST verify compliance with this constitution

### Versioning Policy
- **MAJOR**: Backward-incompatible principle removals or redefinitions
- **MINOR**: New principle/section added or materially expanded guidance
- **PATCH**: Clarifications, wording updates, typo fixes, non-semantic
  refinements

### Compliance Review
- Every feature plan MUST pass the Constitution Check gate before Phase 0
  research begins and MUST be re-checked after Phase 1 design
- Complexity deviations from principles MUST be documented and justified
  in the Complexity Tracking section of the plan

**Version**: 1.0.0 | **Ratified**: 2026-06-11 | **Last Amended**: 2026-06-11
