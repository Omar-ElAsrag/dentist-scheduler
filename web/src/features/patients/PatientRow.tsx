import { useTranslation } from 'react-i18next'
import type { Patient } from '../../hooks/usePatients'

interface PatientRowProps {
  patient: Patient
  clinicName?: string
  dentistName?: string
  onClick: () => void
}

export default function PatientRow({ patient, clinicName, dentistName, onClick }: PatientRowProps) {
  const { t } = useTranslation()

  return (
    <tr
      onClick={onClick}
      className="cursor-pointer border-b border-gray-100 transition-colors hover:bg-blue-50"
    >
      <td className="px-4 py-3 text-sm font-medium text-gray-900">{patient.name}</td>
      <td className="px-4 py-3 text-sm text-gray-600">{clinicName ?? '—'}</td>
      <td className="px-4 py-3 text-sm text-gray-600">{dentistName ?? '—'}</td>
      <td className="px-4 py-3 text-sm">
        <span
          className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${
            patient.is_in_progress
              ? 'bg-green-100 text-green-700'
              : 'bg-gray-100 text-gray-500'
          }`}
        >
          {patient.is_in_progress ? t('patients.status.active') : t('patients.status.inactive')}
        </span>
      </td>
    </tr>
  )
}
