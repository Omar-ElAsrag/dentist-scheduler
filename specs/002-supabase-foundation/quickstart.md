# Quickstart: Supabase Foundation & Data Layer Migration

**Feature**: 002-supabase-foundation
**Date**: 2026-06-11

Validation scenarios that prove the Supabase foundation is correctly set up end-to-end. Run these after completing all tasks in `tasks.md`.

## Prerequisites

- Supabase project created and accessible via dashboard
- Supabase CLI installed (optional, for local migration application)
- Android Studio with the project open
- Two test user accounts created (one admin, one dentist) in different tenants

## Scenario 1: Schema Verification

**Purpose**: Confirm all 13 tables exist with correct structure.

```sql
-- Run in Supabase SQL editor
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
```

**Expected**: 13 tables listed: `appointments`, `clinical_procedure_steps`, `clinical_procedures`, `clinics`, `dentist_clinics`, `medical_files`, `patients`, `procedure_card_steps`, `procedure_cards`, `procedure_payments`, `procedure_types`, `profiles`, `tenants`.

## Scenario 2: Auto-Update Timestamp Trigger

**Purpose**: Confirm `updated_at` updates automatically on row modification.

```sql
-- Insert a test tenant
INSERT INTO tenants (name) VALUES ('Test Practice');

-- Wait 2 seconds, then update
UPDATE tenants SET name = 'Updated Practice'
WHERE name = 'Test Practice';

-- Check timestamps
SELECT created_at, updated_at FROM tenants WHERE name = 'Updated Practice';
```

**Expected**: `updated_at` is later than `created_at`.

**Cleanup**: `DELETE FROM tenants WHERE name = 'Updated Practice';`

## Scenario 3: Seed Data Verification

**Purpose**: Confirm seed data produces 9 clinical procedures.

```sql
-- Run seed.sql first, then:
SELECT id, name, has_types, default_fee
FROM clinical_procedures
WHERE tenant_id IS NULL
ORDER BY display_order;
```

**Expected**: 9 rows returned with procedure names, types flags, and fees.

## Scenario 4: Cross-Tenant Isolation (RLS)

**Purpose**: Confirm a user from tenant A cannot read tenant B's data.

```sql
-- Setup: Create two tenants with users
-- (Use Supabase Auth dashboard to create test users)

-- Test: Authenticate as tenant A's dentist, then query:
SET LOCAL ROLE authenticated;
-- Simulate tenant A's JWT context
SELECT * FROM patients; -- Should only show tenant A's patients

-- Attempt to read tenant B's data:
SELECT * FROM patients
WHERE tenant_id = (SELECT id FROM tenants WHERE name = 'Tenant B');
```

**Expected**: Zero rows returned for tenant B's data.

## Scenario 5: Role-Based Access (RLS)

**Purpose**: Confirm each role sees only what they're allowed to.

```sql
-- As receptionist:
SELECT * FROM medical_files;
-- Expected: 0 rows (receptionist has no access)

-- As dentist:
SELECT * FROM procedure_cards;
-- Expected: Only cards where dentist_id = current user

-- As admin:
SELECT * FROM profiles;
-- Expected: All profiles within the admin's tenant
```

## Scenario 6: Android SDK Initialization

**Purpose**: Confirm the Supabase client initializes on app launch.

1. Build and run the Android app on an emulator
2. Check Logcat for Supabase client initialization logs
3. Verify no crash on startup

**Expected**: App launches successfully. Supabase client connects to the project. No errors in Logcat.

## Scenario 7: Authentication Flow

**Purpose**: Confirm login, session persistence, and sign out work end-to-end.

1. Launch the app — login screen should appear (no existing session)
2. Enter valid email + password → tap Sign In
3. Verify: navigates to main app, profile loaded with role + tenant_id
4. Kill and restart the app — should skip login (session persisted)
5. Tap Sign Out in Settings → verify returns to login screen
6. Enter invalid credentials → verify error message displayed

**Expected**: All 6 steps pass without crashes.

## Scenario 8: Data Layer Migration Verification

**Purpose**: Confirm all screens load data from Supabase with tenant_id populated.

1. Log in as a dentist
2. Open each screen: Schedule, Patients, Clinics, Procedures, Analytics, Profile
3. On each screen, verify data loads (may be empty for fresh tenant)
4. Create a new patient → verify it appears in Supabase dashboard with `tenant_id` set
5. Create a procedure card → verify `tenant_id` is set
6. Log a payment → verify associate cut recalculated correctly
7. Search codebase for `import androidx.room` → should return zero results

**Expected**: All screens functional. All inserts have `tenant_id`. Zero Room imports.

## Scenario 9: Soft Delete Verification

**Purpose**: Confirm soft delete works for patients and procedure cards.

1. Create a patient, then delete it from the app
2. Check Supabase dashboard: patient row still exists with `deleted_at` populated
3. Create a procedure card, then cancel/delete it
4. Check: card row still exists with `deleted_at` populated
5. Delete a procedure card step → verify it is hard-deleted (row gone)

**Expected**: Patients and cards soft-deleted. Steps hard-deleted.
