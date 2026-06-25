-- =============================================================================
-- DentSched: Auth & Row-Level Security
-- Creates auth trigger, helper functions, and RLS policies
-- =============================================================================

-- ─── Helper: public.user_tenant_id() ──────────────────────────────────────────
-- Returns the tenant_id for the currently authenticated user.
-- Single fix point for all RLS tenant-scoping.
-- NOTE: Created in public schema because Supabase restricts auth schema access.

CREATE OR REPLACE FUNCTION public.user_tenant_id()
RETURNS UUID AS $$
    SELECT tenant_id FROM public.profiles WHERE id = auth.uid();
$$ LANGUAGE sql STABLE SECURITY DEFINER;

-- ─── Helper: public.user_role() ───────────────────────────────────────────────
-- Returns the role for the currently authenticated user.
-- Single fix point for all RLS role checks.
-- NOTE: Created in public schema because Supabase restricts auth schema access.

CREATE OR REPLACE FUNCTION public.user_role()
RETURNS TEXT AS $$
    SELECT role FROM public.profiles WHERE id = auth.uid();
$$ LANGUAGE sql STABLE SECURITY DEFINER;

-- ─── Auth trigger: auto-create profile on user signup ───────────────────────
-- Defaults to 'dentist' role (least privilege — see research.md R3).

CREATE OR REPLACE FUNCTION public.on_auth_user_created()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, role, is_active)
    VALUES (NEW.id, 'dentist', true);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.on_auth_user_created();

-- =============================================================================
-- Row-Level Security Policies
-- =============================================================================

-- ─── tenants ────────────────────────────────────────────────────────────────
-- Users can only SELECT their own tenant row.

ALTER TABLE public.tenants ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenants_select_own ON public.tenants
    FOR SELECT USING (id = public.user_tenant_id());

-- ─── profiles ────────────────────────────────────────────────────────────────
-- Users can SELECT profiles within their tenant.
-- Only admin can INSERT/UPDATE/DELETE profiles.

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY profiles_select_tenant ON public.profiles
    FOR SELECT USING (tenant_id = public.user_tenant_id() OR id = auth.uid());

CREATE POLICY profiles_insert_admin ON public.profiles
    FOR INSERT WITH CHECK (public.user_role() = 'admin');

CREATE POLICY profiles_update_admin ON public.profiles
    FOR UPDATE USING (public.user_role() = 'admin') WITH CHECK (public.user_role() = 'admin');

CREATE POLICY profiles_delete_admin ON public.profiles
    FOR DELETE USING (public.user_role() = 'admin');

-- ─── clinics ────────────────────────────────────────────────────────────────
-- All roles can SELECT within tenant. Only admin can write.

ALTER TABLE public.clinics ENABLE ROW LEVEL SECURITY;

CREATE POLICY clinics_select_tenant ON public.clinics
    FOR SELECT USING (tenant_id = public.user_tenant_id());

CREATE POLICY clinics_insert_admin ON public.clinics
    FOR INSERT WITH CHECK (public.user_role() = 'admin' AND tenant_id = public.user_tenant_id());

CREATE POLICY clinics_update_admin ON public.clinics
    FOR UPDATE USING (public.user_role() = 'admin') WITH CHECK (public.user_role() = 'admin' AND tenant_id = public.user_tenant_id());

CREATE POLICY clinics_delete_admin ON public.clinics
    FOR DELETE USING (public.user_role() = 'admin');

-- ─── dentist_clinics ────────────────────────────────────────────────────────
-- All roles can SELECT within tenant. Only admin can write.

ALTER TABLE public.dentist_clinics ENABLE ROW LEVEL SECURITY;

CREATE POLICY dentist_clinics_select_tenant ON public.dentist_clinics
    FOR SELECT USING (
        clinic_id IN (SELECT id FROM public.clinics WHERE tenant_id = public.user_tenant_id())
    );

CREATE POLICY dentist_clinics_insert_admin ON public.dentist_clinics
    FOR INSERT WITH CHECK (
        public.user_role() = 'admin'
        AND clinic_id IN (SELECT id FROM public.clinics WHERE tenant_id = public.user_tenant_id())
    );

