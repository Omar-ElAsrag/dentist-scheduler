# Research: Web App Scaffold

## R1: Vite + React + TypeScript Project Setup

**Decision**: Scaffold using `npm create vite@latest web -- --template react-ts` in the repo root.

**Rationale**: Vite is the build tool specified in KIT-08 and is the modern standard for React SPA projects. The `react-ts` template provides TypeScript + React boilerplate with HMR out of the box. The project lives at `/web` alongside the existing Android app at `/app`.

**Evidence**: KIT-08 tasks specify this exact command. Vite is widely adopted for React projects (used by Vitest, Storybook, Remix). Fast HMR and build times are critical for developer productivity.

**Alternatives considered**:
- Create React App (CRA): Rejected — CRA is deprecated and slow compared to Vite.
- Next.js: Rejected — adds SSR complexity not needed in Phase 1 (constitution says static SPA).
- Remix: Rejected — same SSR complexity concern for a scaffold phase.

---

## R2: Supabase Client Integration

**Decision**: Create a singleton Supabase client in `src/lib/supabase.ts` using `createClient()` from `@supabase/supabase-js`, configured with `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY` environment variables.

**Rationale**: The existing Supabase project (shared with Android) uses the same URL and anon key. The Vite convention is to prefix client-exposed env vars with `VITE_`. The anon key is safe for client use per Supabase docs — all sensitive data access is gated by RLS policies server-side.

**Evidence**: The Android app already configures Supabase with the same URL and anon key. The `.env.example` file will document required variables without exposing secrets.

**Alternatives considered**:
- Using the Supabase service role key: Rejected — would expose admin privileges on the client. Constitution explicitly forbids this.
- Multiple client instances (per tenant): Rejected — single Supabase project serves all tenants via RLS; no need for separate clients.

---

## R3: Authentication Flow (Supabase Auth + React Context)

**Decision**: Use Supabase Auth's `signInWithPassword` for login, `getSession` for session restore, and `onAuthStateChange` for real-time session tracking. Expose session, user, role, and tenantId via React Context (`AuthContext`).

**Rationale**: Supabase Auth handles token management, refresh, and persistence automatically. React Context provides app-wide access to auth state without prop drilling. The `profiles` table (read after auth) provides role and tenantId.

**Evidence**: Similar patterns used in Supabase React examples and starter kits. The Android app equivalent uses `SessionManager` as a singleton with `StateFlow`.

**Alternatives considered**:
- Redux for auth state: Rejected — overkill for auth state; Context + React Query suffices.
- NextAuth / Auth0: Rejected — would introduce a second auth system; Supabase Auth is already the canonical backend auth.

---

## R4: Internationalization (i18next + RTL)

**Decision**: Use `i18next` and `react-i18next` with two namespaces: English (en) and Arabic (ar). Set `document.dir = 'rtl'` and `document.lang = 'ar'` when Arabic is selected. Persist language preference in `localStorage`.

**Rationale**: i18next is the most widely used React i18n library with built-in namespace support, interpolation, and formatting. The `dir` attribute on `<html>` triggers browsers' native RTL layout engines. Arabic support is a constitutional requirement (§2: "Arabic + English — RTL support required").

**Evidence**: i18next has 4.5M+ weekly downloads, excellent TypeScript support, and a `react-i18next` binding maintained by the core team. TailwindCSS's logical properties and RTL variants simplify RTL styling.

**Alternatives considered**:
- react-intl (FormatJS): Rejected — heavier setup; i18next is simpler for a scaffold.
- Custom i18n solution: Rejected — reinventing wheels; i18next handles edge cases (plurals, interpolation, fallbacks).

---

## R5: Routing Strategy (React Router v6)

**Decision**: Use `react-router-dom` v6 with a protected route pattern: a `ProtectedRoute` wrapper component checks auth state from AuthContext and redirects to `/login` if unauthenticated. The app shell (Layout) wraps all protected routes.

**Rationale**: React Router v6 is the standard for React SPAs. The protected route pattern prevents flash-of-unauthorized-content by checking auth before rendering. Nested routes inside Layout enable the sidebar+header shell to persist across page navigation.

**TailwindCSS**: Installed via `npm install -D tailwindcss @tailwindcss/vite` per TailwindCSS v4's Vite plugin approach.

**Alternatives considered**:
- TanStack Router: Rejected — newer, less mature; React Router has broader ecosystem support.
- Next.js App Router: Rejected — not applicable to Vite SPA.

---

## R6: TailwindCSS Setup

**Decision**: Install TailwindCSS v4 with the `@tailwindcss/vite` plugin. Configure in `vite.config.ts` as a plugin. Import `tailwindcss` directives in `src/index.css`.

**Rationale**: TailwindCSS is specified in KIT-08 and is the most popular utility-first CSS framework for React. The Vite plugin approach (v4) eliminates the need for a separate PostCSS config in most cases.

**Alternatives considered**:
- CSS Modules: Rejected — more verbose; TailwindCSS provides faster prototyping.
- styled-components: Rejected — CSS-in-JS adds runtime overhead; TailwindCSS compiles to static CSS.

---

## R7: Environment Variables Strategy

**Decision**: Create `/web/.env.example` with `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY` (placeholder values). Developers copy to `.env` and fill in real values. Add `.env` to `.gitignore`.

**Rationale**: Vite requires `VITE_` prefix for client-exposed env vars. The `.env.example` pattern is standard GitHub practice for documenting required environment variables without exposing secrets.

**Note**: The existing `.gitignore` at repo root already covers `.env` and `web/.env`.
