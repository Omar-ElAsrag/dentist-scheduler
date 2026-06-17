# Data Model: Web App Scaffold

## Overview

The web app scaffold introduces no new database tables or Supabase schema changes. It consumes the existing Supabase backend shared with the Android app. The data model below documents the client-side entities used by the scaffold.

## Client-Side Entities

### AuthSession

**Source**: Supabase Auth (server-side) + React Context (client-side)

Represents the authenticated user's session in the web app.

| Field | Type | Source | Purpose |
|-------|------|--------|---------|
| userId | string (UUID) | `session.user.id` | Unique identifier, used for tenant lookup |
| email | string | `session.user.email` | Display in header |
| role | `'admin' \| 'dentist' \| 'receptionist'` | `profiles.role` | Determines sidebar visibility and page access |
| tenantId | string (UUID) | `profiles.tenant_id` | Tenant scoping for all data queries |
| fullName | string | `profiles.full_name` | Display in header greeting |

**State transitions**:
```
Unauthenticated → [login success] → Authenticated (session active)
Authenticated → [token expiry / logout] → Unauthenticated
Authenticated → [page refresh] → Authenticated (session restored via Supabase)
```

### NavigationItem

Represents a single sidebar navigation link.

| Field | Type | Purpose |
|-------|------|---------|
| label | string (translation key) | Display text in sidebar |
| path | string | React Router route path |
| roles | `Array<'admin' \| 'dentist' \| 'receptionist'>` | Which roles can see this link |

**Static navigation items** (per KIT-08):

| Label (EN) | Route | Visible To |
|------------|-------|------------|
| Patients | `/patients` | admin, dentist, receptionist |
| Schedule | `/schedule` | admin, dentist, receptionist |
| Procedures | `/procedures` | admin, dentist |
| Analytics | `/analytics` | admin |
| Associates | `/associates` | admin |
| Settings | `/settings` | admin |

### LocalePreference

Represents the user's language choice, persisted locally.

| Field | Type | Storage | Purpose |
|-------|------|---------|---------|
| language | `'en' \| 'ar'` | `localStorage` | Active language |

**State transitions**:
```
Default (en) → [user selects Arabic] → ar
ar → [user selects English] → en
```

**Side effects on change**:
- `i18next.changeLanguage(language)` — loads correct translation namespace
- `document.dir = language === 'ar' ? 'rtl' : 'ltr'` — flips layout
- `document.documentElement.lang = language` — accessibility metadata

## Translation Namespaces

Each language has a `translation.json` file with key-value pairs. Initial keys needed for scaffold:

```json
{
  "nav": {
    "patients": "Patients",
    "schedule": "Schedule",
    "procedures": "Procedures",
    "analytics": "Analytics",
    "associates": "Associates",
    "settings": "Settings"
  },
  "auth": {
    "login": "Log In",
    "logout": "Log Out",
    "email": "Email",
    "password": "Password",
    "loginError": "Invalid email or password"
  },
  "header": {
    "languageToggle": "عربي",
    "greeting": "Welcome"
  }
}
```

## Integration Points

```text
Web App (React) ←→ Supabase JS SDK ←→ Supabase Backend
                                      ←→ Android App (Kotlin SDK)

Shared resources:
  - supabase/config.toml (RLS policies, migrations)
  - Same PostgreSQL instance
  - Same Auth users table (profiles)
```
