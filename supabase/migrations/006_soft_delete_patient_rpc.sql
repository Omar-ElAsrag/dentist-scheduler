-- =============================================================================
-- Fix: SECURITY DEFINER RPC function for soft-deleting patients
-- Bypasses RLS corruption on patients table by executing as postgres owner.
-- Security is enforced inside the function body (tenant + role checks).
-- =============================================================================

CREATE OR REPLACE FUNCTION public.soft_delete_patient(p_patient_id INT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_tenant_id UUID;
    v_role TEXT;
    v_patient_tenant_id UUID;
BEGIN
    -- Get the caller's tenant_id and role
    SELECT tenant_id, role INTO v_tenant_id, v_role
    FROM public.profiles
    WHERE id = auth.uid();

    -- Verify the user is authenticated
    IF v_tenant_id IS NULL THEN
        RAISE EXCEPTION 'User not authenticated or no profile found';
    END IF;

    -- Verify the user is an admin
    IF v_role != 'admin' THEN
        RAISE EXCEPTION 'Only admins can delete patients';
    END IF;

    -- Get the patient's tenant_id and verify it exists
    SELECT tenant_id INTO v_patient_tenant_id
    FROM public.patients
    WHERE id = p_patient_id AND deleted_at IS NULL;

    IF v_patient_tenant_id IS NULL THEN
        RAISE EXCEPTION 'Patient not found or already deleted';
    END IF;

    -- Verify tenant isolation
    IF v_patient_tenant_id != v_tenant_id THEN
        RAISE EXCEPTION 'Patient does not belong to your tenant';
    END IF;

    -- Perform the soft delete
    UPDATE public.patients
    SET deleted_at = now()
    WHERE id = p_patient_id AND tenant_id = v_tenant_id;

    RETURN jsonb_build_object('success', true, 'patient_id', p_patient_id);
END;
$$;

-- Grant execute permission to authenticated users
GRANT EXECUTE ON FUNCTION public.soft_delete_patient(INT) TO authenticated;