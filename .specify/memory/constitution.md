<!--
  Sync Impact Report
  Version Change: 1.0.0 → 1.4.0
  Modified Principles:
    - I. Multi-Tenant Data Isolation (expanded with tenant plan model)
    - III. Role-Gated Access Control (expanded with detailed role descriptions
      and full Role × Feature Access Matrix)
    - IV. Clinical Record Lifecycle Integrity (expanded with full schema references
      and explicit formula documentation)
  Added Sections:
    - Project Vision (§1)
    - Architectural Decisions — Locked (§2)
    - Roles & Access Control — detailed per-role descriptions (§4)
    - Database Schema — full 13-table definitions (§5)
    - Feature Modules — 8 module descriptions (§6)
    - Role × Feature Access Matrix (§7)
    - Coding Conventions — General + Android + Web (§9)
  Removed Sections: None
  Templates Requiring Updates:
    - .specify/templates/plan-template.md ✅ no changes needed
      (Constitution Check gates still align with principles I–V)
    - .specify/templates/spec-template.md ✅ no changes needed
    - .specify/templates/tasks-template.md ✅ no changes needed
  Follow-up TODOs: None
-->

# DentSched Constitution

## 1. Project Vision

A multi-device, multi-role dental practice management SaaS. Each dental
practice is an isolated tenant. The platform is accessible on both Android
and Web, serving clinics with multiple dentists, receptionists, and an
admin per tenant.

The system manages the full patient lifecycle: from first contact and
scheduling, through detailed clinical records and step-by-step treatment
tracking, to payments, finances, and business analytics.

## 2. Architectural Decisions (Locked)

| Decision | Choice | Reason |
|---|---|---|
| Platform | Android + Web | Existing Android app + new web dashboard |
| Backend | Supabase | Auth, DB, Storage, Realtime in one service |
| Multi-tenancy | One Supabase project, `tenant_id` on every table | Simpler than separate projects per practice |
| Dentist–Clinic | Many-to-many via junction table | A dentist can work at multiple clinics |
| Appointment slots | Flexible duration per booking | Clinics need control over session length |
| Language | Arabic + English (RTL support required) | Both platforms |
| Offline mode | Always online for now | Clinics have wifi; simplifies Phase 1 |
| Deployment model | SaaS — multiple tenants | Each dental practice is an isolated account |

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
Role × Feature Access Matrix (§7) is the source of truth for what each
role can do. The UI MUST mirror RLS enforcement but MUST NOT be the sole
gate.

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

## 4. Roles & Access Control

Access is enforced at the Supabase RLS layer. Every RLS policy also checks
`tenant_id` so tenants are fully isolated.

### 4.1 Admin

- Full access to everything within their tenant
- Creates and manages user accounts (dentists, receptionists)
- Manages clinics, procedure templates, and system settings
- Views all financial data across all dentists and clinics
- Can delete any record (soft delete)

### 4.2 Dentist

- Sees only patients linked to their assigned clinic(s)
- Creates and edits patient files, procedure cards, and steps
- Logs payments for their own procedure cards
- Views their own schedule (read-only — cannot book, edit, or cancel)
- Views their own financial summary (associate cut, pending balance)
- Cannot see other dentists' financial details
- Cannot manage users or system settings

### 4.3 Receptionist

- Schedules and manages appointments for any dentist/clinic within tenant
- Creates new patient profiles (basic info only)
- Views the full schedule and patient list
- Can view and register payments against procedure cards
- Cannot create or edit procedure cards
- Cannot view financial analytics or reports
- Cannot delete records

## 5. Database Schema

All tables include `created_at TIMESTAMPTZ DEFAULT now()` and `updated_at`
managed by trigger. Every table (except `tenants` itself) includes
`tenant_id UUID REFERENCES tenants(id)` for row-level isolation.

### 5.1 `tenants`

| Column | Type | Notes |
|---|---|---|
| id | UUID PK | DEFAULT gen_random_uuid() |
| name | TEXT NOT NULL | practice name |
| plan | TEXT | DEFAULT 'trial' — 'trial' / 'basic' / 'pro' |
| is_active | BOOLEAN | DEFAULT true |

