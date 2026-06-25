package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

@Serializable
data class ClinicRow(
    val id: Int = 0,
    @SerialName("tenant_id") val tenantId: String? = null,
    val name: String = "",
    @SerialName("default_percentage") val defaultPercentage: Float? = null,
    @SerialName("deduct_lab_fees") val deductLabFeesDefault: Boolean? = null,
    val address: String? = null,
    val phone: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class PatientRow(
    val id: Int = 0,
    @SerialName("tenant_id") val tenantId: String? = null,
    val name: String = "",
    @SerialName("phone_number") val phoneNumber: String = "",
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    @SerialName("systemic_conditions") val systemicConditions: String = "",
    @SerialName("past_dental_treatments") val pastDentalTreatments: String = "",
    val allergies: String = "",
    @SerialName("general_notes") val generalNotes: String = "",
    @SerialName("is_in_progress") val isInProgress: Boolean = true,
    @SerialName("created_date") val createdDate: String = "",
    @SerialName("clinic_id") val clinicId: Int? = null,
    @SerialName("assigned_dentist_id") val assignedDentistId: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @Transient val nextAppointmentDate: String = "",
    @Transient val nextAppointmentTime: String = "",
    @Transient val nextAppointmentNotes: String = ""
)

@Serializable
data class ClinicalProcedureRow(
    val id: Int = 0,
    @SerialName("tenant_id") val tenantId: String? = null,
    val name: String = "",
    @SerialName("has_types") val hasTypes: Boolean = false,
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("is_custom") val isCustom: Boolean = false,
    @SerialName("default_fee") val defaultFee: Double? = null,
    @SerialName("default_lab_fee") val defaultLabFee: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ProcedureTypeRow(
    val id: Int = 0,
    @SerialName("clinical_procedure_id") val clinicalProcedureId: Int = 0,
    val name: String = "",
    val materials: String? = null,
    @SerialName("default_fee") val defaultFee: Double? = null,
    @SerialName("default_lab_fee") val defaultLabFee: Double? = null,
    @SerialName("material_fees") val materialFees: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ClinicalProcedureStepRow(
    val id: Int = 0,
    @SerialName("clinical_procedure_id") val clinicalProcedureId: Int? = null,
    @SerialName("procedure_type_id") val procedureTypeId: Int? = null,
    @SerialName("step_name") val stepName: String = "",
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ProcedureCardRow(
    val id: Int = 0,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("patient_id") val patientId: Int = 0,
    @SerialName("clinic_id") val clinicId: Int = 0,
    @SerialName("dentist_id") val dentistId: String? = null,
    @SerialName("clinical_procedure_id") val clinicalProcedureId: Int = 0,
    @SerialName("procedure_type_id") val procedureTypeId: Int? = null,
    val material: String? = null,
    val status: String = "In Progress",
    @SerialName("date_created") val dateCreated: String? = null,
    @SerialName("tooth_number") val toothNumber: String? = null,
    val notes: String? = null,
    @SerialName("treatment_fee") val treatmentFee: Double = 0.0,
    @SerialName("amount_paid") val amountPaid: Double = 0.0,
    @SerialName("lab_fees") val labFees: Double = 0.0,
    @SerialName("applied_percentage") val appliedPercentage: Float = 0f,
    @SerialName("deduct_lab_fees") val deductLabFees: Boolean = false,
    @SerialName("calculated_associate_cut") val calculatedAssociateCut: Double = 0.0,
    @SerialName("calculated_clinic_share") val calculatedClinicShare: Double = 0.0,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ProcedureCardStepRow(
    val id: Int = 0,
    @SerialName("procedure_card_id") val procedureCardId: Int = 0,
    @SerialName("step_name") val stepName: String = "",
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("photo_urls") val photoUrls: List<String>? = null,
    @SerialName("display_order") val displayOrder: Int = 0,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ProcedurePaymentRow(
    val id: Int = 0,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("procedure_card_id") val procedureCardId: Int = 0,
    val amount: Double = 0.0,
    @SerialName("payment_at") val paymentAt: String = "",
    val notes: String? = null,
    @SerialName("recorded_by") val recordedBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class MedicalFileRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("patient_id") val patientId: Int = 0,
    val title: String = "",
    val content: String = "",
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("file_type") val fileType: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class AppointmentRow(
    val id: Int = 0,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("patient_id") val patientId: Int = 0,
    @SerialName("dentist_id") val dentistId: String? = null,
    @SerialName("clinic_id") val clinicId: Int? = null,
    val date: String = "",
    val time: String = "",
    @SerialName("duration_mins") val durationMins: Int = 30,
    val notes: String? = null,
    val status: String = "scheduled",
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

typealias Clinic = ClinicRow
typealias Patient = PatientRow
typealias ClinicalProcedure = ClinicalProcedureRow
typealias ProcedureType = ProcedureTypeRow
typealias ClinicalProcedureStep = ClinicalProcedureStepRow
typealias ProcedureCard = ProcedureCardRow
typealias ProcedureCardStep = ProcedureCardStepRow
typealias ProcedurePayment = ProcedurePaymentRow
typealias MedicalFile = MedicalFileRow

data class ProcedureCardDetail(
    val id: Int,
    val patientId: Int,
    val clinicId: Int,
    val clinicalProcedureId: Int,
    val procedureTypeId: Int?,
    val material: String?,
    val status: String,
    val dateCreated: String,
    val toothNumber: String?,
    val notes: String?,
    val patientName: String,
    val clinicName: String,
    val procedureName: String,
    val typeName: String?,
    val treatmentFee: Double = 0.0,
    val amountPaid: Double = 0.0,
    val labFees: Double = 0.0,
    val appliedPercentage: Float = 0f,
    val deductLabFees: Boolean = false,
    val calculatedAssociateCut: Double = 0.0,
    val calculatedClinicShare: Double = 0.0
)

data class PaymentWithProcedureName(
    val id: Int,
    val procedureCardId: Int,
    val amount: Double,
    val paymentTimestamp: Long,
    val notes: String?,
    val procedureName: String
)

val ProcedureCardStep.completedTimestamp: Long?
    get() = completedAt?.let {
        try { Instant.parse(it).toEpochMilli() } catch (_: Exception) { null }
    }

val ProcedureCardStep.photoUris: String
    get() = photoUrls?.joinToString(",") ?: ""

val ProcedurePayment.paymentTimestamp: Long
    get() = try { Instant.parse(paymentAt).toEpochMilli() } catch (_: Exception) { 0L }

class SupabaseRepository {

    private val postgrest = SupabaseClient.postgrest
    private val tenantId get() = SessionManager.tenantId.value
    private val userId get() = SessionManager.currentSession.value?.userId

    private fun requireTenantId(): String = tenantId
        ?: throw IllegalStateException("No tenant ID available.")

    // ─── Clinics ─────────────────────────────────────────────────────────────

    fun getClinics(): Flow<List<Clinic>> = flow {
        val tid = requireTenantId()
        emit(postgrest.from("clinics").select { filter { eq("tenant_id", tid) } }.decodeList<Clinic>())
    }.flowOn(Dispatchers.IO)

    suspend fun getClinicById(id: Int): Clinic? {
        val tid = requireTenantId()
        return postgrest.from("clinics").select { filter { eq("id", id); eq("tenant_id", tid) } }.decodeList<Clinic>().firstOrNull()
    }

    suspend fun insertClinic(clinic: Clinic): Int {
        val tid = requireTenantId()
        return postgrest.from("clinics").insert(clinic.copy(tenantId = tid)) { select() }.decodeList<Clinic>().firstOrNull()?.id ?: 0
    }

    suspend fun updateClinic(clinic: Clinic) {
        postgrest.from("clinics").update(clinic) { filter { eq("id", clinic.id) } }
    }

    suspend fun deleteClinic(id: Int) {
        postgrest.from("clinics").delete { filter { eq("id", id) } }
    }

    // ─── Patients ─────────────────────────────────────────────────────────────

    fun getPatients(): Flow<List<Patient>> = flow {
        val tid = requireTenantId()
        emit(postgrest.from("patients").select { filter { eq("tenant_id", tid); exact("deleted_at", null) } }.decodeList<Patient>())
    }.flowOn(Dispatchers.IO)

    suspend fun getPatientById(id: Int): Patient? {
        val tid = requireTenantId()
        return postgrest.from("patients").select { filter { eq("id", id); eq("tenant_id", tid) } }.decodeList<Patient>().firstOrNull()
    }

    fun getPatientByIdFlow(id: Int): Flow<Patient?> = flow {
        emit(getPatientById(id))
    }.flowOn(Dispatchers.IO)

    suspend fun insertPatient(patient: Patient): Int {
        val tid = requireTenantId()
        return postgrest.from("patients").insert(patient.copy(tenantId = tid)) { select() }.decodeList<Patient>().firstOrNull()?.id ?: 0
    }

    suspend fun updatePatient(patient: Patient) {
        val tid = requireTenantId()
        postgrest.from("patients").update(patient) { filter { eq("id", patient.id); eq("tenant_id", tid) } }
    }

    suspend fun softDeletePatient(id: Int) {
        postgrest.rpc("soft_delete_patient", buildJsonObject { put("p_patient_id", id) })
    }

    suspend fun deletePatient(patient: Patient) {
        softDeletePatient(patient.id)
    }

    // ─── Clinical Procedures ───────────────────────────────────────────────────

    fun getClinicalProcedures(): Flow<List<ClinicalProcedure>> = flow {
        val tid = requireTenantId()
        val system = postgrest.from("clinical_procedures").select { filter { exact("tenant_id", null) } }.decodeList<ClinicalProcedure>()
        val tenant = postgrest.from("clinical_procedures").select { filter { eq("tenant_id", tid) } }.decodeList<ClinicalProcedure>()
        emit(system + tenant)
    }.flowOn(Dispatchers.IO)

    suspend fun getAllClinicalProceduresOnce(): List<ClinicalProcedure> {
        val tid = requireTenantId()
        val system = postgrest.from("clinical_procedures").select { filter { exact("tenant_id", null) } }.decodeList<ClinicalProcedure>()
        val tenant = postgrest.from("clinical_procedures").select { filter { eq("tenant_id", tid) } }.decodeList<ClinicalProcedure>()
        return system + tenant
    }

    suspend fun getClinicalProcedureById(id: Int): ClinicalProcedure? {
        return postgrest.from("clinical_procedures").select { filter { eq("id", id) } }.decodeList<ClinicalProcedure>().firstOrNull()
    }

    suspend fun insertClinicalProcedure(proc: ClinicalProcedure): Int {
        val tid = requireTenantId()
        return postgrest.from("clinical_procedures").insert(proc.copy(tenantId = tid)) { select() }.decodeList<ClinicalProcedure>().firstOrNull()?.id ?: 0
    }

    suspend fun updateClinicalProcedure(proc: ClinicalProcedure) {
        postgrest.from("clinical_procedures").update(proc) { filter { eq("id", proc.id) } }
    }

    suspend fun deleteCustomClinicalProcedure(id: Int) {
        postgrest.from("clinical_procedures").delete { filter { eq("id", id); eq("is_custom", true) } }
    }

    // ─── Procedure Types ───────────────────────────────────────────────────────

    fun getTypesByClinicalProcedureId(cpId: Int): Flow<List<ProcedureType>> = flow {
        emit(postgrest.from("procedure_types").select { filter { eq("clinical_procedure_id", cpId) } }.decodeList<ProcedureType>())
    }.flowOn(Dispatchers.IO)

    suspend fun getTypesByClinicalProcedureIdOnce(cpId: Int): List<ProcedureType> {
        return postgrest.from("procedure_types").select { filter { eq("clinical_procedure_id", cpId) } }.decodeList<ProcedureType>()
    }

    suspend fun getProcedureTypeById(id: Int): ProcedureType? {
        return postgrest.from("procedure_types").select { filter { eq("id", id) } }.decodeList<ProcedureType>().firstOrNull()
    }

    suspend fun insertProcedureType(type: ProcedureType): Int {
        return postgrest.from("procedure_types").insert(type) { select() }.decodeList<ProcedureType>().firstOrNull()?.id ?: 0
    }

    suspend fun updateProcedureType(type: ProcedureType) {
        postgrest.from("procedure_types").update(type) { filter { eq("id", type.id) } }
    }

    suspend fun deleteProcedureType(type: ProcedureType) {
        postgrest.from("procedure_types").delete { filter { eq("id", type.id) } }
    }

    // ─── Clinical Procedure Steps ──────────────────────────────────────────────

    suspend fun getStepsByClinicalProcedureIdOnce(cpId: Int): List<ClinicalProcedureStep> {
        return postgrest.from("clinical_procedure_steps").select { filter { eq("clinical_procedure_id", cpId) } }.decodeList<ClinicalProcedureStep>()
    }

    suspend fun getStepsByProcedureTypeIdOnce(ptId: Int): List<ClinicalProcedureStep> {
        return postgrest.from("clinical_procedure_steps").select { filter { eq("procedure_type_id", ptId) } }.decodeList<ClinicalProcedureStep>()
    }

    suspend fun insertClinicalProcedureStep(step: ClinicalProcedureStep): Int {
        return postgrest.from("clinical_procedure_steps").insert(step) { select() }.decodeList<ClinicalProcedureStep>().firstOrNull()?.id ?: 0
    }

    suspend fun deleteClinicalProcedureStep(step: ClinicalProcedureStep) {
        postgrest.from("clinical_procedure_steps").delete { filter { eq("id", step.id) } }
    }

    // ─── Procedure Cards ──────────────────────────────────────────────────────

    fun getProcedureCards(): Flow<List<ProcedureCard>> = flow {
        val tid = requireTenantId()
        emit(postgrest.from("procedure_cards").select { filter { eq("tenant_id", tid); exact("deleted_at", null) } }.decodeList<ProcedureCard>())
    }.flowOn(Dispatchers.IO)

    fun getProcedureCardsByPatientId(patientId: Int): Flow<List<ProcedureCard>> = flow {
        val tid = requireTenantId()
        emit(postgrest.from("procedure_cards").select { filter { eq("patient_id", patientId); eq("tenant_id", tid); exact("deleted_at", null) } }.decodeList<ProcedureCard>())
    }.flowOn(Dispatchers.IO)

    suspend fun getProcedureCardById(id: Int): ProcedureCard? {
        return postgrest.from("procedure_cards").select { filter { eq("id", id) } }.decodeList<ProcedureCard>().firstOrNull()
    }

    suspend fun insertProcedureCard(card: ProcedureCard): Int {
        val tid = requireTenantId()
        val uid = userId
        return postgrest.from("procedure_cards").insert(card.copy(tenantId = tid, dentistId = uid)) { select() }.decodeList<ProcedureCard>().firstOrNull()?.id ?: 0
    }

    suspend fun updateProcedureCard(card: ProcedureCard) {
        val tid = requireTenantId()
        postgrest.from("procedure_cards").update(card) { filter { eq("id", card.id); eq("tenant_id", tid) } }
    }

    suspend fun deleteProcedureCard(card: ProcedureCard) {
        softDeleteProcedureCard(card.id)
    }

    suspend fun softDeleteProcedureCard(id: Int) {
        val tid = requireTenantId()
        postgrest.from("procedure_cards").update(mapOf("deleted_at" to Instant.now().toString())) { 
            filter { eq("id", id); eq("tenant_id", tid) } 
        }
    }

    // ─── Procedure Cards with Details (client-side join) ──────────────────────

    fun getAllProcedureCardsWithDetails(): Flow<List<ProcedureCardDetail>> = flow {
        val tid = requireTenantId()
        val cards = postgrest.from("procedure_cards").select { filter { eq("tenant_id", tid); exact("deleted_at", null) } }.decodeList<ProcedureCard>()
        if (cards.isEmpty()) { emit(emptyList()); return@flow }
        val details = buildCardDetails(cards)
        emit(details)
    }.flowOn(Dispatchers.IO)

    fun getProcedureCardsByPatientIdWithDetails(patientId: Int): Flow<List<ProcedureCardDetail>> = flow {
        val tid = requireTenantId()
        val cards = postgrest.from("procedure_cards").select { filter { eq("patient_id", patientId); eq("tenant_id", tid); exact("deleted_at", null) } }.decodeList<ProcedureCard>()
        if (cards.isEmpty()) { emit(emptyList()); return@flow }
        val details = buildCardDetails(cards)
        emit(details)
    }.flowOn(Dispatchers.IO)

    private suspend fun buildCardDetails(cards: List<ProcedureCard>): List<ProcedureCardDetail> {
        val patientIds = cards.map { it.patientId }.distinct()
        val clinicIds = cards.map { it.clinicId }.distinct()
        val procIds = cards.map { it.clinicalProcedureId }.distinct()
        val typeIds = cards.mapNotNull { it.procedureTypeId }.distinct()

        val patients = postgrest.from("patients").select { filter { isIn("id", patientIds) } }.decodeList<Patient>().associateBy { it.id }
        val clinics = postgrest.from("clinics").select { filter { isIn("id", clinicIds) } }.decodeList<Clinic>().associateBy { it.id }
        val procs = postgrest.from("clinical_procedures").select { filter { isIn("id", procIds) } }.decodeList<ClinicalProcedure>().associateBy { it.id }
        val types = if (typeIds.isNotEmpty()) postgrest.from("procedure_types").select { filter { isIn("id", typeIds) } }.decodeList<ProcedureType>().associateBy { it.id } else emptyMap()

        return cards.map { c ->
            ProcedureCardDetail(
                id = c.id, patientId = c.patientId, clinicId = c.clinicId,
                clinicalProcedureId = c.clinicalProcedureId, procedureTypeId = c.procedureTypeId,
                material = c.material, status = c.status, dateCreated = c.dateCreated ?: "",
                toothNumber = c.toothNumber, notes = c.notes,
                patientName = patients[c.patientId]?.name ?: "",
                clinicName = clinics[c.clinicId]?.name ?: "",
                procedureName = procs[c.clinicalProcedureId]?.name ?: "",
                typeName = c.procedureTypeId?.let { types[it]?.name },
                treatmentFee = c.treatmentFee, amountPaid = c.amountPaid, labFees = c.labFees,
                appliedPercentage = c.appliedPercentage, deductLabFees = c.deductLabFees,
                calculatedAssociateCut = c.calculatedAssociateCut, calculatedClinicShare = c.calculatedClinicShare
            )
        }
    }

    // ─── Procedure Card Steps ──────────────────────────────────────────────────

    fun getStepsByProcedureCardId(cardId: Int): Flow<List<ProcedureCardStep>> = flow {
        emit(postgrest.from("procedure_card_steps").select { filter { eq("procedure_card_id", cardId) } }.decodeList<ProcedureCardStep>())
    }.flowOn(Dispatchers.IO)

    suspend fun getStepsByProcedureCardIdOnce(cardId: Int): List<ProcedureCardStep> {
        return postgrest.from("procedure_card_steps").select { filter { eq("procedure_card_id", cardId) } }.decodeList<ProcedureCardStep>()
    }

    suspend fun insertProcedureCardStep(step: ProcedureCardStep): Int {
        return postgrest.from("procedure_card_steps").insert(step) { select() }.decodeList<ProcedureCardStep>().firstOrNull()?.id ?: 0
    }

    suspend fun updateProcedureCardStep(step: ProcedureCardStep) {
        postgrest.from("procedure_card_steps").update(step) { filter { eq("id", step.id) } }
    }

    suspend fun deleteProcedureCardStep(step: ProcedureCardStep) {
        postgrest.from("procedure_card_steps").delete { filter { eq("id", step.id) } }
    }

    // ─── Procedure Payments ────────────────────────────────────────────────────

    fun getPaymentsByProcedureCardId(cardId: Int): Flow<List<ProcedurePayment>> = flow {
        emit(postgrest.from("procedure_payments").select { filter { eq("procedure_card_id", cardId) } }.decodeList<ProcedurePayment>())
    }.flowOn(Dispatchers.IO)

    suspend fun getPaymentsByProcedureCardIdOnce(cardId: Int): List<ProcedurePayment> {
        return postgrest.from("procedure_payments").select { filter { eq("procedure_card_id", cardId) } }.decodeList<ProcedurePayment>()
    }

    fun getPaymentsByPatientId(patientId: Int): Flow<List<PaymentWithProcedureName>> = flow {
        val tid = requireTenantId()
        val cards = postgrest.from("procedure_cards").select { filter { eq("patient_id", patientId); eq("tenant_id", tid) } }.decodeList<ProcedureCard>()
        if (cards.isEmpty()) { emit(emptyList()); return@flow }
        val cardIds = cards.map { it.id }
        val payments = postgrest.from("procedure_payments").select { filter { isIn("procedure_card_id", cardIds) } }.decodeList<ProcedurePayment>()
        val procIds = cards.map { it.clinicalProcedureId }.distinct()
        val procs = postgrest.from("clinical_procedures").select { filter { isIn("id", procIds) } }.decodeList<ClinicalProcedure>().associateBy { it.id }
        val cardMap = cards.associateBy { it.id }
        emit(payments.map { p ->
            val card = cardMap[p.procedureCardId]
            PaymentWithProcedureName(
                id = p.id, procedureCardId = p.procedureCardId, amount = p.amount,
                paymentTimestamp = p.paymentTimestamp, notes = p.notes,
                procedureName = card?.let { procs[it.clinicalProcedureId]?.name } ?: ""
            )
        })
    }.flowOn(Dispatchers.IO)

    suspend fun insertProcedurePayment(payment: ProcedurePayment): Int {
        val tid = requireTenantId()
        val uid = userId
        return postgrest.from("procedure_payments").insert(payment.copy(tenantId = tid, recordedBy = uid)) { select() }.decodeList<ProcedurePayment>().firstOrNull()?.id ?: 0
    }

    suspend fun deleteProcedurePayment(payment: ProcedurePayment) {
        postgrest.from("procedure_payments").delete { filter { eq("id", payment.id) } }
    }

    suspend fun updateProcedurePayment(payment: ProcedurePayment) {
        postgrest.from("procedure_payments").update(payment) { filter { eq("id", payment.id) } }
    }

    // ─── Medical Files ─────────────────────────────────────────────────────────

    fun getMedicalFilesByPatientId(patientId: Int): Flow<List<MedicalFile>> = flow {
        val tid = requireTenantId()
        emit(postgrest.from("medical_files").select { filter { eq("patient_id", patientId); eq("tenant_id", tid) } }.decodeList<MedicalFile>())
    }.flowOn(Dispatchers.IO)

    suspend fun insertMedicalFile(file: MedicalFile) {
        val tid = requireTenantId()
        val uid = userId
        postgrest.from("medical_files").insert(file.copy(tenantId = tid, createdBy = uid))
    }

    suspend fun deleteMedicalFile(fileId: String) {
        val tid = requireTenantId()
        postgrest.from("medical_files").delete { filter { eq("id", fileId); eq("tenant_id", tid) } }
    }
}