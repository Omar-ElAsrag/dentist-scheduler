# Research: Repository & Project Structure

**Feature**: 001-repo-project-setup
**Date**: 2026-06-11

## R1: Monorepo Structure for Android + React Co-location

**Decision**: Use a flat monorepo with four top-level directories: `app` (Android), `web` (React), `supabase` (database config), `docs` (documentation).

**Rationale**: A flat structure keeps the repo simple and avoids unnecessary nesting. Each platform is self-contained in its own directory, matching the constitution's platform separation principle. The `supabase` directory is separate because it is neither platform-specific nor application code — it is infrastructure configuration.

**Alternatives considered**:
- **Nested `platforms/` directory** (e.g., `platforms/android`, `platforms/web`): Rejected — adds an extra level of nesting with no benefit for a two-platform project.
- **Separate repos per platform**: Rejected — increases coordination overhead, makes cross-platform changes (schema, API contracts) harder to track, and contradicts the monorepo decision in the implementation plan.
- **Nx/Turborepo workspace tooling**: Rejected — overkill for a solo developer with two platforms. Standard npm workspaces and Gradle settings are sufficient.

## R2: .gitignore Composition

**Decision**: A single `.gitignore` at the repo root covering all platforms. Rules are grouped by section (secrets, Android, Web, Supabase, system/editor, logs, testing, misc).

**Rationale**: A single root `.gitignore` is the GitHub-recommended pattern for monorepos. Section comments make it easy to find and update rules. The file already exists and covers Android (Gradle, Android Studio), web (node_modules, build output, Vite, TypeScript), Supabase (local dev state), OS files, and IDE directories.

**Alternatives considered**:
- **Per-directory `.gitignore` files** (e.g., `app/.gitignore`, `web/.gitignore`): Rejected — fragments rules across files, harder to audit for security-sensitive patterns like credentials.
- **Global gitignore only**: Rejected — not portable across team members or CI environments.

## R3: README Conventions

**Decision**: Root README contains: project name + tagline, technology stack summary, prerequisites (Android Studio, Node.js, Supabase CLI), step-by-step setup for Android and web, and links to further documentation.

**Rationale**: The README is the first thing a new developer reads. It must answer: "What is this project?", "What do I need?", and "How do I get it running?" — in that order. Keeping it concise (under 100 lines) prevents staleness.

**Alternatives considered**:
- **Separate READMEs per directory**: Rejected for Phase 1 — adds maintenance burden. Can be added later when the project grows.
- **Wiki or external docs site**: Rejected — overkill for a solo developer in Phase 1.

## R4: .env.example Placement

**Decision**: Two `.env.example` files — one at the repo root (for shared variables like `SUPABASE_URL` and `SUPABASE_ANON_KEY`) and one inside `/web` (for Vite-specific prefixed variables like `VITE_SUPABASE_URL`).

**Rationale**: The Android app reads environment variables from `local.properties` (gitignored), so the root `.env.example` documents what the Android developer needs to put in `local.properties`. The web app uses Vite's `VITE_` prefix convention, so it needs its own `.env.example` inside `/web`.

**Alternatives considered**:
- **Single root `.env.example` only**: Rejected — web app uses Vite's `VITE_` prefix which is different from the Android convention.
- **Shared `.env` for both platforms**: Rejected — Android and web have different environment variable loading mechanisms (BuildConfig vs. Vite import.meta.env).
