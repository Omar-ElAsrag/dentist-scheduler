# Implementation Plan: Web Patients Module

**Branch**: `kit-09-web-patients` | **Date**: 2026-06-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/008-web-patients-module/spec.md`

## Summary

Build the patients module for the React web app under `/web/src/features/patients/`. This module provides a searchable, filterable patient list, a patient detail page with tabbed sub-views (Procedure Cards, Appointments, Payments, Medical Files), a new patient form, and role-gated editing where receptionists cannot modify medical history fields. All data flows through React Query hooks to the existing Supabase backend shared with the Android app.

## Technical Context

**Language/Version**: TypeScript 5.x (strict mode), React 18+

**Primary Dependencies**: @supabase/supabase-js (data), @tanstack/react-query (data fetching/caching), react-router-dom (routing), tailwindcss (styling), i18next + react-i18next (AR/EN translations)

**Storage**: Supabase PostgreSQL — `patients` table (already exists with full schema and RLS policies from KIT-02/KIT-03)

**Testing**: Manual verification via `npm run dev` + browser

**Target Platform**: Web browser — Chrome, Firefox, Safari (desktop + mobile)

**Project Type**: Single-page web application (React SPA) — feature module within existing scaffold

**Performance Goals**: Patient list load <3s for 500 patients, search/filter results <1s, patient create <2s

**Constraints**: TypeScript strict mode; RTL layout for Arabic; RLS-enforced role gating at DB level; shares Supabase backend with Android app; no server-side rendering; no offline mode in Phase 1

**Scale/Scope**: One feature module: 2 main pages (list + detail), 1 modal (new patient), 2 edit flows (demographics + medical history). Tab content displayed read-only (full management deferred to KIT-10 through KIT-13).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **I. Multi-Tenant Isolation**: All queries include `tenant_id` filter sourced from `AuthContext`. RLS policies already enforced at DB level (KIT-03). Web client uses anon key only — no service role key.
- [x] **II. Supabase-First**: No local DB. Supabase JS SDK is the sole data layer. No service role key in client bundle. Patients table already exists in Supabase.
- [x] **III. Role-Gated Access**: RLS policies on patients table already enforce dentist = own clinic(s) only, receptionist = read-all / write-basic / no-medical-edit. Web UI mirrors RLS enforcement: receptionist sees read-only medical fields. Full role matrix (§7) applied.
- [x] **IV. Clinical Integrity**: Soft delete on patients via `deleted_at` column already in schema. This module respects soft-delete (excludes deleted patients from list by default). No financial calculations in this module.
- [x] **V. Separation of Concerns**: One custom hook (`usePatients`) for all patient data operations. UI components in `src/features/patients/` render data; no business logic in components. TypeScript strict mode. i18next for AR/EN with RTL support.

## Project Structure

### Documentation (this feature)

```text
specs/008-web-patients-module/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (to be created under /web/src/features/patients/)

```text
web/src/features/patients/
├── PatientsPage.tsx           # Patient list with search, filters, pagination
├── PatientDetailPage.tsx      # Patient profile with demographics, medical history, tabs
├── NewPatientModal.tsx        # Create patient form (role-gated fields)
├── EditDemographicsForm.tsx   # Edit demographics form (inline or modal)
├── EditMedicalHistoryForm.tsx # Edit medical history form (hidden for receptionist)
├── PatientsFilters.tsx        # Filter bar component (clinic, status, dentist selects)
├── PatientRow.tsx             # Single row in patient list table
├── PatientTabs.tsx            # Tab navigation: Procedure Cards | Appointments | Payments | Medical Files
└── index.ts                   # Barrel export
```

Shared hooks:
```text
web/src/hooks/
└── usePatients.ts             # React Query hook: list, getById, create, update, search, filter
```

**Structure Decision**: Single feature module under `src/features/patients/` per constitution web conventions. One React Query hook (`usePatients`) in shared hooks. All UI components co-located in the feature directory. Tab sub-views render read-only summary data; full management of sub-entities (procedure cards, appointments, payments, medical files) is handled by their respective features in KIT-10 through KIT-13.

## Complexity Tracking

No constitution violations. All checks pass.
