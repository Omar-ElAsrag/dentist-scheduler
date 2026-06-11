# Implementation Plan: Repository & Project Structure

**Branch**: `001-repo-project-setup` | **Date**: 2026-06-11 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-repo-project-setup/spec.md`

## Summary

Establish the DentSched monorepo structure with four top-level directories (`app`, `web`, `supabase`, `docs`), a comprehensive `.gitignore`, developer onboarding documentation (README + `.env.example`), and Supabase scaffolding. This is a pure infrastructure feature — no application code, no database, no auth. The goal is to give every developer a consistent, discoverable project layout before any feature work begins.

## Technical Context

**Language/Version**: Kotlin (Android), TypeScript (Web)

**Primary Dependencies**: Jetpack Compose + Supabase Kotlin SDK (Android); React + TanStack Query + Supabase JS SDK (Web)

**Storage**: N/A (repo scaffolding only — no data layer in this feature)

**Testing**: N/A (no code to test in this feature)

**Target Platform**: Android (mobile app), Web (browser)

**Project Type**: Mobile app + Web app (monorepo)

**Performance Goals**: N/A

**Constraints**: Existing Android project must be moved into `/app` without breaking its build configuration (Gradle paths, resource references, signing configs)

**Scale/Scope**: 1 monorepo, 2 app projects (Android + Web), 1 Supabase scaffold, 1 developer

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **I. Multi-Tenant Isolation**: N/A — no queries or data access in this feature. No `tenant_id` required.
- [x] **II. Supabase-First**: N/A — Supabase project not yet created. No client SDK usage in this feature.
- [x] **III. Role-Gated Access**: N/A — no authentication or authorization code in this feature.
- [x] **IV. Clinical Integrity**: N/A — no clinical data, procedure cards, or payments in this feature.
- [x] **V. Separation of Concerns**: ✅ PASS — repo structure (`app`/`web`/`supabase`/`docs`) aligns with platform separation principle. Each platform is isolated in its own directory.

**Result**: All gates pass. No violations. Complexity Tracking section not required.

## Project Structure

### Documentation (this feature)

```text
specs/001-repo-project-setup/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (repo conventions)
├── quickstart.md        # Phase 1 output (validation guide)
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (not created by /speckit.plan)
```

### Source Code (repository root)

```text
/
├── app/                 # Android app (Kotlin + Jetpack Compose)
│   ├── src/
│   ├── build.gradle.kts
│   └── ...
├── web/                 # React web app (TypeScript)
│   ├── src/
│   │   ├── features/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── lib/
│   │   └── locales/
│   ├── package.json
│   └── ...
├── supabase/            # Supabase configuration
│   ├── migrations/      # Database migrations
│   └── seed.sql         # Seed data
├── docs/                # Project documentation
│   └── implementation-plan.md
├── .gitignore           # Root gitignore
├── .env.example         # Root environment variables template
└── README.md            # Project overview + setup instructions
```

**Structure Decision**: Monorepo with platform-isolated directories. Android lives in `/app`, web lives in `/web`, Supabase config in `/supabase`, and documentation in `/docs`. This matches the constitution's platform separation principle and enables clear ownership boundaries.

## Complexity Tracking

No violations to justify.
