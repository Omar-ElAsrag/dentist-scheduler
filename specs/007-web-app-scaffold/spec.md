# Feature Specification: Web App Scaffold

**Feature Branch**: `kit-08-web-scaffold`

**Created**: 2026-06-15

**Status**: Draft

**Input**: KIT-08 from `docs/implementation-plan.md` — "Create the React web app scaffold with routing, Supabase client, auth, and i18n wired up."

## User Scenarios & Testing *(mandatory)*

### User Story 1 — User Logs In on the Web (Priority: P1)

A user with a valid account opens the web app in their browser. They see a login page prompting for email and password. After entering their credentials, they are authenticated via Supabase Auth and redirected to the main dashboard. Their role (admin, dentist, receptionist) determines which navigation links they see.

**Why this priority**: Authentication is the gate for all other functionality. Without login, no other feature can be accessed or tested on the web platform.

**Independent Test**: Open `http://localhost:5173`, enter valid credentials for each role (admin, dentist, receptionist), verify successful login and role-appropriate sidebar links.

**Acceptance Scenarios**:

1. **Given** an unauthenticated user, **When** they visit any URL in the web app, **Then** they are redirected to the login page.
2. **Given** a user with valid credentials, **When** they submit the login form, **Then** they are authenticated and redirected to the main dashboard.
3. **Given** a user with invalid credentials, **When** they submit the login form, **Then** an error message is displayed and they remain on the login page.
4. **Given** an authenticated admin, **When** they view the sidebar, **Then** all navigation links are visible (Patients, Schedule, Analytics, Procedures, Associates, Settings).
5. **Given** an authenticated dentist, **When** they view the sidebar, **Then** only role-appropriate links are visible (Patients, Schedule, Procedures — no Analytics or Settings management).
6. **Given** an authenticated receptionist, **When** they view the sidebar, **Then** only role-appropriate links are visible (Patients, Schedule — no clinical or financial sections).

---

### User Story 2 — User Toggles Between Arabic and English (Priority: P2)

A user prefers to use the app in Arabic. They click a language toggle in the app header. The entire interface switches to Arabic with right-to-left text direction. All labels, buttons, and navigation items are translated. Toggling back to English restores left-to-right layout.

**Why this priority**: Bilingual support (AR/EN) is a core constitutional requirement for the DentSched platform. The scaffold must establish the i18n infrastructure before any feature pages are built.

**Independent Test**: Click the language toggle, verify all UI text switches to Arabic, verify page layout flips to RTL, verify toggle back to English works.

**Acceptance Scenarios**:

1. **Given** the app is in English (default), **When** the user clicks the language toggle, **Then** all labels and navigation items switch to Arabic and the page direction changes to right-to-left.
2. **Given** the app is in Arabic, **When** the user clicks the language toggle, **Then** all labels switch back to English and the page direction returns to left-to-right.
3. **Given** a user who previously selected Arabic, **When** they close and reopen the app, **Then** the app loads in Arabic (language preference is persisted).

---

### User Story 3 — App Shell Provides Consistent Layout (Priority: P3)

After login, every page in the web app shares a consistent layout: a sidebar for navigation and a header bar with the user's name and language toggle. The main content area renders the active page. On narrow screens, the sidebar collapses to a hamburger menu.

**Why this priority**: A consistent shell ensures all future feature pages (Patients, Schedule, Analytics, etc.) have a common container and navigation, reducing duplicate UI work.

**Independent Test**: Log in, verify sidebar + header present on all pages. Resize browser to mobile width, verify sidebar collapses. Click sidebar links, verify correct page content renders.

**Acceptance Scenarios**:

1. **Given** an authenticated user on any page, **When** they look at the screen, **Then** a sidebar with navigation links and a header bar with their name and language toggle are visible.
2. **Given** a user on a desktop browser, **When** they click a sidebar link, **Then** the main content area updates to show the corresponding page without a full page reload.
3. **Given** a user on a mobile-width screen, **When** they tap the hamburger menu, **Then** the sidebar slides in and they can select navigation items.
4. **Given** a user clicks their profile or logout, **When** they confirm logout, **Then** their session is cleared and they return to the login page.

---

### Edge Cases

- **Session expiry**: If the user's Supabase session expires while on a page, the app detects the expired session and redirects to the login page.
- **Empty state — no translations loaded**: If a translation key is missing, the app falls back to English text rather than showing a raw key.
- **Network failure during login**: If the Supabase Auth endpoint is unreachable, the login form shows a user-friendly error message (not a stack trace).
- **Browser back/forward navigation**: Browser history navigation works correctly with React Router — back returns to the previous page, forward returns to the next, without re-authenticating.
- **Concurrent sessions**: The same user can be logged in on Android and Web simultaneously; both sessions are independent and do not interfere.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The web app MUST present a login page as the default entry point for unauthenticated users, with email and password fields.
- **FR-002**: The web app MUST authenticate users via Supabase Auth and persist the session across page refreshes and browser restarts.
- **FR-003**: Authenticated users MUST be redirected to a dashboard shell with a sidebar navigation and header bar.
- **FR-004**: The sidebar MUST display navigation links filtered by the user's role (admin sees all links; dentist sees Patients, Schedule, Procedures; receptionist sees Patients, Schedule).
- **FR-005**: The header bar MUST display the logged-in user's name and a language toggle control.
- **FR-006**: The language toggle MUST switch all UI text between Arabic and English and toggle the page direction between RTL and LTR.
- **FR-007**: The app MUST store the user's language preference and restore it on subsequent visits.
- **FR-008**: Protected routes MUST redirect unauthenticated users to the login page rather than showing partial or error UI.
- **FR-009**: The sidebar MUST collapse into a hamburger menu on narrow viewports (mobile) and expand on wide viewports (desktop).
- **FR-010**: A logout action MUST clear the Supabase session and return the user to the login page.

### Key Entities

- **User Session**: Links to Supabase Auth user. Contains user ID, email, role (admin/dentist/receptionist), tenant ID, and display name. Exposed to the app via an auth context or equivalent state mechanism.
- **Navigation Items**: A role-filtered set of sidebar links, each with a label (translated), an icon, and a route path.
- **Locale Preference**: The user's chosen language (AR or EN), persisted locally. Determines which translation files to load and the document's text direction.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can log in and reach the dashboard in under 5 seconds from the login page.
- **SC-002**: Switching between Arabic and English updates all visible UI text within 200 milliseconds with no page reload.
- **SC-003**: The web app displays correctly in both LTR (English) and RTL (Arabic) modes across Chrome, Firefox, and Safari on desktop.
- **SC-004**: The sidebar collapses to a hamburger menu at viewport widths under 768px and expands above 768px.
- **SC-005**: 100% of protected routes redirect unauthenticated users to the login page.
- **SC-006**: The app starts without errors via `npm run dev` with zero console errors in the browser.

## Assumptions

- The existing Supabase project (from the Android app) is reused for the web app — no new Supabase project is created.
- The `profiles` table in Supabase already includes `role`, `tenant_id`, and `full_name` fields that the web auth context will consume.
- The web app is deployed as a static site (Vite build output), served from a CDN or static hosting. Server-side rendering is not required for Phase 1.
- The Arabic and English translation files will start with a curated set of labels covering the shell, login, and navigation. Additional translations will be added per feature.
- The Vite dev server runs on `http://localhost:5173` by default.
- The folder structure follows the constitution's web conventions: `src/features/{domain}/`, `src/components/`, `src/hooks/`, `src/lib/`, `src/locales/`.