### 5.2 `profiles` (extends Supabase auth.users)

| Column | Type | Notes |
|---|---|---|
| id | UUID PK | references auth.users |
| tenant_id | UUID FK | REFERENCES tenants(id) |
| full_name | TEXT | |
| role | TEXT | CHECK (role IN ('admin','dentist','receptionist')) |
| avatar_url | TEXT | |
| is_active | BOOLEAN | DEFAULT true |

### 5.3 `clinics`

| Column | Type | Notes |
|---|---|---|
| id | SERIAL PK | |
| tenant_id | UUID FK | REFERENCES tenants(id) |
| name | TEXT NOT NULL | |
| default_percentage | FLOAT | dentist revenue share % (clinic default) |
| deduct_lab_fees | BOOLEAN | DEFAULT false |
| address | TEXT | |
| phone | TEXT | |

### 5.4 `dentist_clinics` (many-to-many junction)

| Column | Type | Notes |
|---|---|---|
| dentist_id | UUID FK | REFERENCES profiles(id) ON DELETE CASCADE |
| clinic_id | INT FK | REFERENCES clinics(id) ON DELETE CASCADE |
| percentage_override | FLOAT | NULL = use clinic default |
| | PK | (dentist_id, clinic_id) |

### 5.5 `patients`

| Column | Type | Notes |
|---|---|---|
| id | SERIAL PK | |
| tenant_id | UUID FK | REFERENCES tenants(id) |
| name | TEXT NOT NULL | |
| phone_number | TEXT | |
| date_of_birth | DATE | |
| gender | TEXT | |
| systemic_conditions | TEXT | |
| past_dental_treatments | TEXT | |
| allergies | TEXT | |
| general_notes | TEXT | |
| is_in_progress | BOOLEAN | DEFAULT true |
| created_date | DATE | |
| clinic_id | INT FK | REFERENCES clinics(id) |
| assigned_dentist_id | UUID FK | REFERENCES profiles(id) |
| deleted_at | TIMESTAMPTZ | soft delete |

### 5.6 `appointments`

| Column | Type | Notes |
|---|---|---|
| id | SERIAL PK | |
| tenant_id | UUID FK | REFERENCES tenants(id) |
| patient_id | INT FK | REFERENCES patients(id) ON DELETE CASCADE |
| dentist_id | UUID FK | REFERENCES profiles(id) |
| clinic_id | INT FK | REFERENCES clinics(id) |
| date | DATE NOT NULL | |
| time | TIME NOT NULL | |
| duration_mins | INT NOT NULL | flexible, no default forced |
| notes | TEXT | |
| status | TEXT | DEFAULT 'scheduled' — 'scheduled' / 'completed' / 'canceled' / 'no_show' |
| created_by | UUID FK | REFERENCES profiles(id) |

### 5.7 `clinical_procedures` (procedure templates)

| Column | Type | Notes |
|---|---|---|
| id | SERIAL PK | |
| tenant_id | UUID FK | REFERENCES tenants(id) — NULL = system-wide seed |
| name | TEXT NOT NULL | |
| has_types | BOOLEAN | |
| display_order | INT | |
| is_custom | BOOLEAN | DEFAULT false |
| default_fee | FLOAT | |
| default_lab_fee | FLOAT | |

### 5.8 `procedure_types`

| Column | Type | Notes |
|---|---|---|
| id | SERIAL PK | |
| clinical_procedure_id | INT FK | REFERENCES clinical_procedures(id) ON DELETE CASCADE |
| name | TEXT NOT NULL | |
| materials | TEXT | |
| default_fee | FLOAT | |
| default_lab_fee | FLOAT | |
| material_fees | TEXT | "Material:fee,Material:fee" |

### 5.9 `clinical_procedure_steps`

| Column | Type | Notes |
|---|---|---|
| id | SERIAL PK | |
| clinical_procedure_id | INT FK | REFERENCES clinical_procedures(id) ON DELETE CASCADE |
| procedure_type_id | INT FK | REFERENCES procedure_types(id) ON DELETE CASCADE |
| step_name | TEXT NOT NULL | |
| display_order | INT | |

