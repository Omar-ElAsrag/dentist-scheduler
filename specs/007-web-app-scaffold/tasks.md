# Tasks: Web App Scaffold

**Input**: Design documents from `specs/007-web-app-scaffold/`

**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: Manual verification only per quickstart.md — no automated tests in scope.

**Organization**: Tasks grouped by user story per spec.md priorities (P1 → P2 → P3).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story label (US1, US2, US3)
- Exact file paths in descriptions

---

## Phase 1: Setup (Project Initialization)

**Purpose**: Scaffold the Vite + React + TypeScript project under `/web`, install all dependencies, and configure tooling.

- [X] T001 Scaffold Vite project: run `npm create vite@latest web -- --template react-ts` from repo root `D:\dentist-scheduler`
- [X] T002 Install core dependencies: run `npm install @supabase/supabase-js @tanstack/react-query react-router-dom i18next react-i18next` in `D:\dentist-scheduler\web`
- [X] T003 Install dev dependencies: run `npm install -D tailwindcss @tailwindcss/vite` in `D:\dentist-scheduler\web`
- [X] T004 [P] Create `web/.env.example` with `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY` placeholder values
- [X] T005 Configure TailwindCSS: update `web/vite.config.ts` to add `@tailwindcss/vite` plugin; replace `web/src/index.css` with `@import "tailwindcss"`

**Checkpoint**: `npm run dev` starts without errors — blank Vite page at `http://localhost:5173`

---

## Phase 2: Foundational (Supabase Client + i18n + Context Providers)

**Purpose**: Core libraries and configuration that all user stories depend on. Must complete before any UI work.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T006 Create `web/src/lib/supabase.ts` — Supabase client singleton using `createClient(url, anonKey)` from env vars
- [X] T007 [P] Create `web/src/lib/i18n.ts` — i18next setup with `init()` configuring AR and EN resources, fallback to EN, detection from localStorage
- [X] T008 [P] Create `web/src/context/AuthContext.tsx` — React context exposing `session`, `user`, `role`, `tenantId`, `fullName`, `login()`, and `logout()` functions. On mount, call `supabase.auth.getSession()` to restore session. Subscribe to `onAuthStateChange`. After auth, read `profiles` table to populate role/tenantId/name.
- [X] T009 [P] Create `web/src/hooks/useAuth.ts` — convenience hook wrapping `useContext(AuthContext)`
- [X] T010 Create English translations: `web/src/locales/en/translation.json` with keys for `nav` (patients, schedule, procedures, analytics, associates, settings), `auth` (login, logout, email, password, loginError), and `header` (languageToggle, greeting)
- [X] T011 [P] Create Arabic translations: `web/src/locales/ar/translation.json` with Arabic equivalents of all keys in T010

**Checkpoint**: Supabase client, i18next, auth context, and translation files all in place — UI components can now be built.

---

## Phase 3: User Story 1 — User Logs In on the Web (Priority: P1) 🎯 MVP

**Goal**: Unauthenticated users see a login page. After entering credentials, they are authenticated and redirected to a dashboard with role-appropriate sidebar links. Invalid credentials show an error.

**Independent Test**: Open `http://localhost:5173`, enter valid admin credentials → dashboard with all 6 sidebar links. Log out, log in as receptionist → only Patients and Schedule links visible.

### Implementation for User Story 1

- [X] T012 [US1] Create `web/src/pages/LoginPage.tsx` — email/password form using `useAuth().login()`. Shows error message on auth failure. Redirects to `/` on success via React Router.
- [X] T013 [US1] Create `web/src/components/ProtectedRoute.tsx` — wrapper component that reads auth state from `useAuth()`. If no session, redirects to `/login`. Otherwise renders children.
- [X] T014 [US1] Create `web/src/components/Sidebar.tsx` — renders a list of navigation links filtered by the user's role. Links include Patients, Schedule, Procedures (admin+dentist), Analytics, Associates, Settings (admin only). Each link uses `react-router-dom` `<NavLink>` with active state styling.
- [X] T015 [US1] Create `web/src/components/Layout.tsx` — shell component with Sidebar on the left and `<Outlet />` for page content on the right. Wraps children with `ProtectedRoute`.
- [X] T016 [US1] Create `web/src/App.tsx` — set up `react-router-dom` `BrowserRouter` with routes: `/login` (LoginPage, public), `/` and all feature routes (Layout wrapper, protected). Wrap everything in `AuthContext.Provider`, `QueryClientProvider`, and i18next `I18nextProvider`.
- [X] T017 [US1] Create `web/src/main.tsx` — entry point rendering `<App />` into the DOM. Import `index.css`.

