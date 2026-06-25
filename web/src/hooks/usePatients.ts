import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { supabase } from '../lib/supabase'
import { useAuth } from './useAuth'

export interface Patient {
  id: number
  tenant_id: string
  name: string
  phone_number: string | null
  date_of_birth: string | null
  gender: string | null
  systemic_conditions: string | null
  past_dental_treatments: string | null
  allergies: string | null
  general_notes: string | null
  is_in_progress: boolean
  created_date: string | null
  clinic_id: number | null
  assigned_dentist_id: string | null
  deleted_at: string | null
  created_at: string | null
  updated_at: string | null
}

export interface PatientListFilters {
  search?: string
  clinicId?: number | null
  dentistId?: string | null
  status?: boolean | null
}

export interface PatientListResponse {
  data: Patient[]
  count: number
}

export interface CreatePatientInput {
  name: string
  phone_number: string
  date_of_birth?: string | null
  gender?: string | null
  clinic_id: number | null
  assigned_dentist_id?: string | null
  systemic_conditions?: string | null
  past_dental_treatments?: string | null
  allergies?: string | null
  general_notes?: string | null
}

export interface UpdatePatientInput {
  id: number
  name?: string
  phone_number?: string
  date_of_birth?: string | null
  gender?: string | null
  clinic_id?: number | null
  assigned_dentist_id?: string | null
  systemic_conditions?: string | null
  past_dental_treatments?: string | null
  allergies?: string | null
  general_notes?: string | null
  is_in_progress?: boolean
}

export function usePatientList(filters: PatientListFilters, page: number, pageSize = 20) {
  const { tenantId } = useAuth()

  return useQuery({
    queryKey: ['patients', 'list', filters, page, pageSize],
    queryFn: async () => {
      if (!tenantId) return { data: [], count: 0 } as PatientListResponse

      let query = supabase
        .from('patients')
        .select('*', { count: 'exact' })
        .eq('tenant_id', tenantId)
        .is('deleted_at', null)
        .order('created_at', { ascending: false })

      if (filters.search) {
        query = query.ilike('name', `%${filters.search}%`)
      }
      if (filters.clinicId !== undefined && filters.clinicId !== null) {
        query = query.eq('clinic_id', filters.clinicId)
      }
      if (filters.dentistId !== undefined && filters.dentistId !== null) {
        query = query.eq('assigned_dentist_id', filters.dentistId)
      }
      if (filters.status !== undefined && filters.status !== null) {
        query = query.eq('is_in_progress', filters.status)
      }

      const from = (page - 1) * pageSize
      const to = from + pageSize - 1
      query = query.range(from, to)

      const { data, count, error } = await query
      if (error) throw error
      return { data: (data as Patient[]) ?? [], count: count ?? 0 }
    },
    staleTime: 30_000,
    placeholderData: (prev) => prev,
  })
}

export function usePatientDetail(id: number | null) {
  const { tenantId } = useAuth()

  return useQuery({
    queryKey: ['patients', 'detail', id],
    queryFn: async () => {
      if (!id || !tenantId) throw new Error('Missing patient ID or tenant')

      const { data, error } = await supabase
        .from('patients')
        .select('*')
        .eq('id', id)
        .eq('tenant_id', tenantId)
        .is('deleted_at', null)
        .single()

      if (error) throw error
      return data as Patient
    },
    enabled: !!id && !!tenantId,
    staleTime: 30_000,
  })
}

export function useCreatePatient() {
  const queryClient = useQueryClient()
  const { tenantId } = useAuth()

  return useMutation({
    mutationFn: async (input: CreatePatientInput) => {
      if (!tenantId) throw new Error('No tenant context')

      const { data, error } = await supabase
        .from('patients')
        .insert({
          tenant_id: tenantId,
          name: input.name,
          phone_number: input.phone_number,
          date_of_birth: input.date_of_birth ?? null,
          gender: input.gender ?? null,
          clinic_id: input.clinic_id,
          assigned_dentist_id: input.assigned_dentist_id ?? null,
          systemic_conditions: input.systemic_conditions ?? null,
          past_dental_treatments: input.past_dental_treatments ?? null,
          allergies: input.allergies ?? null,
          general_notes: input.general_notes ?? null,
          is_in_progress: true,
          created_date: new Date().toISOString().split('T')[0],
        })
        .select()
        .single()

      if (error) throw error
      return data as Patient
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['patients'] })
    },
  })
}

export function useUpdatePatient() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (input: UpdatePatientInput) => {
      const { id, ...updates } = input
      const { data, error } = await supabase
        .from('patients')
        .update(updates)
        .eq('id', id)
        .select()
        .single()

      if (error) throw error
      return data as Patient
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['patients'] })
    },
  })
}

export function useSoftDeletePatient() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (patientId: number) => {
      const { error } = await supabase
        .rpc('soft_delete_patient', { p_patient_id: patientId })

      if (error) throw error
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['patients', 'list'] })
    },
  })
}

export function useClinics() {
  const { tenantId } = useAuth()

  return useQuery({
    queryKey: ['clinics'],
    queryFn: async () => {
      if (!tenantId) return []

      const { data, error } = await supabase
        .from('clinics')
        .select('id, name')
        .eq('tenant_id', tenantId)
        .order('name', { ascending: true })

      if (error) throw error
      return data as { id: number; name: string }[]
    },
    staleTime: 60_000,
  })
}

export function useDentists() {
  const { tenantId } = useAuth()

  return useQuery({
    queryKey: ['dentists'],
    queryFn: async () => {
      if (!tenantId) return []

      const { data, error } = await supabase
        .from('profiles')
        .select('id, full_name')
        .eq('tenant_id', tenantId)
        .eq('role', 'dentist')
        .order('full_name', { ascending: true })

      if (error) throw error
      return data as { id: string; full_name: string }[]
    },
    staleTime: 60_000,
  })
}
