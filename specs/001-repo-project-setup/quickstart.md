# Quickstart: Repository & Project Structure

**Feature**: 001-repo-project-setup
**Date**: 2026-06-11

Validation scenarios that prove the repository structure is correctly set up. Run these after completing all tasks in `tasks.md`.

## Prerequisites

- Git installed and configured
- Android Studio installed (latest stable)
- Node.js 18+ installed
- npm installed (comes with Node.js)

## Scenario 1: Directory Structure Verification

**Purpose**: Confirm all four top-level directories exist.

```bash
# From repo root
ls -d app web supabase docs
```

**Expected**: All four directories are listed. No errors.

## Scenario 2: .gitignore Coverage

**Purpose**: Confirm secrets, build outputs, and IDE files are ignored.

```bash
# Create test files that should be ignored
echo "SECRET=test" > .env
echo "SECRET=test" > web/.env
mkdir -p build web/dist
touch build/output.txt web/dist/bundle.js

# Check git status
git status
```

**Expected**: `git status` does NOT list `.env`, `web/.env`, `build/`, or `web/dist/` as untracked files.

**Cleanup**:
```bash
rm .env web/.env
rm -rf build web/dist
```

## Scenario 3: Android Project Integrity

**Purpose**: Confirm the Android project in `/app` can be opened and built.

1. Open Android Studio → File → Open → select the `app` directory
2. Wait for Gradle sync to complete
3. Build → Make Project

**Expected**: Build succeeds with no errors. Gradle sync completes without missing dependencies.

## Scenario 4: Web Project Integrity

**Purpose**: Confirm the web project in `/web` can install dependencies and start.

```bash
cd web
npm install
npm run dev
```

**Expected**: Dev server starts without errors. Opening the URL in a browser shows the app (even if it's a blank scaffold).

## Scenario 5: .env.example Completeness

**Purpose**: Confirm `.env.example` files exist and contain placeholder values.

```bash
# Check root .env.example
cat .env.example

# Check web .env.example
cat web/.env.example
```

**Expected**:
- Root file lists `SUPABASE_URL` and `SUPABASE_ANON_KEY` with placeholder values
- Web file lists `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY` with placeholder values
- Neither file contains real credentials

## Scenario 6: Supabase Directory Scaffolding

**Purpose**: Confirm the Supabase directory exists with expected structure.

```bash
ls supabase/
ls supabase/migrations/
cat supabase/seed.sql
```

**Expected**:
- `supabase/` contains `migrations/` directory and `seed.sql` file
- `migrations/` directory exists (may be empty at this stage)
- `seed.sql` exists (may be a placeholder comment at this stage)

## Scenario 7: README Accessibility

**Purpose**: Confirm the README is present and contains required sections.

```bash
cat README.md
```

**Expected**: README contains:
- Project name and description (DentSched — dental practice management SaaS)
- Technology stack summary (Android + Web + Supabase)
- Prerequisites section
- Setup instructions for Android
- Setup instructions for Web