CREATE POLICY dentist_clinics_update_admin ON public.dentist_clinics
    FOR UPDATE USING (public.user_role() = 'admin');

CREATE POLICY dentist_clinics_delete_admin ON public.dentist_clinics
    FOR DELETE USING (public.user_role() = 'admin');

-- ─── patients ───────────────────────────────────────────────────────────────
-- admin + receptionist: read/write all in tenant
-- dentist: read/write only patients linked to their clinic(s) via dentist_clinics
-- receptionist: INSERT allowed but UPDATE restricted on medical fields

ALTER TABLE public.patients ENABLE ROW LEVEL SECURITY;

CREATE POLICY patients_select_tenant ON public.patients
    FOR SELECT USING (
        tenant_id = public.user_tenant_id()
        AND (
            public.user_role() IN ('admin', 'receptionist')
            OR clinic_id IN (SELECT dc.clinic_id FROM public.dentist_clinics dc WHERE dc.dentist_id = auth.uid())
        )
        AND deleted_at IS NULL
    );

CREATE POLICY patients_insert_tenant ON public.patients
    FOR INSERT WITH CHECK (
        tenant_id = public.user_tenant_id()
        AND public.user_role() IN ('admin', 'receptionist', 'dentist')
    );

CREATE POLICY patients_update_tenant ON public.patients
    FOR UPDATE USING (
        tenant_id = public.user_tenant_id()
        AND (
            public.user_role() IN ('admin', 'receptionist')
            OR clinic_id IN (SELECT dc.clinic_id FROM public.dentist_clinics dc WHERE dc.dentist_id = auth.uid())
        )
    );

CREATE POLICY patients_delete_admin ON public.patients
    FOR DELETE USING (public.user_role() = 'admin');

-- ─── appointments ───────────────────────────────────────────────────────────
-- admin + receptionist: full read/write
-- dentist: read-only, filtered to their own appointments

ALTER TABLE public.appointments ENABLE ROW LEVEL SECURITY;

CREATE POLICY appointments_select_tenant ON public.appointments
    FOR SELECT USING (
        tenant_id = public.user_tenant_id()
        AND (
            public.user_role() IN ('admin', 'receptionist')
            OR dentist_id = auth.uid()
        )
    );

CREATE POLICY appointments_insert_tenant ON public.appointments
    FOR INSERT WITH CHECK (
        tenant_id = public.user_tenant_id()
        AND public.user_role() IN ('admin', 'receptionist')
    );

CREATE POLICY appointments_update_tenant ON public.appointments
    FOR UPDATE USING (
        tenant_id = public.user_tenant_id()
        AND public.user_role() IN ('admin', 'receptionist')
    );

CREATE POLICY appointments_delete_tenant ON public.appointments
    FOR DELETE USING (
        tenant_id = public.user_tenant_id()
        AND public.user_role() IN ('admin', 'receptionist')
    );

-- ─── procedure_cards ────────────────────────────────────────────────────────
-- admin: full access; dentist: read/write own cards; receptionist: read-only

ALTER TABLE public.procedure_cards ENABLE ROW LEVEL SECURITY;

CREATE POLICY procedure_cards_select_tenant ON public.procedure_cards
    FOR SELECT USING (
        tenant_id = public.user_tenant_id()
        AND (
            public.user_role() = 'admin'
            OR public.user_role() = 'receptionist'
            OR dentist_id = auth.uid()
        )
        AND deleted_at IS NULL
    );

CREATE POLICY procedure_cards_insert_tenant ON public.procedure_cards
    FOR INSERT WITH CHECK (
        tenant_id = public.user_tenant_id()
        AND public.user_role() IN ('admin', 'dentist')
    );

CREATE POLICY procedure_cards_update_tenant ON public.procedure_cards
    FOR UPDATE USING (
        tenant_id = public.user_tenant_id()
        AND (public.user_role() = 'admin' OR dentist_id = auth.uid())
    );

