-- =============================================================================
-- DentSched: Seed Data — 9 System-Wide Clinical Procedures
-- Uses ON CONFLICT DO NOTHING for idempotency (see research.md R6)
-- System-wide procedures have tenant_id = NULL
-- =============================================================================

-- 1. Endodontic TT — steps only
INSERT INTO public.clinical_procedures (tenant_id, name, has_types, display_order, is_custom, default_fee, default_lab_fee)
VALUES (NULL, 'Endodontic TT', false, 1, false, 1500.0, 0.0) ON CONFLICT (name) WHERE tenant_id IS NULL DO NOTHING;

WITH endo AS (
    SELECT id FROM public.clinical_procedures WHERE name = 'Endodontic TT' AND tenant_id IS NULL
)
INSERT INTO public.clinical_procedure_steps (clinical_procedure_id, step_name, display_order)
SELECT endo.id, s.step_name, s.display_order
FROM endo, (VALUES
    ('Access opening', 1),
    ('Working length determination', 2),
    ('Shaping', 3),
    ('Cleaning', 4),
    ('Obturation', 5)
) AS s(step_name, display_order)
ON CONFLICT DO NOTHING;

-- 2. Fixed Prosthodontic TT — types + shared steps
INSERT INTO public.clinical_procedures (tenant_id, name, has_types, display_order, is_custom, default_fee, default_lab_fee)
VALUES (NULL, 'Fixed Prosthodontic TT', true, 2, false, 1000.0, 150.0) ON CONFLICT (name) WHERE tenant_id IS NULL DO NOTHING;

WITH fixed AS (
    SELECT id FROM public.clinical_procedures WHERE name = 'Fixed Prosthodontic TT' AND tenant_id IS NULL
)
INSERT INTO public.procedure_types (clinical_procedure_id, name, materials, default_fee, default_lab_fee, material_fees)
SELECT fixed.id, t.name, t.materials, t.default_fee, t.default_lab_fee, t.material_fees
FROM fixed, (VALUES
    ('Single crown'::text, 'PFM,Zirconia,Composite,EMAX'::text, 800.0, 120.0, 'PFM:800:150,Zirconia:1200:200,Composite:900:120,EMAX:1100:180'::text),
    ('Bridge', 'PFM,Zirconia,Composite,EMAX', 2000.0, 300.0, 'PFM:2000:300,Zirconia:2800:400,Composite:1800:280,EMAX:2500:350'),
    ('Post and core build up', NULL, 500.0, 75.0, NULL),
    ('Overlay', 'PFM,Zirconia,Composite,EMAX', 700.0, 105.0, 'PFM:700:100,Zirconia:900:150,Composite:750:120,EMAX:850:140'),
    ('Endocrown', 'PFM,Zirconia,Composite,EMAX', 900.0, 135.0, 'PFM:900:130,Zirconia:1200:180,Composite:950:140,EMAX:1100:160'),
    ('Veneers', 'Composite,EMAX,Zirconia', 600.0, 90.0, 'Composite:600:80,EMAX:800:100,Zirconia:1000:120')
) AS t(name, materials, default_fee, default_lab_fee, material_fees)
ON CONFLICT DO NOTHING;

WITH fixed AS (
    SELECT id FROM public.clinical_procedures WHERE name = 'Fixed Prosthodontic TT' AND tenant_id IS NULL
)
INSERT INTO public.clinical_procedure_steps (clinical_procedure_id, step_name, display_order)
SELECT fixed.id, s.step_name, s.display_order
FROM fixed, (VALUES
    ('Preparation', 1),
    ('Impression', 2),
    ('Try in', 3),
    ('Delivery', 4)
) AS s(step_name, display_order)
ON CONFLICT DO NOTHING;

-- 3. Removable Prosthodontic TT — types + shared steps
INSERT INTO public.clinical_procedures (tenant_id, name, has_types, display_order, is_custom, default_fee, default_lab_fee)
VALUES (NULL, 'Removable Prosthodontic TT', true, 3, false, 2000.0, 300.0) ON CONFLICT (name) WHERE tenant_id IS NULL DO NOTHING;

WITH removable AS (
    SELECT id FROM public.clinical_procedures WHERE name = 'Removable Prosthodontic TT' AND tenant_id IS NULL
)
INSERT INTO public.procedure_types (clinical_procedure_id, name, default_fee, default_lab_fee)
SELECT removable.id, t.name, t.default_fee, t.default_lab_fee
FROM removable, (VALUES
    ('Complete denture class 1 KC'::text, 2500.0, 375.0),
    ('Class 2 KC', 2000.0, 300.0),
    ('Class 3 KC', 1500.0, 225.0),
    ('Class 4 KC', 1000.0, 150.0)
) AS t(name, default_fee, default_lab_fee)
ON CONFLICT DO NOTHING;

WITH removable AS (
    SELECT id FROM public.clinical_procedures WHERE name = 'Removable Prosthodontic TT' AND tenant_id IS NULL
)
INSERT INTO public.clinical_procedure_steps (clinical_procedure_id, step_name, display_order)
SELECT removable.id, s.step_name, s.display_order
FROM removable, (VALUES
    ('Primary impression', 1),
    ('Secondary impression', 2),
    ('Bite registration', 3),
    ('Try in', 4),
    ('Delivery', 5)
) AS s(step_name, display_order)
ON CONFLICT DO NOTHING;

-- 4. Oral Surgery — types only, no steps
INSERT INTO public.clinical_procedures (tenant_id, name, has_types, display_order, is_custom, default_fee, default_lab_fee)
VALUES (NULL, 'Oral Surgery', true, 4, false, 600.0, 0.0) ON CONFLICT (name) WHERE tenant_id IS NULL DO NOTHING;

