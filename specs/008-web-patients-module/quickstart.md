# Quickstart: Web Patients Module

**Phase 1 Output** | **Date**: 2026-06-17

## Prerequisites

1. **KIT-08 (Web App Scaffold) complete** — the React app at `/web` is running with:
   - Supabase Auth login working
   - AuthContext providing `session`, `role`, `tenantId`
   - i18next AR/EN translation infrastructure
   - React Router with protected routes
   - TailwindCSS configured
   - Folder structure: `src/features/patients/` directory exists

2. **KIT-02/KIT-03 (Supabase) complete** — the `patients` table exists with:
   - All columns per constitution §5.5
   - RLS policies active (tenant isolation + role gating)
   - At least one test patient in the database

3. **Environment**:
   - `web/.env` has `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY`
   - `npm install` completed in `/web`

## Setup

```bash
cd web
npm run dev
# Open http://localhost:5173
```

No additional dependencies are needed — the patients module uses the same stack as the scaffold.

## Verification Scenarios

### Scenario 1: Patient List (P1)

1. Log in as **admin** (or any role)
2. Navigate to Patients via sidebar
3. **Verify**: Patient list loads within 3 seconds with correct columns (name, clinic, dentist, status)
4. **Verify**: Only patients from the current tenant are displayed
5. Type a name in the search box
6. **Verify**: List filters to matching names within 1 second
7. Apply a clinic filter dropdown
8. **Verify**: List shows only patients from that clinic
9. Apply a status filter (active / inactive)
10. **Verify**: List shows only matching status
11. Combine multiple filters
12. **Verify**: Filters apply additively (AND logic)
13. If tenant has >20 patients, scroll or click next page
14. **Verify**: Pagination controls work and load next page

### Scenario 2: Patient Detail (P2)

1. Click any patient row in the list
2. **Verify**: Navigates to patient detail page
3. **Verify**: Demographics section shows name, phone, DOB, gender, clinic, dentist
4. **Verify**: Medical history section shows conditions, allergies, past treatments, notes
5. Click the "Procedure Cards" tab
6. **Verify**: Loads procedure cards for this patient (may be empty)
7. Click the "Appointments" tab
8. **Verify**: Loads appointments for this patient (may be empty)
9. Click the "Payments" tab
10. **Verify**: Loads payments across all cards for this patient (may be empty)
11. Click the "Medical Files" tab
12. **Verify**: Loads medical files for this patient (may be empty)

### Scenario 3: Create Patient (P3)

1. Log in as **receptionist**
2. Navigate to Patients, click "New Patient"
3. **Verify**: Form shows only: name, phone, DOB, gender, clinic, dentist — NO medical history fields
4. Fill required fields (name, phone, clinic) and submit
5. **Verify**: Patient appears in list immediately
6. Log in on Android app, refresh patient list
7. **Verify**: New patient is visible on Android

### Scenario 4: Edit Patient — Role Gates (P4)

1. Log in as **admin** or **dentist**
2. Open any patient detail page
3. **Verify**: Demographics fields are editable (name, phone, etc.)
4. **Verify**: Medical history fields are editable (conditions, allergies, etc.)
5. Edit a field, save
6. **Verify**: Changes persist on reload

7. Log in as **receptionist**
8. Open the same patient detail page
9. **Verify**: Demographics fields are editable
10. **Verify**: Medical history fields are **read-only** (disabled inputs or plain text)
11. Attempt to edit medical fields — should be impossible in UI

### Scenario 5: Cross-Platform Sync

1. Create a patient on the web app
2. Open the Android app, navigate to patient list, pull to refresh
3. **Verify**: New patient appears on Android

4. Edit a patient's phone number on Android
5. Refresh the patient detail page on web
6. **Verify**: Updated phone number appears on web

### Scenario 6: Error & Edge Cases

1. Navigate to `http://localhost:5173/patients/99999` (non-existent ID)
2. **Verify**: "Patient not found" message, not a crash or empty page

3. Disconnect from network, try to load patient list
4. **Verify**: Error message with retry button, no stack trace

5. Search for a name that matches no patients
6. **Verify**: "No patients found" empty state message

6. Log in to a tenant with zero patients
7. **Verify**: Empty state with prompt to create first patient

### Scenario 7: i18n (AR/EN)

1. With English selected, navigate through all patients pages
2. **Verify**: All labels, buttons, messages in English

3. Toggle language to Arabic
4. **Verify**: All labels, buttons, messages in Arabic
5. **Verify**: Layout flips to RTL (sidebar on right, content flows right-to-left)
6. **Verify**: Patient list columns, detail sections, tabs all render correctly in RTL

## Expected Results Summary

| Scenario | Pass Criteria |
|----------|--------------|
| 1 — Patient List | List loads <3s, search/filter <1s, pagination works |
| 2 — Patient Detail | Demographics + medical history + 4 tabs load correctly |
| 3 — Create Patient | Receptionist sees basic-only form; patient syncs to Android |
| 4 — Edit Role Gates | Receptionist cannot edit medical history; admin/dentist can |
| 5 — Cross-Platform | Creates/edits on one platform appear on the other |
| 6 — Error States | Not found, network error, empty states all handled gracefully |
| 7 — i18n | Full AR/EN translation + RTL layout in both languages |