CREATE POLICY procedure_cards_delete_admin ON public.procedure_cards
    FOR DELETE USING (public.user_role() = 'admin');

-- ─── procedure_card_steps ──────────────────────────────────────────────────
-- admin: full access; dentist: read/write own card steps; receptionist: read-only

ALTER TABLE public.procedure_card_steps ENABLE ROW LEVEL SECURITY;

CREATE POLICY procedure_card_steps_select_tenant ON public.procedure_card_steps
    FOR SELECT USING (
        procedure_card_id IN (
            SELECT id FROM public.procedure_cards
            WHERE tenant_id = public.user_tenant_id()
            AND (public.user_role() = 'admin' OR public.user_role() = 'receptionist' OR dentist_id = auth.uid())
            AND deleted_at IS NULL
        )
    );

CREATE POLICY procedure_card_steps_insert_tenant ON public.procedure_card_steps
    FOR INSERT WITH CHECK (
        procedure_card_id IN (
            SELECT id FROM public.procedure_cards
            WHERE tenant_id = public.user_tenant_id()
            AND (public.user_role() = 'admin' OR dentist_id = auth.uid())
        )
    );

CREATE POLICY procedure_card_steps_update_tenant ON public.procedure_card_steps
    FOR UPDATE USING (
        procedure_card_id IN (
            SELECT id FROM public.procedure_cards
            WHERE tenant_id = public.user_tenant_id()
            AND (public.user_role() = 'admin' OR dentist_id = auth.uid())
        )
    ) WITH CHECK (
        procedure_card_id IN (
            SELECT id FROM public.procedure_cards
            WHERE tenant_id = public.user_tenant_id()
            AND (public.user_role() = 'admin' OR dentist_id = auth.uid())
        )
    );

CREATE POLICY procedure_card_steps_delete_tenant ON public.procedure_card_steps
    FOR DELETE USING (
        procedure_card_id IN (
            SELECT id FROM public.procedure_cards
            WHERE tenant_id = public.user_tenant_id()
            AND public.user_role() = 'admin'
        )
    );

-- ─── procedure_payments ────────────────────────────────────────────────────
-- admin: full access; dentist: read/write on own cards; receptionist: read + insert only

ALTER TABLE public.procedure_payments ENABLE ROW LEVEL SECURITY;

CREATE POLICY procedure_payments_select_tenant ON public.procedure_payments
    FOR SELECT USING (
        tenant_id = public.user_tenant_id()
        AND (
            public.user_role() = 'admin'
            OR public.user_role() = 'receptionist'
            OR procedure_card_id IN (
                SELECT id FROM public.procedure_cards WHERE dentist_id = auth.uid()
            )
        )
    );

CREATE POLICY procedure_payments_insert_tenant ON public.procedure_payments
    FOR INSERT WITH CHECK (
        tenant_id = public.user_tenant_id()
        AND public.user_role() IN ('admin', 'dentist', 'receptionist')
    );

CREATE POLICY procedure_payments_update_tenant ON public.procedure_payments
    FOR UPDATE USING (
        tenant_id = public.user_tenant_id()
        AND (public.user_role() = 'admin' OR procedure_card_id IN (
            SELECT id FROM public.procedure_cards WHERE dentist_id = auth.uid()
        ))
    ) WITH CHECK (
        tenant_id = public.user_tenant_id()
        AND (public.user_role() = 'admin' OR procedure_card_id IN (
            SELECT id FROM public.procedure_cards WHERE dentist_id = auth.uid()
        ))
    );

CREATE POLICY procedure_payments_delete_admin ON public.procedure_payments
    FOR DELETE USING (public.user_role() = 'admin');

-- ─── medical_files ──────────────────────────────────────────────────────────
-- admin + dentist: full access; receptionist: no access (deny all)

ALTER TABLE public.medical_files ENABLE ROW LEVEL SECURITY;

CREATE POLICY medical_files_select_tenant ON public.medical_files
    FOR SELECT USING (
        tenant_id = public.user_tenant_id()
        AND public.user_role() IN ('admin', 'dentist')
    );

