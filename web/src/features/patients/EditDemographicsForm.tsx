import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useUpdatePatient } from '../../hooks/usePatients'
import type { Patient, UpdatePatientInput } from '../../hooks/usePatients'

interface EditDemographicsFormProps {
  patient: Patient
  clinics: { id: number; name: string }[]
  dentists: { id: string; full_name: string }[]
  onSaved: () => void
  onCancel: () => void
}

export default function EditDemographicsForm({ patient, clinics, dentists, onSaved, onCancel }: EditDemographicsFormProps) {
  const { t } = useTranslation()
  const updatePatient = useUpdatePatient()

  const [form, setForm] = useState({
    name: patient.name ?? '',
    phone_number: patient.phone_number ?? '',
    date_of_birth: patient.date_of_birth ?? '',
    gender: patient.gender ?? '',
    clinic_id: patient.clinic_id ? String(patient.clinic_id) : '',
    assigned_dentist_id: patient.assigned_dentist_id ?? '',
  })

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const input: UpdatePatientInput = {
      id: patient.id,
      name: form.name.trim() || undefined,
      phone_number: form.phone_number.trim() || undefined,
      date_of_birth: form.date_of_birth || null,
      gender: form.gender || null,
      clinic_id: form.clinic_id ? Number(form.clinic_id) : null,
      assigned_dentist_id: form.assigned_dentist_id || null,
    }

    await updatePatient.mutateAsync(input)
    onSaved()
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.form.name')}</label>
          <input
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.form.phone')}</label>
          <input
            value={form.phone_number}
            onChange={(e) => setForm({ ...form, phone_number: e.target.value })}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.form.dateOfBirth')}</label>
          <input
            type="date"
            value={form.date_of_birth}
            onChange={(e) => setForm({ ...form, date_of_birth: e.target.value })}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.form.gender')}</label>
          <select
            value={form.gender}
            onChange={(e) => setForm({ ...form, gender: e.target.value })}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
          >
            <option value="">—</option>
            <option value="male">{t('patients.form.male')}</option>
            <option value="female">{t('patients.form.female')}</option>
          </select>
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.form.clinic')}</label>
          <select
            value={form.clinic_id}
            onChange={(e) => setForm({ ...form, clinic_id: e.target.value })}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
          >
            <option value="">{t('patients.form.selectClinic')}</option>
            {clinics.map((clinic) => (
              <option key={clinic.id} value={clinic.id}>
                {clinic.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.form.dentist')}</label>
          <select
            value={form.assigned_dentist_id}
            onChange={(e) => setForm({ ...form, assigned_dentist_id: e.target.value })}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
          >
            <option value="">{t('patients.form.selectDentist')}</option>
            {dentists.map((dentist) => (
              <option key={dentist.id} value={dentist.id}>
                {dentist.full_name}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="flex items-center justify-end gap-3">
        <button
          type="button"
          onClick={onCancel}
          className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-100"
        >
          {t('patients.form.cancel')}
        </button>
        <button
          type="submit"
          disabled={updatePatient.isPending}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {updatePatient.isPending ? '...' : t('patients.form.save')}
        </button>
      </div>
    </form>
  )
}
