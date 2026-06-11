# Research: Supabase Foundation & Data Layer Migration

**Feature**: 002-supabase-foundation
**Date**: 2026-06-11

## R1: Supabase Kotlin SDK Selection

**Decision**: Use the `supabase-kt` library by jan-tennert (`io.github.jan-tennert.supabase`) with modules: `postgrest-kt`, `auth-kt`, `storage-kt`, `realtime-kt`, backed by `io.ktor:ktor-client-android`.

**Rationale**: This is the most actively maintained Kotlin SDK for Supabase with first-class support for all Supabase products (PostgREST, Auth, Storage, Realtime). It uses Ktor as the HTTP engine, which is the recommended client for Kotlin Multiplatform and Android. The library supports coroutine-based APIs and integrates naturally with `StateFlow` for reactive UI.

**Alternatives considered**:
- **Official Supabase Kotlin SDK (supabase-kt by Supabase)**: Rejected — less mature, fewer community examples, less frequent updates at time of evaluation.
- **Direct PostgREST calls via Retrofit**: Rejected — loses Auth, Storage, and Realtime integration; requires manual JWT handling; more boilerplate.
- **Appwrite or Firebase**: Rejected — constitution mandates Supabase-First Backend (Principle II).

## R2: RLS Policy Architecture

**Decision**: Use a helper function `auth.user_tenant_id()` that returns the `tenant_id` from the authenticated user's `profiles` row. All RLS policies reference this function for tenant scoping. Role checks use a similar helper `auth.user_role()`.

**Rationale**: Centralizing tenant and role lookups in SQL functions avoids repeating JOIN logic across dozens of policies. It also makes policies easier to audit — every policy references the same function, so a bug in tenant isolation has a single fix point.

**Alternatives considered**:
- **Inline JOINs in every policy**: Rejected — verbose, error-prone, harder to audit.
- **Application-level tenant filtering only (no RLS)**: Rejected — violates Constitution Principle I (defense in depth requires server-enforced isolation).
- **Separate Supabase project per tenant**: Rejected — constitution mandates single project with `tenant_id` column.

## R3: Auth Profile Auto-Creation

**Decision**: Use a PostgreSQL trigger on `auth.users` that fires on INSERT and creates a corresponding row in `profiles` with `role = 'dentist'` as default. The admin can change the role after creation via the Users panel.

**Rationale**: Automatic profile creation ensures every authenticated user has a profile row immediately, preventing null-reference errors in RLS policies and application queries. Defaulting to `dentist` is the safest role — it has the least access, so a misconfigured user cannot accidentally access admin features.

**Alternatives considered**:
- **Create profile in application code after login**: Rejected — race condition if two requests fire simultaneously; RLS policies would fail before profile exists.
- **Default role = admin**: Rejected — security risk; a misconfigured user would have full access.
- **Require manual profile creation by admin before login**: Rejected — poor UX; blocks self-service onboarding.

## R4: Room-to-Supabase Migration Strategy

**Decision**: Create `SupabaseRepository.kt` with the same interface as the existing `DentistRepository`. Migrate one domain at a time (Clinics → Patients → Procedures → Cards → Steps → Payments → Medical Files), verifying each screen works after each domain migration. Remove Room dependency only after all domains are migrated.

**Rationale**: Incremental migration reduces risk — if one domain breaks, only that screen is affected and can be rolled back. Keeping the same interface means ViewModels need minimal changes. Verifying after each domain catches issues early.

**Alternatives considered**:
- **Big-bang migration (all domains at once)**: Rejected — too risky; if something breaks, debugging is harder across 7 domains.
- **Parallel repositories (keep Room + add Supabase side-by-side)**: Rejected — doubles maintenance burden; data sync between local and remote is complex and unnecessary.
- **Database migration tool (Flyway/Liquibase)**: Rejected — overkill for replacing a local DB; Supabase migrations handle the schema side.

## R5: Financial Calculation Placement

**Decision**: Keep `recalculateCard()` logic in the Android ViewModel (not as a database trigger) during Phase 1. The formula: if `deduct_lab_fees` then `associate_cut = (paid - lab_fees) * percentage`, else `associate_cut = paid * percentage`.

**Rationale**: ViewModel-level calculation is simpler to debug and test during the migration phase. Moving to a database trigger or Supabase Edge Function is deferred to a later phase when the system is stable. The constitution allows this as a temporary tradeoff.

**Alternatives considered**:
- **PostgreSQL trigger on `procedure_payments`**: Rejected for Phase 1 — harder to debug during migration; will be considered in Phase 3 or 4.
- **Supabase Edge Function**: Rejected for Phase 1 — adds deployment complexity; not needed while the app is the only client.
- **Client-side only (no recalculation on server)**: This is the chosen approach for Phase 1.

## R6: Seed Data Idempotency

**Decision**: Use `INSERT ... ON CONFLICT DO NOTHING` with a unique constraint on `(tenant_id, name)` for `clinical_procedures` (where `tenant_id IS NULL` for system-wide procedures). This makes `seed.sql` safe to run multiple times.

**Rationale**: Idempotent seed data prevents duplicate procedures if the seed is re-run during development or after a schema reset. Using `ON CONFLICT` is the standard PostgreSQL pattern for this.

**Alternatives considered**:
- **DELETE + INSERT**: Rejected — destructive; would lose any custom procedures if run accidentally.
- **Check existence before INSERT**: Rejected — more verbose, same result as `ON CONFLICT`.
- **Non-idempotent seed (fail on duplicate)**: Rejected — blocks re-seeding during development.