**Checkpoint**: Login works for all 3 roles. Sidebar links filtered per role. Protected routes redirect to login.

---

## Phase 4: User Story 2 — Language Toggle (AR/EN + RTL) (Priority: P2)

**Goal**: User clicks a language toggle in the header. All UI text switches between Arabic and English. Page direction toggles between RTL and LTR. Preference persists across sessions.

**Independent Test**: Log in, click language toggle → Arabic labels + RTL layout. Refresh page → Arabic persists. Click again → English + LTL.

### Implementation for User Story 2

- [X] T018 [US2] Create `web/src/components/Header.tsx` — displays user name from `useAuth().user` and a language toggle button. On toggle click, call `i18next.changeLanguage()` and set `document.dir` to `rtl` or `ltr` based on new language.
- [X] T019 [US2] Wire Header into Layout: update `web/src/components/Layout.tsx` to render `<Header />` above the `<Sidebar />` + `<Outlet />` area.
- [X] T020 [US2] Add RTL initialization: update `web/src/lib/i18n.ts` to set `document.dir` and `document.documentElement.lang` on initial load based on stored language preference.
- [X] T021 [US2] Update `web/src/main.tsx` — import i18n configuration before rendering App to ensure translations are loaded before first render.
- [X] T022 [US2] Add TailwindCSS RTL utilities: ensure TailwindCSS logical property classes (e.g., `me-*`/`ms-*` instead of `ml-*`/`mr-*`) are used throughout Sidebar and Header for correct RTL layout.

**Checkpoint**: Language toggle switches UI text and layout direction. Preference persists across page refreshes.

---

## Phase 5: User Story 3 — App Shell Provides Consistent Layout (Priority: P3)

**Goal**: Every protected page shows a consistent sidebar + header shell. On mobile-width screens, the sidebar collapses to a hamburger menu. Logout clears the session and returns to login.

**Independent Test**: Login → sidebar + header visible on all routes. Resize to <768px → hamburger menu. Click links → content area updates. Click logout → redirected to login.

### Implementation for User Story 3

- [X] T023 [US3] Add responsive sidebar: update `web/src/components/Sidebar.tsx` to collapse to hamburger menu at <768px viewport width. Use React state for open/close. Overlay on mobile, inline on desktop.
- [X] T024 [US3] Add logout button: update `web/src/components/Sidebar.tsx` or `web/src/components/Header.tsx` with a logout button that calls `useAuth().logout()` and navigates to `/login`.
- [X] T025 [US3] Create placeholder pages: create stub files under `web/src/features/` — `patients/PatientsPage.tsx`, `schedule/SchedulePage.tsx`, `procedures/ProceduresPage.tsx`, `analytics/AnalyticsPage.tsx`, `settings/SettingsPage.tsx`. Each renders a simple `<div>` with the page name (e.g., "Patients — coming soon").
- [X] T026 [US3] Wire placeholder routes: update `web/src/App.tsx` to add routes for each feature page (all wrapped in ProtectedRoute + Layout).

**Checkpoint**: Consistent app shell across all routes. Responsive sidebar on mobile. Logout works. All navigation links load their placeholder page.

---

## Phase 6: Polish & Verification

**Purpose**: Build validation, regression check, and quickstart scenario verification.

- [X] T027 Verify `npm run dev` starts without errors in `D:\dentist-scheduler\web`
- [ ] T028 Run all 12 quickstart scenarios from `specs/007-web-app-scaffold/quickstart.md` — verify VS-01 through VS-12
- [ ] T029 Verify no regression: Android app (`./gradlew assembleDebug`) still builds successfully (web app is separate, no shared Kotlin code)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — T001 first (scaffolds project), then T002-T005 in parallel
- **Foundational (Phase 2)**: Depends on Phase 1. T006 (supabase) must come first. T007-T011 can run in parallel after T006.
- **US1 (Phase 3)**: Depends on Phase 2. T013 (ProtectedRoute) → T014 (Sidebar) → T015 (Layout) → T016 (App) → T017 (main). T012 (LoginPage) parallel with T013.
- **US2 (Phase 4)**: Depends on Phase 2 and Phase 3 (needs Layout from US1). T018-T022 within this phase.
- **US3 (Phase 5)**: Depends on Phase 2, Phase 3 (sidebar, layout), Phase 4 (header). T023-T026 within this phase.
- **Polish (Phase 6)**: Depends on all phases.

