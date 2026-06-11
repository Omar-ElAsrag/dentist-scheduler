-- =============================================================================
-- DentSched: Initial Schema Migration
-- Creates all 13 tables, updated_at trigger, and indexes
-- =============================================================================

-- ─── Trigger Function: auto-update updated_at ──────────────────────────────

CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ─── 1. tenants ────────────────────────────────────────────────────────────

CREATE TABLE public.tenants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    plan        TEXT DEFAULT 'trial',
    is_active   BOOLEAN DEFAULT true,
    created_at  TIMESTAMPTZ DEFAULT now(),
    updated_at  TIMESTAMPTZ DEFAULT now()
);

-- ─── 2. profiles ────────────────────────────────────────────────────────────

CREATE TABLE public.profiles (
    id          UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    tenant_id   UUID REFERENCES public.tenants(id),
    full_name   TEXT,
    role        TEXT CHECK (role IN ('admin', 'dentist', 'receptionist')),
    avatar_url  TEXT,
    is_active   BOOLEAN DEFAULT true,
    created_at  TIMESTAMPTZ DEFAULT now(),
    updated_at  TIMESTAMPTZ DEFAULT now()
);

-- ─── 3. clinics ─────────────────────────────────────────────────────────────

CREATE TABLE public.clinics (
    id                  SERIAL PRIMARY KEY,
    tenant_id           UUID REFERENCES public.tenants(id),
    name                TEXT NOT NULL,
    default_percentage  FLOAT,
    deduct_lab_fees     BOOLEAN DEFAULT false,
    address             TEXT,
    phone               TEXT,
    created_at          TIMESTAMPTZ DEFAULT now(),
    updated_at          TIMESTAMPTZ DEFAULT now()
);

-- ─── 4. dentist_clinics (junction) ──────────────────────────────────────────

CREATE TABLE public.dentist_clinics (
    dentist_id          UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    clinic_id           INT REFERENCES public.clinics(id) ON DELETE CASCADE,
    percentage_override FLOAT,
    PRIMARY KEY (dentist_id, clinic_id)
);

-- ─── 5. patients ────────────────────────────────────────────────────────────

CREATE TABLE public.patients (
    id                      SERIAL PRIMARY KEY,
    tenant_id               UUID REFERENCES public.tenants(id),
    name                    TEXT NOT NULL,
    phone_number            TEXT NOT NULL DEFAULT '',
    date_of_birth           DATE,
    gender                  TEXT,
    systemic_conditions     TEXT NOT NULL DEFAULT '',
    past_dental_treatments  TEXT NOT NULL DEFAULT '',
    allergies               TEXT NOT NULL DEFAULT '',
    general_notes           TEXT NOT NULL DEFAULT '',
    is_in_progress         BOOLEAN DEFAULT true,
    created_date            DATE NOT NULL DEFAULT CURRENT_DATE,
    clinic_id               INT REFERENCES public.clinics(id),
    assigned_dentist_id     UUID REFERENCES public.profiles(id),
    deleted_at              TIMESTAMPTZ,
    created_at              TIMESTAMPTZ DEFAULT now(),
    updated_at              TIMESTAMPTZ DEFAULT now()
);

-- ─── 6. appointments ────────────────────────────────────────────────────────

