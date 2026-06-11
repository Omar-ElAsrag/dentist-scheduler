# Data Model: Supabase Foundation & Data Layer Migration

**Feature**: 002-supabase-foundation
**Date**: 2026-06-11

## Entity Definitions

### tenants
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | |
| name | TEXT | NOT NULL | Practice name |
| plan | TEXT | DEFAULT 'trial' | 'trial' / 'basic' / 'pro' |
| is_active | BOOLEAN | DEFAULT true | |
| created_at | TIMESTAMPTZ | DEFAULT now() | |
| updated_at | TIMESTAMPTZ | Managed by trigger | |

### profiles
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PRIMARY KEY, REFERENCES auth.users | |
| tenant_id | UUID | REFERENCES tenants(id) | |
| full_name | TEXT | | |
| role | TEXT | CHECK (role IN ('admin','dentist','receptionist')) | |
| avatar_url | TEXT | | |
| is_active | BOOLEAN | DEFAULT true | |
| created_at | TIMESTAMPTZ | DEFAULT now() | |
| updated_at | TIMESTAMPTZ | Managed by trigger | |

### clinics
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | SERIAL | PRIMARY KEY | |
| tenant_id | UUID | REFERENCES tenants(id) | |
| name | TEXT | NOT NULL | |
| default_percentage | FLOAT | | Dentist revenue share % |
| deduct_lab_fees | BOOLEAN | DEFAULT false | |
| address | TEXT | | |
| phone | TEXT | | |
| created_at | TIMESTAMPTZ | DEFAULT now() | |
| updated_at | TIMESTAMPTZ | Managed by trigger | |

### dentist_clinics (junction)
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| dentist_id | UUID | REFERENCES profiles(id) ON DELETE CASCADE | |
| clinic_id | INT | REFERENCES clinics(id) ON DELETE CASCADE | |
| percentage_override | FLOAT | | NULL = use clinic default |
| PRIMARY KEY | (dentist_id, clinic_id) | | |

### patients
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | SERIAL | PRIMARY KEY | |
| tenant_id | UUID | REFERENCES tenants(id) | |
| name | TEXT | NOT NULL | |
| phone_number | TEXT | | |
| date_of_birth | DATE | | |
| gender | TEXT | | |
| systemic_conditions | TEXT | | |
| past_dental_treatments | TEXT | | |
| allergies | TEXT | | |
| general_notes | TEXT | | |
| is_in_progress | BOOLEAN | DEFAULT true | |
| created_date | DATE | | |
| clinic_id | INT | REFERENCES clinics(id) | |
| assigned_dentist_id | UUID | REFERENCES profiles(id) | |
| deleted_at | TIMESTAMPTZ | | Soft delete |
| created_at | TIMESTAMPTZ | DEFAULT now() | |
| updated_at | TIMESTAMPTZ | Managed by trigger | |

### appointments
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | SERIAL | PRIMARY KEY | |
| tenant_id | UUID | REFERENCES tenants(id) | |
| patient_id | INT | REFERENCES patients(id) ON DELETE CASCADE | |
| dentist_id | UUID | REFERENCES profiles(id) | |
| clinic_id | INT | REFERENCES clinics(id) | |
| date | DATE | NOT NULL | |
| time | TIME | NOT NULL | |
| duration_mins | INT | NOT NULL | Flexible duration |
| notes | TEXT | | |
| status | TEXT | DEFAULT 'scheduled' | 'scheduled'/'completed'/'canceled'/'no_show' |
| created_by | UUID | REFERENCES profiles(id) | |
| created_at | TIMESTAMPTZ | DEFAULT now() | |
| updated_at | TIMESTAMPTZ | Managed by trigger | |

### clinical_procedures
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | SERIAL | PRIMARY KEY | |
| tenant_id | UUID | REFERENCES tenants(id) | NULL = system-wide seed |
| name | TEXT | NOT NULL | |
| has_types | BOOLEAN | | |
| display_order | INT | | |
| is_custom | BOOLEAN | DEFAULT false | |
| default_fee | FLOAT | | |
| default_lab_fee | FLOAT | | |
| created_at | TIMESTAMPTZ | DEFAULT now() | |
| updated_at | TIMESTAMPTZ | Managed by trigger | |

### procedure_types
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | SERIAL | PRIMARY KEY | |
| clinical_procedure_id | INT | REFERENCES clinical_procedures(id) ON DELETE CASCADE | |
| name | TEXT | NOT NULL | |
| materials | TEXT | | |
| default_fee | FLOAT | | |
| default_lab_fee | FLOAT | | |
| material_fees | TEXT | | "Material:fee,Material:fee" |
| created_at | TIMESTAMPTZ | DEFAULT now() | |
| updated_at | TIMESTAMPTZ | Managed by trigger | |

### clinical_procedure_steps
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | SERIAL | PRIMARY KEY | |
| clinical_procedure_id | INT | REFERENCES clinical_procedures(id) ON DELETE CASCADE | |
| procedure_type_id | INT | REFERENCES procedure_types(id) ON DELETE CASCADE | |
| step_name | TEXT | NOT NULL | |
| display_order | INT | | |
| created_at | TIMESTAMPTZ | DEFAULT now() | |
| updated_at | TIMESTAMPTZ | Managed by trigger | |

