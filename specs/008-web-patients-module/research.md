# Research: Web Patients Module

**Phase 0 Output** | **Date**: 2026-06-17

## Research Topics & Decisions

### 1. Data Fetching Strategy

**Decision**: React Query (`@tanstack/react-query`) with a single `usePatients` hook.

**Rationale**: Constitution §V mandates React Query for all web data fetching. One domain hook (`usePatients`) encapsulates all patient CRUD operations: list, getById, create, update, search, and filter. This pattern is already established in KIT-08 and matches the constitutional convention of "one custom hook per domain."

**Alternatives considered**: SWR (less mature ecosystem), direct Supabase SDK calls in components (violates separation of concerns).

---

### 2. Pagination Approach

**Decision**: Server-side pagination using Supabase `range()` with limit/offset parameters tracked in React state.

**Rationale**: The patient list can exceed 100 records for active practices. Client-side loading of all records would violate the <3s load target. Supabase `range(from, to)` maps to PostgreSQL `LIMIT/OFFSET`, allowing efficient page-sized queries. Page size defaults to 20 with user-configurable options (20/50/100).

**Alternatives considered**: Cursor-based pagination (overkill for this use case, no real-time insertion race condition concern), infinite scroll (adds complexity for modest list sizes).

---

### 3. Search Implementation

**Decision**: Supabase `ilike` query on `patients.name` column, debounced by 300ms.

**Rationale**: Case-insensitive name search via PostgreSQL `ILIKE` is simple, effective for name-based search, and works in both Arabic and English. 300ms debounce prevents excessive queries during typing.

**Alternatives considered**: Full-text search via `to_tsvector` (requires additional setup, language-specific stemming complicates Arabic support), client-side filtering (fails for large lists).

---

### 4. Filter Controls

**Decision**: Supabase `eq` and `in` filters for clinic, status, and dentist. Filters applied additively (AND logic). A dedicated `PatientsFilters` component manages filter state and passes it to the `usePatients` hook.

**Rationale**: Clinic and dentist filters use `eq` on foreign key columns. Status filter maps to `is_in_progress` boolean column (active = true, inactive = false). All filters are server-side for consistency with pagination.

**Alternatives considered**: Client-side filtering after loading all data (breaks pagination, fails performance targets).

---

### 5. Role-Gated UI for Patient Editing

**Decision**: The `AuthContext` (from KIT-08) exposes `role`. UI components conditionally render editable inputs vs. read-only displays based on role. RLS policies provide the server-side enforcement layer.

**Rationale**: Constitution §III requires "RLS at DB layer, never UI alone." The UI mirrors RLS constraints: medical history fields render as plain text for receptionists but as editable inputs for admin/dentist. The new patient form hides medical history fields entirely for receptionists (they only see name, phone, DOB, gender, clinic, dentist fields).

**Alternatives considered**: Separate pages per role (duplicates code), server-driven UI configuration (over-engineering for three roles).

---

### 6. Tabbed Patient Detail View

**Decision**: React state-driven tab switching with lazy loading. Each tab fetches its own data on activation.

**Rationale**: The patient detail page includes four tabs (Procedure Cards, Appointments, Payments, Medical Files). Loading all tab data upfront would be wasteful — most users view only one or two tabs. Lazy loading on tab activation minimizes initial load time and query volume.

**Alternatives considered**: Single page with all sections visible (too much data, poor UX), URL-driven tabs via nested routes (adds routing complexity for a simple tab pattern).

---

### 7. Form Handling & Optimistic Updates

**Decision**: Controlled React components with local state for form fields. React Query `useMutation` with `onMutate` optimistic update and `onError` rollback.

**Rationale**: Controlled components give full control over validation and submission state. React Query mutations with optimistic updates provide instant UI feedback — the patient list updates immediately on create/edit, and rolls back if the server rejects the change.

**Alternatives considered**: React Hook Form (adds dependency, not needed for simple form shapes), SWR mutations (less mature optimistic update API).

---

### 8. Translation Strategy

**Decision**: All user-facing strings in the patients module reference keys in the AR/EN translation JSON files set up in KIT-08. New keys are added under `patients.*` namespace.

**Rationale**: i18next `useTranslation` hook provides translation function in each component. Keys are organized by domain (`patients.list.title`, `patients.detail.medicalHistory`, etc.). English serves as the fallback language if a key is missing in Arabic.

**Alternatives considered**: Inline bilingual strings (not maintainable), separate component variants per language (duplicates logic).

---

### 9. Error Handling Pattern

**Decision**: React Query `isError` and `error` states drive user-facing error messages. Network failures display a retry button. Supabase RLS rejections (e.g., receptionist trying to edit medical fields) return a generic "Permission denied" message.

**Rationale**: The spec requires user-friendly errors, no stack traces. React Query's built-in error state handling provides a clean pattern. RLS errors are indistinguishable from other server errors at the client level (by design), so a generic message covers both.

**Alternatives considered**: Custom error boundaries (overkill for data-fetch errors), Sentry/error tracking (deferred to later phase).

---

## Summary

All technical decisions align with the constitutional conventions established in KIT-08 and KIT-03. No new dependencies or architectural patterns are introduced. The patients module follows the same React Query + Supabase + i18next stack as the scaffold, extending it with domain-specific hooks, filter patterns, and role-gated UI components.
