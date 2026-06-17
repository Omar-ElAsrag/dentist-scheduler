# Quickstart: Web App Scaffold

## Prerequisites

- Node.js 18+ installed
- npm 9+ installed
- Existing Supabase project with `profiles` table populated
- At least one test user per role (admin, dentist, receptionist) in the same tenant
- `docs/implementation-plan.md` KIT-08 checklist for reference

## Setup (first time only)

```bash
# 1. Scaffold the Vite project
cd dentist-scheduler
npm create vite@latest web -- --template react-ts

# 2. Install dependencies
cd web
npm install @supabase/supabase-js @tanstack/react-query react-router-dom i18next react-i18next
npm install -D tailwindcss @tailwindcss/vite

# 3. Copy environment template and fill in real values
cp .env.example .env
# Edit .env with actual Supabase URL and anon key

# 4. Start development server
npm run dev
```

## Verification Scenarios

### VS-01: App Starts Without Errors

1. Run `npm run dev` from `/web`
2. Open `http://localhost:5173` in browser
3. **Expected**: No console errors, app renders (shows login page since not authenticated)

### VS-02: Login with Valid Credentials

1. On the login page, enter a valid admin email and password
2. Click "Log In"
3. **Expected**: Redirected to dashboard, sidebar visible with all nav links
4. **Expected**: User name shown in header

### VS-03: Invalid Login Shows Error

1. Enter invalid email/password
2. Click "Log In"
3. **Expected**: Error message displayed ("Invalid email or password")
4. **Expected**: User remains on login page, no redirect

### VS-04: Role-Based Sidebar (Admin)

1. Log in as admin
2. View sidebar navigation
3. **Expected**: All links visible — Patients, Schedule, Procedures, Analytics, Associates, Settings

### VS-05: Role-Based Sidebar (Dentist)

1. Log in as dentist
2. View sidebar navigation
3. **Expected**: Patients, Schedule, Procedures visible; Analytics, Associates, Settings hidden

### VS-06: Role-Based Sidebar (Receptionist)

1. Log in as receptionist
2. View sidebar navigation
3. **Expected**: Patients, Schedule visible; all other links hidden

### VS-07: Language Toggle (EN → AR)

1. Click language toggle in header
2. **Expected**: All UI labels switch to Arabic
3. **Expected**: Page layout flips to right-to-left (sidebar on right, content flows RTL)
4. **Expected**: "عربي" label changes to "English" (or equivalent)

### VS-08: Language Toggle (AR → EN)

1. From Arabic mode, click language toggle again
2. **Expected**: All labels return to English
3. **Expected**: Page layout returns to left-to-right

### VS-09: Protected Route Redirect

1. In the browser address bar, navigate directly to `/patients` while logged out
2. **Expected**: Redirected to `/login`
3. Log in, then navigate to `/patients`
4. **Expected**: Page renders (even if empty — content comes in KIT-09)

### VS-10: Session Persistence

1. Log in, close browser tab
2. Open new tab, navigate to `http://localhost:5173`
3. **Expected**: Automatically logged in (session restored), no need to re-enter credentials

### VS-11: Logout

1. Click logout button in header/sidebar
2. **Expected**: Session cleared, redirected to login page
3. Navigate to a protected route directly → redirected to login

### VS-12: Responsive Sidebar

1. On desktop (viewport > 768px): sidebar is always visible
2. Resize browser to mobile width (< 768px)
3. **Expected**: Sidebar collapses to hamburger menu
4. Tap hamburger → sidebar slides in

## Run

```bash
cd dentist-scheduler/web
npm run dev
# Open http://localhost:5173
```

## Expected Outcomes

| VS | Action | Expected |
|----|--------|----------|
| 1 | `npm run dev` | No console errors |
| 2 | Valid login | Redirect to dashboard, sidebar visible |
| 3 | Invalid login | Error message, stay on login |
| 4 | Admin sidebar | All 6 links visible |
| 5 | Dentist sidebar | 3 links visible |
| 6 | Receptionist sidebar | 2 links visible |
| 7 | EN→AR toggle | RTL layout + Arabic labels |
| 8 | AR→EN toggle | LTR layout + English labels |
| 9 | Direct URL while logged out | Redirect to login |
| 10 | Close/reopen browser | Session restored |
| 11 | Logout | Session cleared, redirect to login |
| 12 | Mobile viewport | Hamburger menu |
