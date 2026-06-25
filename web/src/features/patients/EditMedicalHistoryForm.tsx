import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useUpdatePatient } from '../../hooks/usePatients'
import type { Patient, UpdatePatientInput } from '../../hooks/usePatients'
import { useAuth } from '../../hooks/useAuth'

interface EditMedicalHistoryFormProps {
  patient: Patient
  onSaved: () => void
  onCancel: () => void
}

export default function EditMedicalHistoryForm({ patient, onSaved, onCancel }: EditMedicalHistoryFormProps) {
  const { t } = useTranslation()
  const { role } = useAuth()
  const updatePatient = useUpdatePatient()
  const isReadOnly = role !== 'admin' && role !== 'dentist'

  const [form, setForm] = useState({
    systemic_conditions: patient.systemic_conditions ?? '',
    past_dental_treatments: patient.past_dental_treatments ?? '',
    allergies: patient.allergies ?? '',
    general_notes: patient.general_notes ?? '',
  })

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (isReadOnly) return

    const input: UpdatePatientInput = {
      id: patient.id,
      systemic_conditions: form.systemic_conditions || null,
      past_dental_treatments: form.past_dental_treatments || null,
      allergies: form.allergies || null,
      general_notes: form.general_notes || null,
    }

    await updatePatient.mutateAsync(input)
    onSaved()
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.detail.systemicConditions')}</label>
        {isReadOnly ? (
          <p className="rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-500">
            {patient.systemic_conditions || '—'}
          </p>
        ) : (
          <textarea
            value={form.systemic_conditions}
            onChange={(e) => setForm({ ...form, systemic_conditions: e.target.value })}
            rows={2}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
          />
        )}
      </div>

      <div>
        <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.detail.pastTreatments')}</label>
        {isReadOnly ? (
          <p className="rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-500">
            {patient.past_dental_treatments || '—'}
          </p>
        ) : (
          <textarea
            value={form.past_dental_treatments}
            onChange={(e) => setForm({ ...form, past_dental_treatments: e.target.value })}
            rows={2}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
          />
        )}
      </div>

      <div>
        <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.detail.allergies')}</label>
        {isReadOnly ? (
          <p className="rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-500">
            {patient.allergies || '—'}
          </p>
        ) : (
          <textarea
            value={form.allergies}
            onChange={(e) => setForm({ ...form, allergies: e.target.value })}
            rows={2}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
          />
        )}
      </div>

      <div>
        <label className="mb-1 block text-sm font-medium text-gray-700">{t('patients.detail.generalNotes')}</label>
        <textarea
          value={form.general_notes}
          onChange={(e) => setForm({ ...form, general_notes: e.target.value })}
          rows={2}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
          readOnly={isReadOnly}
        />
      </div>

      {!isReadOnly && (
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
      )}
    </form>
  )
}
