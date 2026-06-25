import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import { supabase } from '../../lib/supabase'

interface PatientTabsProps {
  patientId: number
  tenantId: string
}

type TabKey = 'procedureCards' | 'appointments' | 'payments' | 'medicalFiles'

const tabs: { key: TabKey; labelKey: string }[] = [
  { key: 'procedureCards', labelKey: 'patients.detail.tabs.procedureCards' },
  { key: 'appointments', labelKey: 'patients.detail.tabs.appointments' },
  { key: 'payments', labelKey: 'patients.detail.tabs.payments' },
  { key: 'medicalFiles', labelKey: 'patients.detail.tabs.medicalFiles' },
]

function ProcedureCardsTab({ patientId, tenantId }: PatientTabsProps) {
  const { t } = useTranslation()
  const { data, isLoading } = useQuery({
    queryKey: ['patient-procedure-cards', patientId],
    queryFn: async () => {
      const { data, error } = await supabase
        .from('procedure_cards')
        .select('id, status, date_created, treatment_fee, clinical_procedure_id')
        .eq('patient_id', patientId)
        .eq('tenant_id', tenantId)
        .order('date_created', { ascending: false })
        .limit(50)
      if (error) throw error
      return data
    },
    enabled: !!patientId && !!tenantId,
  })

  if (isLoading) return <div className="h-20 animate-pulse rounded bg-gray-100" />
  if (!data?.length) return <p className="py-8 text-center text-sm text-gray-500">{t('patients.list.empty')}</p>

  return (
    <div className="space-y-2">
      {data.map((card) => (
        <div key={card.id} className="rounded-lg border border-gray-200 p-3 text-sm">
          <div className="flex items-center justify-between">
            <span className="font-medium text-gray-900">#{card.id}</span>
            <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs text-blue-700">{card.status}</span>
          </div>
          <p className="mt-1 text-gray-500">{card.date_created}</p>
          <p className="font-medium text-gray-700">{card.treatment_fee?.toLocaleString()} EGP</p>
        </div>
      ))}
    </div>
  )
}

function AppointmentsTab({ patientId, tenantId }: PatientTabsProps) {
  const { t } = useTranslation()
  const { data, isLoading } = useQuery({
    queryKey: ['patient-appointments', patientId],
    queryFn: async () => {
      const { data, error } = await supabase
        .from('appointments')
        .select('id, date, time, status, dentist_id')
        .eq('patient_id', patientId)
        .eq('tenant_id', tenantId)
        .order('date', { ascending: false })
        .limit(50)
      if (error) throw error
      return data
    },
    enabled: !!patientId && !!tenantId,
  })

  if (isLoading) return <div className="h-20 animate-pulse rounded bg-gray-100" />
  if (!data?.length) return <p className="py-8 text-center text-sm text-gray-500">{t('patients.list.empty')}</p>

  return (
    <div className="space-y-2">
      {data.map((apt) => (
        <div key={apt.id} className="rounded-lg border border-gray-200 p-3 text-sm">
          <div className="flex items-center justify-between">
            <span className="font-medium text-gray-900">{apt.date}</span>
            <span
              className={`rounded-full px-2 py-0.5 text-xs ${
                apt.status === 'scheduled'
                  ? 'bg-yellow-100 text-yellow-700'
                  : apt.status === 'completed'
                    ? 'bg-green-100 text-green-700'
                    : 'bg-red-100 text-red-700'
              }`}
            >
              {apt.status}
            </span>
          </div>
          <p className="text-gray-500">{apt.time}</p>
        </div>
      ))}
    </div>
  )
}

function PaymentsTab({ patientId, tenantId }: PatientTabsProps) {
  const { t } = useTranslation()
  const { data, isLoading } = useQuery({
    queryKey: ['patient-payments', patientId],
    queryFn: async () => {
      const { data: cards } = await supabase
        .from('procedure_cards')
        .select('id')
        .eq('patient_id', patientId)
        .eq('tenant_id', tenantId)
        .is('deleted_at', null)

      const cardIds = (cards ?? []).map((c) => c.id)

      if (cardIds.length === 0) return []

      const { data, error } = await supabase
        .from('procedure_payments')
        .select('id, amount, payment_at, notes, procedure_card_id')
        .eq('tenant_id', tenantId)
        .in('procedure_card_id', cardIds)
        .order('payment_at', { ascending: false })
        .limit(50)

      if (error) throw error
      return data
    },
    enabled: !!patientId && !!tenantId,
  })

  if (isLoading) return <div className="h-20 animate-pulse rounded bg-gray-100" />
  if (!data?.length) return <p className="py-8 text-center text-sm text-gray-500">{t('patients.list.empty')}</p>

  return (
    <div className="space-y-2">
      {data.map((payment) => (
        <div key={payment.id} className="rounded-lg border border-gray-200 p-3 text-sm">
          <div className="flex items-center justify-between">
            <span className="font-medium text-gray-900">{payment.amount?.toLocaleString()} EGP</span>
            <span className="text-gray-500">{payment.payment_at?.split('T')[0]}</span>
          </div>
          {payment.notes && <p className="mt-1 text-gray-500">{payment.notes}</p>}
        </div>
      ))}
    </div>
  )
}

function MedicalFilesTab({ patientId, tenantId }: PatientTabsProps) {
  const { t } = useTranslation()
  const { data, isLoading } = useQuery({
    queryKey: ['patient-medical-files', patientId],
    queryFn: async () => {
      const { data, error } = await supabase
        .from('medical_files')
        .select('id, title, file_type, created_at')
        .eq('patient_id', patientId)
        .eq('tenant_id', tenantId)
        .order('created_at', { ascending: false })
        .limit(50)
      if (error) throw error
      return data
    },
    enabled: !!patientId && !!tenantId,
  })

  if (isLoading) return <div className="h-20 animate-pulse rounded bg-gray-100" />
  if (!data?.length) return <p className="py-8 text-center text-sm text-gray-500">{t('patients.list.empty')}</p>

  return (
    <div className="space-y-2">
      {data.map((file) => (
        <div key={file.id} className="rounded-lg border border-gray-200 p-3 text-sm">
          <div className="flex items-center justify-between">
            <span className="font-medium text-gray-900">{file.title}</span>
            <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-600">{file.file_type}</span>
          </div>
          <p className="text-gray-500">{file.created_at?.split('T')[0]}</p>
        </div>
      ))}
    </div>
  )
}

export default function PatientTabs({ patientId, tenantId }: PatientTabsProps) {
  const { t } = useTranslation()
  const [activeTab, setActiveTab] = useState<TabKey>('procedureCards')

  const tabComponents: Record<TabKey, React.FC<PatientTabsProps>> = {
    procedureCards: ProcedureCardsTab,
    appointments: AppointmentsTab,
    payments: PaymentsTab,
    medicalFiles: MedicalFilesTab,
  }

  const ActiveComponent = tabComponents[activeTab]

  return (
    <div>
      <div className="flex border-b border-gray-200">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2.5 text-sm font-medium transition-colors ${
              activeTab === tab.key
                ? 'border-b-2 border-blue-600 text-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            {t(tab.labelKey)}
          </button>
        ))}
      </div>
      <div className="pt-4">
        <ActiveComponent patientId={patientId} tenantId={tenantId} />
      </div>
    </div>
  )
}
