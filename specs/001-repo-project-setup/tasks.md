# Tasks: Repository & Project Structure

**Input**: Design documents from `/specs/001-repo-project-setup/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, quickstart.md

**Tests**: Not requested in the feature specification. Test tasks are omitted.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Monorepo root**: `/` (repository root)
- **Android app**: `app/`
- **Web app**: `web/`
- **Supabase config**: `supabase/`
- **Documentation**: `docs/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the top-level directory structure and move existing code into place.

- [x] T001 Create `app/` directory at repository root
- [x] T002 Move existing Android project files (src/, build.gradle.kts, proguard-rules.pro, etc.) into `app/`
- [x] T003 Update root `settings.gradle.kts` to reference `app` module if not already configured
- [x] T004 [P] Create `web/` directory at repository root
- [x] T005 [P] Create `web/src/` directory with subdirectories: `features/`, `components/`, `hooks/`, `lib/`, `locales/`
- [x] T006 [P] Create `web/src/locales/ar/` and `web/src/locales/en/` directories for i18n translation files
- [x] T007 [P] Create `supabase/` directory at repository root
- [x] T008 [P] Create `supabase/migrations/` directory for SQL migration files
- [x] T009 [P] Create placeholder `supabase/seed.sql` with a comment header: `-- DentSched seed data`
- [x] T010 [P] Ensure `docs/` directory exists at repository root

**Checkpoint**: All four top-level directories (`app`, `web`, `supabase`, `docs`) exist.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure files that all user stories depend on.

**CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T011 Create `.gitignore` at repository root covering: secrets, Android build, Android Studio, web dependencies, web build, web cache, TypeScript, Supabase local state, OS files, IDE files, logs, temp, testing, and binary assets (see data-model.md for full rule categories)
- [ ] T012 [P] Create `.env.example` at repository root with `SUPABASE_URL` and `SUPABASE_ANON_KEY` placeholder values
- [ ] T013 [P] Create `web/.env.example` with `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY` placeholder values

**Checkpoint**: `.gitignore` exists and covers all categories. Both `.env.example` files exist with placeholder values.

---

## Phase 3: User Story 1 - Consistent Monorepo Structure (Priority: P1)

**Goal**: A developer can clone the repo and immediately identify which directory contains the Android code, which contains the web code, and which contains the database configuration.

**Independent Test**: List top-level directories and verify `app/`, `web/`, `supabase/`, and `docs/` all exist with expected contents.

### Implementation for User Story 1

- [ ] T014 [US1] Verify `app/build.gradle.kts` exists and references correct Android SDK and dependencies
- [ ] T015 [US1] Verify `app/src/main/AndroidManifest.xml` exists with correct package name
- [ ] T016 [US1] Open `app/` in Android Studio and confirm Gradle sync completes without errors
- [ ] T017 [US1] Create `web/package.json` with project name, scripts (dev, build, preview), and dependency placeholders for React, TypeScript, Vite, Supabase JS SDK, TanStack Query, react-router-dom, i18next, and Tailwind CSS
- [ ] T018 [US1] Create `web/vite.config.ts` with React plugin and base configuration
- [ ] T019 [US1] Create `web/tsconfig.json` with strict mode enabled
- [ ] T020 [US1] Create `web/index.html` as the Vite entry point
- [ ] T021 [US1] Create `web/src/main.tsx` as the React app entry point
- [ ] T022 [US1] Run `npm install` in `web/` and verify it completes without errors
- [ ] T023 [US1] Verify `supabase/migrations/` directory exists (may be empty at this stage)
- [ ] T024 [US1] Verify `supabase/seed.sql` exists with placeholder content
- [ ] T025 [US1] Verify `docs/` directory contains `implementation-plan.md`

**Checkpoint**: All four directories contain their expected scaffolding. Android project syncs. Web project installs.

---

## Phase 4: User Story 2 - Secret and Build Output Protection (Priority: P2)

**Goal**: Secrets, credentials, and build artifacts are automatically excluded from version control.

**Independent Test**: Create dummy `.env`, build the Android and web projects, then run `git status` — no untracked build artifacts or secrets should appear.

### Implementation for User Story 2