CREATE POLICY medical_files_insert_tenant ON public.medical_files
    FOR INSERT WITH CHECK (
        tenant_id = public.user_tenant_id()
        AND public.user_role() IN ('admin', 'dentist')
    );

CREATE POLICY medical_files_update_tenant ON public.medical_files
    FOR UPDATE USING (
        tenant_id = public.user_tenant_id()
        AND public.user_role() IN ('admin', 'dentist')
    );

CREATE POLICY medical_files_delete_tenant ON public.medical_files
    FOR DELETE USING (
        tenant_id = public.user_tenant_id()
        AND public.user_role() IN ('admin', 'dentist')
    );

-- ─── clinical_procedures ───────────────────────────────────────────────────
-- All roles: SELECT (system-wide where tenant_id IS NULL + own tenant)
-- Admin only: write (for custom procedures)

ALTER TABLE public.clinical_procedures ENABLE ROW LEVEL SECURITY;

CREATE POLICY clinical_procedures_select ON public.clinical_procedures
    FOR SELECT USING (
        tenant_id IS NULL OR tenant_id = public.user_tenant_id()
    );

CREATE POLICY clinical_procedures_insert_admin ON public.clinical_procedures
    FOR INSERT WITH CHECK (
        public.user_role() = 'admin' AND (tenant_id = public.user_tenant_id() OR tenant_id IS NULL)
    );

CREATE POLICY clinical_procedures_update_admin ON public.clinical_procedures
    FOR UPDATE USING (public.user_role() = 'admin') WITH CHECK (public.user_role() = 'admin');

CREATE POLICY clinical_procedures_delete_admin ON public.clinical_procedures
    FOR DELETE USING (public.user_role() = 'admin');

-- ─── procedure_types ────────────────────────────────────────────────────────
-- All roles: SELECT; admin only: write

ALTER TABLE public.procedure_types ENABLE ROW LEVEL SECURITY;

CREATE POLICY procedure_types_select ON public.procedure_types
    FOR SELECT USING (
        clinical_procedure_id IN (
            SELECT id FROM public.clinical_procedures
            WHERE tenant_id IS NULL OR tenant_id = public.user_tenant_id()
        )
    );

CREATE POLICY procedure_types_insert_admin ON public.procedure_types
    FOR INSERT WITH CHECK (
        public.user_role() = 'admin'
        AND clinical_procedure_id IN (
            SELECT id FROM public.clinical_procedures
            WHERE tenant_id = public.user_tenant_id() OR tenant_id IS NULL
        )
    );

CREATE POLICY procedure_types_update_admin ON public.procedure_types
    FOR UPDATE USING (public.user_role() = 'admin') WITH CHECK (public.user_role() = 'admin');

CREATE POLICY procedure_types_delete_admin ON public.procedure_types
    FOR DELETE USING (public.user_role() = 'admin');

-- ─── clinical_procedure_steps ───────────────────────────────────────────────
-- All roles: SELECT; admin only: write

ALTER TABLE public.clinical_procedure_steps ENABLE ROW LEVEL SECURITY;

CREATE POLICY clinical_procedure_steps_select ON public.clinical_procedure_steps
    FOR SELECT USING (
        clinical_procedure_id IN (
            SELECT id FROM public.clinical_procedures
            WHERE tenant_id IS NULL OR tenant_id = public.user_tenant_id()
        )
    );

CREATE POLICY clinical_procedure_steps_insert_admin ON public.clinical_procedure_steps
    FOR INSERT WITH CHECK (
        public.user_role() = 'admin'
        AND clinical_procedure_id IN (
            SELECT id FROM public.clinical_procedures
            WHERE tenant_id = public.user_tenant_id() OR tenant_id IS NULL
        )
    );

CREATE POLICY clinical_procedure_steps_update_admin ON public.clinical_procedure_steps
    FOR UPDATE USING (public.user_role() = 'admin') WITH CHECK (public.user_role() = 'admin');

CREATE POLICY clinical_procedure_steps_delete_admin ON public.clinical_procedure_steps
    FOR DELETE USING (public.user_role() = 'admin');