### 5.10 `procedure_cards`

| Column | Type | Notes |
|---|---|---|
| id | SERIAL PK | |
| tenant_id | UUID FK | REFERENCES tenants(id) |
| patient_id | INT FK | REFERENCES patients(id) ON DELETE CASCADE |
| clinic_id | INT FK | REFERENCES clinics(id) |
| dentist_id | UUID FK | REFERENCES profiles(id) |
| clinical_procedure_id | INT FK | REFERENCES clinical_procedures(id) |
| procedure_type_id | INT FK | REFERENCES procedure_types(id) |
| material | TEXT | |
| status | TEXT | DEFAULT 'In Progress' — 'In Progress' / 'Completed' / 'Canceled' |
| date_created | DATE | |
| tooth_number | TEXT | |
| notes | TEXT | |
| treatment_fee | FLOAT | DEFAULT 0 |
| amount_paid | FLOAT | DEFAULT 0 |
| lab_fees | FLOAT | DEFAULT 0 |
| applied_percentage | FLOAT | DEFAULT 0 |
| deduct_lab_fees | BOOLEAN | DEFAULT false |
| calculated_associate_cut | FLOAT | DEFAULT 0 |
| calculated_clinic_share | FLOAT | DEFAULT 0 |
| deleted_at | TIMESTAMPTZ | soft delete |

### 5.11 `procedure_card_steps`

| Column | Type | Notes |
|---|---|---|
| id | SERIAL PK | |
| procedure_card_id | INT FK | REFERENCES procedure_cards(id) ON DELETE CASCADE |
| step_name | TEXT NOT NULL | |
| is_completed | BOOLEAN | DEFAULT false |
| completed_at | TIMESTAMPTZ | |
| photo_urls | TEXT[] | Supabase Storage URLs |
| display_order | INT | DEFAULT 0 |
| notes | TEXT | |

### 5.12 `procedure_payments`

| Column | Type | Notes |
|---|---|---|
| id | SERIAL PK | |
| tenant_id | UUID FK | REFERENCES tenants(id) |
| procedure_card_id | INT FK | REFERENCES procedure_cards(id) ON DELETE CASCADE |
| amount | FLOAT NOT NULL | |
| payment_at | TIMESTAMPTZ NOT NULL | |
| notes | TEXT | |
| recorded_by | UUID FK | REFERENCES profiles(id) |

### 5.13 `medical_files`

| Column | Type | Notes |
|---|---|---|
| id | UUID PK | DEFAULT gen_random_uuid() |
| tenant_id | UUID FK | REFERENCES tenants(id) |
| patient_id | INT FK | REFERENCES patients(id) ON DELETE CASCADE |
| title | TEXT NOT NULL | |
| content | TEXT | |
| file_url | TEXT | Supabase Storage URL (images/X-rays) |
| file_type | TEXT | 'note' / 'xray' / 'image' / 'document' |
| created_by | UUID FK | REFERENCES profiles(id) |

## 6. Feature Modules

### 6.1 Authentication & User Management

- Login with email + password via Supabase Auth
- On login, role and tenant are read from `profiles`
- Persistent session on Android and Web
- Admin can create/deactivate accounts from a Users panel
- Password reset via Supabase email flow

### 6.2 Scheduling

- Calendar view (day / week / month) per clinic
- Admin and Receptionist can create/edit/cancel appointments for any dentist
- Dentist can view their own schedule only — no booking or editing
- Flexible duration set per appointment at booking time
- Color-coded by dentist or by status

### 6.3 Patient Management

- Full patient profile: demographics, medical history, allergies, systemic
  conditions
- Patient list: search, filter by clinic, filter by status, filter by dentist
- Each patient has a unified timeline: procedure cards + appointments +
  payments
- Receptionist creates patient (basic info); dentist fills clinical detail

### 6.4 Procedure Cards (Clinical Records)

- One card = one treatment course for one patient
- Links to: patient, clinic, dentist, clinical procedure + optional sub-type
- Tracks: tooth number, material, treatment steps with completion +
  timestamps + photos, notes
