package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import com.example.ui.DentistViewModel
import com.example.ui.localizedProcedureName
import com.example.ui.components.AppCard
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientPortalDialog(
    patient: Patient,
    viewModel: DentistViewModel,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    var nameField by remember { mutableStateOf(patient.name) }
    var phoneField by remember { mutableStateOf(patient.phoneNumber) }
    var systemicField by remember { mutableStateOf(patient.systemicConditions) }
    var allergiesField by remember { mutableStateOf(patient.allergies) }

    var showAddConditionDialog by remember { mutableStateOf(false) }
    var newConditionText by remember { mutableStateOf("") }
    var showAddAllergyDialog by remember { mutableStateOf(false) }
    var newAllergyText by remember { mutableStateOf("") }

    val procedureCards by viewModel.selectedPatientProcedureCards.collectAsState()
    val allPayments by viewModel.selectedPatientPayments.collectAsState()
    var fullPreviewUri by remember { mutableStateOf<String?>(null) }

    val clinics by viewModel.clinics.collectAsState()
    val clinicalProcedures by viewModel.clinicalProcedures.collectAsState()

    var showAddCardDialog by remember { mutableStateOf(false) }
    var showDeletePatientConfirm by remember { mutableStateOf(false) }
    var showScheduleAppointmentDialog by remember { mutableStateOf(false) }
    var dialogAppointmentDate by remember { mutableStateOf(patient.nextAppointmentDate) }
    var dialogAppointmentTime by remember { mutableStateOf(patient.nextAppointmentTime) }
    var dialogAppointmentNotes by remember { mutableStateOf(patient.nextAppointmentNotes) }

    LaunchedEffect(patient.id) {
        viewModel.selectPatient(patient.id)
    }

    val patientOverallStatus = remember(procedureCards) {
        val patientCards = procedureCards.filter { it.patientId == patient.id }
        if (patientCards.isEmpty()) null
        else if (patientCards.any { it.status == "In Progress" }) "In Progress"
        else if (patientCards.any { it.status == "Canceled" }) "Canceled"
        else "Completed"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = patient.name.firstOrNull()?.toString()?.uppercase() ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = patient.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (patient.phoneNumber.isNotBlank()) {
                                Text(
                                    text = stringResource(R.string.phone_label, patient.phoneNumber),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (patient.systemicConditions.isNotBlank() || patient.allergies.isNotBlank()) Color.Red else Color.Green,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (patient.systemicConditions.isNotBlank() || patient.allergies.isNotBlank()) stringResource(R.string.medical_alerts_pending) else stringResource(R.string.health_record_safe),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                            if (patientOverallStatus != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val statusColor = when (patientOverallStatus) {
                                        "Completed" -> Color(0xFF2E7D32)
                                        "Canceled" -> MaterialTheme.colorScheme.error
                                        else -> Color(0xFFEF6C00)
                                    }
                                    val statusLabel = when (patientOverallStatus) {
                                        "Completed" -> stringResource(R.string.completed)
                                        "Canceled" -> stringResource(R.string.canceled)
                                        else -> stringResource(R.string.in_progress)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(statusColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_portal_button")) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close_portal))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedButton(
                        onClick = { activeTab = 0 },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (activeTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (activeTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("portal_tab_treatment")
                    ) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.treatment_portal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    ElevatedButton(
                        onClick = { activeTab = 1 },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (activeTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (activeTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("portal_tab_medical")
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.medical_file_tab), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (activeTab == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            var nextTreatmentStep by remember { mutableStateOf<Pair<String, String>?>(null) }
                            LaunchedEffect(procedureCards) {
                                for (card in procedureCards) {
                                    val steps = viewModel.getStepsForCard(card.id).first()
                                    val firstPending = steps.filter { !it.isCompleted }.minByOrNull { it.displayOrder }
                                    if (firstPending != null) {
                                        nextTreatmentStep = card.procedureName to firstPending.stepName
                                        break
                                    }
                                }
                            }

                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.next_appointment_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(8.dp))

                                if (patient.nextAppointmentDate.isBlank()) {
                                    Text(stringResource(R.string.no_appointment_scheduled), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    Row(Modifier.fillMaxWidth()) {
                                        Text(stringResource(R.string.date) + ": ", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        Text(patient.nextAppointmentDate, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (patient.nextAppointmentTime.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Row(Modifier.fillMaxWidth()) {
                                            Text(stringResource(R.string.time) + ": ", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                            Text(patient.nextAppointmentTime, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    if (patient.nextAppointmentNotes.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Row(Modifier.fillMaxWidth()) {
                                            Text(stringResource(R.string.notes) + ": ", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                            Text(patient.nextAppointmentNotes, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(8.dp))

                                val nextStepText = nextTreatmentStep?.let { stringResource(R.string.next_step_text, it.first, it.second) }
                                if (nextStepText != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(6.dp))
                                        Text(nextStepText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    Text(stringResource(R.string.no_pending_treatment_steps), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                                }

                                Spacer(Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = {
                                        dialogAppointmentDate = patient.nextAppointmentDate
                                        dialogAppointmentTime = patient.nextAppointmentTime
                                        dialogAppointmentNotes = patient.nextAppointmentNotes
                                        showScheduleAppointmentDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.schedule_next), fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            val totalFees = procedureCards.sumOf { it.treatmentFee }
                            val totalPaid = allPayments.sumOf { it.amount }
                            val remainingBalance = totalFees - totalPaid
                            val paidProgress = if (totalFees > 0) (totalPaid / totalFees).toFloat() else 0f

                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.payment_summary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(8.dp))

                                Row(Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.total_fees), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text("$${String.format(Locale.US, "%.2f", totalFees)}", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.total_paid), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text("$${String.format(Locale.US, "%.2f", totalPaid)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.remaining_balance), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text("$${String.format(Locale.US, "%.2f", remainingBalance)}", fontWeight = FontWeight.Bold, color = if (remainingBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { paidProgress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(R.string.percent_paid, (paidProgress * 100).toInt()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(8.dp))

                                Text(stringResource(R.string.payment_history), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(8.dp))

                                val sortedPayments = allPayments.sortedBy { it.paymentTimestamp }
                                if (sortedPayments.isEmpty()) {
                                    Text(stringResource(R.string.no_payments_recorded), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    sortedPayments.forEachIndexed { index, payment ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(payment.paymentTimestamp)),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = localizedProcedureName(payment.procedureName),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "$${String.format(Locale.US, "%.2f", payment.amount)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                        if (index < sortedPayments.size - 1) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { showAddCardDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.add_procedure_card), fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Procedure Cards List
                            if (procedureCards.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.EventNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = stringResource(R.string.no_cards_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground.copy(0.6f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(R.string.no_cards_subtitle),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onBackground.copy(0.4f)
                                        )
                                    }
                                }
                            } else {
                                procedureCards.forEach { card ->
                                    ProcedureCardSection(
                                        cardDetail = card,
                                        viewModel = viewModel,
                                        onRequestPreview = { fullPreviewUri = it }
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showDeletePatientConfirm = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C)),
                                border = BorderStroke(1.dp, Color(0xFFB71C1C)),
                                modifier = Modifier.fillMaxWidth().testTag("delete_patient_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.delete_patient_record), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                AppCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.MedicalServices, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.systemic_conditions_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))

                                    val conditions = systemicField.split("\n").filter { it.isNotBlank() }
                                    if (conditions.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.no_systemic_text),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        conditions.forEachIndexed { index, condition ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = condition,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val lines = systemicField.split("\n").filter { it.isNotBlank() }.toMutableList()
                                                        lines.remove(condition)
                                                        systemicField = lines.joinToString("\n")
                                                        viewModel.updatePatient(patient.copy(systemicConditions = systemicField))
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = stringResource(R.string.cd_delete_item),
                                                        modifier = Modifier.size(18.dp),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                            if (index < conditions.size - 1) {
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))
                                    OutlinedButton(
                                        onClick = { showAddConditionDialog = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.add_systemic_condition), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            item {
                                Spacer(Modifier.height(24.dp))
                            }
                            item {
                                AppCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.drug_allergies_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))

                                    val allergies = allergiesField.split("\n").filter { it.isNotBlank() }
                                    if (allergies.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.no_allergies_text),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        allergies.forEachIndexed { index, allergy ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = allergy,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val lines = allergiesField.split("\n").filter { it.isNotBlank() }.toMutableList()
                                                        lines.remove(allergy)
                                                        allergiesField = lines.joinToString("\n")
                                                        viewModel.updatePatient(patient.copy(allergies = allergiesField))
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = stringResource(R.string.cd_delete_item),
                                                        modifier = Modifier.size(18.dp),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                            if (index < allergies.size - 1) {
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))
                                    OutlinedButton(
                                        onClick = { showAddAllergyDialog = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.add_allergy), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            item {
                                HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showDeletePatientConfirm = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C)),
                                    border = BorderStroke(1.dp, Color(0xFFB71C1C)),
                                    modifier = Modifier.fillMaxWidth().testTag("delete_patient_button")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.delete_patient_record), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddConditionDialog) {
        AlertDialog(
            onDismissRequest = { showAddConditionDialog = false; newConditionText = "" },
            title = { Text(stringResource(R.string.add_alert_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = newConditionText,
                    onValueChange = { newConditionText = it },
                    label = { Text(stringResource(R.string.systemic_condition_label_single)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newConditionText.isNotBlank()) {
                        systemicField = if (systemicField.isBlank()) newConditionText else "$systemicField\n$newConditionText"
                        viewModel.updatePatient(patient.copy(systemicConditions = systemicField))
                        newConditionText = ""
                        showAddConditionDialog = false
                    }
                }) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddConditionDialog = false; newConditionText = "" }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddAllergyDialog) {
        AlertDialog(
            onDismissRequest = { showAddAllergyDialog = false; newAllergyText = "" },
            title = { Text(stringResource(R.string.add_allergy)) },
            text = {
                OutlinedTextField(
                    value = newAllergyText,
                    onValueChange = { newAllergyText = it },
                    label = { Text(stringResource(R.string.allergy_label_single)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newAllergyText.isNotBlank()) {
                        allergiesField = if (allergiesField.isBlank()) newAllergyText else "$allergiesField\n$newAllergyText"
                        viewModel.updatePatient(patient.copy(allergies = allergiesField))
                        newAllergyText = ""
                        showAddAllergyDialog = false
                    }
                }) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAllergyDialog = false; newAllergyText = "" }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddCardDialog) {
        var dialogProcExpanded by remember { mutableStateOf(false) }
        var dialogSelectedProcedure by remember { mutableStateOf<ClinicalProcedure?>(null) }
        var dialogTypes by remember { mutableStateOf<List<ProcedureType>>(emptyList()) }
        var dialogTypeExpanded by remember { mutableStateOf(false) }
        var dialogSelectedType by remember { mutableStateOf<ProcedureType?>(null) }
        var dialogMaterials by remember { mutableStateOf<List<String>>(emptyList()) }
        var dialogMatExpanded by remember { mutableStateOf(false) }
        var dialogSelectedMaterial by remember { mutableStateOf<String?>(null) }
        var dialogTooth by remember { mutableStateOf("") }
        var dialogClinicExpanded by remember { mutableStateOf(false) }
        var dialogSelectedClinic by remember { mutableStateOf<Clinic?>(null) }
        var dialogDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
        var dialogShowDatePicker by remember { mutableStateOf(false) }
        var dialogNotes by remember { mutableStateOf("") }
        var dialogFee by remember { mutableStateOf("") }
        var dialogLabFee by remember { mutableStateOf("") }

        LaunchedEffect(dialogSelectedProcedure) {
            if (dialogSelectedProcedure != null) {
                dialogTypes = viewModel.getTypesForProcedureOnce(dialogSelectedProcedure!!.id)
                dialogSelectedType = null
                dialogSelectedMaterial = null
                dialogMaterials = emptyList()
            } else {
                dialogTypes = emptyList()
            }
        }

        LaunchedEffect(dialogSelectedType) {
            dialogMaterials = if (dialogSelectedType != null && !dialogSelectedType!!.materials.isNullOrBlank()) {
                dialogSelectedType!!.materials!!.split(",").map { it.trim() }.filter { it.isNotBlank() }
            } else emptyList()
            dialogSelectedMaterial = null
        }

        LaunchedEffect(dialogSelectedProcedure, dialogSelectedType) {
            val procDefaultFee = dialogSelectedProcedure?.defaultFee ?: 0.0
            val typeDefaultFee = dialogSelectedType?.defaultFee ?: 0.0
            dialogFee = if (typeDefaultFee > 0) typeDefaultFee.toString()
                        else if (procDefaultFee > 0) procDefaultFee.toString()
                        else ""

            val procDefaultLabFee = dialogSelectedProcedure?.defaultLabFee ?: 0.0
            val typeDefaultLabFee = dialogSelectedType?.defaultLabFee ?: 0.0
            dialogLabFee = if (typeDefaultLabFee > 0) typeDefaultLabFee.toString()
                           else if (procDefaultLabFee > 0) procDefaultLabFee.toString()
                           else ""
        }

        AlertDialog(
            onDismissRequest = { showAddCardDialog = false },
            title = { Text(stringResource(R.string.add_procedure_card), fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = localizedProcedureName(dialogSelectedProcedure?.name ?: stringResource(R.string.dropdown_select_procedure)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.clinical_procedure_field)) },
                            trailingIcon = {
                                IconButton(onClick = { dialogProcExpanded = !dialogProcExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(expanded = dialogProcExpanded, onDismissRequest = { dialogProcExpanded = false }) {
                            clinicalProcedures.forEach { cp ->
                                DropdownMenuItem(
                                    text = { Text(localizedProcedureName(cp.name)) },
                                    onClick = {
                                        dialogSelectedProcedure = cp
                                        dialogProcExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (dialogSelectedProcedure?.hasTypes == true && dialogTypes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = dialogSelectedType?.name ?: stringResource(R.string.dropdown_select_type),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.type)) },
                                trailingIcon = {
                                    IconButton(onClick = { dialogTypeExpanded = !dialogTypeExpanded }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(expanded = dialogTypeExpanded, onDismissRequest = { dialogTypeExpanded = false }) {
                                dialogTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.name) },
                                        onClick = {
                                            dialogSelectedType = type
                                            dialogTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (dialogMaterials.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = dialogSelectedMaterial ?: stringResource(R.string.dropdown_select_material),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.material)) },
                                trailingIcon = {
                                    IconButton(onClick = { dialogMatExpanded = !dialogMatExpanded }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(expanded = dialogMatExpanded, onDismissRequest = { dialogMatExpanded = false }) {
                                dialogMaterials.forEach { mat ->
                                    DropdownMenuItem(
                                        text = { Text(mat) },
                                        onClick = {
                                            dialogSelectedMaterial = mat
                                            dialogMatExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dialogTooth,
                        onValueChange = { dialogTooth = it },
                        label = { Text(stringResource(R.string.tooth_label)) },
                        placeholder = { Text(stringResource(R.string.hint_tooth)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dialogSelectedClinic?.name ?: stringResource(R.string.dropdown_select_clinic),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.select_associate_clinic)) },
                            trailingIcon = {
                                IconButton(onClick = { dialogClinicExpanded = !dialogClinicExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(expanded = dialogClinicExpanded, onDismissRequest = { dialogClinicExpanded = false }) {
                            clinics.forEach { clinic ->
                                DropdownMenuItem(
                                    text = { Text(clinic.name) },
                                    onClick = {
                                        dialogSelectedClinic = clinic
                                        dialogClinicExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Box {
                        OutlinedTextField(
                            value = dialogDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.date)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { dialogShowDatePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.cd_pick_date))
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dialogNotes,
                        onValueChange = { dialogNotes = it },
                        label = { Text(stringResource(R.string.notes)) },
                        placeholder = { Text(stringResource(R.string.hint_notes)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dialogSelectedClinic != null && dialogSelectedProcedure != null) {
                            viewModel.createProcedureCard(
                                patientId = patient.id,
                                clinicId = dialogSelectedClinic!!.id,
                                clinicalProcedureId = dialogSelectedProcedure!!.id,
                                procedureTypeId = dialogSelectedType?.id,
                                material = dialogSelectedMaterial,
                                dateCreated = dialogDate.trim(),
                                toothNumber = dialogTooth.trim(),
                                notes = dialogNotes.trim(),
                                selectedStepNames = null,
                                additionalStepNames = emptyList(),
                                treatmentFee = dialogFee.toDoubleOrNull() ?: 0.0,
                                labFees = dialogLabFee.toDoubleOrNull() ?: 0.0
                            )
                            showAddCardDialog = false
                        }
                    },
                    enabled = dialogSelectedClinic != null && dialogSelectedProcedure != null
                ) {
                    Text(stringResource(R.string.create_button), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCardDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )

        if (dialogShowDatePicker) {
            val dpState = rememberDatePickerState(
                initialSelectedDateMillis = try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dialogDate)?.time
                } catch (_: Exception) { System.currentTimeMillis() }
            )
            DatePickerDialog(
                onDismissRequest = { dialogShowDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        dpState.selectedDateMillis?.let { millis ->
                            dialogDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                        }
                        dialogShowDatePicker = false
                    }) { Text(stringResource(R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { dialogShowDatePicker = false }) { Text(stringResource(R.string.cancel)) }
                }
            ) {
                DatePicker(state = dpState)
            }
        }
    }

    if (showScheduleAppointmentDialog) {
        var showSchedDatePicker by remember { mutableStateOf(false) }
        var showSchedTimePicker by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showScheduleAppointmentDialog = false },
            title = { Text(stringResource(R.string.schedule_next), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Box {
                        OutlinedTextField(
                            value = dialogAppointmentDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.date)) },
                            modifier = Modifier.fillMaxWidth().testTag("dialog_appointment_date_input"),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showSchedDatePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.cd_pick_date))
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Box {
                        OutlinedTextField(
                            value = dialogAppointmentTime,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.time)) },
                            placeholder = { Text(stringResource(R.string.hint_time)) },
                            modifier = Modifier.fillMaxWidth().testTag("dialog_appointment_time_input"),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showSchedTimePicker = true }) {
                                    Icon(Icons.Default.Schedule, contentDescription = stringResource(R.string.cd_pick_time))
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dialogAppointmentNotes,
                        onValueChange = { dialogAppointmentNotes = it },
                        label = { Text(stringResource(R.string.notes)) },
                        placeholder = { Text(stringResource(R.string.appointment_notes_placeholder)) },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_appointment_notes_input"),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updatePatientNextAppointment(
                        patientId = patient.id,
                        nextDate = dialogAppointmentDate.trim(),
                        nextTime = dialogAppointmentTime.trim(),
                        nextNotes = dialogAppointmentNotes.trim()
                    )
                    showScheduleAppointmentDialog = false
                }) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleAppointmentDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )

        if (showSchedDatePicker) {
            val dpState = rememberDatePickerState(
                initialSelectedDateMillis = try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dialogAppointmentDate)?.time
                } catch (_: Exception) { System.currentTimeMillis() }
            )
            DatePickerDialog(
                onDismissRequest = { showSchedDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        dpState.selectedDateMillis?.let { millis ->
                            dialogAppointmentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                        }
                        showSchedDatePicker = false
                    }) { Text(stringResource(R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showSchedDatePicker = false }) { Text(stringResource(R.string.cancel)) }
                }
            ) {
                DatePicker(state = dpState)
            }
        }

        if (showSchedTimePicker) {
            val initialHour = try {
                if (dialogAppointmentTime.isNotBlank()) dialogAppointmentTime.substringBefore(":").toInt() else 9
            } catch (_: Exception) { 9 }
            val initialMinute = try {
                if (dialogAppointmentTime.isNotBlank()) dialogAppointmentTime.substringAfter(":").toInt() else 0
            } catch (_: Exception) { 0 }
            val tpState = rememberTimePickerState(
                initialHour = initialHour.coerceIn(0, 23),
                initialMinute = initialMinute.coerceIn(0, 59),
                is24Hour = false
            )
            AlertDialog(
                onDismissRequest = { showSchedTimePicker = false },
                title = { Text(stringResource(R.string.select_time_title), fontWeight = FontWeight.Bold) },
                text = { TimePicker(state = tpState) },
                confirmButton = {
                    TextButton(onClick = {
                        val hour = tpState.hour
                        val minute = tpState.minute
                        val amPm = if (hour < 12) "AM" else "PM"
                        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                        dialogAppointmentTime = String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
                        showSchedTimePicker = false
                    }) { Text(stringResource(R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showSchedTimePicker = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }

    if (showDeletePatientConfirm) {
        AlertDialog(
            onDismissRequest = { showDeletePatientConfirm = false },
            title = { Text(stringResource(R.string.delete_patient_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(stringResource(R.string.delete_patient_dialog_body, patient.name))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePatient(patient)
                        showDeletePatientConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePatientConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun SummaryRow(label: String, value: Double, large: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (large) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (large) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (value == 0.0) "$0.00" else "$${String.format(Locale.US, "%.2f", value)}",
            style = if (large) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (large) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProcedureCardSection(
    cardDetail: ProcedureCardDetail,
    viewModel: DentistViewModel,
    onRequestPreview: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val stepsFlow = remember(cardDetail.id) { viewModel.getStepsForCard(cardDetail.id) }
    val cardSteps by stepsFlow.collectAsState(initial = emptyList())

    var newStepDesc by remember { mutableStateOf("") }
    var targetStepForPhoto by remember { mutableStateOf<ProcedureCardStep?>(null) }
    var showDeleteCardConfirm by remember { mutableStateOf(false) }
    var showAddPayment by remember { mutableStateOf(false) }
    var pendingAmount by remember { mutableStateOf("") }
    var pendingNotes by remember { mutableStateOf("") }
    var pendingDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var pendingDateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var showAddPaymentDatePicker by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<ProcedurePayment?>(null) }
    var showCancelConfirm by remember { mutableStateOf(false) }
    var showReopenConfirm by remember { mutableStateOf(false) }
    var showDeletePaymentConfirm by remember { mutableStateOf(false) }
    var finTreatmentFee by remember(cardDetail.id) { mutableStateOf(if (cardDetail.treatmentFee == 0.0) "" else cardDetail.treatmentFee.toString()) }
    var finLabFees by remember(cardDetail.id) { mutableStateOf(if (cardDetail.labFees == 0.0) "" else cardDetail.labFees.toString()) }
    var finPercentage by remember(cardDetail.id) { mutableStateOf(if (cardDetail.appliedPercentage == 0f) "" else cardDetail.appliedPercentage.toString()) }
    var finDeduct by remember(cardDetail.id) { mutableStateOf(cardDetail.deductLabFees) }

    LaunchedEffect(cardDetail.clinicId) {
        if (cardDetail.appliedPercentage == 0f) {
            val clinic = viewModel.getClinicByIdOnce(cardDetail.clinicId)
            if (clinic != null) {
                if (clinic.defaultPercentage != null) {
                    finPercentage = clinic.defaultPercentage.toString()
                }
                if (clinic.deductLabFeesDefault != null) {
                    finDeduct = clinic.deductLabFeesDefault
                }
            }
        }
    }

    val docPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { localUri: Uri? ->
        if (localUri != null) {
            val target = targetStepForPhoto
            if (target != null) {
                val currentUris = target.photoUris
                val newUris = if (currentUris.isBlank()) localUri.toString() else "$currentUris,${localUri}"
                viewModel.addCardStep(
                    cardId = cardDetail.id,
                    description = target.stepName,
                    photoUriList = newUris.split(",")
                )
                viewModel.deleteCardStep(target)
                targetStepForPhoto = null
            } else {
                viewModel.addCardStep(
                    cardId = cardDetail.id,
                    description = "Uploaded clinical photo on ${cardDetail.dateCreated}",
                    photoUriList = listOf(localUri.toString())
                )
            }
        }
    }

    val sortedSteps = remember(cardSteps) { cardSteps.sortedBy { it.id } }
    val totalPaid = cardDetail.amountPaid
    val totalFee = cardDetail.treatmentFee
    val isFullyPaid = totalFee > 0 && totalPaid >= totalFee

    // ── Debounced auto-save for financial settings ──
    LaunchedEffect(finTreatmentFee, finLabFees, finPercentage, finDeduct) {
        if (finTreatmentFee.isBlank() && finLabFees.isBlank() && finPercentage.isBlank() && !finDeduct) return@LaunchedEffect
        delay(800)
        viewModel.updateCardFinancialSettings(
            cardId = cardDetail.id,
            treatmentFee = finTreatmentFee.toDoubleOrNull() ?: 0.0,
            labFees = finLabFees.toDoubleOrNull() ?: 0.0,
            percentage = finPercentage.toFloatOrNull() ?: 0f,
            deductLabFees = finDeduct
        )
    }

    // ── Card (header + expanded sections inside) ──
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = cardDetail.dateCreated,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = localizedProcedureName(cardDetail.procedureName),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (cardDetail.typeName != null || cardDetail.material != null) {
                Text(
                    text = buildString {
                        if (cardDetail.typeName != null) append(cardDetail.typeName)
                        if (cardDetail.typeName != null && cardDetail.material != null) append(" • ")
                        if (cardDetail.material != null) append(cardDetail.material)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Text(
                text = cardDetail.clinicName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!cardDetail.toothNumber.isNullOrBlank()) {
                Text(
                    text = "${stringResource(R.string.tooth_label)}: #${cardDetail.toothNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Text(
                text = stringResource(R.string.paid_text, String.format(Locale.US, "%.2f", totalPaid), String.format(Locale.US, "%.2f", totalFee)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isFullyPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showDeleteCardConfirm = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_delete_card),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (cardDetail.status) {
                                "Completed" -> Color(0xFFE8F5E9)
                                "Canceled" -> MaterialTheme.colorScheme.errorContainer
                                else -> Color(0xFFFFF3E0)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (cardDetail.status) {
                            "Completed" -> stringResource(R.string.completed)
                            "Canceled" -> stringResource(R.string.canceled)
                            else -> stringResource(R.string.in_progress)
                        }.uppercase(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (cardDetail.status) {
                            "Completed" -> Color(0xFF2E7D32)
                            "Canceled" -> MaterialTheme.colorScheme.error
                            else -> Color(0xFFEF6C00)
                        }
                    )
                }
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) stringResource(R.string.cd_collapse_card) else stringResource(R.string.cd_expand_card),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            }

            // ── Expanded sections inside the card ──
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ── Remaining Steps ──
                Text(stringResource(R.string.remaining_steps), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                if (sortedSteps.isEmpty()) {
                    Text(stringResource(R.string.no_steps_recorded), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                } else {
                    sortedSteps.forEach { step ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = step.isCompleted,
                                onCheckedChange = { viewModel.toggleStepCompleted(step, !step.isCompleted) }
                            )
                            Text(
                                text = step.stepName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (step.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    targetStepForPhoto = step
                                    docPhotoLauncher.launch("image/*")
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = stringResource(R.string.cd_attach_photo), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newStepDesc,
                    onValueChange = { newStepDesc = it },
                    placeholder = { Text(stringResource(R.string.hint_custom_step)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (newStepDesc.isNotBlank()) {
                                    viewModel.addCardStep(cardId = cardDetail.id, description = newStepDesc.trim())
                                    newStepDesc = ""
                                }
                            },
                            enabled = newStepDesc.isNotBlank()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = stringResource(R.string.cd_post_step), tint = if (newStepDesc.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                        }
                    },
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))
                val allStepsCompleted = sortedSteps.isNotEmpty() && sortedSteps.all { it.isCompleted }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            sortedSteps.forEach { step ->
                                viewModel.toggleStepCompleted(step, !allStepsCompleted)
                            }
                        },
                        enabled = sortedSteps.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (allStepsCompleted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (allStepsCompleted) Icons.Default.Clear else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (allStepsCompleted) stringResource(R.string.mark_all_incomplete)
                            else stringResource(R.string.mark_all_complete),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = { docPhotoLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.add_photo))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ── Finance ──
                val payments by viewModel.getPaymentsForCard(cardDetail.id).collectAsState(initial = emptyList())

                Text(stringResource(R.string.finance), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = finTreatmentFee,
                    onValueChange = { finTreatmentFee = it },
                    label = { Text(stringResource(R.string.treatment_fee_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = finLabFees,
                    onValueChange = { finLabFees = it },
                    label = { Text(stringResource(R.string.lab_fees_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = finPercentage,
                    onValueChange = { finPercentage = it },
                    label = { Text(stringResource(R.string.associate_share_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = finDeduct, onCheckedChange = { finDeduct = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.deduct_lab_fees), style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.payments_section), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = {
                        pendingDateMillis = System.currentTimeMillis()
                        pendingDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        pendingAmount = ""
                        pendingNotes = ""
                        showAddPayment = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.add_payment), fontWeight = FontWeight.Bold)
                    }
                }

                if (payments.isEmpty()) {
                    Text(stringResource(R.string.no_payments_yet), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                } else {
                    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
                    val groupedPayments = remember(payments) {
                        payments.groupBy { payment ->
                            Instant.ofEpochMilli(payment.paymentTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        }.entries.sortedByDescending { (date, _) -> date }
                    }
                    groupedPayments.forEach { (ld, datePayments) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = dateFormat.format(Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant())),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                datePayments.forEach { payment ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("$${String.format(Locale.US, "%.2f", payment.amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            payment.notes?.let { note ->
                                                Text(text = note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        IconButton(onClick = { paymentToDelete = payment; showDeletePaymentConfirm = true }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete_payment), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error.copy(0.7f))
                                        }
                                    }
                                    if (payment != datePayments.last()) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                val effectiveFee = finTreatmentFee.toDoubleOrNull() ?: cardDetail.treatmentFee
                val effectiveLab = finLabFees.toDoubleOrNull() ?: cardDetail.labFees
                val pctValue = (finPercentage.toFloatOrNull() ?: cardDetail.appliedPercentage) / 100f
                val totalPaymentsSum = payments.sumOf { it.amount }
                val remainingBalance = effectiveFee - totalPaymentsSum
                val assocCut: Double
                val clinicShare: Double
                if (finDeduct) {
                    assocCut = if (totalPaymentsSum - effectiveLab > 0) (totalPaymentsSum - effectiveLab) * pctValue else 0.0
                    clinicShare = (totalPaymentsSum - effectiveLab) - assocCut
                } else {
                    assocCut = totalPaymentsSum * pctValue
                    clinicShare = (totalPaymentsSum - assocCut) - effectiveLab
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SummaryRow(stringResource(R.string.total_paid), totalPaymentsSum)
                        SummaryRow(stringResource(R.string.remaining_balance), remainingBalance)
                        if (totalPaymentsSum > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            SummaryRow(stringResource(R.string.associate_cut), assocCut, large = true)
                            SummaryRow(stringResource(R.string.clinic_share), clinicShare, large = true)
                        }
                    }
                }

                if (cardDetail.status == "Canceled") {
                    OutlinedButton(
                        onClick = { showReopenConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF388E3C)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF388E3C))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.reopen_treatment), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { showCancelConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.cancel_treatment), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.close), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // CANCEL CONFIRMATION DIALOG
    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text(stringResource(R.string.confirm_cancel_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.confirm_cancel_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProcedureCardStatus(cardDetail.id, "Canceled")
                        showCancelConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.cancel_treatment), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // REOPEN CONFIRMATION DIALOG
    if (showReopenConfirm) {
        AlertDialog(
            onDismissRequest = { showReopenConfirm = false },
            title = { Text(stringResource(R.string.confirm_reopen_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.confirm_reopen_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProcedureCardStatus(cardDetail.id, "In Progress")
                        showReopenConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) {
                    Text(stringResource(R.string.reopen_treatment), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReopenConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ADD PAYMENT DIALOG
    if (expanded && showAddPayment) {
        AlertDialog(
            onDismissRequest = { showAddPayment = false },
            title = { Text(stringResource(R.string.add_payment), fontWeight = FontWeight.Bold) },
            text = {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Box {
                            OutlinedTextField(
                                value = pendingDateStr,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.date)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { showAddPaymentDatePicker = true }) {
                                        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.cd_pick_date))
                                    }
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pendingAmount,
                            onValueChange = { pendingAmount = it },
                            label = { Text(stringResource(R.string.amount_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pendingNotes,
                            onValueChange = { pendingNotes = it },
                            label = { Text(stringResource(R.string.payment_note_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = pendingAmount.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            viewModel.addPayment(cardDetail.id, amount, pendingDateMillis, pendingNotes.takeIf { it.isNotBlank() })
                            pendingAmount = ""
                            pendingNotes = ""
                            showAddPayment = false
                        }
                    },
                    enabled = pendingAmount.toDoubleOrNull() != null && (pendingAmount.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Text(stringResource(R.string.add), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPayment = false }) { Text(stringResource(R.string.cancel)) }
            }
        )

        if (showAddPaymentDatePicker) {
            val dpState = rememberDatePickerState(
                initialSelectedDateMillis = try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(pendingDateStr)?.time
                } catch (_: Exception) { System.currentTimeMillis() }
            )
            DatePickerDialog(
                onDismissRequest = { showAddPaymentDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        dpState.selectedDateMillis?.let { millis ->
                            pendingDateMillis = millis
                            pendingDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                        }
                        showAddPaymentDatePicker = false
                    }) { Text(stringResource(R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showAddPaymentDatePicker = false }) { Text(stringResource(R.string.cancel)) }
                }
            ) {
                DatePicker(state = dpState)
            }
        }
    }

    // DELETE PAYMENT CONFIRMATION
    if (showDeletePaymentConfirm && paymentToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeletePaymentConfirm = false; paymentToDelete = null },
            title = { Text(stringResource(R.string.delete_payment_dialog_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_payment_dialog_body, String.format(Locale.US, "%.2f", paymentToDelete!!.amount))) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePayment(paymentToDelete!!)
                        showDeletePaymentConfirm = false
                        paymentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePaymentConfirm = false; paymentToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // DELETE CARD CONFIRMATION
    if (showDeleteCardConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteCardConfirm = false },
            title = { Text(stringResource(R.string.delete_card_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(stringResource(R.string.delete_card_dialog_body))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProcedureCard(
                            ProcedureCard(
                                id = cardDetail.id,
                                patientId = cardDetail.patientId,
                                clinicId = cardDetail.clinicId,
                                clinicalProcedureId = cardDetail.clinicalProcedureId,
                                procedureTypeId = cardDetail.procedureTypeId,
                                material = cardDetail.material,
                                status = cardDetail.status,
                                dateCreated = cardDetail.dateCreated,
                                toothNumber = cardDetail.toothNumber,
                                notes = cardDetail.notes
                            )
                        )
                        showDeleteCardConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCardConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}