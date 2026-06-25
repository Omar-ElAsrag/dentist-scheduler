import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useCreatePatient, useClinics, useDentists } from '../../hooks/usePatients'
import { useAuth } from '../../hooks/useAuth'

interface NewPatientModalProps {
  onClose: () => void
}

export default function NewPatientModal({ onClose }: NewPatientModalProps) {
  const { t } = useTranslation()
  const { role } = useAuth()
  const createPatient = useCreatePatient()
  const { data: clinics = [] } = useClinics()
  const { data: dentists = [] } = useDentists()

  const canEditMedical = role === 'admin' || role === 'dentist'

  const [form, setForm] = useState({
    name: '',
    phone_number: '',
    date_of_birth: '',
    gender: '',
    clinic_id: '',
    assigned_dentist_id: '',
    systemic_conditions: '',
    past_dental_treatments: '',
    allergies: '',
    general_notes: '',
  })

  const [errors, setErrors] = useState<Record<string, string>>({})

  function validate() {
    const errs: Record<string, string> = {}
    if (!form.name.trim()) errs.name = t('patients.form.requiredField')
    if (!form.phone_number.trim()) errs.phone_number = t('patients.form.requiredField')
    if (!form.clinic_id) errs.clinic_id = t('patients.form.requiredField')
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!validate()) return

    await createPatient.mutateAsync({
      name: form.name.trim(),
      phone_number: form.phone_number.trim(),
      date_of_birth: form.date_of_birth || null,
      gender: form.gender || null,
      clinic_id: form.clinic_id ? Number(form.clinic_id) : null,
      assigned_dentist_id: form.assigned_dentist_id || null,
      systemic_conditions: canEditMedical ? form.systemic_conditions || null : undefined,
      past_dental_treatments: canEditMedical ? form.past_dental_treatments || null : undefined,
      allergies: canEditMedical ? form.allergies || null : undefined,
      general_notes: form.general_notes || null,
    })

    onClose()
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-xl bg-white p-6 shadow-xl">
        <h2 className="mb-6 text-xl font-bold text-gray-900">{t('patients.form.create')}</h2>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.form.name')} *</label>
            <input
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              className={`w-full rounded-lg border px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500 ${errors.name ? 'border-red-500' : 'border-gray-300'}`}
            />
            {errors.name && <p className="mt-1 text-xs text-red-500">{errors.name}</p>}
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.form.phone')} *</label>
            <input
              value={form.phone_number}
              onChange={(e) => setForm({ ...form, phone_number: e.target.value })}
              className={`w-full rounded-lg border px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500 ${errors.phone_number ? 'border-red-500' : 'border-gray-300'}`}
            />
            {errors.phone_number && <p className="mt-1 text-xs text-red-500">{errors.phone_number}</p>}
          </div>

          <div className="grid grid-cols-2 gap-4">
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
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.form.clinic')} *</label>
            <select
              value={form.clinic_id}
              onChange={(e) => setForm({ ...form, clinic_id: e.target.value })}
              className={`w-full rounded-lg border px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500 ${errors.clinic_id ? 'border-red-500' : 'border-gray-300'}`}
            >
              <option value="">{t('patients.form.selectClinic')}</option>
              {clinics.map((clinic) => (
                <option key={clinic.id} value={clinic.id}>
                  {clinic.name}
                </option>
              ))}
            </select>
            {errors.clinic_id && <p className="mt-1 text-xs text-red-500">{errors.clinic_id}</p>}
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

          {canEditMedical && (
            <>
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.detail.systemicConditions')}</label>
                <textarea
                  value={form.systemic_conditions}
                  onChange={(e) => setForm({ ...form, systemic_conditions: e.target.value })}
                  rows={2}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.detail.pastTreatments')}</label>
                <textarea
                  value={form.past_dental_treatments}
                  onChange={(e) => setForm({ ...form, past_dental_treatments: e.target.value })}
                  rows={2}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.detail.allergies')}</label>
                <textarea
                  value={form.allergies}
                  onChange={(e) => setForm({ ...form, allergies: e.target.value })}
                  rows={2}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
                />
              </div>
            </>
          )}

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.detail.generalNotes')}</label>
            <textarea
              value={form.general_notes}
              onChange={(e) => setForm({ ...form, general_notes: e.target.value })}
              rows={2}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-100"
            >
              {t('patients.form.cancel')}
            </button>
            <button
              type="submit"
              disabled={createPatient.isPending}
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {createPatient.isPending ? '...' : t('patients.form.create')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