### User Story Dependencies

| Story | Depends On | Can Parallel With |
|-------|-----------|-------------------|
| US1 (P1) | Phase 2 | — |
| US2 (P2) | Phase 2 + US1 (Layout, Sidebar) | — |
| US3 (P3) | Phase 2 + US1 + US2 | — |

### Within Each Phase

- T001 → T002, T003, T004, T005 (scaffold first, then parallel installs)
- T006 → T007 through T011 (supabase client first, rest parallel)
- T012 || T013 (parallel — different files)
- T013 → T014 → T015 → T016 → T017 (sequential — each builds on previous)
- T018 → T019, T020, T021, T022 (Header first, rest parallel)
- T023, T024, T025, T026 (parallel after Header and Layout exist)

### Parallel Opportunities

- T004, T005 can run in parallel (different files)
- T007, T008, T009, T010, T011 can run in parallel after T006
- T012 and T013 can run in parallel (LoginPage vs ProtectedRoute)
- T023, T024, T025, T026 can run in parallel

---

## Parallel Example: Foundational Phase

```bash
# After T006 (Supabase client) is done, launch all in parallel:
Task: "T007: Create i18next config in web/src/lib/i18n.ts"
Task: "T008: Create AuthContext in web/src/context/AuthContext.tsx"
Task: "T009: Create useAuth hook in web/src/hooks/useAuth.ts"
Task: "T010: Create English translations in web/src/locales/en/translation.json"
Task: "T011: Create Arabic translations in web/src/locales/ar/translation.json"
```

## Parallel Example: US3 — App Shell Polish

```bash
# All four tasks modify different files, can run in parallel:
Task: "T023: Responsive sidebar in web/src/components/Sidebar.tsx"
Task: "T024: Logout button in web/src/components/Header.tsx"
Task: "T025: Placeholder pages in web/src/features/"
Task: "T026: Wire routes in web/src/App.tsx"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: US1 — Login + Auth + Role-Based Sidebar
4. **STOP and VALIDATE**: Test login for all 3 roles, verify sidebar filtering
5. This delivers a working authenticated web app — security gate is functional

### Incremental Delivery

1. Phase 1 + 2 → Project scaffold with all dependencies installed
2. + Phase 3 (US1) → Users can log in and see role-appropriate navigation
3. + Phase 4 (US2) → Arabic/English toggle with RTL layout
4. + Phase 5 (US3) → Responsive shell, logout, placeholder pages
5. + Phase 6 → Verified and ready for KIT-09 feature pages

### Fast Path

- T001 (scaffold), then T002-T005 in parallel
- T006 (supabase), then T007-T011 in parallel
- T012 || T013, then T014 → T015 → T016 → T017
- T018 → T019-T022 in parallel
- T023-T026 in parallel
- T027-T029 sequential

### Estimated Effort

| Phase | Tasks | Est. Time |
|-------|-------|-----------|
| Phase 1 (Setup) | 5 | 10 min |
| Phase 2 (Foundational) | 6 | 20 min |
| Phase 3 (US1) | 6 | 30 min |
| Phase 4 (US2) | 5 | 15 min |
| Phase 5 (US3) | 4 | 15 min |
| Phase 6 (Polish) | 3 | 10 min |
| **Total** | **29** | **~100 min** |

---

## Notes

- All paths under `web/` are relative to `D:\dentist-scheduler\web\`
- Supabase URL and anon key are the SAME as the Android app — reuse existing values
- The `profiles` table read in AuthContext must filter by `tenant_id` to support multi-tenancy
- TailwindCSS v4 uses `@tailwindcss/vite` Vite plugin — no separate `tailwind.config.js` needed beyond basic setup
- Placeholder pages in KIT-09 through KIT-12 will replace the stub pages created in T025
- No Supabase migrations needed — the web app uses the existing database schema
