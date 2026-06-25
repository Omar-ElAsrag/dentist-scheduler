import { useTranslation } from 'react-i18next'
import type { PatientListFilters } from '../../hooks/usePatients'

interface PatientsFiltersProps {
  filters: PatientListFilters
  clinics: { id: number; name: string }[]
  dentists: { id: string; full_name: string }[]
  onFilterChange: (filters: PatientListFilters) => void
}

export default function PatientsFilters({ filters, clinics, dentists, onFilterChange }: PatientsFiltersProps) {
  const { t } = useTranslation()

  return (
    <div className="flex flex-wrap items-center gap-3">
      <input
        type="text"
        placeholder={t('patients.list.searchPlaceholder')}
        value={filters.search ?? ''}
        onChange={(e) => onFilterChange({ ...filters, search: e.target.value })}
        className="min-w-[200px] rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
      />

      <select
        value={filters.clinicId ?? ''}
        onChange={(e) =>
          onFilterChange({
            ...filters,
            clinicId: e.target.value ? Number(e.target.value) : null,
          })
        }
        className="rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
      >
        <option value="">{t('patients.list.allClinics')}</option>
        {clinics.map((clinic) => (
          <option key={clinic.id} value={clinic.id}>
            {clinic.name}
          </option>
        ))}
      </select>

      <select
        value={filters.dentistId ?? ''}
        onChange={(e) =>
          onFilterChange({
            ...filters,
            dentistId: e.target.value || null,
          })
        }
        className="rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
      >
        <option value="">{t('patients.list.allDentists')}</option>
        {dentists.map((dentist) => (
          <option key={dentist.id} value={dentist.id}>
            {dentist.full_name}
          </option>
        ))}
      </select>

      <select
        value={filters.status === null || filters.status === undefined ? '' : String(filters.status)}
        onChange={(e) =>
          onFilterChange({
            ...filters,
            status: e.target.value !== '' ? e.target.value === 'true' : null,
          })
        }
        className="rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
      >
        <option value="">{t('patients.list.allStatus')}</option>
        <option value="true">{t('patients.status.active')}</option>
        <option value="false">{t('patients.status.inactive')}</option>
      </select>
    </div>
  )
}
