# Implementation Plan: Web App Scaffold

**Branch**: `kit-08-web-scaffold` | **Date**: 2026-06-15 | **Spec**: [spec.md](spec.md)

**Input**: KIT-08 from `docs/implementation-plan.md` — feature specification at `specs/007-web-app-scaffold/spec.md`

## Summary

Scaffold the React + TypeScript web app under `/web` using Vite. Wire up Supabase Auth for login, React Router for protected routing, i18next for Arabic/English with RTL support, and TailwindCSS for styling. Create a role-filtered sidebar shell that all future feature pages (KIT-09+) plug into.

## Technical Context

**Language/Version**: TypeScript 5.x (strict mode), React 18+

**Primary Dependencies**: Vite (build tool), @supabase/supabase-js (auth + data), @tanstack/react-query (data fetching), react-router-dom (routing), i18next + react-i18next (internationalization), tailwindcss (styling)

**Storage**: Supabase PostgreSQL (existing backend, shared with Android app)

**Testing**: Manual verification via `npm run dev` + browser

**Target Platform**: Web browser — Chrome, Firefox, Safari (desktop + mobile)

**Project Type**: Single-page web application (React SPA)

**Performance Goals**: Login to dashboard < 5s, language switch < 200ms, 60 fps interactions

**Constraints**: Must share Supabase backend with Android app; no server-side rendering in Phase 1; RTL layout for Arabic; TypeScript strict mode required

**Scale/Scope**: App shell only — login, auth context, sidebar, header, language toggle, protected routing. Feature pages (Patients, Schedule, etc.) are out of scope for this kit.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **I. Multi-Tenant Isolation**: Supabase JS SDK queries will filter by `tenant_id` from auth context. RLS policies already enforce tenant isolation server-side — web client has no bypass. Same backend as Android.
- [x] **II. Supabase-First**: No local DB, no alternate backend. Supabase JS SDK is the sole data layer. Service role key will not be present in Vite client bundle (anon key only).
- [x] **III. Role-Gated Access**: Sidebar links filtered by role. Auth context exposes role from Supabase `profiles` table. Full feature-level gates per §7 matrix applied in KIT-09+.
- [x] **IV. Clinical Integrity**: Not applicable to scaffold — no clinical data modified. Feature pages (KIT-09+) will enforce clinical lifecycle rules.
- [x] **V. Separation of Concerns**: React Query hooks for data, context providers for auth/i18n, feature modules under `src/features/`, shared components under `src/components/`. No business logic in UI components.

## Project Structure

### Documentation (this feature)

```text
specs/007-web-app-scaffold/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (to be created under /web)

```text
web/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── tailwind.config.js
├── postcss.config.js
├── .env.example
├── src/
│   ├── main.tsx                    # App entry point
│   ├── App.tsx                     # Router + providers
│   ├── lib/
│   │   ├── supabase.ts             # Supabase client singleton
│   │   └── i18n.ts                 # i18next setup (AR + EN)
│   ├── context/
│   │   └── AuthContext.tsx          # Session + role + tenantId
│   ├── components/
│   │   ├── Layout.tsx              # Sidebar + header shell
│   │   ├── Sidebar.tsx             # Role-filtered nav links
│   │   ├── Header.tsx              # User name + language toggle
│   │   └── ProtectedRoute.tsx      # Auth guard wrapper
│   ├── pages/
│   │   └── LoginPage.tsx           # Email/password form
│   ├── features/
│   │   ├── patients/               # KIT-09
│   │   ├── schedule/               # KIT-10
│   │   ├── procedures/             # KIT-11
│   │   ├── finance/                # KIT-11
│   │   ├── analytics/              # later
│   │   └── settings/               # later
│   ├── hooks/
│   │   └── useAuth.ts             # Auth convenience hook
│   └── locales/
│       ├── en/
│       │   └── translation.json    # English labels
│       └── ar/
│           └── translation.json    # Arabic labels
```

**Structure Decision**: Single Vite React SPA under `/web/`. Feature modules under `src/features/` per constitution web conventions. No monorepo tooling needed — the web directory is a standalone Vite project that shares only the Supabase backend and `/specs/` documentation with the Android app.

## Complexity Tracking

No constitution violations. All checks pass.
