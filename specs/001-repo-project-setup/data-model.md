# Data Model: Repository & Project Structure

**Feature**: 001-repo-project-setup
**Date**: 2026-06-11

This feature has no business data entities. The "data model" here documents the repository structure conventions and file naming rules that all subsequent features must follow.

## Repository Directory Convention

```text
/
├── app/                 # Android project root (Gradle module)
│   ├── src/main/        # Main source set
│   ├── src/test/        # Unit tests
│   ├── build.gradle.kts # Module-level build config
│   └── proguard-rules.pro
├── web/                 # React + TypeScript project root
│   ├── src/
│   │   ├── features/    # One directory per feature module
│   │   ├── components/  # Shared UI components
│   │   ├── hooks/       # Shared React Query hooks
│   │   ├── lib/         # Utility functions, Supabase client
│   │   └── locales/     # i18n translation files (ar/, en/)
│   ├── public/          # Static assets
│   ├── package.json
│   └── vite.config.ts
├── supabase/            # Supabase project configuration
│   ├── migrations/      # SQL migration files (numbered sequentially)
│   └── seed.sql         # Seed data for development
├── docs/                # Project documentation
│   └── implementation-plan.md
├── .gitignore           # Root-level ignore rules
├── .env.example         # Root environment variable template
└── README.md            # Project overview + setup guide
```

## .gitignore Rule Categories

| Category | Patterns | Purpose |
|----------|----------|---------|
| Secrets & Environment | `.env`, `.env.*`, `*.keystore`, `*.jks`, `keystore.properties`, `google-services.json` | Prevent credential leaks |
| Android Build | `.gradle/`, `build/`, `**/build/`, `local.properties`, `*.iml` | Exclude Gradle outputs |
| Android Studio | `.idea/`, `captures/`, `.externalNativeBuild/`, `.cxx/` | Exclude IDE metadata |
| Web Dependencies | `node_modules/`, `web/node_modules/` | Exclude npm packages |
| Web Build | `web/dist/`, `web/build/`, `web/.next/`, `web/out/` | Exclude build outputs |
| Web Cache | `web/.cache/`, `web/.parcel-cache/`, `web/.turbo/`, `web/.vite/` | Exclude tool caches |
| TypeScript | `*.tsbuildinfo` | Exclude incremental build info |
| Supabase Local | `supabase/.branches/`, `supabase/.temp/` | Exclude local dev state |
| OS Files | `.DS_Store`, `Thumbs.db`, `Desktop.ini`, `$RECYCLE.BIN/` | Exclude OS metadata |
| IDE | `.vscode/` (with exceptions), `.idea/`, `*.iml`, `*.ipr`, `*.iws` | Exclude editor config |
| Logs & Temp | `*.log`, `logs/`, `tmp/`, `temp/`, `.tmp/` | Exclude transient files |
| Binary Assets | `*.png`, `*.jpg`, `*.gif`, `*.mp4`, `*.pdf` (with exceptions for `res/`, `web/public/`, `docs/`) | Keep repo lean |

## .env.example Variable Naming

| Location | Prefix | Example | Used By |
|----------|--------|---------|---------|
| Root `.env.example` | None | `SUPABASE_URL=https://your-project.supabase.co` | Android (via `local.properties`) |
| `web/.env.example` | `VITE_` | `VITE_SUPABASE_URL=https://your-project.supabase.co` | Web (via `import.meta.env`) |

## File Naming Conventions

| Context | Convention | Example |
|---------|-----------|---------|
| SQL migrations | `NNN_descriptive_name.sql` | `001_initial_schema.sql` |
| React components | PascalCase | `PatientDetailPage.tsx` |
| React hooks | camelCase with `use` prefix | `usePatients.ts` |
| Feature directories | kebab-case | `procedure-cards/` |
| Documentation | kebab-case | `implementation-plan.md` |