CREATE TABLE public.appointments (
    id              SERIAL PRIMARY KEY,
    tenant_id       UUID REFERENCES public.tenants(id),
    patient_id      INT REFERENCES public.patients(id) ON DELETE CASCADE,
    dentist_id      UUID REFERENCES public.profiles(id),
    clinic_id       INT REFERENCES public.clinics(id),
    date            DATE NOT NULL,
    time            TIME NOT NULL,
    duration_mins   INT NOT NULL,
    notes           TEXT,
    status          TEXT DEFAULT 'scheduled' CHECK (status IN ('scheduled', 'completed', 'canceled', 'no_show')),
    created_by      UUID REFERENCES public.profiles(id),
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

-- ─── 7. clinical_procedures ─────────────────────────────────────────────────

CREATE TABLE public.clinical_procedures (
    id              SERIAL PRIMARY KEY,
    tenant_id       UUID REFERENCES public.tenants(id),
    name            TEXT NOT NULL,
    has_types       BOOLEAN,
    display_order   INT,
    is_custom       BOOLEAN DEFAULT false,
    default_fee     FLOAT,
    default_lab_fee FLOAT,
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

-- ─── 8. procedure_types ─────────────────────────────────────────────────────

CREATE TABLE public.procedure_types (
    id                      SERIAL PRIMARY KEY,
    clinical_procedure_id   INT REFERENCES public.clinical_procedures(id) ON DELETE CASCADE,
    name                    TEXT NOT NULL,
    materials               TEXT,
    default_fee             FLOAT,
    default_lab_fee         FLOAT,
    material_fees           TEXT,
    created_at              TIMESTAMPTZ DEFAULT now(),
    updated_at              TIMESTAMPTZ DEFAULT now()
);

-- ─── 9. clinical_procedure_steps ────────────────────────────────────────────

CREATE TABLE public.clinical_procedure_steps (
    id                      SERIAL PRIMARY KEY,
    clinical_procedure_id   INT REFERENCES public.clinical_procedures(id) ON DELETE CASCADE,
    procedure_type_id       INT REFERENCES public.procedure_types(id) ON DELETE CASCADE,
    step_name               TEXT NOT NULL,
    display_order           INT,
    created_at              TIMESTAMPTZ DEFAULT now(),
    updated_at              TIMESTAMPTZ DEFAULT now()
);

-- ─── 10. procedure_cards ────────────────────────────────────────────────────

CREATE TABLE public.procedure_cards (
    id                      SERIAL PRIMARY KEY,
    tenant_id               UUID REFERENCES public.tenants(id),
    patient_id              INT REFERENCES public.patients(id) ON DELETE CASCADE,
    clinic_id               INT REFERENCES public.clinics(id),
    dentist_id              UUID REFERENCES public.profiles(id),
    clinical_procedure_id   INT REFERENCES public.clinical_procedures(id),
    procedure_type_id       INT REFERENCES public.procedure_types(id),
    material                TEXT,
    status                  TEXT DEFAULT 'In Progress' CHECK (status IN ('In Progress', 'Completed', 'Canceled')),
    date_created            DATE,
    tooth_number            TEXT,
    notes                   TEXT,
    treatment_fee           FLOAT DEFAULT 0,
    amount_paid             FLOAT DEFAULT 0,
    lab_fees                FLOAT DEFAULT 0,
    applied_percentage      FLOAT DEFAULT 0,
    deduct_lab_fees         BOOLEAN DEFAULT false,
    calculated_associate_cut FLOAT DEFAULT 0,
    calculated_clinic_share  FLOAT DEFAULT 0,
    deleted_at              TIMESTAMPTZ,
    created_at              TIMESTAMPTZ DEFAULT now(),
    updated_at              TIMESTAMPTZ DEFAULT now()
);

-- ─── 11. procedure_card_steps ───────────────────────────────────────────────

CREATE TABLE public.procedure_card_steps (
    id                  SERIAL PRIMARY KEY,
    procedure_card_id   INT REFERENCES public.procedure_cards(id) ON DELETE CASCADE,
    step_name           TEXT NOT NULL,
    is_completed        BOOLEAN DEFAULT false,
    completed_at        TIMESTAMPTZ,
    photo_urls          TEXT[],
    display_order       INT DEFAULT 0,
    notes               TEXT,
    created_at          TIMESTAMPTZ DEFAULT now(),
    updated_at          TIMESTAMPTZ DEFAULT now()
);

-- ─── 12. procedure_payments ─────────────────────────────────────────────────

CREATE TABLE public.procedure_payments (
    id                  SERIAL PRIMARY KEY,
    tenant_id           UUID REFERENCES public.tenants(id),
    procedure_card_id   INT REFERENCES public.procedure_cards(id) ON DELETE CASCADE,
    amount              FLOAT NOT NULL CHECK (amount > 0),
    payment_at          TIMESTAMPTZ NOT NULL,
    notes               TEXT,
    recorded_by         UUID REFERENCES public.profiles(id),
    created_at          TIMESTAMPTZ DEFAULT now(),
    updated_at          TIMESTAMPTZ DEFAULT now()
);

-- ─── 13. medical_files ──────────────────────────────────────────────────────

CREATE TABLE public.medical_files (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID REFERENCES public.tenants(id),
    patient_id  INT REFERENCES public.patients(id) ON DELETE CASCADE,
    title       TEXT NOT NULL,
    content     TEXT NOT NULL DEFAULT '',
    file_url    TEXT,
    file_type   TEXT CHECK (file_type IN ('note', 'xray', 'image', 'document')),
    created_by   UUID REFERENCES public.profiles(id),
    created_at   TIMESTAMPTZ DEFAULT now(),
    updated_at   TIMESTAMPTZ DEFAULT now()
);

-- =============================================================================
-- Triggers: auto-update updated_at on every table
-- =============================================================================

CREATE TRIGGER set_updated_at_tenants
    BEFORE UPDATE ON public.tenants
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER set_updated_at_profiles
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER set_updated_at_clinics
    BEFORE UPDATE ON public.clinics
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER set_updated_at_patients
    BEFORE UPDATE ON public.patients
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER set_updated_at_appointments
    BEFORE UPDATE ON public.appointments
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER set_updated_at_clinical_procedures
    BEFORE UPDATE ON public.clinical_procedures
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER set_updated_at_procedure_types
    BEFORE UPDATE ON public.procedure_types
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER set_updated_at_clinical_procedure_steps
    BEFORE UPDATE ON public.clinical_procedure_steps
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER set_updated_at_procedure_cards
    BEFORE UPDATE ON public.procedure_cards
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER set_updated_at_procedure_card_steps
    BEFORE UPDATE ON public.procedure_card_steps
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER set_updated_at_procedure_payments
    BEFORE UPDATE ON public.procedure_payments
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER set_updated_at_medical_files
    BEFORE UPDATE ON public.medical_files
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- =============================================================================
-- Indexes: tenant_id on all tenant-scoped tables
-- =============================================================================

CREATE INDEX idx_profiles_tenant_id ON public.profiles(tenant_id);
CREATE INDEX idx_clinics_tenant_id ON public.clinics(tenant_id);
CREATE INDEX idx_patients_tenant_id ON public.patients(tenant_id);
CREATE INDEX idx_appointments_tenant_id ON public.appointments(tenant_id);
CREATE INDEX idx_clinical_procedures_tenant_id ON public.clinical_procedures(tenant_id);
CREATE INDEX idx_procedure_cards_tenant_id ON public.procedure_cards(tenant_id);
CREATE INDEX idx_procedure_payments_tenant_id ON public.procedure_payments(tenant_id);
CREATE INDEX idx_medical_files_tenant_id ON public.medical_files(tenant_id);

-- =============================================================================
-- Indexes: foreign key columns
-- =============================================================================

CREATE INDEX idx_patients_clinic_id ON public.patients(clinic_id);
CREATE INDEX idx_patients_assigned_dentist_id ON public.patients(assigned_dentist_id);
CREATE INDEX idx_appointments_patient_id ON public.appointments(patient_id);
CREATE INDEX idx_appointments_dentist_id ON public.appointments(dentist_id);
CREATE INDEX idx_appointments_clinic_id ON public.appointments(clinic_id);
CREATE INDEX idx_procedure_cards_patient_id ON public.procedure_cards(patient_id);
CREATE INDEX idx_procedure_cards_clinic_id ON public.procedure_cards(clinic_id);
CREATE INDEX idx_procedure_cards_dentist_id ON public.procedure_cards(dentist_id);
CREATE INDEX idx_procedure_cards_clinical_procedure_id ON public.procedure_cards(clinical_procedure_id);
CREATE INDEX idx_procedure_cards_procedure_type_id ON public.procedure_cards(procedure_type_id);
CREATE INDEX idx_procedure_card_steps_procedure_card_id ON public.procedure_card_steps(procedure_card_id);
CREATE INDEX idx_procedure_payments_procedure_card_id ON public.procedure_payments(procedure_card_id);
CREATE INDEX idx_medical_files_patient_id ON public.medical_files(patient_id);
CREATE INDEX idx_clinical_procedure_steps_cp_id ON public.clinical_procedure_steps(clinical_procedure_id);
CREATE INDEX idx_clinical_procedure_steps_pt_id ON public.clinical_procedure_steps(procedure_type_id);
CREATE INDEX idx_procedure_types_cp_id ON public.procedure_types(clinical_procedure_id);
CREATE INDEX idx_dentist_clinics_clinic_id ON public.dentist_clinics(clinic_id);

-- =============================================================================
-- Unique constraint for seed idempotency on clinical_procedures
-- =============================================================================

CREATE UNIQUE INDEX idx_clinical_procedures_tenant_name ON public.clinical_procedures(tenant_id, name) WHERE tenant_id IS NOT NULL;
CREATE UNIQUE INDEX idx_clinical_procedures_system_name ON public.clinical_procedures(name) WHERE tenant_id IS NULL;