- Financial fields: treatment fee, lab fees, payments, associate cut,
  clinic share
- Status flow: In Progress → Completed or Canceled

### 6.5 Procedures Library

- System-wide templates seeded by default (Endo, Fixed Prostho, Operative,
  Oral Surgery, etc.)
- Admin can add tenant-specific custom procedures with types, materials,
  steps, default fees
- Dentist selects from library when creating a procedure card

### 6.6 Payments & Finances

- Log payments against a procedure card
- Auto-calculate associate cut and clinic share on every payment
- Formula: if `deduct_lab_fees` → `associate_cut = (paid − lab_fees) ×
  percentage`; else `associate_cut = paid × percentage`
- Per-dentist summary: earned, pending, lab fees
- Per-clinic summary: total revenue, clinic share, outstanding

### 6.7 Business Analytics (Admin only)

- Revenue by clinic (monthly / quarterly / yearly)
- Revenue per dentist
- Most performed procedures
- Patient growth over time
- Outstanding balances across the practice
- Appointment completion rate vs cancellations / no-shows

### 6.8 Medical Files & Attachments

- Free-text clinical notes per patient
- Upload X-rays and images stored in Supabase Storage
- File type tagging: note, X-ray, image, document

## 7. Role × Feature Access Matrix

| Feature | Admin | Dentist | Receptionist |
|---|---|---|---|
| View full schedule | ✅ | ❌ | ✅ |
| View own schedule (read-only) | ✅ | ✅ | — |
| Book / edit / cancel appointments | ✅ | ❌ | ✅ |
| Create patient profile | ✅ | ✅ | ✅ (basic only) |
| Edit patient medical info | ✅ | ✅ | ❌ |
| Create procedure cards | ✅ | ✅ | ❌ |
| Log payments | ✅ | ✅ | ✅ |
| Edit treatment fee | ✅ | ✅ | ✅ |
| Edit lab fees | ✅ | ✅ | ✅ |
| Edit percentage | ✅ | ❌ | ❌ |
| View payment records | ✅ | ✅ | ✅ (no analytics) |
| View all financials | ✅ | ❌ | ❌ |
| Business analytics | ✅ | ❌ | ❌ |
| Manage procedure templates | ✅ | ❌ | ❌ |
| Manage clinics | ✅ | ❌ | ❌ |
| Manage users | ✅ | ❌ | ❌ |
| Upload medical files | ✅ | ✅ | ❌ |
| Delete records (soft) | ✅ | ❌ | ❌ |

## Technical Architecture & Conventions

### Tech Stack

| Layer | Technology |
|---|---|
| Android App | Kotlin + Jetpack Compose |
| Web App | React (TypeScript) |
| Backend / Database | Supabase (PostgreSQL + Auth + Storage + Realtime) |
| Auth | Supabase Auth (email/password, role-based) |
| File Storage | Supabase Storage (X-rays, medical images, photos) |
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
- Never use Supabase service role key on the client

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

## 9. Coding Conventions

### General

- Every table has `tenant_id` — all queries filter by it; RLS enforces it
- All IDs: SERIAL integers for domain tables, UUID for profiles and tenants
- All timestamps: `TIMESTAMPTZ` (stored UTC, displayed in local timezone)
- Deletion: soft delete (`deleted_at`) for patients and procedure cards;
  hard delete for steps and payments
- Never use Supabase service role key on the client

### Android

- MVVM: Composable → ViewModel → Repository → Supabase SDK
- One ViewModel per feature domain
- `StateFlow` for UI state, `viewModelScope` for coroutines
- No business logic in Composables

### Web (React)

- React Query for all data fetching and caching
- One custom hook per domain: `usePatients`, `useSchedule`,
  `useProcedureCards`, etc.
- TypeScript strict mode throughout
- Folder structure: `/features/patients/`, `/features/schedule/`,
  `/features/finance/`, etc.
- i18next for AR/EN with RTL layout switching on `dir="rtl"`

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

**Version**: 1.4.0 | **Ratified**: 2026-06-11 | **Last Amended**: 2026-06-12