WITH oral AS (
    SELECT id FROM public.clinical_procedures WHERE name = 'Oral Surgery' AND tenant_id IS NULL
)
INSERT INTO public.procedure_types (clinical_procedure_id, name, default_fee, default_lab_fee)
SELECT oral.id, t.name, t.default_fee, t.default_lab_fee
FROM oral, (VALUES
    ('Open extraction'::text, 400.0, 0.0),
    ('Surgical extraction', 800.0, 0.0)
) AS t(name, default_fee, default_lab_fee)
ON CONFLICT DO NOTHING;

-- 5. Periodontic TT — types only, no steps
INSERT INTO public.clinical_procedures (tenant_id, name, has_types, display_order, is_custom, default_fee, default_lab_fee)
VALUES (NULL, 'Periodontic TT', true, 5, false, 400.0, 0.0) ON CONFLICT (name) WHERE tenant_id IS NULL DO NOTHING;

WITH perio AS (
    SELECT id FROM public.clinical_procedures WHERE name = 'Periodontic TT' AND tenant_id IS NULL
)
INSERT INTO public.procedure_types (clinical_procedure_id, name, default_fee, default_lab_fee)
SELECT perio.id, t.name, t.default_fee, t.default_lab_fee
FROM perio, (VALUES
    ('Scaling'::text, 300.0, 0.0),
    ('Root planing', 500.0, 0.0)
) AS t(name, default_fee, default_lab_fee)
ON CONFLICT DO NOTHING;

-- 6. Operative TT — types + shared steps
INSERT INTO public.clinical_procedures (tenant_id, name, has_types, display_order, is_custom, default_fee, default_lab_fee)
VALUES (NULL, 'Operative TT', true, 6, false, 400.0, 0.0) ON CONFLICT (name) WHERE tenant_id IS NULL DO NOTHING;

WITH operative AS (
    SELECT id FROM public.clinical_procedures WHERE name = 'Operative TT' AND tenant_id IS NULL
)
INSERT INTO public.procedure_types (clinical_procedure_id, name, default_fee, default_lab_fee)
SELECT operative.id, t.name, t.default_fee, t.default_lab_fee
FROM operative, (VALUES
    ('Class 1'::text, 300.0, 0.0),
    ('Class 2', 400.0, 0.0),
    ('Class 3', 400.0, 0.0),
    ('Class 4', 800.0, 0.0),
    ('Class 5', 500.0, 0.0),
    ('Composite veneer', 600.0, 0.0),
    ('Composite crown', 700.0, 0.0)
) AS t(name, default_fee, default_lab_fee)
ON CONFLICT DO NOTHING;

WITH operative AS (
    SELECT id FROM public.clinical_procedures WHERE name = 'Operative TT' AND tenant_id IS NULL
)
INSERT INTO public.clinical_procedure_steps (clinical_procedure_id, step_name, display_order)
SELECT operative.id, s.step_name, s.display_order
FROM operative, (VALUES
    ('Caries removal', 1),
    ('Restoration completed', 2)
) AS s(step_name, display_order)
ON CONFLICT DO NOTHING;

-- 7. Pedodontics TT — types + type-specific steps for pulpotomy/pulpectomy
INSERT INTO public.clinical_procedures (tenant_id, name, has_types, display_order, is_custom, default_fee, default_lab_fee)
VALUES (NULL, 'Pedodontics TT', true, 7, false, 400.0, 0.0) ON CONFLICT (name) WHERE tenant_id IS NULL DO NOTHING;

WITH pedo AS (
    SELECT id FROM public.clinical_procedures WHERE name = 'Pedodontics TT' AND tenant_id IS NULL
)
INSERT INTO public.procedure_types (clinical_procedure_id, name, default_fee, default_lab_fee)
SELECT pedo.id, t.name, t.default_fee, t.default_lab_fee
FROM pedo, (VALUES
    ('Extraction'::text, 300.0, 0.0),
    ('Pulpotomy', 400.0, 0.0),
    ('Pulpectomy', 500.0, 0.0),
    ('Stainless steel crown', 600.0, 0.0),
    ('Ethatic crown', 500.0, 0.0),
    ('Floride session', 200.0, 0.0)
) AS t(name, default_fee, default_lab_fee)
ON CONFLICT DO NOTHING;

WITH pedo AS (
    SELECT id FROM public.clinical_procedures WHERE name = 'Pedodontics TT' AND tenant_id IS NULL
),
pedo_types AS (
    SELECT pt.id AS type_id, pt.name FROM public.procedure_types pt
    JOIN public.clinical_procedures cp ON pt.clinical_procedure_id = cp.id
    WHERE cp.name = 'Pedodontics TT' AND cp.tenant_id IS NULL
    AND pt.name IN ('Pulpotomy', 'Pulpectomy')
)
INSERT INTO public.clinical_procedure_steps (procedure_type_id, step_name, display_order)
SELECT pt.type_id, s.step_name, s.display_order
FROM pedo_types pt, (VALUES
    ('Pulp removal', 1),
    ('Obturation', 2)
) AS s(step_name, display_order)
ON CONFLICT DO NOTHING;

-- 8. General Examination — no types, no steps
INSERT INTO public.clinical_procedures (tenant_id, name, has_types, display_order, is_custom, default_fee, default_lab_fee)
VALUES (NULL, 'General Examination', false, 8, false, NULL, NULL) ON CONFLICT (name) WHERE tenant_id IS NULL DO NOTHING;

-- 9. Orthodontics — no types, no steps
INSERT INTO public.clinical_procedures (tenant_id, name, has_types, display_order, is_custom, default_fee, default_lab_fee)
VALUES (NULL, 'Orthodontics', false, 9, false, NULL, NULL) ON CONFLICT (name) WHERE tenant_id IS NULL DO NOTHING;