- [ ] T026 [US2] Verify `.gitignore` contains secrets section: `.env`, `.env.*`, `*.keystore`, `*.jks`, `keystore.properties`, `google-services.json`, `supabase/.env`, `supabase/config.toml.local`
- [ ] T027 [US2] Verify `.gitignore` contains Android section: `.gradle/`, `build/`, `**/build/`, `local.properties`, `*.iml`, `.idea/`, `captures/`, `.externalNativeBuild/`, `.cxx/`
- [ ] T028 [US2] Verify `.gitignore` contains web section: `node_modules/`, `web/node_modules/`, `web/dist/`, `web/build/`, `web/.next/`, `web/out/`, `web/.cache/`, `web/.vite/`, `*.tsbuildinfo`
- [ ] T029 [US2] Verify `.gitignore` contains Supabase section: `supabase/.branches/`, `supabase/.temp/` with exceptions for `supabase/migrations/`, `supabase/seed.sql`, `supabase/config.toml`
- [ ] T030 [US2] Verify `.gitignore` contains OS files section: `.DS_Store`, `Thumbs.db`, `Desktop.ini`, `$RECYCLE.BIN/`
- [ ] T031 [US2] Verify `.gitignore` contains IDE section: `.vscode/` (with exceptions for `extensions.json` and `settings.json`), `.idea/`, `*.iml`, `*.ipr`, `*.iws`
- [ ] T032 [US2] Verify `.gitignore` contains binary assets section: `*.png`, `*.jpg`, `*.gif`, `*.mp4`, `*.pdf` with exceptions for `**/res/**`, `web/public/**`, `docs/**`
- [ ] T033 [US2] Create a dummy `.env` file at repo root and run `git status` — confirm it does not appear as untracked
- [ ] T034 [US2] Create dummy `build/` and `web/dist/` directories and run `git status` — confirm they do not appear as untracked
- [ ] T035 [US2] Remove all dummy test files created during verification

**Checkpoint**: `git status` shows zero untracked secrets, build outputs, or IDE files after dummy file creation.

---

## Phase 5: User Story 3 - Developer Onboarding Documentation (Priority: P3)

**Goal**: A new developer can follow the README from a fresh clone to a running development environment in under 30 minutes.

**Independent Test**: Read the README end-to-end and verify every step is actionable and every referenced file exists.

### Implementation for User Story 3

- [ ] T036 [US3] Write `README.md` project title and one-paragraph description: "DentSched is a multi-tenant dental practice management SaaS with Android and Web clients backed by Supabase."
- [ ] T037 [US3] Add technology stack summary section to `README.md`: Kotlin + Jetpack Compose (Android), React + TypeScript (Web), Supabase (Backend)
- [ ] T038 [US3] Add prerequisites section to `README.md`: Android Studio, Node.js 18+, npm, Supabase CLI (optional for local dev)
- [ ] T039 [US3] Add Android setup instructions to `README.md`: open `app/` in Android Studio, copy `.env.example` to `local.properties`, sync Gradle, run on emulator
- [ ] T040 [US3] Add Web setup instructions to `README.md`: `cd web && npm install`, copy `web/.env.example` to `web/.env`, `npm run dev`
- [ ] T041 [US3] Add project structure section to `README.md` showing the directory tree (app, web, supabase, docs)
- [ ] T042 [US3] Verify `.env.example` at root lists `SUPABASE_URL` and `SUPABASE_ANON_KEY` with descriptive comments
- [ ] T043 [US3] Verify `web/.env.example` lists `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY` with descriptive comments
- [ ] T044 [US3] Cross-check README: every file and directory referenced in setup instructions actually exists in the repo

**Checkpoint**: README is complete, accurate, and every referenced path exists.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup across all user stories.

- [ ] T045 Run quickstart scenario 1: verify all four top-level directories exist (`app`, `web`, `supabase`, `docs`)
- [ ] T046 Run quickstart scenario 2: create dummy `.env` and build outputs, verify `git status` is clean
- [ ] T047 Run quickstart scenario 5: verify both `.env.example` files contain placeholder values and no real secrets
- [ ] T048 Run quickstart scenario 7: verify README contains all required sections (overview, stack, prerequisites, setup)
- [ ] T049 Remove any temporary or test files created during validation
- [ ] T050 Final `git status` check: confirm only intended files are tracked

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) — May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) — References US1 and US2 artifacts but should be independently testable

### Within Each User Story

- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel (T004–T010)
- All Foundational tasks marked [P] can run in parallel within Phase 2 (T012, T013)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- Within US1: web scaffold tasks (T017–T021) can run in parallel with Android verification (T014–T016)

---

## Parallel Example: Phase 1 Setup

```bash
# Launch all independent setup tasks together:
Task: "T004 [P] Create web/ directory at repository root"
Task: "T005 [P] Create web/src/ directory with subdirectories"
Task: "T006 [P] Create web/src/locales/ar/ and web/src/locales/en/"
Task: "T007 [P] Create supabase/ directory at repository root"
Task: "T008 [P] Create supabase/migrations/ directory"
Task: "T009 [P] Create placeholder supabase/seed.sql"
Task: "T010 [P] Ensure docs/ directory exists"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1 (Monorepo Structure)
4. **STOP and VALIDATE**: Test User Story 1 independently (list dirs, open Android, start web)
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Verify structure (MVP!)
3. Add User Story 2 → Test independently → Verify gitignore
4. Add User Story 3 → Test independently → Verify README
5. Each story adds value without breaking previous stories

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- The `.gitignore` file was partially created in an earlier session — verify it matches all categories in data-model.md before proceeding with US2 verification tasks
