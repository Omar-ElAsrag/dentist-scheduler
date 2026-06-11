# Feature Specification: Repository & Project Structure

**Feature Branch**: `001-repo-project-setup`

**Created**: 2026-06-11

**Status**: Draft

**Input**: User description: "Set up the monorepo structure, gitignore, and folder conventions before any code is written for the DentSched project (Android + Web + Supabase stack)."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consistent Monorepo Structure (Priority: P1)

As a developer joining the DentSched project, I want a clearly organised monorepo with separated directories for the Android app, the React web app, and Supabase configuration, so I can quickly locate the code I need to work on without guessing where things live.

**Why this priority**: The directory structure is the foundation every developer relies on. Without it, all subsequent work (building, deploying, collaborating) is slower and error-prone.

**Independent Test**: A developer can clone a fresh copy of the repo and identify which directory contains the Android code, which contains the web code, and which contains the database configuration within 30 seconds of opening the project.

**Acceptance Scenarios**:

1. **Given** a freshly cloned repository, **When** I list the top-level directories, **Then** I see at minimum `app`, `web`, `supabase`, and `docs`.
2. **Given** the repository structure, **When** I look inside the `app` directory, **Then** I find a complete Android project that can be opened in Android Studio.
3. **Given** the repository structure, **When** I look inside the `web` directory, **Then** I find a complete React / TypeScript project that can be started with a standard package manager command.

---

### User Story 2 - Secret and Build Output Protection (Priority: P2)

As a developer, I want secrets, credentials, and build artifacts automatically excluded from version control by a comprehensive `.gitignore`, so they never get accidentally committed into the repository history.

**Why this priority**: Accidentally committing credentials or signing keys is a security incident. Prevention via `.gitignore` is the simplest and most reliable defence.

**Independent Test**: After running the standard build commands for both the Android and web projects, `git status` shows zero build artifacts, credentials, or environment files as untracked.

**Acceptance Scenarios**:

1. **Given** a developer has created a `.env` file at the repo root with dummy credentials, **When** they run `git status`, **Then** the `.env` file is not listed as untracked.
2. **Given** the Android or web project has been built, **When** they run `git status`, **Then** no build output directories (e.g., `build/`, `dist/`) are listed as untracked.
3. **Given** an IDE has created project files (e.g., `.idea/`, `.vscode/`), **When** they run `git status`, **Then** those IDE directories are not listed as untracked.

---

### User Story 3 - Developer Onboarding Documentation (Priority: P3)

As a new developer joining the team, I want a README with setup instructions and `.env.example` files showing what configuration is required, so I can get the project running on my machine within minutes rather than hunting through code for environment variables.

**Why this priority**: Slow onboarding increases time-to-first-commit for new team members. Clear documentation reduces support interruptions for existing team members.

**Independent Test**: A developer unfamiliar with the project can follow the README from a fresh clone to a running development environment in under 30 minutes.

**Acceptance Scenarios**:

1. **Given** a freshly cloned repository, **When** I open the README at the repo root, **Then** it contains a project overview, a summary of the technology stack, and step-by-step setup instructions for both Android and web.
2. **Given** a freshly cloned repository, **When** I look for `.env.example` files, **Then** they exist at the repo root and inside the `web` directory, listing every required variable with placeholder values and no real secrets.
3. **Given** a developer has followed the setup instructions in the README, **When** they attempt to start the Android app, **Then** it builds and runs without missing configuration errors.

---

### Edge Cases

- What happens when `.env.example` is missing a variable that the app actually needs? The first developer to set up the project will encounter a runtime error with no clear indication of which variable is missing.
- What happens when `.gitignore` does not cover a new tool's build output (e.g., a new bundler or code generator)? That output becomes untracked noise in every `git status`, increasing the chance of accidental commits.
- What happens when the existing Android project cannot easily be moved into a subdirectory without breaking paths or gradle configuration? The migration must preserve all existing build and run capabilities.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The repository MUST be structured as a monorepo with at minimum these top-level directories: `app` (Android), `web` (React/TypeScript), `supabase` (database and configuration), `docs` (documentation).
- **FR-002**: The repository MUST include a `.gitignore` file at the root that excludes environment files (`.env`, `.env.*`), credentials (`*.keystore`, `*.jks`, `keystore.properties`, `google-services.json`), build outputs (`build/`, `dist/`, `.next/`), IDE directories (`.idea/`, `.vscode/`), OS files (`.DS_Store`, `Thumbs.db`), and package manager lock files for managers NOT in use.
- **FR-003**: A README file MUST exist at the repository root containing a project overview, technology stack summary, and step-by-step local development setup instructions for both the Android app and the web app.
- **FR-004**: `.env.example` files MUST exist at the repository root and inside the `web` directory, listing all required environment variables with placeholder values and no real secrets.
- **FR-005**: A `supabase` directory MUST exist containing placeholder directories for database migrations (`migrations/`) and a placeholder for the seed data file (`seed.sql`).

### Key Entities *(include if feature involves data)*

No business data entities. This feature establishes project infrastructure only.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new developer can clone the repository and have both the Android app and web app running locally within 30 minutes by following only the README instructions.
- **SC-002**: After running the standard build commands for Android (assemble) and web (build), `git status` shows no untracked build artifacts, credentials, or environment files.
- **SC-003**: The repository contains all four top-level directories (`app`, `web`, `supabase`, `docs`) at the time of the initial commit.
- **SC-004**: The README accurately describes the project purpose (dental practice management SaaS), its target users (dental clinics), and every step required to configure and run the development environment.

## Assumptions

- The Android project already exists in a runnable state and can be moved into the `app` subdirectory without breaking its build configuration.
- npm is the package manager for the web project (denoted by `package-lock.json` as the committed lock file).
- The Supabase project will be created separately through the Supabase dashboard; this feature only scaffolds the directory structure and placeholder files.
- The team uses Git for version control with standard branching conventions.
- Developers are expected to have Android Studio and Node.js installed on their machines before attempting setup.
