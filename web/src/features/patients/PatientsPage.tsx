import { useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { usePatientList, useClinics, useDentists } from '../../hooks/usePatients'
import PatientRow from './PatientRow'
import PatientsFilters from './PatientsFilters'
import NewPatientModal from './NewPatientModal'
import type { PatientListFilters } from '../../hooks/usePatients'

export default function PatientsPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [page, setPage] = useState(1)
  const [filters, setFilters] = useState<PatientListFilters>({})
  const [modalOpen, setModalOpen] = useState(false)

  const { data, isLoading, isError, refetch } = usePatientList(filters, page)
  const { data: clinics = [] } = useClinics()
  const { data: dentists = [] } = useDentists()

  const clinicMap = new Map(clinics.map((c) => [c.id, c.name]))
  const dentistMap = new Map(dentists.map((d) => [d.id, d.full_name]))

  const totalPages = data ? Math.ceil(data.count / 20) : 0

  const handleFilterChange = useCallback((newFilters: PatientListFilters) => {
    setFilters(newFilters)
    setPage(1)
  }, [])

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center py-20">
        <p className="mb-4 text-gray-600">{t('patients.list.loadError')}</p>
        <button
          onClick={() => refetch()}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm text-white transition-colors hover:bg-blue-700"
        >
          {t('patients.list.retry')}
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t('patients.list.title')}</h1>
        <button
          onClick={() => setModalOpen(true)}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
        >
          {t('patients.list.newPatient')}
        </button>
      </div>

      <PatientsFilters
        filters={filters}
        clinics={clinics}
        dentists={dentists}
        onFilterChange={handleFilterChange}
      />

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="h-12 animate-pulse rounded-lg bg-gray-100" />
          ))}
        </div>
      ) : data && data.data.length > 0 ? (
        <>
          <div className="overflow-x-auto rounded-lg border border-gray-200">
            <table className="w-full">
              <thead>
                <tr className="bg-gray-50 text-left text-xs font-semibold uppercase text-gray-500">
                  <th className="px-4 py-3">{t('patients.list.name')}</th>
                  <th className="px-4 py-3">{t('patients.list.clinic')}</th>
                  <th className="px-4 py-3">{t('patients.list.dentist')}</th>
                  <th className="px-4 py-3">{t('patients.list.status')}</th>
                </tr>
              </thead>
              <tbody>
                {data.data.map((patient) => (
                  <PatientRow
                    key={patient.id}
                    patient={patient}
                    clinicName={patient.clinic_id ? clinicMap.get(patient.clinic_id) : undefined}
                    dentistName={
                      patient.assigned_dentist_id
                        ? dentistMap.get(patient.assigned_dentist_id)
                        : undefined
                    }
                    onClick={() => navigate(`/patients/${patient.id}`)}
                  />
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={page <= 1}
                className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {t('patients.list.previous')}
              </button>
              <span className="text-sm text-gray-600">
                {t('patients.list.pageInfo', { page, total: totalPages })}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                disabled={page >= totalPages}
                className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {t('patients.list.next')}
              </button>
            </div>
          )}
        </>
      ) : (
        <div className="py-20 text-center">
          <p className="text-lg text-gray-500">
            {filters.search || filters.clinicId !== undefined || filters.dentistId !== undefined
              ? t('patients.list.noSearchResults')
              : t('patients.list.empty')}
          </p>
        </div>
      )}

      {modalOpen && <NewPatientModal onClose={() => setModalOpen(false)} />}
    </div>
  )
}
