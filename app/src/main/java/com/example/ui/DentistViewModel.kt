package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DentistViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        DentistDatabase::class.java,
        "dentist_scheduler.db"
    ).fallbackToDestructiveMigration(true).build()

    private val repository = DentistRepository(db)

    // Profile SharedPreferences
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

    // Database flows
    val clinics: StateFlow<List<Clinic>> = repository.clinics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val patients: StateFlow<List<Patient>> = repository.patients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clinicalProcedures: StateFlow<List<ClinicalProcedure>> = repository.clinicalProcedures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val procedureCards: StateFlow<List<ProcedureCardDetail>> = repository.procedureCards
        .map { cards -> sortProcedureCards(cards) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Patient selection
    private val _selectedPatientId = MutableStateFlow<Int?>(null)
    val selectedPatientId = _selectedPatientId.asStateFlow()

    val selectedPatientState: StateFlow<Patient?> = _selectedPatientId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.getPatientByIdFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedPatientProcedureCards: StateFlow<List<ProcedureCardDetail>> = _selectedPatientId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getProcedureCardsByPatientId(id)
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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clinicalProcedures.first().let { existing ->
                if (existing.isEmpty()) {
                    seedBaselineData()
                } else {
                    if (existing.none { it.name == "General Examination" }) {
                        repository.insertClinicalProcedure(
                            ClinicalProcedure(name = "General Examination", hasTypes = false, displayOrder = 8)
                        )
                    }
                    if (existing.none { it.name == "Orthodontics" }) {
                        repository.insertClinicalProcedure(
                            ClinicalProcedure(name = "Orthodontics", hasTypes = false, displayOrder = 9)
                        )
                    }
                }
            }
        }
    }

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
            val patient = Patient(
                name = name.trim(),
                phoneNumber = phoneNumber.trim(),
                systemicConditions = systemicConditions.trim(),
                allergies = allergies.trim(),
                generalNotes = notes.trim(),
                isInProgress = true,
                createdDate = sdf.format(Date())
            )
            val id = repository.insertPatient(patient)
            onComplete(id.toInt())
        }
    }

    fun updatePatientNextAppointment(patientId: Int, nextDate: String, nextTime: String, nextNotes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getPatientById(patientId)?.let { patient ->
                repository.updatePatient(patient.copy(
                    nextAppointmentDate = nextDate,
                    nextAppointmentTime = nextTime.trim(),
                    nextAppointmentNotes = nextNotes.trim()
                ))
            }
        }
    }

    fun updatePatientAppointmentAndProgress(patientId: Int, nextDate: String, nextTime: String, nextNotes: String, isInProgress: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getPatientById(patientId)?.let { patient ->
                repository.updatePatient(patient.copy(
                    nextAppointmentDate = nextDate,
                    nextAppointmentTime = nextTime.trim(),
                    nextAppointmentNotes = nextNotes.trim(),
                    isInProgress = isInProgress
                ))
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
        name: String,
        phoneNumber: String,
        dateString: String,
        timeString: String,
        notes: String,
        addOtherDetails: Boolean,
        selectedClinicId: Int?,
        selectedClinicalProcedureId: Int?,
        selectedProcedureTypeId: Int?,
        selectedMaterial: String?,
        toothNumber: String,
        systemic: String,
        allergies: String,
        selectedStepNames: Set<String>,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val patientId = withContext(Dispatchers.IO) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                repository.insertPatient(
                    Patient(
                        name = name.trim(),
                        phoneNumber = phoneNumber.trim(),
                        systemicConditions = systemic.trim(),
                        allergies = allergies.trim(),
                        generalNotes = notes.trim(),
                        isInProgress = true,
                        createdDate = sdf.format(Date()),
                        clinicId = selectedClinicId,
                    )
                ).toInt()
            }

            if (addOtherDetails && selectedClinicId != null && selectedClinicalProcedureId != null) {
                withContext(Dispatchers.IO) {
                    repository.getPatientById(patientId)?.let { patient ->
                        if (!patient.isInProgress) {
                            repository.updatePatient(patient.copy(isInProgress = true))
                        }
                    }

                    val card = ProcedureCard(
                        patientId = patientId,
                        clinicId = selectedClinicId,
                        clinicalProcedureId = selectedClinicalProcedureId,
                        procedureTypeId = selectedProcedureTypeId,
                        material = selectedMaterial?.takeIf { it.isNotBlank() },
                        dateCreated = dateString.trim(),
                        toothNumber = toothNumber.trim().takeIf { it.isNotBlank() },
                        notes = notes.trim().takeIf { it.isNotBlank() }
                    )
                    val cardId = repository.insertProcedureCard(card).toInt()

                    val steps = if (selectedProcedureTypeId != null) {
                        val typeSteps = repository.getStepsByProcedureTypeIdOnce(selectedProcedureTypeId)
                        if (typeSteps.isNotEmpty()) typeSteps
                        else repository.getStepsByClinicalProcedureIdOnce(selectedClinicalProcedureId)
                    } else {
                        repository.getStepsByClinicalProcedureIdOnce(selectedClinicalProcedureId)
                    }

                    val stepsToInsert = if (selectedStepNames.isNotEmpty()) {
                        steps.filter { it.stepName in selectedStepNames }
                    } else {
                        steps
                    }

                    stepsToInsert.forEachIndexed { idx, step ->
                        repository.insertProcedureCardStep(
                            ProcedureCardStep(procedureCardId = cardId, stepName = step.stepName, displayOrder = idx)
                        )
                    }

                    recalculateCard(cardId)
                }
            }

            withContext(Dispatchers.IO) {
                repository.getPatientById(patientId)?.let { patient ->
                    repository.updatePatient(patient.copy(
                        nextAppointmentDate = dateString.trim(),
                        nextAppointmentTime = timeString.trim(),
                        nextAppointmentNotes = notes.trim(),
                        isInProgress = if (addOtherDetails && selectedClinicId != null && selectedClinicalProcedureId != null) patient.isInProgress else false,
                        clinicId = selectedClinicId
                    ))
                }
            }

            onComplete()
        }
    }

    fun scheduleExistingPatientAppointment(
        patientId: Int,
        dateString: String,
        timeString: String,
        notes: String,
        selectedClinicId: Int?,
        selectedClinicalProcedureId: Int?,
        selectedProcedureTypeId: Int?,
        selectedMaterial: String?,
        toothNumber: String,
        selectedStepNames: Set<String>,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.getPatientById(patientId)?.let { patient ->
                    repository.updatePatient(patient.copy(
                        nextAppointmentDate = dateString.trim(),
                        nextAppointmentTime = timeString.trim(),
                        nextAppointmentNotes = notes.trim(),
                        clinicId = selectedClinicId
                    ))
                }
            }

            if (selectedClinicId != null && selectedClinicalProcedureId != null) {
                withContext(Dispatchers.IO) {
                    val card = ProcedureCard(
                        patientId = patientId,
                        clinicId = selectedClinicId,
                        clinicalProcedureId = selectedClinicalProcedureId,
                        procedureTypeId = selectedProcedureTypeId,
                        material = selectedMaterial?.takeIf { it.isNotBlank() },
                        dateCreated = dateString.trim(),
                        toothNumber = toothNumber.trim().takeIf { it.isNotBlank() },
                        notes = notes.trim().takeIf { it.isNotBlank() }
                    )
                    val cardId = repository.insertProcedureCard(card).toInt()

                    val steps = if (selectedProcedureTypeId != null) {
                        val typeSteps = repository.getStepsByProcedureTypeIdOnce(selectedProcedureTypeId)
                        if (typeSteps.isNotEmpty()) typeSteps
                        else repository.getStepsByClinicalProcedureIdOnce(selectedClinicalProcedureId)
                    } else {
                        repository.getStepsByClinicalProcedureIdOnce(selectedClinicalProcedureId)
                    }

                    val stepsToInsert = if (selectedStepNames.isNotEmpty()) {
                        steps.filter { it.stepName in selectedStepNames }
                    } else {
                        steps
                    }

                    stepsToInsert.forEachIndexed { idx, step ->
                        repository.insertProcedureCardStep(
                            ProcedureCardStep(procedureCardId = cardId, stepName = step.stepName, displayOrder = idx)
                        )
                    }

                    recalculateCard(cardId)
                }
            }

            onComplete()
        }
    }

    fun updatePatient(patient: Patient) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePatient(patient)
        }
    }

    fun deletePatient(patient: Patient) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePatient(patient)
        }
    }

    fun selectPatient(patientId: Int?) {
        _selectedPatientId.value = patientId
    }

    // --- CLINIC ---

    fun insertClinic(
        name: String,
        defaultPercentage: Float? = null,
        deductLabFeesDefault: Boolean? = null,
        onComplete: (Int) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertClinic(Clinic(
                name = name.trim(),
                defaultPercentage = defaultPercentage,
                deductLabFeesDefault = deductLabFeesDefault
            ))
            onComplete(id.toInt())
        }
    }

    fun updateClinic(clinic: Clinic) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateClinic(clinic)
        }
    }

    suspend fun getClinicByIdOnce(id: Int): Clinic? = repository.getClinicById(id)

    fun deleteClinic(clinic: Clinic) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteClinic(clinic)
        }
    }

    // --- CLINICAL PROCEDURE / TYPE / STEP LOOKUPS ---

    fun getTypesForProcedure(cpId: Int): Flow<List<ProcedureType>> =
        repository.getTypesByClinicalProcedureId(cpId)

    suspend fun getTypesForProcedureOnce(cpId: Int): List<ProcedureType> =
        repository.getTypesByClinicalProcedureIdOnce(cpId)

    suspend fun getStepsForProcedure(cpId: Int): List<ClinicalProcedureStep> =
        repository.getStepsByClinicalProcedureIdOnce(cpId)

    suspend fun getStepsForType(ptId: Int): List<ClinicalProcedureStep> =
        repository.getStepsByProcedureTypeIdOnce(ptId)

    suspend fun getProcedureTypeByIdOnce(ptId: Int): ProcedureType? =
        repository.getProcedureTypeById(ptId)

    // --- PROCEDURE CARD ---

    fun createProcedureCard(
        patientId: Int,
        clinicId: Int,
        clinicalProcedureId: Int,
        procedureTypeId: Int? = null,
        material: String? = null,
        dateCreated: String,
        toothNumber: String = "",
        notes: String = "",
        selectedStepNames: Set<String>? = null,
        additionalStepNames: List<String> = emptyList(),
        treatmentFee: Double = 0.0,
        labFees: Double = 0.0,
        appliedPercentage: Float = 0f,
        deductLabFees: Boolean = false,
        onComplete: (Int) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getPatientById(patientId)?.let { patient ->
                if (!patient.isInProgress) {
                    repository.updatePatient(patient.copy(isInProgress = true))
                }
            }

            val card = ProcedureCard(
                patientId = patientId,
                clinicId = clinicId,
                clinicalProcedureId = clinicalProcedureId,
                procedureTypeId = procedureTypeId,
                material = material?.takeIf { it.isNotBlank() },
                dateCreated = dateCreated.trim(),
                toothNumber = toothNumber.trim().takeIf { it.isNotBlank() },
                notes = notes.trim().takeIf { it.isNotBlank() },
                treatmentFee = treatmentFee,
                labFees = labFees,
                appliedPercentage = appliedPercentage,
                deductLabFees = deductLabFees
            )
            val cardId = repository.insertProcedureCard(card).toInt()

            // Resolve template steps
            val steps = if (procedureTypeId != null) {
                val typeSteps = repository.getStepsByProcedureTypeIdOnce(procedureTypeId)
                if (typeSteps.isNotEmpty()) typeSteps
                else repository.getStepsByClinicalProcedureIdOnce(clinicalProcedureId)
            } else {
                repository.getStepsByClinicalProcedureIdOnce(clinicalProcedureId)
            }

            // Filter by user selection if provided, otherwise include all
            val stepsToInsert = if (selectedStepNames != null) {
                steps.filter { it.stepName in selectedStepNames }
            } else {
                steps
            }

            stepsToInsert.forEachIndexed { idx, step ->
                repository.insertProcedureCardStep(
                    ProcedureCardStep(procedureCardId = cardId, stepName = step.stepName, displayOrder = idx)
                )
            }

            additionalStepNames.forEachIndexed { idx, name ->
                repository.insertProcedureCardStep(
                    ProcedureCardStep(procedureCardId = cardId, stepName = name.trim(), displayOrder = stepsToInsert.size + idx)
                )
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
                    val now = System.currentTimeMillis()
                    allSteps.forEach { step ->
                        if (!step.isCompleted) {
                            repository.updateProcedureCardStep(
                                step.copy(isCompleted = true, completedTimestamp = now)
                            )
                        }
                    }
                }
            }
        }
    }

    fun updateProcedureCardMaterial(cardId: Int, material: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getProcedureCardById(cardId)?.let { card ->
                repository.updateProcedureCard(
                    card.copy(material = material.trim().takeIf { it.isNotBlank() })
                )
            }
        }
    }

    fun updateProcedureCardNotes(cardId: Int, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getProcedureCardById(cardId)?.let { card ->
                repository.updateProcedureCard(
                    card.copy(notes = notes.trim().takeIf { it.isNotBlank() })
                )
            }
        }
    }

    fun deleteProcedureCard(card: ProcedureCard) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProcedureCard(card)
        }
    }

    // --- MEDICAL FILE ---

    fun insertMedicalFile(patientId: Int, title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val file = MedicalFile(
                id = UUID.randomUUID().toString(),
                patientId = patientId,
                title = title.trim(),
                content = content.trim(),
                createdAt = sdf.format(Date())
            )
            repository.insertMedicalFile(file)
        }
    }

    fun deleteMedicalFile(fileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMedicalFile(fileId)
        }
    }

    // --- PROCEDURE CARD STEP ---

    fun getStepsForCard(cardId: Int): Flow<List<ProcedureCardStep>> =
        repository.getStepsByProcedureCardId(cardId)

    suspend fun getFirstPendingStepNameForCard(cardId: Int): String? {
        val allSteps = repository.getStepsByProcedureCardIdOnce(cardId)
        return allSteps.filter { !it.isCompleted }.sortedBy { it.displayOrder }.firstOrNull()?.stepName
    }

    fun toggleStepCompleted(step: ProcedureCardStep, completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProcedureCardStep(
                step.copy(
                    isCompleted = completed,
                    completedTimestamp = if (completed) System.currentTimeMillis() else null
                )
            )
            repository.getProcedureCardById(step.procedureCardId)?.let { card ->
                if (card.status == "Canceled") return@let
                val allSteps = repository.getStepsByProcedureCardIdOnce(step.procedureCardId)
                val allDone = allSteps.isNotEmpty() && allSteps.all { it.isCompleted }
                val newStatus = if (allDone) "Completed" else "In Progress"
                if (card.status != newStatus) {
                    repository.updateProcedureCard(card.copy(status = newStatus))
                }
            }
        }
    }

    fun updateStepDateGroup(steps: List<ProcedureCardStep>, newDateMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val newDate = Instant.ofEpochMilli(newDateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            steps.forEach { step ->
                val originalTime = Instant.ofEpochMilli(
                    step.completedTimestamp ?: newDateMillis
                ).atZone(ZoneId.systemDefault()).toLocalTime()
                val combined = LocalDateTime.of(newDate, originalTime)
                val resultMillis = combined.atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                repository.updateProcedureCardStep(
                    step.copy(completedTimestamp = resultMillis)
                )
            }
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
            val commaUris = photoUriList.filter { it.isNotBlank() }.joinToString(",")
            repository.insertProcedureCardStep(
                ProcedureCardStep(
                    procedureCardId = cardId,
                    stepName = description.trim(),
                    photoUris = commaUris,
                    displayOrder = newDisplayOrder
                )
            )
        }
    }

    fun deleteCardStep(step: ProcedureCardStep) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProcedureCardStep(step)
        }
    }

    // --- PROCEDURE PAYMENT ---

    fun getPaymentsForCard(cardId: Int): Flow<List<ProcedurePayment>> =
        repository.getPaymentsByProcedureCardId(cardId)

    fun addPayment(cardId: Int, amount: Double, timestamp: Long, notes: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProcedurePayment(
                ProcedurePayment(
                    procedureCardId = cardId,
                    amount = amount,
                    paymentTimestamp = timestamp,
                    notes = notes?.takeIf { it.isNotBlank() }
                )
            )
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
            val newDate = Instant.ofEpochMilli(newDateMillis)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            payments.forEach { payment ->
                val originalTime = Instant.ofEpochMilli(payment.paymentTimestamp)
                    .atZone(ZoneId.systemDefault()).toLocalTime()
                val combined = LocalDateTime.of(newDate, originalTime)
                repository.updateProcedurePayment(
                    payment.copy(
                        paymentTimestamp = combined.atZone(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                    )
                )
            }
        }
    }

    private suspend fun recalculateCard(cardId: Int) {
        val totalPaid = repository.getPaymentsByProcedureCardIdOnce(cardId).sumOf { it.amount }
        repository.getProcedureCardById(cardId)?.let { card ->
            val associateCut: Double
            val clinicShare: Double
            if (card.deductLabFees) {
                associateCut = if (totalPaid - card.labFees > 0)
                    (totalPaid - card.labFees) * (card.appliedPercentage / 100.0) else 0.0
                clinicShare = (totalPaid - card.labFees) - associateCut
            } else {
                associateCut = totalPaid * (card.appliedPercentage / 100.0)
                clinicShare = (totalPaid - associateCut) - card.labFees
            }
            repository.updateProcedureCard(card.copy(
                amountPaid = totalPaid,
                calculatedAssociateCut = associateCut,
                calculatedClinicShare = clinicShare
            ))
        }
    }

    fun updateCardFinancialSettings(
        cardId: Int,
        treatmentFee: Double,
        labFees: Double,
        percentage: Float,
        deductLabFees: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getProcedureCardById(cardId)?.let { card ->
                repository.updateProcedureCard(card.copy(
                    treatmentFee = treatmentFee,
                    labFees = labFees,
                    appliedPercentage = percentage,
                    deductLabFees = deductLabFees
                ))
            }
            recalculateCard(cardId)
        }
    }

    // --- CUSTOM PROCEDURE TEMPLATES ---

    fun insertCustomClinicalProcedure(
        name: String,
        hasTypes: Boolean,
        typesText: String,
        stepsText: String,
        typeStepsText: String,
        defaultFee: Double? = null,
        defaultLabFee: Double? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val maxOrder = (repository.getAllClinicalProceduresOnce().maxOfOrNull { it.displayOrder } ?: 0) + 1
            val cpId = repository.insertClinicalProcedure(
                ClinicalProcedure(name = name.trim(), hasTypes = hasTypes, displayOrder = maxOrder, isCustom = true, defaultFee = defaultFee, defaultLabFee = defaultLabFee)
            ).toInt()

            if (hasTypes) {
                val typeLines = typesText.split("\n").filter { it.isNotBlank() }
                val typeStepMap = mutableMapOf<String, List<String>>()
                typeStepsText.split("|").filter { it.isNotBlank() }.forEach { entry ->
                    val parts = entry.split("::", limit = 2)
                    if (parts.size == 2) {
                        typeStepMap[parts[0].trim()] = parts[1].split("\n").filter { it.isNotBlank() }
                    }
                }

                typeLines.forEach { typeLine ->
                    val parts = typeLine.split("|")
                    val typeName = parts[0].trim()
                    val materials = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
                    val typeFee = parts.getOrNull(2)?.trim()?.toDoubleOrNull()
                    val typeLabFee = parts.getOrNull(3)?.trim()?.toDoubleOrNull()
                    val materialFees = parts.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() }

                    val ptId = repository.insertProcedureType(
                        ProcedureType(clinicalProcedureId = cpId, name = typeName, materials = materials, defaultFee = typeFee, defaultLabFee = typeLabFee, materialFees = materialFees)
                    ).toInt()

                    val typeSteps = typeStepMap[typeName] ?: emptyList()
                    typeSteps.forEachIndexed { idx, stepName ->
                        repository.insertClinicalProcedureStep(
                            ClinicalProcedureStep(procedureTypeId = ptId, stepName = stepName.trim(), displayOrder = idx + 1)
                        )
                    }
                }

                val sharedSteps = stepsText.split("\n").filter { it.isNotBlank() }
                sharedSteps.forEachIndexed { idx, stepName ->
                    repository.insertClinicalProcedureStep(
                        ClinicalProcedureStep(clinicalProcedureId = cpId, stepName = stepName.trim(), displayOrder = idx + 1)
                    )
                }
            } else {
                val steps = stepsText.split("\n").filter { it.isNotBlank() }
                steps.forEachIndexed { idx, stepName ->
                    repository.insertClinicalProcedureStep(
                        ClinicalProcedureStep(clinicalProcedureId = cpId, stepName = stepName.trim(), displayOrder = idx + 1)
                    )
                }
            }
        }
    }

    fun deleteCustomClinicalProcedure(proc: ClinicalProcedure) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomClinicalProcedure(proc.id)
        }
    }

    fun updateClinicalProcedure(
        proc: ClinicalProcedure,
        hasTypes: Boolean,
        typesText: String,
        stepsText: String,
        typeStepsText: String,
        defaultFee: Double? = null,
        defaultLabFee: Double? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateClinicalProcedure(proc.copy(name = proc.name.trim(), hasTypes = hasTypes, defaultFee = defaultFee, defaultLabFee = defaultLabFee))

            val existingTypes = repository.getTypesByClinicalProcedureIdOnce(proc.id)
            existingTypes.forEach { repository.deleteProcedureType(it) }

            val existingSteps = repository.getStepsByClinicalProcedureIdOnce(proc.id)
            existingSteps.forEach { repository.deleteClinicalProcedureStep(it) }

            if (hasTypes) {
                val typeLines = typesText.split("\n").filter { it.isNotBlank() }
                val typeStepMap = mutableMapOf<String, List<String>>()
                typeStepsText.split("|").filter { it.isNotBlank() }.forEach { entry ->
                    val parts = entry.split("::", limit = 2)
                    if (parts.size == 2) {
                        typeStepMap[parts[0].trim()] = parts[1].split("\n").filter { it.isNotBlank() }
                    }
                }

                typeLines.forEach { typeLine ->
                    val parts = typeLine.split("|")
                    val typeName = parts[0].trim()
                    val materials = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
                    val typeFee = parts.getOrNull(2)?.trim()?.toDoubleOrNull()
                    val typeLabFee = parts.getOrNull(3)?.trim()?.toDoubleOrNull()
                    val materialFees = parts.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() }

                    val ptId = repository.insertProcedureType(
                        ProcedureType(clinicalProcedureId = proc.id, name = typeName, materials = materials, defaultFee = typeFee, defaultLabFee = typeLabFee, materialFees = materialFees)
                    ).toInt()

                    val typeSteps = typeStepMap[typeName] ?: emptyList()
                    typeSteps.forEachIndexed { idx, stepName ->
                        repository.insertClinicalProcedureStep(
                            ClinicalProcedureStep(procedureTypeId = ptId, stepName = stepName.trim(), displayOrder = idx + 1)
                        )
                    }
                }

                val sharedSteps = stepsText.split("\n").filter { it.isNotBlank() }
                sharedSteps.forEachIndexed { idx, stepName ->
                    repository.insertClinicalProcedureStep(
                        ClinicalProcedureStep(clinicalProcedureId = proc.id, stepName = stepName.trim(), displayOrder = idx + 1)
                    )
                }
            } else {
                val steps = stepsText.split("\n").filter { it.isNotBlank() }
                steps.forEachIndexed { idx, stepName ->
                    repository.insertClinicalProcedureStep(
                        ClinicalProcedureStep(clinicalProcedureId = proc.id, stepName = stepName.trim(), displayOrder = idx + 1)
                    )
                }
            }
        }
    }

    suspend fun getProcedureTypesWithStepsOnce(procId: Int): ProcedureEditData {
        val types = repository.getTypesByClinicalProcedureIdOnce(procId)
        val steps = repository.getStepsByClinicalProcedureIdOnce(procId)
        val typeStepsMap = mutableMapOf<String, List<String>>()
        types.forEach { type ->
            val ts = repository.getStepsByProcedureTypeIdOnce(type.id)
            if (ts.isNotEmpty()) {
                typeStepsMap[type.name] = ts.map { it.stepName }
            }
        }
        return ProcedureEditData(
            types = types,
            sharedSteps = steps,
            typeStepsMap = typeStepsMap
        )
    }

    suspend fun deleteClinicalProcedureStep(step: ClinicalProcedureStep) {
        repository.deleteClinicalProcedureStep(step)
    }

    // --- SEED DATA ---

    private suspend fun seedBaselineData() {
        // 1. Endodontic TT — steps only
        val endoId = repository.insertClinicalProcedure(
            ClinicalProcedure(name = "Endodontic TT", hasTypes = false, displayOrder = 1, defaultFee = 1500.0, defaultLabFee = 0.0)
        ).toInt()
        listOf("Access opening", "Working length determination", "Shaping", "Cleaning", "Obturation").forEachIndexed { i, s ->
            repository.insertClinicalProcedureStep(
                ClinicalProcedureStep(clinicalProcedureId = endoId, stepName = s, displayOrder = i + 1)
            )
        }

        // 2. Fixed Prosthodontic TT — types + shared steps
        val fixedId = repository.insertClinicalProcedure(
            ClinicalProcedure(name = "Fixed Prosthodontic TT", hasTypes = true, displayOrder = 2, defaultFee = 1000.0, defaultLabFee = 150.0)
        ).toInt()
        val fixedMaterials = "PFM,Zirconia,Composite,EMAX"
        listOf(
            Triple("Single crown", 800.0, "PFM:800:150,Zirconia:1200:200,Composite:900:120,EMAX:1100:180"),
            Triple("Bridge", 2000.0, "PFM:2000:300,Zirconia:2800:400,Composite:1800:280,EMAX:2500:350"),
            Triple("Post and core build up", 500.0, null),
            Triple("Overlay", 700.0, "PFM:700:100,Zirconia:900:150,Composite:750:120,EMAX:850:140"),
            Triple("Endocrown", 900.0, "PFM:900:130,Zirconia:1200:180,Composite:950:140,EMAX:1100:160"),
            Triple("Veneers", 600.0, "Composite:600:80,EMAX:800:100,Zirconia:1000:120")
        ).forEach { (name, fee, materialFees) ->
            repository.insertProcedureType(
                ProcedureType(clinicalProcedureId = fixedId, name = name, materials = fixedMaterials, defaultFee = fee, defaultLabFee = fee * 0.15, materialFees = materialFees)
            )
        }
        listOf("Preparation", "Impression", "Try in", "Delivery").forEachIndexed { i, s ->
            repository.insertClinicalProcedureStep(
                ClinicalProcedureStep(clinicalProcedureId = fixedId, stepName = s, displayOrder = i + 1)
            )
        }

        // 3. Removable Prosthodontic TT — types + shared steps
        val removableId = repository.insertClinicalProcedure(
            ClinicalProcedure(name = "Removable Prosthodontic TT", hasTypes = true, displayOrder = 3, defaultFee = 2000.0, defaultLabFee = 300.0)
        ).toInt()
        listOf(
            "Complete denture class 1 KC" to 2500.0,
            "Class 2 KC" to 2000.0,
            "Class 3 KC" to 1500.0,
            "Class 4 KC" to 1000.0
        ).forEach { (name, fee) ->
            repository.insertProcedureType(ProcedureType(clinicalProcedureId = removableId, name = name, defaultFee = fee, defaultLabFee = fee * 0.15))
        }
        listOf("Primary impression", "Secondary impression", "Bite registration", "Try in", "Delivery").forEachIndexed { i, s ->
            repository.insertClinicalProcedureStep(
                ClinicalProcedureStep(clinicalProcedureId = removableId, stepName = s, displayOrder = i + 1)
            )
        }

        // 4. Oral Surgery — types only, no steps
        val oralId = repository.insertClinicalProcedure(
            ClinicalProcedure(name = "Oral Surgery", hasTypes = true, displayOrder = 4, defaultFee = 600.0, defaultLabFee = 0.0)
        ).toInt()
        listOf(
            "Open extraction" to 400.0,
            "Surgical extraction" to 800.0
        ).forEach { (name, fee) ->
            repository.insertProcedureType(ProcedureType(clinicalProcedureId = oralId, name = name, defaultFee = fee))
        }

        // 5. Periodontic TT — types only, no steps
        val perioId = repository.insertClinicalProcedure(
            ClinicalProcedure(name = "Periodontic TT", hasTypes = true, displayOrder = 5, defaultFee = 400.0, defaultLabFee = 0.0)
        ).toInt()
        listOf(
            "Scaling" to 300.0,
            "Root planing" to 500.0
        ).forEach { (name, fee) ->
            repository.insertProcedureType(ProcedureType(clinicalProcedureId = perioId, name = name, defaultFee = fee))
        }

        // 6. Operative TT — types + shared steps
        val operativeId = repository.insertClinicalProcedure(
            ClinicalProcedure(name = "Operative TT", hasTypes = true, displayOrder = 6, defaultFee = 400.0, defaultLabFee = 0.0)
        ).toInt()
        listOf(
            "Class 1" to 300.0,
            "Class 2" to 400.0,
            "Class 3" to 400.0,
            "Class 4" to 800.0,
            "Class 5" to 500.0,
            "Composite veneer" to 600.0,
            "Composite crown" to 700.0
        ).forEach { (name, fee) ->
            repository.insertProcedureType(ProcedureType(clinicalProcedureId = operativeId, name = name, defaultFee = fee))
        }
        listOf("Caries removal", "Restoration completed").forEachIndexed { i, s ->
            repository.insertClinicalProcedureStep(
                ClinicalProcedureStep(clinicalProcedureId = operativeId, stepName = s, displayOrder = i + 1)
            )
        }

        // 7. Pedodontics TT — types + type-specific steps for pulpotomy/pulpectomy
        val pedoId = repository.insertClinicalProcedure(
            ClinicalProcedure(name = "Pedodontics TT", hasTypes = true, displayOrder = 7, defaultFee = 400.0, defaultLabFee = 0.0)
        ).toInt()
        listOf(
            "Extraction" to 300.0,
            "Pulpotomy" to 400.0,
            "Pulpectomy" to 500.0,
            "Stainless steel crown" to 600.0,
            "Ethatic crown" to 500.0,
            "Floride session" to 200.0
        ).forEach { (name, fee) ->
            repository.insertProcedureType(ProcedureType(clinicalProcedureId = pedoId, name = name, defaultFee = fee))
        }
        val pedoTypes = repository.getTypesByClinicalProcedureIdOnce(pedoId)
        pedoTypes.filter { it.name == "Pulpotomy" || it.name == "Pulpectomy" }.forEach { pt ->
            listOf("Pulp removal", "Obturation").forEachIndexed { i, s ->
                repository.insertClinicalProcedureStep(
                    ClinicalProcedureStep(procedureTypeId = pt.id, stepName = s, displayOrder = i + 1)
                )
            }
        }

        // 8. General Examination — no types, no steps, no materials
        repository.insertClinicalProcedure(
            ClinicalProcedure(name = "General Examination", hasTypes = false, displayOrder = 8)
        )

        // 9. Orthodontics — no types, no steps, no materials
        repository.insertClinicalProcedure(
            ClinicalProcedure(name = "Orthodontics", hasTypes = false, displayOrder = 9)
        )
    }

    private fun sortProcedureCards(cards: List<ProcedureCardDetail>): List<ProcedureCardDetail> {
        return cards.sortedWith(
            compareByDescending<ProcedureCardDetail> {
                it.dateCreated
            }.thenByDescending {
                it.id
            }
        )
    }
}