### procedure_cards
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | SERIAL | PRIMARY KEY | |
| tenant_id | UUID | REFERENCES tenants(id) | |
| patient_id | INT | REFERENCES patients(id) ON DELETE CASCADE | |
| clinic_id | INT | REFERENCES clinics(id) | |
| dentist_id | UUID | REFERENCES profiles(id) | |
| clinical_procedure_id | INT | REFERENCES clinical_procedures(id) | |
| procedure_type_id | INT | REFERENCES procedure_types(id) | |
| material | TEXT | | |
| status | TEXT | DEFAULT 'In Progress' | 'In Progress'/'Completed'/'Canceled' |
| date_created | DATE | | |
| tooth_number | TEXT | | |
| notes | TEXT | | |
| treatment_fee | FLOAT | DEFAULT 0 | |
| amount_paid | FLOAT | DEFAULT 0 | |
| lab_fees | FLOAT | DEFAULT 0 | |
| applied_percentage | FLOAT | DEFAULT 0 | |
| deduct_lab_fees | BOOLEAN | DEFAULT false | |
| calculated_associate_cut | FLOAT | DEFAULT 0 | |
| calculated_clinic_share | FLOAT | DEFAULT 0 | |
| deleted_at | TIMESTAMPTZ | | Soft delete |
| created_at | TIMESTAMPTZ | DEFAULT now() | |
| updated_at | TIMESTAMPTZ | Managed by trigger | |

### procedure_card_steps
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | SERIAL | PRIMARY KEY | |
| procedure_card_id | INT | REFERENCES procedure_cards(id) ON DELETE CASCADE | |
| step_name | TEXT | NOT NULL | |
| is_completed | BOOLEAN | DEFAULT false | |
| completed_at | TIMESTAMPTZ | | |
| photo_urls | TEXT[] | | Supabase Storage URLs |
| display_order | INT | DEFAULT 0 | |
| notes | TEXT | | |
| created_at | TIMESTAMPTZ | DEFAULT now() | |
| updated_at | TIMESTAMPTZ | Managed by trigger | |

### procedure_payments
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | SERIAL | PRIMARY KEY | |
| tenant_id | UUID | REFERENCES tenants(id) | |
| procedure_card_id | INT | REFERENCES procedure_cards(id) ON DELETE CASCADE | |
| amount | FLOAT | NOT NULL | |
| payment_at | TIMESTAMPTZ | NOT NULL | |
| notes | TEXT | | |
| recorded_by | UUID | REFERENCES profiles(id) | |
| created_at | TIMESTAMPTZ | DEFAULT now() | |
| updated_at | TIMESTAMPTZ | Managed by trigger | |

### medical_files
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | |
| tenant_id | UUID | REFERENCES tenants(id) | |
| patient_id | INT | REFERENCES patients(id) ON DELETE CASCADE | |
| title | TEXT | NOT NULL | |
| content | TEXT | | |
| file_url | TEXT | | Supabase Storage URL |
| file_type | TEXT | | 'note'/'xray'/'image'/'document' |
| created_by | UUID | REFERENCES profiles(id) | |
| created_at | TIMESTAMPTZ | DEFAULT now() | |
| updated_at | TIMESTAMPTZ | Managed by trigger | |

## Relationships

```
tenants 1──* profiles
tenants 1──* clinics
tenants 1──* patients
tenants 1──* appointments
tenants 1──* clinical_procedures (NULL tenant_id = system-wide)
tenants 1──* procedure_cards
tenants 1──* procedure_payments
tenants 1──* medical_files

profiles 1──* dentist_clinics (as dentist)
clinics 1──* dentist_clinics
profiles *──* clinics (via dentist_clinics junction)

profiles 1──* patients (as assigned_dentist)
clinics 1──* patients

patients 1──* appointments
patients 1──* procedure_cards
patients 1──* medical_files

profiles 1──* appointments (as dentist)
clinics 1──* appointments

clinical_procedures 1──* procedure_types
clinical_procedures 1──* clinical_procedure_steps
procedure_types 1──* clinical_procedure_steps

procedure_cards 1──* procedure_card_steps
procedure_cards 1──* procedure_payments
```

## State Transitions

### Procedure Card Status
```
In Progress → Completed
In Progress → Canceled
```

### Appointment Status
```
scheduled → completed
scheduled → canceled
scheduled → no_show
```

## Validation Rules

- `role` in profiles: must be one of 'admin', 'dentist', 'receptionist'
- `status` in procedure_cards: must be one of 'In Progress', 'Completed', 'Canceled'
- `status` in appointments: must be one of 'scheduled', 'completed', 'canceled', 'no_show'
- `file_type` in medical_files: must be one of 'note', 'xray', 'image', 'document'
- `amount` in procedure_payments: must be > 0
- `duration_mins` in appointments: must be > 0
- `tenant_id`: required on all tables except `tenants` itself

## Financial Calculation

```
IF deduct_lab_fees = true:
    associate_cut = (amount_paid - lab_fees) * applied_percentage
ELSE:
    associate_cut = amount_paid * applied_percentage

clinic_share = amount_paid - associate_cut
```
