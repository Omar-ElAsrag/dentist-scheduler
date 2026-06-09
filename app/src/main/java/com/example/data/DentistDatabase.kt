package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- ENTITIES ---

@Entity(tableName = "clinics")
data class Clinic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val defaultPercentage: Float? = null,
    val deductLabFeesDefault: Boolean? = null
)

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNumber: String = "",
    val systemicConditions: String = "",
    val pastDentalTreatments: String = "",
    val allergies: String = "",
    val generalNotes: String = "",
    val nextAppointmentDate: String = "",
    val nextAppointmentTime: String = "",
    val nextAppointmentNotes: String = "",
    val isInProgress: Boolean = false,
    val createdDate: String = "",
    val clinicId: Int? = null
)

@Entity(tableName = "clinical_procedures")
data class ClinicalProcedure(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val hasTypes: Boolean,
    val displayOrder: Int,
    val isCustom: Boolean = false,
    val defaultFee: Double? = null,
    val defaultLabFee: Double? = null
)

@Entity(
    tableName = "procedure_types",
    foreignKeys = [
        ForeignKey(
            entity = ClinicalProcedure::class,
            parentColumns = ["id"],
            childColumns = ["clinicalProcedureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clinicalProcedureId")]
)
data class ProcedureType(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clinicalProcedureId: Int,
    val name: String,
    val materials: String? = null,
    val defaultFee: Double? = null,
    val defaultLabFee: Double? = null,
    val materialFees: String? = null
)

@Entity(
    tableName = "clinical_procedure_steps",
    foreignKeys = [
        ForeignKey(
            entity = ClinicalProcedure::class,
            parentColumns = ["id"],
            childColumns = ["clinicalProcedureId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProcedureType::class,
            parentColumns = ["id"],
            childColumns = ["procedureTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clinicalProcedureId"), Index("procedureTypeId")]
)
data class ClinicalProcedureStep(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clinicalProcedureId: Int? = null,
    val procedureTypeId: Int? = null,
    val stepName: String,
    val displayOrder: Int
)

@Entity(
    tableName = "procedure_cards",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Clinic::class,
            parentColumns = ["id"],
            childColumns = ["clinicId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClinicalProcedure::class,
            parentColumns = ["id"],
            childColumns = ["clinicalProcedureId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProcedureType::class,
            parentColumns = ["id"],
            childColumns = ["procedureTypeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("patientId"), Index("clinicId"), Index("clinicalProcedureId"), Index("procedureTypeId")]
)
data class ProcedureCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val clinicId: Int,
    val clinicalProcedureId: Int,
    val procedureTypeId: Int? = null,
    val material: String? = null,
    val status: String = "In Progress",
    val dateCreated: String,
    val toothNumber: String? = null,
    val notes: String? = null,
    val treatmentFee: Double = 0.0,
    val amountPaid: Double = 0.0,
    val labFees: Double = 0.0,
    val appliedPercentage: Float = 0f,
    val deductLabFees: Boolean = false,
    val calculatedAssociateCut: Double = 0.0,
    val calculatedClinicShare: Double = 0.0
)

@Entity(
    tableName = "procedure_card_steps",
    foreignKeys = [
        ForeignKey(
            entity = ProcedureCard::class,
            parentColumns = ["id"],
            childColumns = ["procedureCardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("procedureCardId")]
)
data class ProcedureCardStep(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val procedureCardId: Int,
    val stepName: String,
    val isCompleted: Boolean = false,
    val completedTimestamp: Long? = null,
    val photoUris: String = "",
    val displayOrder: Int = 0
)

@Entity(
    tableName = "procedure_payments",
    foreignKeys = [
        ForeignKey(
            entity = ProcedureCard::class,
            parentColumns = ["id"],
            childColumns = ["procedureCardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("procedureCardId")]
)
data class ProcedurePayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val procedureCardId: Int,
    val amount: Double,
    val paymentTimestamp: Long,
    val notes: String? = null
)

@Entity(tableName = "medical_files")
data class MedicalFile(
    @PrimaryKey val id: String,
    val patientId: Int,
    val title: String,
    val content: String,
    val createdAt: String
)

// --- RELATION / DETAIL DATA CLASSES ---

data class PaymentWithProcedureName(
    val id: Int,
    val procedureCardId: Int,
    val amount: Double,
    val paymentTimestamp: Long,
    val notes: String?,
    val procedureName: String
)

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

data class ProcedureEditData(
    val types: List<ProcedureType>,
    val sharedSteps: List<ClinicalProcedureStep>,
    val typeStepsMap: Map<String, List<String>>
)

// --- DAOs ---

@Dao
interface ClinicDao {
    @Query("SELECT * FROM clinics ORDER BY name ASC")
    fun getAllClinics(): Flow<List<Clinic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinic(clinic: Clinic): Long

    @Query("SELECT * FROM clinics WHERE id = :id")
    suspend fun getClinicById(id: Int): Clinic?

    @Update
    suspend fun updateClinic(clinic: Clinic)

    @Delete
    suspend fun deleteClinic(clinic: Clinic)
}

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY name ASC")
    fun getAllPatients(): Flow<List<Patient>>

    @Query("SELECT * FROM patients WHERE id = :id")
    fun getPatientByIdFlow(id: Int): Flow<Patient?>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getPatientById(id: Int): Patient?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient): Long

    @Update
    suspend fun updatePatient(patient: Patient)

    @Delete
    suspend fun deletePatient(patient: Patient)
}

@Dao
interface ClinicalProcedureDao {
    @Query("SELECT * FROM clinical_procedures ORDER BY displayOrder ASC")
    fun getAll(): Flow<List<ClinicalProcedure>>

    @Query("SELECT * FROM clinical_procedures ORDER BY displayOrder ASC")
    suspend fun getAllOnce(): List<ClinicalProcedure>

    @Query("SELECT * FROM clinical_procedures WHERE id = :id")
    suspend fun getById(id: Int): ClinicalProcedure?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(proc: ClinicalProcedure): Long

    @Update
    suspend fun update(proc: ClinicalProcedure)

    @Delete
    suspend fun delete(proc: ClinicalProcedure)

    @Query("DELETE FROM clinical_procedures WHERE isCustom = 1 AND id = :id")
    suspend fun deleteCustom(id: Int)
}

@Dao
interface ProcedureTypeDao {
    @Query("SELECT * FROM procedure_types WHERE clinicalProcedureId = :clinicalProcedureId ORDER BY name ASC")
    fun getByClinicalProcedureId(clinicalProcedureId: Int): Flow<List<ProcedureType>>

    @Query("SELECT * FROM procedure_types WHERE clinicalProcedureId = :clinicalProcedureId ORDER BY name ASC")
    suspend fun getByClinicalProcedureIdOnce(clinicalProcedureId: Int): List<ProcedureType>

    @Query("SELECT * FROM procedure_types WHERE id = :id")
    suspend fun getById(id: Int): ProcedureType?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(type: ProcedureType): Long

    @Update
    suspend fun update(type: ProcedureType)

    @Delete
    suspend fun delete(type: ProcedureType)
}

@Dao
interface ClinicalProcedureStepDao {
    @Query("SELECT * FROM clinical_procedure_steps WHERE clinicalProcedureId = :clinicalProcedureId ORDER BY displayOrder ASC")
    fun getByClinicalProcedureId(clinicalProcedureId: Int): Flow<List<ClinicalProcedureStep>>

    @Query("SELECT * FROM clinical_procedure_steps WHERE clinicalProcedureId = :clinicalProcedureId ORDER BY displayOrder ASC")
    suspend fun getByClinicalProcedureIdOnce(clinicalProcedureId: Int): List<ClinicalProcedureStep>

    @Query("SELECT * FROM clinical_procedure_steps WHERE procedureTypeId = :procedureTypeId ORDER BY displayOrder ASC")
    fun getByProcedureTypeId(procedureTypeId: Int): Flow<List<ClinicalProcedureStep>>

    @Query("SELECT * FROM clinical_procedure_steps WHERE procedureTypeId = :procedureTypeId ORDER BY displayOrder ASC")
    suspend fun getByProcedureTypeIdOnce(procedureTypeId: Int): List<ClinicalProcedureStep>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: ClinicalProcedureStep): Long

    @Delete
    suspend fun delete(step: ClinicalProcedureStep)
}

@Dao
interface ProcedureCardDao {
    @Query("""
        SELECT pc.*, p.name AS patientName, c.name AS clinicName, 
               cp.name AS procedureName, pt.name AS typeName
        FROM procedure_cards pc
        INNER JOIN patients p ON pc.patientId = p.id
        INNER JOIN clinics c ON pc.clinicId = c.id
        INNER JOIN clinical_procedures cp ON pc.clinicalProcedureId = cp.id
        LEFT JOIN procedure_types pt ON pc.procedureTypeId = pt.id
        ORDER BY pc.dateCreated DESC
    """)
    fun getAllWithDetails(): Flow<List<ProcedureCardDetail>>

    @Query("""
        SELECT pc.*, p.name AS patientName, c.name AS clinicName, 
               cp.name AS procedureName, pt.name AS typeName
        FROM procedure_cards pc
        INNER JOIN patients p ON pc.patientId = p.id
        INNER JOIN clinics c ON pc.clinicId = c.id
        INNER JOIN clinical_procedures cp ON pc.clinicalProcedureId = cp.id
        LEFT JOIN procedure_types pt ON pc.procedureTypeId = pt.id
        WHERE pc.patientId = :patientId
        ORDER BY pc.dateCreated DESC
    """)
    fun getByPatientIdWithDetails(patientId: Int): Flow<List<ProcedureCardDetail>>

    @Query("SELECT * FROM procedure_cards WHERE id = :id")
    suspend fun getById(id: Int): ProcedureCard?

    @Query("SELECT * FROM procedure_cards WHERE patientId = :patientId")
    suspend fun getByPatientId(patientId: Int): List<ProcedureCard>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: ProcedureCard): Long

    @Update
    suspend fun update(card: ProcedureCard)

    @Delete
    suspend fun delete(card: ProcedureCard)
}

@Dao
interface ProcedureCardStepDao {
    @Query("SELECT * FROM procedure_card_steps WHERE procedureCardId = :procedureCardId ORDER BY displayOrder ASC")
    fun getByProcedureCardId(procedureCardId: Int): Flow<List<ProcedureCardStep>>

    @Query("SELECT * FROM procedure_card_steps WHERE procedureCardId = :procedureCardId ORDER BY displayOrder ASC")
    suspend fun getByProcedureCardIdOnce(procedureCardId: Int): List<ProcedureCardStep>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: ProcedureCardStep): Long

    @Update
    suspend fun update(step: ProcedureCardStep)

    @Delete
    suspend fun delete(step: ProcedureCardStep)
}

@Dao
interface MedicalFileDao {
    @Query("SELECT * FROM medical_files WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun getByPatientId(patientId: Int): Flow<List<MedicalFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: MedicalFile)

    @Query("DELETE FROM medical_files WHERE id = :fileId")
    suspend fun deleteById(fileId: String)
}

@Dao
interface ProcedurePaymentDao {
    @Query("SELECT * FROM procedure_payments WHERE procedureCardId = :cardId ORDER BY paymentTimestamp DESC, id DESC")
    fun getByProcedureCardId(cardId: Int): Flow<List<ProcedurePayment>>

    @Query("SELECT * FROM procedure_payments WHERE procedureCardId = :cardId ORDER BY paymentTimestamp DESC, id DESC")
    suspend fun getByProcedureCardIdOnce(cardId: Int): List<ProcedurePayment>

    @Query("""
        SELECT pp.id, pp.procedureCardId, pp.amount, pp.paymentTimestamp, pp.notes, cp.name AS procedureName
        FROM procedure_payments pp
        INNER JOIN procedure_cards pc ON pp.procedureCardId = pc.id
        INNER JOIN clinical_procedures cp ON pc.clinicalProcedureId = cp.id
        WHERE pc.patientId = :patientId
        ORDER BY pp.paymentTimestamp DESC, pp.id DESC
    """)
    fun getByPatientId(patientId: Int): Flow<List<PaymentWithProcedureName>>

    @Insert
    suspend fun insert(payment: ProcedurePayment): Long

    @Delete
    suspend fun delete(payment: ProcedurePayment)

    @Update
    suspend fun update(payment: ProcedurePayment)
}

// --- DATABASE ---

@Database(
    entities = [
        Clinic::class, Patient::class,
        ClinicalProcedure::class, ProcedureType::class, ClinicalProcedureStep::class,
        ProcedureCard::class, ProcedureCardStep::class, ProcedurePayment::class, MedicalFile::class
    ],
    version = 11,
    exportSchema = false
)
abstract class DentistDatabase : RoomDatabase() {
    abstract fun clinicDao(): ClinicDao
    abstract fun patientDao(): PatientDao
    abstract fun clinicalProcedureDao(): ClinicalProcedureDao
    abstract fun procedureTypeDao(): ProcedureTypeDao
    abstract fun clinicalProcedureStepDao(): ClinicalProcedureStepDao
    abstract fun procedureCardDao(): ProcedureCardDao
    abstract fun procedureCardStepDao(): ProcedureCardStepDao
    abstract fun medicalFileDao(): MedicalFileDao
    abstract fun procedurePaymentDao(): ProcedurePaymentDao
}

// --- REPOSITORY ---

class DentistRepository(private val db: DentistDatabase) {
    val clinics: Flow<List<Clinic>> = db.clinicDao().getAllClinics()
    val patients: Flow<List<Patient>> = db.patientDao().getAllPatients()
    val clinicalProcedures: Flow<List<ClinicalProcedure>> = db.clinicalProcedureDao().getAll()
    val procedureCards: Flow<List<ProcedureCardDetail>> = db.procedureCardDao().getAllWithDetails()

    // Patient
    suspend fun getPatientById(id: Int): Patient? = db.patientDao().getPatientById(id)
    fun getPatientByIdFlow(id: Int): Flow<Patient?> = db.patientDao().getPatientByIdFlow(id)
    suspend fun insertPatient(patient: Patient): Long = db.patientDao().insertPatient(patient)
    suspend fun updatePatient(patient: Patient) = db.patientDao().updatePatient(patient)
    suspend fun deletePatient(patient: Patient) = db.patientDao().deletePatient(patient)

    // Clinic
    suspend fun getClinicById(id: Int): Clinic? = db.clinicDao().getClinicById(id)
    suspend fun insertClinic(clinic: Clinic): Long = db.clinicDao().insertClinic(clinic)
    suspend fun updateClinic(clinic: Clinic) = db.clinicDao().updateClinic(clinic)
    suspend fun deleteClinic(clinic: Clinic) = db.clinicDao().deleteClinic(clinic)

    // ClinicalProcedure
    suspend fun getAllClinicalProceduresOnce(): List<ClinicalProcedure> = db.clinicalProcedureDao().getAllOnce()
    suspend fun getClinicalProcedureById(id: Int): ClinicalProcedure? = db.clinicalProcedureDao().getById(id)
    suspend fun insertClinicalProcedure(proc: ClinicalProcedure): Long = db.clinicalProcedureDao().insert(proc)
    suspend fun updateClinicalProcedure(proc: ClinicalProcedure) = db.clinicalProcedureDao().update(proc)
    suspend fun deleteCustomClinicalProcedure(id: Int) = db.clinicalProcedureDao().deleteCustom(id)

    // ProcedureType
    fun getTypesByClinicalProcedureId(cpId: Int): Flow<List<ProcedureType>> =
        db.procedureTypeDao().getByClinicalProcedureId(cpId)
    suspend fun getTypesByClinicalProcedureIdOnce(cpId: Int): List<ProcedureType> =
        db.procedureTypeDao().getByClinicalProcedureIdOnce(cpId)
    suspend fun getProcedureTypeById(id: Int): ProcedureType? = db.procedureTypeDao().getById(id)
    suspend fun insertProcedureType(type: ProcedureType): Long = db.procedureTypeDao().insert(type)
    suspend fun updateProcedureType(type: ProcedureType) = db.procedureTypeDao().update(type)
    suspend fun deleteProcedureType(type: ProcedureType) = db.procedureTypeDao().delete(type)

    // ClinicalProcedureStep
    suspend fun getStepsByClinicalProcedureIdOnce(cpId: Int): List<ClinicalProcedureStep> =
        db.clinicalProcedureStepDao().getByClinicalProcedureIdOnce(cpId)
    suspend fun getStepsByProcedureTypeIdOnce(ptId: Int): List<ClinicalProcedureStep> =
        db.clinicalProcedureStepDao().getByProcedureTypeIdOnce(ptId)
    suspend fun insertClinicalProcedureStep(step: ClinicalProcedureStep): Long =
        db.clinicalProcedureStepDao().insert(step)
    suspend fun deleteClinicalProcedureStep(step: ClinicalProcedureStep) =
        db.clinicalProcedureStepDao().delete(step)

    // ProcedureCard
    fun getProcedureCardsByPatientId(patientId: Int): Flow<List<ProcedureCardDetail>> =
        db.procedureCardDao().getByPatientIdWithDetails(patientId)
    suspend fun getProcedureCardById(id: Int): ProcedureCard? = db.procedureCardDao().getById(id)
    suspend fun insertProcedureCard(card: ProcedureCard): Long = db.procedureCardDao().insert(card)
    suspend fun updateProcedureCard(card: ProcedureCard) = db.procedureCardDao().update(card)
    suspend fun deleteProcedureCard(card: ProcedureCard) = db.procedureCardDao().delete(card)

    // ProcedureCardStep
    fun getStepsByProcedureCardId(cardId: Int): Flow<List<ProcedureCardStep>> =
        db.procedureCardStepDao().getByProcedureCardId(cardId)
    suspend fun getStepsByProcedureCardIdOnce(cardId: Int): List<ProcedureCardStep> =
        db.procedureCardStepDao().getByProcedureCardIdOnce(cardId)
    suspend fun insertProcedureCardStep(step: ProcedureCardStep): Long =
        db.procedureCardStepDao().insert(step)
    suspend fun updateProcedureCardStep(step: ProcedureCardStep) =
        db.procedureCardStepDao().update(step)
    suspend fun deleteProcedureCardStep(step: ProcedureCardStep) =
        db.procedureCardStepDao().delete(step)

    // MedicalFile
    fun getMedicalFilesByPatientId(patientId: Int): Flow<List<MedicalFile>> =
        db.medicalFileDao().getByPatientId(patientId)
    suspend fun insertMedicalFile(file: MedicalFile) =
        db.medicalFileDao().insert(file)
    suspend fun deleteMedicalFile(fileId: String) =
        db.medicalFileDao().deleteById(fileId)

    // ProcedurePayment
    fun getPaymentsByProcedureCardId(cardId: Int): Flow<List<ProcedurePayment>> =
        db.procedurePaymentDao().getByProcedureCardId(cardId)
    suspend fun getPaymentsByProcedureCardIdOnce(cardId: Int): List<ProcedurePayment> =
        db.procedurePaymentDao().getByProcedureCardIdOnce(cardId)
    fun getPaymentsByPatientId(patientId: Int): Flow<List<PaymentWithProcedureName>> =
        db.procedurePaymentDao().getByPatientId(patientId)
    suspend fun insertProcedurePayment(payment: ProcedurePayment): Long =
        db.procedurePaymentDao().insert(payment)
    suspend fun deleteProcedurePayment(payment: ProcedurePayment) =
        db.procedurePaymentDao().delete(payment)
    suspend fun updateProcedurePayment(payment: ProcedurePayment) =
        db.procedurePaymentDao().update(payment)
}
