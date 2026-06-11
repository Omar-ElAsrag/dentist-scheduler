package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DentistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SupabaseRepository()
    private val sharedPrefs = application.getSharedPreferences("dentist_profile", android.content.Context.MODE_PRIVATE)

    private val _dentistName = MutableStateFlow(sharedPrefs.getString("dentist_name", "Dr. Alexander") ?: "Dr. Alexander")
    val dentistName = _dentistName.asStateFlow()

    private val _dentistBio = MutableStateFlow(sharedPrefs.getString("dentist_bio", "") ?: "")
    val dentistBio = _dentistBio.asStateFlow()

    private val _dentistPhotoPath = MutableStateFlow(sharedPrefs.getString("photo_path", "") ?: "")
    val dentistPhotoPath = _dentistPhotoPath.asStateFlow()

    fun updateProfile(name: String, bio: String, photoPath: String) {
        viewModelScope.launch {
            sharedPrefs.edit()
                .putString("dentist_name", name.trim())
                .putString("dentist_bio", bio.trim())
                .putString("photo_path", photoPath.trim())
                .apply()
            _dentistName.value = name.trim()
            _dentistBio.value = bio.trim()
            _dentistPhotoPath.value = photoPath.trim()
        }
    }

    val clinics: StateFlow<List<Clinic>> = repository.getClinics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val patients: StateFlow<List<Patient>> = repository.getPatients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clinicalProcedures: StateFlow<List<ClinicalProcedure>> = repository.getClinicalProcedures()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val procedureCards: StateFlow<List<ProcedureCardDetail>> = repository.getAllProcedureCardsWithDetails()
        .map { cards -> sortProcedureCards(cards) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPatientId = MutableStateFlow<Int?>(null)
    val selectedPatientId = _selectedPatientId.asStateFlow()

    val selectedPatientState: StateFlow<Patient?> = _selectedPatientId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.getPatientByIdFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedPatientProcedureCards: StateFlow<List<ProcedureCardDetail>> = _selectedPatientId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getProcedureCardsByPatientIdWithDetails(id)
        }
        .map { cards -> sortProcedureCards(cards) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedPatientMedicalFiles: StateFlow<List<MedicalFile>> = _selectedPatientId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getMedicalFilesByPatientId(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedPatientPayments: StateFlow<List<PaymentWithProcedureName>> = _selectedPatientId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getPaymentsByPatientId(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- PATIENT ---

    fun insertPatient(
        name: String,
        phoneNumber: String = "",
        systemicConditions: String = "",
        allergies: String = "",
        notes: String = "",
        onComplete: (Int) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val id = repository.insertPatient(Patient(
                name = name.trim(),
                phoneNumber = phoneNumber.trim(),
                systemicConditions = systemicConditions.trim(),
                allergies = allergies.trim(),
                generalNotes = notes.trim(),
                isInProgress = true,
                createdDate = sdf.format(Date())
            ))
            onComplete(id)
        }
    }

    fun updatePatientNextAppointment(patientId: Int, nextDate: String, nextTime: String, nextNotes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getPatientById(patientId)?.let { patient ->
                repository.updatePatient(patient)
            }
        }
    }

    fun updatePatientAppointmentAndProgress(patientId: Int, nextDate: String, nextTime: String, nextNotes: String, isInProgress: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getPatientById(patientId)?.let { patient ->
                repository.updatePatient(patient.copy(isInProgress = isInProgress))
            }
        }
    }

    fun updatePatientInProgressStatus(patientId: Int, isInProgress: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getPatientById(patientId)?.let { patient ->
                repository.updatePatient(patient.copy(isInProgress = isInProgress))
            }
        }
    }

    fun scheduleAppointment(
        name: String, phoneNumber: String, dateString: String, timeString: String,
        notes: String, addOtherDetails: Boolean, selectedClinicId: Int?,
        selectedClinicalProcedureId: Int?, selectedProcedureTypeId: Int?,
        selectedMaterial: String?, toothNumber: String, systemic: String,
        allergies: String, selectedStepNames: Set<String>, onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val patientId = repository.insertPatient(Patient(
                name = name.trim(), phoneNumber = phoneNumber.trim(),
                systemicConditions = systemic.trim(), allergies = allergies.trim(),
                generalNotes = notes.trim(), isInProgress = true,
                createdDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                clinicId = selectedClinicId
            ))

            if (addOtherDetails && selectedClinicId != null && selectedClinicalProcedureId != null) {
                repository.getPatientById(patientId)?.let { patient ->
                    if (!patient.isInProgress) repository.updatePatient(patient.copy(isInProgress = true))
                }
                val cardId = createCardWithSteps(patientId, selectedClinicId, selectedClinicalProcedureId, selectedProcedureTypeId, selectedMaterial, dateString, toothNumber, notes, selectedStepNames)
                recalculateCard(cardId)
            }
            onComplete()
        }
    }

    fun scheduleExistingPatientAppointment(
        patientId: Int, dateString: String, timeString: String, notes: String,
        selectedClinicId: Int?, selectedClinicalProcedureId: Int?,
        selectedProcedureTypeId: Int?, selectedMaterial: String?,
        toothNumber: String, selectedStepNames: Set<String>, onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.getPatientById(patientId)?.let { patient ->
                repository.updatePatient(patient.copy(clinicId = selectedClinicId))
            }
            if (selectedClinicId != null && selectedClinicalProcedureId != null) {
                val cardId = createCardWithSteps(patientId, selectedClinicId, selectedClinicalProcedureId, selectedProcedureTypeId, selectedMaterial, dateString, toothNumber, notes, selectedStepNames)
                recalculateCard(cardId)
            }
            onComplete()
        }
    }

    private suspend fun createCardWithSteps(
        patientId: Int, clinicId: Int, clinicalProcedureId: Int, procedureTypeId: Int?,
        material: String?, dateCreated: String, toothNumber: String, notes: String,
        selectedStepNames: Set<String>
    ): Int {
        val card = ProcedureCard(
            patientId = patientId, clinicId = clinicId,
            clinicalProcedureId = clinicalProcedureId, procedureTypeId = procedureTypeId,
            material = material?.takeIf { it.isNotBlank() }, dateCreated = dateCreated.trim(),
            toothNumber = toothNumber.trim().takeIf { it.isNotBlank() },
            notes = notes.trim().takeIf { it.isNotBlank() }
        )
        val cardId = repository.insertProcedureCard(card)
        val steps = if (procedureTypeId != null) {
            val typeSteps = repository.getStepsByProcedureTypeIdOnce(procedureTypeId)
            if (typeSteps.isNotEmpty()) typeSteps else repository.getStepsByClinicalProcedureIdOnce(clinicalProcedureId)
        } else {
            repository.getStepsByClinicalProcedureIdOnce(clinicalProcedureId)
        }
        val stepsToInsert = if (selectedStepNames.isNotEmpty()) steps.filter { it.stepName in selectedStepNames } else steps
        stepsToInsert.forEachIndexed { idx, step ->
            repository.insertProcedureCardStep(ProcedureCardStep(procedureCardId = cardId, stepName = step.stepName, displayOrder = idx))
        }
        return cardId
    }

    fun updatePatient(patient: Patient) {
        viewModelScope.launch(Dispatchers.IO) { repository.updatePatient(patient) }
    }

    fun deletePatient(patient: Patient) {
        viewModelScope.launch(Dispatchers.IO) { repository.deletePatient(patient) }
    }

    fun selectPatient(patientId: Int?) { _selectedPatientId.value = patientId }

    // --- CLINIC ---

    fun insertClinic(name: String, defaultPercentage: Float? = null, deductLabFeesDefault: Boolean? = null, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertClinic(Clinic(name = name.trim(), defaultPercentage = defaultPercentage, deductLabFeesDefault = deductLabFeesDefault))
            onComplete(id)
        }
    }

    fun updateClinic(clinic: Clinic) { viewModelScope.launch(Dispatchers.IO) { repository.updateClinic(clinic) } }
    suspend fun getClinicByIdOnce(id: Int): Clinic? = repository.getClinicById(id)
    fun deleteClinic(clinic: Clinic) { viewModelScope.launch(Dispatchers.IO) { repository.deleteClinic(clinic.id) } }

    // --- CLINICAL PROCEDURE / TYPE / STEP LOOKUPS ---

    fun getTypesForProcedure(cpId: Int): Flow<List<ProcedureType>> = repository.getTypesByClinicalProcedureId(cpId)
    suspend fun getTypesForProcedureOnce(cpId: Int): List<ProcedureType> = repository.getTypesByClinicalProcedureIdOnce(cpId)
    suspend fun getStepsForProcedure(cpId: Int): List<ClinicalProcedureStep> = repository.getStepsByClinicalProcedureIdOnce(cpId)
    suspend fun getStepsForType(ptId: Int): List<ClinicalProcedureStep> = repository.getStepsByProcedureTypeIdOnce(ptId)
    suspend fun getProcedureTypeByIdOnce(ptId: Int): ProcedureType? = repository.getProcedureTypeById(ptId)

    // --- PROCEDURE CARD ---

    fun createProcedureCard(
        patientId: Int, clinicId: Int, clinicalProcedureId: Int, procedureTypeId: Int? = null,
        material: String? = null, dateCreated: String, toothNumber: String = "", notes: String = "",
        selectedStepNames: Set<String>? = null, additionalStepNames: List<String> = emptyList(),
        treatmentFee: Double = 0.0, labFees: Double = 0.0, appliedPercentage: Float = 0f,
        deductLabFees: Boolean = false, onComplete: (Int) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getPatientById(patientId)?.let { patient ->
                if (!patient.isInProgress) repository.updatePatient(patient.copy(isInProgress = true))
            }
            val card = ProcedureCard(
                patientId = patientId, clinicId = clinicId, clinicalProcedureId = clinicalProcedureId,
                procedureTypeId = procedureTypeId, material = material?.takeIf { it.isNotBlank() },
                dateCreated = dateCreated.trim(), toothNumber = toothNumber.trim().takeIf { it.isNotBlank() },
                notes = notes.trim().takeIf { it.isNotBlank() }, treatmentFee = treatmentFee,
                labFees = labFees, appliedPercentage = appliedPercentage, deductLabFees = deductLabFees
            )
            val cardId = repository.insertProcedureCard(card)
            val steps = if (procedureTypeId != null) {
                val ts = repository.getStepsByProcedureTypeIdOnce(procedureTypeId)
                if (ts.isNotEmpty()) ts else repository.getStepsByClinicalProcedureIdOnce(clinicalProcedureId)
            } else repository.getStepsByClinicalProcedureIdOnce(clinicalProcedureId)
            val stepsToInsert = if (selectedStepNames != null) steps.filter { it.stepName in selectedStepNames } else steps
            stepsToInsert.forEachIndexed { idx, step ->
                repository.insertProcedureCardStep(ProcedureCardStep(procedureCardId = cardId, stepName = step.stepName, displayOrder = idx))
            }
            additionalStepNames.forEachIndexed { idx, n ->
                repository.insertProcedureCardStep(ProcedureCardStep(procedureCardId = cardId, stepName = n.trim(), displayOrder = stepsToInsert.size + idx))
            }
            onComplete(cardId)
            recalculateCard(cardId)
        }
    }

    fun updateProcedureCardStatus(cardId: Int, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getProcedureCardById(cardId)?.let { card ->
                repository.updateProcedureCard(card.copy(status = newStatus))
                if (newStatus == "Completed") {
                    val allSteps = repository.getStepsByProcedureCardIdOnce(cardId)
                    val now = Instant.now().toString()
                    allSteps.forEach { step ->
                        if (!step.isCompleted) repository.updateProcedureCardStep(step.copy(isCompleted = true, completedAt = now))
                    }
                }
            }
        }
    }

    fun updateProcedureCardMaterial(cardId: Int, material: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getProcedureCardById(cardId)?.let { repository.updateProcedureCard(it.copy(material = material.trim().takeIf { it.isNotBlank() })) }
        }
    }

    fun updateProcedureCardNotes(cardId: Int, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getProcedureCardById(cardId)?.let { repository.updateProcedureCard(it.copy(notes = notes.trim().takeIf { it.isNotBlank() })) }
        }
    }

    fun deleteProcedureCard(card: ProcedureCard) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteProcedureCard(card) }
    }

    // --- MEDICAL FILE ---

    fun insertMedicalFile(patientId: Int, title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMedicalFile(MedicalFile(id = UUID.randomUUID().toString(), patientId = patientId, title = title.trim(), content = content.trim(), fileType = "note"))
        }
    }

    fun deleteMedicalFile(fileId: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteMedicalFile(fileId) }
    }

    // --- PROCEDURE CARD STEP ---

    fun getStepsForCard(cardId: Int): Flow<List<ProcedureCardStep>> = repository.getStepsByProcedureCardId(cardId)

    suspend fun getFirstPendingStepNameForCard(cardId: Int): String? {
        return repository.getStepsByProcedureCardIdOnce(cardId).filter { !it.isCompleted }.sortedBy { it.displayOrder }.firstOrNull()?.stepName
    }

    fun toggleStepCompleted(step: ProcedureCardStep, completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProcedureCardStep(step.copy(isCompleted = completed, completedAt = if (completed) Instant.now().toString() else null))
            repository.getProcedureCardById(step.procedureCardId)?.let { card ->
                if (card.status == "Canceled") return@let
                val allSteps = repository.getStepsByProcedureCardIdOnce(step.procedureCardId)
                val allDone = allSteps.isNotEmpty() && allSteps.all { it.isCompleted }
                val newStatus = if (allDone) "Completed" else "In Progress"
                if (card.status != newStatus) repository.updateProcedureCard(card.copy(status = newStatus))
            }
        }
    }

    fun updateStepDateGroup(steps: List<ProcedureCardStep>, newDateMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val newDate = Instant.ofEpochMilli(newDateMillis).toString()
            steps.forEach { step -> repository.updateProcedureCardStep(step.copy(completedAt = newDate)) }
        }
    }

    fun addCardStep(cardId: Int, description: String, photoUriList: List<String> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getStepsByProcedureCardIdOnce(cardId).sortedBy { it.displayOrder }
            val firstPendingOrder = existing.firstOrNull { !it.isCompleted }?.displayOrder
            val newDisplayOrder = firstPendingOrder ?: (existing.maxOfOrNull { it.displayOrder }?.plus(1) ?: 0)
            if (firstPendingOrder != null) {
                existing.filter { it.displayOrder >= firstPendingOrder }.forEach { step ->
                    repository.updateProcedureCardStep(step.copy(displayOrder = step.displayOrder + 1))
                }
            }
            repository.insertProcedureCardStep(ProcedureCardStep(
                procedureCardId = cardId, stepName = description.trim(),
                photoUrls = photoUriList.filter { it.isNotBlank() }, displayOrder = newDisplayOrder
            ))
        }
    }

    fun deleteCardStep(step: ProcedureCardStep) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteProcedureCardStep(step) }
    }

    // --- PROCEDURE PAYMENT ---

    fun getPaymentsForCard(cardId: Int): Flow<List<ProcedurePayment>> = repository.getPaymentsByProcedureCardId(cardId)

    fun addPayment(cardId: Int, amount: Double, timestamp: Long, notes: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProcedurePayment(ProcedurePayment(
                procedureCardId = cardId, amount = amount,
                paymentAt = Instant.ofEpochMilli(timestamp).toString(),
                notes = notes?.takeIf { it.isNotBlank() }
            ))
            recalculateCard(cardId)
        }
    }

    fun deletePayment(payment: ProcedurePayment) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProcedurePayment(payment)
            recalculateCard(payment.procedureCardId)
        }
    }

    fun updatePaymentDateGroup(payments: List<ProcedurePayment>, newDateMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val newDate = Instant.ofEpochMilli(newDateMillis).toString()
            payments.forEach { payment -> repository.updateProcedurePayment(payment.copy(paymentAt = newDate)) }
        }
    }

    private suspend fun recalculateCard(cardId: Int) {
        val totalPaid = repository.getPaymentsByProcedureCardIdOnce(cardId).sumOf { it.amount }
        repository.getProcedureCardById(cardId)?.let { card ->
            val associateCut: Double
            val clinicShare: Double
            if (card.deductLabFees) {
                associateCut = if (totalPaid - card.labFees > 0) (totalPaid - card.labFees) * (card.appliedPercentage / 100.0) else 0.0
                clinicShare = (totalPaid - card.labFees) - associateCut
            } else {
                associateCut = totalPaid * (card.appliedPercentage / 100.0)
                clinicShare = (totalPaid - associateCut) - card.labFees
            }
            repository.updateProcedureCard(card.copy(amountPaid = totalPaid, calculatedAssociateCut = associateCut, calculatedClinicShare = clinicShare))
        }
    }

    fun updateCardFinancialSettings(cardId: Int, treatmentFee: Double, labFees: Double, percentage: Float, deductLabFees: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getProcedureCardById(cardId)?.let { repository.updateProcedureCard(it.copy(treatmentFee = treatmentFee, labFees = labFees, appliedPercentage = percentage, deductLabFees = deductLabFees)) }
            recalculateCard(cardId)
        }
    }

    // --- CUSTOM PROCEDURE TEMPLATES ---

    fun insertCustomClinicalProcedure(name: String, hasTypes: Boolean, typesText: String, stepsText: String, typeStepsText: String, defaultFee: Double? = null, defaultLabFee: Double? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val maxOrder = (repository.getAllClinicalProceduresOnce().maxOfOrNull { it.displayOrder } ?: 0) + 1
            val cpId = repository.insertClinicalProcedure(ClinicalProcedure(name = name.trim(), hasTypes = hasTypes, displayOrder = maxOrder, isCustom = true, defaultFee = defaultFee, defaultLabFee = defaultLabFee))
            insertProcedureTypesAndSteps(cpId, hasTypes, typesText, stepsText, typeStepsText)
        }
    }

    fun deleteCustomClinicalProcedure(proc: ClinicalProcedure) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteCustomClinicalProcedure(proc.id) }
    }

    fun updateClinicalProcedure(proc: ClinicalProcedure, hasTypes: Boolean, typesText: String, stepsText: String, typeStepsText: String, defaultFee: Double? = null, defaultLabFee: Double? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateClinicalProcedure(proc.copy(name = proc.name.trim(), hasTypes = hasTypes, defaultFee = defaultFee, defaultLabFee = defaultLabFee))
            repository.getTypesByClinicalProcedureIdOnce(proc.id).forEach { repository.deleteProcedureType(it) }
            repository.getStepsByClinicalProcedureIdOnce(proc.id).forEach { repository.deleteClinicalProcedureStep(it) }
            insertProcedureTypesAndSteps(proc.id, hasTypes, typesText, stepsText, typeStepsText)
        }
    }

    private suspend fun insertProcedureTypesAndSteps(cpId: Int, hasTypes: Boolean, typesText: String, stepsText: String, typeStepsText: String) {
        if (hasTypes) {
            val typeLines = typesText.split("\n").filter { it.isNotBlank() }
            val typeStepMap = mutableMapOf<String, List<String>>()
            typeStepsText.split("|").filter { it.isNotBlank() }.forEach { entry ->
                val parts = entry.split("::", limit = 2)
                if (parts.size == 2) typeStepMap[parts[0].trim()] = parts[1].split("\n").filter { it.isNotBlank() }
            }
            typeLines.forEach { typeLine ->
                val parts = typeLine.split("|")
                val typeName = parts[0].trim()
                val materials = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
                val typeFee = parts.getOrNull(2)?.trim()?.toDoubleOrNull()
                val typeLabFee = parts.getOrNull(3)?.trim()?.toDoubleOrNull()
                val materialFees = parts.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() }
                val ptId = repository.insertProcedureType(ProcedureType(clinicalProcedureId = cpId, name = typeName, materials = materials, defaultFee = typeFee, defaultLabFee = typeLabFee, materialFees = materialFees))
                val typeSteps = typeStepMap[typeName] ?: emptyList()
                typeSteps.forEachIndexed { idx, stepName ->
                    repository.insertClinicalProcedureStep(ClinicalProcedureStep(procedureTypeId = ptId, stepName = stepName.trim(), displayOrder = idx + 1))
                }
            }
            val sharedSteps = stepsText.split("\n").filter { it.isNotBlank() }
            sharedSteps.forEachIndexed { idx, stepName ->
                repository.insertClinicalProcedureStep(ClinicalProcedureStep(clinicalProcedureId = cpId, stepName = stepName.trim(), displayOrder = idx + 1))
            }
        } else {
            val steps = stepsText.split("\n").filter { it.isNotBlank() }
            steps.forEachIndexed { idx, stepName ->
                repository.insertClinicalProcedureStep(ClinicalProcedureStep(clinicalProcedureId = cpId, stepName = stepName.trim(), displayOrder = idx + 1))
            }
        }
    }

    suspend fun getProcedureTypesWithStepsOnce(procId: Int): ProcedureEditData {
        val types = repository.getTypesByClinicalProcedureIdOnce(procId)
        val steps = repository.getStepsByClinicalProcedureIdOnce(procId)
        val typeStepsMap = mutableMapOf<String, List<String>>()
        types.forEach { type ->
            val ts = repository.getStepsByProcedureTypeIdOnce(type.id)
            if (ts.isNotEmpty()) typeStepsMap[type.name] = ts.map { it.stepName }
        }
        return ProcedureEditData(types = types, sharedSteps = steps, typeStepsMap = typeStepsMap)
    }

    suspend fun deleteClinicalProcedureStep(step: ClinicalProcedureStep) {
        repository.deleteClinicalProcedureStep(step)
    }

    data class ProcedureEditData(
        val types: List<ProcedureType>,
        val sharedSteps: List<ClinicalProcedureStep>,
        val typeStepsMap: Map<String, List<String>>
    )

    private fun sortProcedureCards(cards: List<ProcedureCardDetail>): List<ProcedureCardDetail> {
        return cards.sortedWith(compareByDescending<ProcedureCardDetail> { it.dateCreated }.thenByDescending { it.id })
    }
}