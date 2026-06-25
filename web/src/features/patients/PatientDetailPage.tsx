import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { usePatientDetail, useClinics, useDentists, useSoftDeletePatient } from '../../hooks/usePatients'
import { useAuth } from '../../hooks/useAuth'
import { Button } from '../../components/ui/button'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '../../components/ui/alert-dialog'
import PatientTabs from './PatientTabs'
import EditDemographicsForm from './EditDemographicsForm'
import EditMedicalHistoryForm from './EditMedicalHistoryForm'

export default function PatientDetailPage() {
  const { t } = useTranslation()
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { role, tenantId } = useAuth()
  const patientId = id ? Number(id) : null

  const { data: patient, isLoading, isError } = usePatientDetail(patientId)
  const { data: clinics = [] } = useClinics()
  const { data: dentists = [] } = useDentists()
  const softDeletePatient = useSoftDeletePatient()

  const [editSection, setEditSection] = useState<'demographics' | 'medical' | null>(null)
  const [deleteOpen, setDeleteOpen] = useState(false)

  async function handleDelete() {
    if (!patient) return
    await softDeletePatient.mutateAsync(patient.id)
    navigate('/patients')
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="h-8 w-48 animate-pulse rounded bg-gray-100" />
        <div className="h-40 animate-pulse rounded-lg bg-gray-100" />
        <div className="h-20 animate-pulse rounded-lg bg-gray-100" />
      </div>
    )
  }

  if (isError || !patient) {
    return (
      <div className="flex items-center justify-center py-20">
        <p className="text-gray-500">{t('patients.detail.notFound')}</p>
      </div>
    )
  }

  const clinicName = patient.clinic_id
    ? clinics.find((c) => c.id === patient.clinic_id)?.name
    : undefined

  const dentistName = patient.assigned_dentist_id
    ? dentists.find((d) => d.id === patient.assigned_dentist_id)?.full_name
    : undefined

  const canEditMedical = role === 'admin' || role === 'dentist'

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">
          {patient.name}
        </h1>
        {role === 'admin' && (
          <Button variant="destructive" onClick={() => setDeleteOpen(true)}>
            {t('patients.detail.delete')}
          </Button>
        )}
      </div>

      <AlertDialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t('patients.detail.confirmDeleteTitle')}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('patients.detail.confirmDeleteDescription')}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('patients.detail.cancel')}</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-destructive/10 text-destructive hover:bg-destructive/20"
            >
              {t('patients.detail.confirmDelete')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <section className="rounded-lg border border-gray-200 p-6">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-900">{t('patients.detail.demographics')}</h2>
          <button
            onClick={() => setEditSection(editSection === 'demographics' ? null : 'demographics')}
            className="text-sm font-medium text-blue-600 hover:text-blue-700"
          >
            {editSection === 'demographics' ? t('patients.form.cancel') : t('patients.form.editDemographics')}
          </button>
        </div>

        {editSection === 'demographics' ? (
          <EditDemographicsForm
            patient={patient}
            clinics={clinics}
            dentists={dentists}
            onSaved={() => setEditSection(null)}
            onCancel={() => setEditSection(null)}
          />
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <span className="text-xs font-medium uppercase text-gray-500">{t('patients.form.name')}</span>
              <p className="mt-0.5 text-gray-900">{patient.name}</p>
            </div>
            <div>
              <span className="text-xs font-medium uppercase text-gray-500">{t('patients.form.phone')}</span>
              <p className="mt-0.5 text-gray-900">{patient.phone_number ?? '—'}</p>
            </div>
            <div>
              <span className="text-xs font-medium uppercase text-gray-500">{t('patients.form.dateOfBirth')}</span>
              <p className="mt-0.5 text-gray-900">{patient.date_of_birth ?? '—'}</p>
            </div>
            <div>
              <span className="text-xs font-medium uppercase text-gray-500">{t('patients.form.gender')}</span>
              <p className="mt-0.5 text-gray-900">
                {patient.gender
                  ? t(`patients.form.${patient.gender.toLowerCase()}` as any)
                  : '—'}
              </p>
            </div>
            <div>
              <span className="text-xs font-medium uppercase text-gray-500">{t('patients.form.clinic')}</span>
              <p className="mt-0.5 text-gray-900">{clinicName ?? '—'}</p>
            </div>
            <div>
              <span className="text-xs font-medium uppercase text-gray-500">{t('patients.form.dentist')}</span>
              <p className="mt-0.5 text-gray-900">{dentistName ?? '—'}</p>
            </div>
          </div>
        )}
      </section>

      <section className="rounded-lg border border-gray-200 p-6">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-900">{t('patients.detail.medicalHistory')}</h2>
          {canEditMedical && (
            <button
              onClick={() => setEditSection(editSection === 'medical' ? null : 'medical')}
              className="text-sm font-medium text-blue-600 hover:text-blue-700"
            >
              {editSection === 'medical' ? t('patients.form.cancel') : t('patients.form.editMedicalHistory')}
            </button>
          )}
        </div>

        {editSection === 'medical' ? (
          <EditMedicalHistoryForm
            patient={patient}
            onSaved={() => setEditSection(null)}
            onCancel={() => setEditSection(null)}
          />
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <span className="text-xs font-medium uppercase text-gray-500">{t('patients.detail.systemicConditions')}</span>
              <p className="mt-0.5 text-gray-900">{patient.systemic_conditions || '—'}</p>
            </div>
            <div className="sm:col-span-2">
              <span className="text-xs font-medium uppercase text-gray-500">{t('patients.detail.pastTreatments')}</span>
              <p className="mt-0.5 text-gray-900">{patient.past_dental_treatments || '—'}</p>
            </div>
            <div className="sm:col-span-2">
              <span className="text-xs font-medium uppercase text-gray-500">{t('patients.detail.allergies')}</span>
              <p className="mt-0.5 text-gray-900">{patient.allergies || '—'}</p>
            </div>
            <div className="sm:col-span-2">
              <span className="text-xs font-medium uppercase text-gray-500">{t('patients.detail.generalNotes')}</span>
              <p className="mt-0.5 text-gray-900">{patient.general_notes || '—'}</p>
            </div>
          </div>
        )}
      </section>

      {tenantId && patient.id && (
        <section className="rounded-lg border border-gray-200 p-6">
          <PatientTabs patientId={patient.id} tenantId={tenantId} />
        </section>
      )}
    </div>
  )
}
