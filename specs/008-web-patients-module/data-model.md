# Data Model: Web Patients Module

**Phase 1 Output** | **Date**: 2026-06-17

## Entities

### Patient (Primary Entity)

Source table: `patients` (defined in constitution §5.5)

| Field | Type | Required | Editable By | Notes |
|-------|------|----------|-------------|-------|
| `id` | SERIAL (PK) | Auto | N/A | System-generated |
| `tenant_id` | UUID (FK → tenants) | Auto | N/A | Injected from auth context on every query |
| `name` | TEXT | Yes | Admin, Dentist, Receptionist | Patient's full name |
| `phone_number` | TEXT | Yes | Admin, Dentist, Receptionist | Primary contact |
| `date_of_birth` | DATE | No | Admin, Dentist, Receptionist | |
| `gender` | TEXT | No | Admin, Dentist, Receptionist | |
| `systemic_conditions` | TEXT | No | Admin, Dentist | Medical history field — receptionist read-only |
| `past_dental_treatments` | TEXT | No | Admin, Dentist | Medical history field — receptionist read-only |
| `allergies` | TEXT | No | Admin, Dentist | Medical history field — receptionist read-only |
| `general_notes` | TEXT | No | Admin, Dentist, Receptionist | |
| `is_in_progress` | BOOLEAN | Yes (default: true) | Admin, Dentist | true = active patient, false = inactive |
| `created_date` | DATE | Auto (default: now) | N/A | Set on insert |
| `clinic_id` | INT (FK → clinics) | Yes | Admin, Dentist, Receptionist | Assigned clinic |
| `assigned_dentist_id` | UUID (FK → profiles) | No | Admin, Dentist, Receptionist | Primary dentist |
| `deleted_at` | TIMESTAMPTZ | No | Admin only | Soft delete (NULL = active, non-NULL = deleted) |
| `created_at` | TIMESTAMPTZ | Auto | N/A | Managed by trigger |
| `updated_at` | TIMESTAMPTZ | Auto | N/A | Managed by trigger |

### Field Groupings for UI

**Demographics** (editable by all roles):
- name, phone_number, date_of_birth, gender, clinic_id, assigned_dentist_id

**Medical History** (editable by Admin + Dentist, read-only for Receptionist):
- systemic_conditions, past_dental_treatments, allergies

**Administrative** (auto-managed):
- is_in_progress, created_date, deleted_at, created_at, updated_at

## Relationships

| Relationship | Type | Access Via |
|-------------|------|------------|
| Patient → Appointments | 1:N | Patient Detail → Appointments tab (read-only list) |
| Patient → Procedure Cards | 1:N | Patient Detail → Procedure Cards tab (read-only list) |
| Patient → Medical Files | 1:N | Patient Detail → Medical Files tab (read-only list) |
| Patient → Payments | 1:N (via procedure_cards) | Patient Detail → Payments tab (read-only list) |
| Patient → Clinic | N:1 | Displayed as clinic name in list and detail |
| Patient → Dentist | N:1 (via profiles) | Displayed as dentist name in list and detail |

## State Transitions

```
                    create
    [no patient] ─────────→ active (is_in_progress=true, deleted_at=NULL)
                                 │
                                 │ mark inactive
                                 ▼
                            inactive (is_in_progress=false, deleted_at=NULL)
                                 │
                                 │ soft delete (admin only)
                                 ▼
                            deleted (deleted_at=timestamp, excluded from default queries)
```

- **Active → Inactive**: Admin or Dentist sets `is_in_progress = false`. Patient still appears in lists (with inactive filter visible).
- **Active/Inactive → Deleted**: Admin sets `deleted_at`. Patient excluded from default list queries.
- **Deleted → Active/Inactive**: Admin clears `deleted_at` to restore. Not a common workflow but supported.
- **Create → Active**: New patients always start as active. Receptionists can only create with demographic fields; medical history fields remain NULL until an Admin or Dentist populates them.

## Validation Rules

| Rule | Applies To |
|------|-----------|
| `name` must not be empty or whitespace | All creates/updates |
| `phone_number` must not be empty | Create (required field) |
| `clinic_id` must reference an existing clinic in the same tenant | All creates/updates |
| `assigned_dentist_id` must reference a dentist profile in the same tenant (if provided) | All creates/updates |
| Receptionist cannot modify `systemic_conditions`, `past_dental_treatments`, `allergies` | Updates by receptionist role |
| `tenant_id` must match the authenticated user's tenant | All operations (enforced by RLS) |

## Query Patterns

### Patient List (with pagination + filters)

```
SELECT * FROM patients
WHERE tenant_id = $1
  AND deleted_at IS NULL
  [AND name ILIKE '%search%']
  [AND clinic_id = $2]
  [AND assigned_dentist_id = $3]
  [AND is_in_progress = $4]
ORDER BY name ASC
LIMIT $limit OFFSET $offset
```

### Patient Detail

```
SELECT * FROM patients
WHERE id = $1 AND tenant_id = $2 AND deleted_at IS NULL
```

### Create Patient

```
INSERT INTO patients (tenant_id, name, phone_number, date_of_birth, gender,
                       clinic_id, assigned_dentist_id, general_notes, is_in_progress, created_date)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8, true, now())
RETURNING *
```

Receptionist insert excludes: `systemic_conditions`, `past_dental_treatments`, `allergies`.

### Update Patient

Demographics update (all roles):
```
UPDATE patients SET name=$1, phone_number=$2, ..., updated_at=now()
WHERE id=$N AND tenant_id=$T
```

Medical history update (admin + dentist only):
```
UPDATE patients SET systemic_conditions=$1, past_dental_treatments=$2, allergies=$3, updated_at=now()
WHERE id=$4 AND tenant_id=$5
```
