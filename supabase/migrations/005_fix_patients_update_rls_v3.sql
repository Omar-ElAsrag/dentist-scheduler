-- =============================================================================
-- Fix: patients_update_tenant RLS policy (v3)
-- Use public.user_tenant_id() and public.user_role() helper functions
-- (they were created in public schema, not auth, due to Supabase restrictions)
-- =============================================================================

DROP POLICY IF EXISTS patients_update_tenant ON public.patients;

CREATE POLICY patients_update_tenant ON public.patients
    FOR UPDATE USING (
        tenant_id = public.user_tenant_id()
        AND (
            public.user_role() IN ('admin', 'receptionist')
            OR clinic_id IN (SELECT dc.clinic_id FROM public.dentist_clinics dc WHERE dc.dentist_id = auth.uid())
        )
    ) WITH CHECK (
        tenant_id = public.user_tenant_id()
    );