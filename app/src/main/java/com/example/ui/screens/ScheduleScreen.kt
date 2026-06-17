package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.*
import com.example.ui.DentistViewModel
import com.example.ui.components.AppCard
import com.example.ui.components.RoleGate
import com.example.ui.localizedProcedureName
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: DentistViewModel) {
    val clinics by viewModel.clinics.collectAsState()
    val patients by viewModel.patients.collectAsState()
    val procedureCards by viewModel.procedureCards.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPatientForPortal by remember { mutableStateOf<Patient?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }
    var showExistingPatientSearch by remember { mutableStateOf(false) }
    var existingPatientForSchedule by remember { mutableStateOf<Patient?>(null) }

    var filterClinicId by remember { mutableStateOf<Int?>(null) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale(Locale.US) }
    var selectedDate by remember { mutableStateOf(LocalDate.now().format(dateFormatter)) }

    fun formatDayOfWeek(dateStr: String): String {
        return LocalDate.parse(dateStr).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }

    val todayStr = remember { LocalDate.now().format(dateFormatter) }

    val scheduledPatients = remember(patients) {
        patients.filter { it.nextAppointmentDate.isNotBlank() }
            .sortedWith(compareBy<Patient> { it.nextAppointmentDate }.thenBy { it.nextAppointmentTime })
    }

    val filteredPatients = remember(scheduledPatients, procedureCards, filterClinicId, selectedDate) {
        scheduledPatients.filter { p ->
            p.nextAppointmentDate == selectedDate &&
            (filterClinicId == null || procedureCards.any { it.patientId == p.id && it.clinicId == filterClinicId })
        }
    }

    Scaffold(
        floatingActionButton = {
            RoleGate(allowedRoles = setOf("admin", "receptionist")) {
                Box {
                    Column(horizontalAlignment = Alignment.End) {
                        AnimatedVisibility(visible = fabExpanded, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FloatingActionButton(
                                    onClick = {
                                        fabExpanded = false
                                        showExistingPatientSearch = true
                                    },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = stringResource(R.string.existing_patient), modifier = Modifier.size(18.dp))
                                }
                                FloatingActionButton(
                                    onClick = {
                                        fabExpanded = false
                                        showAddDialog = true
                                    },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.new_patient_title), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        FloatingActionButton(
                            onClick = { fabExpanded = !fabExpanded },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("add_patient_schedule_fab")
                        ) {
                            Icon(
                                imageVector = if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = stringResource(R.string.cd_schedule_appointment_fab)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = stringResource(R.string.schedule_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.schedule_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.quick_filters),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.clickable {
                            val clinicIds = clinics.map { it.id }
                            val currentIndex = clinicIds.indexOf(filterClinicId)
                            filterClinicId = if (currentIndex == -1) clinicIds.firstOrNull()
                            else if (currentIndex == clinicIds.lastIndex) null
                            else clinicIds[currentIndex + 1]
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (filterClinicId != null) Color(0xFFE8EAF6) else MaterialTheme.colorScheme.surface,
                            contentColor = if (filterClinicId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (filterClinicId != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.HomeWork, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (filterClinicId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (filterClinicId != null) {
                                    clinics.find { it.id == filterClinicId }?.name ?: stringResource(R.string.select_associate_clinic)
                                } else "All Associates / Clinics",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Month header + Today
            val monthText = remember(selectedDate) {
                try {
                    val d = LocalDate.parse(selectedDate)
                    "${d.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${d.year}"
                } catch (_: Exception) { "" }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(40.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (selectedDate != todayStr) {
                        TextButton(onClick = { selectedDate = todayStr }) {
                            Text(stringResource(R.string.today), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Compact Week View
            val selectedLocalDate = remember(selectedDate) {
                LocalDate.parse(selectedDate)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { selectedDate = selectedLocalDate.minusDays(1).format(dateFormatter) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.previous_day),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (-2..2).forEach { offset ->
                        val date = selectedLocalDate.plusDays(offset.toLong())
                        val dateStr = date.format(dateFormatter)
                        AnimatedContent(
                            targetState = dateStr,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(200)) togetherWith
                                fadeOut(animationSpec = tween(200))
                            },
                            label = "dayChip_$offset"
                        ) { currentDateStr ->
                            val d = LocalDate.parse(currentDateStr)
                            DayChip(
                                dayOfWeek = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                dayNumber = d.dayOfMonth.toString(),
                                isSelected = offset == 0,
                                onClick = { selectedDate = currentDateStr }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { selectedDate = selectedLocalDate.plusDays(1).format(dateFormatter) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.next_day),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            val formattedSelectedDate = remember(selectedDate) {
                try {
                    val d = LocalDate.parse(selectedDate)
                    "${d.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${d.dayOfMonth}"
                } catch (_: Exception) { selectedDate }
            }
            Text(
                text = pluralStringResource(R.plurals.appointments_count, filteredPatients.size, filteredPatients.size, formattedSelectedDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (filteredPatients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = stringResource(R.string.cd_empty),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_appointments_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.no_appointments_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPatients) { patient ->
                        val patientCards = procedureCards.filter { it.patientId == patient.id }
                        val patientStatus = remember(patient.id, procedureCards) {
                            val pCards = procedureCards.filter { it.patientId == patient.id }
                            if (pCards.isEmpty()) null
                            else if (pCards.any { it.status == "In Progress" }) "In Progress"
                            else if (pCards.any { it.status == "Canceled" }) "Canceled"
                            else "Completed"
                        }
                        ScheduledPatientItem(
                            patient = patient,
                            latestCards = patientCards,
                            status = patientStatus,
                            onClick = { selectedPatientForPortal = patient }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPatientFromScheduleDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }

    if (selectedPatientForPortal != null) {
        PatientPortalDialog(
            patient = selectedPatientForPortal!!,
            viewModel = viewModel,
            onDismiss = { selectedPatientForPortal = null }
        )
    }
}

@Composable
private fun DayChip(
    dayOfWeek: String,
    dayNumber: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(60.dp)
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 1.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayOfWeek,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dayNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
fun ScheduledPatientItem(
    patient: Patient,
    latestCards: List<ProcedureCardDetail>,
    status: String? = null,
    onClick: () -> Unit
) {
    val latestCard = latestCards.maxByOrNull { it.dateCreated }
    val clinicDisplay = latestCard?.clinicName ?: stringResource(R.string.scheduled_visit)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("scheduled_patient_card_${patient.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HomeWork,
                        contentDescription = stringResource(R.string.cd_clinic_icon),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = clinicDisplay,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = stringResource(R.string.cd_date_icon),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (patient.nextAppointmentTime.isNotBlank()) "${patient.nextAppointmentDate} — ${patient.nextAppointmentTime}" else patient.nextAppointmentDate,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = patient.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (status != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    when (status) {
                                        "Completed" -> Color(0xFF2E7D32)
                                        "Canceled" -> MaterialTheme.colorScheme.error
                                        else -> Color(0xFFEF6C00)
                                    },
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                when (status) {
                                    "Completed" -> R.string.completed
                                    "Canceled" -> R.string.canceled
                                    else -> R.string.in_progress
                                }
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (status) {
                                "Completed" -> Color(0xFF2E7D32)
                                "Canceled" -> MaterialTheme.colorScheme.error
                                else -> Color(0xFFEF6C00)
                            }
                        )
                    }
                }
                if (patient.phoneNumber.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = stringResource(R.string.cd_phone_icon),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = patient.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (patient.nextAppointmentNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                        contentDescription = stringResource(R.string.cd_notes_icon),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = patient.nextAppointmentNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientFromScheduleDialog(
    viewModel: DentistViewModel,
    onDismiss: () -> Unit
) {
    val clinics by viewModel.clinics.collectAsState()
    val clinicalProcedures by viewModel.clinicalProcedures.collectAsState()

    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale(Locale.US) }
    var dateString by remember { mutableStateOf(LocalDate.now().format(dateFormatter)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var timeString by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    var addOtherDetails by remember { mutableStateOf(false) }

    var selectedClinicId by remember { mutableStateOf<Int?>(null) }
    var selectedProcedure by remember { mutableStateOf<ClinicalProcedure?>(null) }
    var selectedType by remember { mutableStateOf<ProcedureType?>(null) }
    var selectedMaterial by remember { mutableStateOf<String?>(null) }
    var toothNumber by remember { mutableStateOf("") }
    var systemic by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }

    var typesList by remember { mutableStateOf<List<ProcedureType>>(emptyList()) }
    var materialsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewSteps by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedStepNames by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(selectedProcedure) {
        if (selectedProcedure != null) {
            typesList = viewModel.getTypesForProcedureOnce(selectedProcedure!!.id)
            selectedType = null
            selectedMaterial = null
            materialsList = emptyList()
        } else {
            typesList = emptyList()
        }
    }

    LaunchedEffect(selectedType) {
        materialsList = if (selectedType != null && !selectedType!!.materials.isNullOrBlank()) {
            selectedType!!.materials!!.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            emptyList()
        }
        selectedMaterial = null
    }

    LaunchedEffect(selectedProcedure, selectedType) {
        val steps = if (selectedType != null) {
            val typeSteps = viewModel.getStepsForType(selectedType!!.id)
            if (typeSteps.isNotEmpty()) typeSteps.map { it.stepName }
            else viewModel.getStepsForProcedure(selectedProcedure?.id ?: 0).map { it.stepName }
        } else if (selectedProcedure != null) {
            viewModel.getStepsForProcedure(selectedProcedure!!.id).map { it.stepName }
        } else {
            emptyList()
        }
        previewSteps = steps
        selectedStepNames = steps.toSet()
    }

    var clinicExpanded by remember { mutableStateOf(false) }
    var procedureExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var materialExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 700.dp)
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 8.dp, top = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.schedule_appointment),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.schedule_appointment_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))

                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                    ) {
                        // ── Patient Information ──
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.patient_information),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text(stringResource(R.string.patient_full_name)) },
                                placeholder = { Text(stringResource(R.string.hint_patient_name)) },
                                modifier = Modifier.fillMaxWidth().testTag("schedule_form_name_input"),
                                singleLine = true
                            )

                            Spacer(Modifier.height(16.dp))

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text(stringResource(R.string.phone_number)) },
                                placeholder = { Text(stringResource(R.string.hint_phone)) },
                                modifier = Modifier.fillMaxWidth().testTag("schedule_form_phone_input"),
                                singleLine = true
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // ── Appointment Details ──
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.appointment_details_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(8.dp))

                            Box {
                                OutlinedTextField(
                                    value = dateString,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.appointment_date)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("schedule_form_date_input"),
                                    singleLine = true,
                                    trailingIcon = {
                                        Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.cd_pick_date))
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { showDatePicker = true }
                                )
                            }

                            if (showDatePicker) {
                                val datePickerState = rememberDatePickerState(
                                    initialSelectedDateMillis = try {
                                        LocalDate.parse(dateString).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    } catch (_: Exception) { System.currentTimeMillis() }
                                )
                                DatePickerDialog(
                                    onDismissRequest = { showDatePicker = false },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            datePickerState.selectedDateMillis?.let { millis ->
                                                dateString = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
                                            }
                                            showDatePicker = false
                                        }) { Text(stringResource(R.string.ok)) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
                                    }
                                ) {
                                    DatePicker(state = datePickerState)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Box {
                                OutlinedTextField(
                                    value = if (timeString.isNotBlank()) timeString else stringResource(R.string.no_time_set),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.appointment_time)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("schedule_form_time_input"),
                                    singleLine = true,
                                    trailingIcon = {
                                        Icon(Icons.Filled.Schedule, contentDescription = stringResource(R.string.cd_pick_time))
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { showTimePicker = true }
                                )
                            }

                            if (showTimePicker) {
                                val initialHour = try {
                                    if (timeString.isNotBlank()) timeString.substringBefore(":").toInt() else 9
                                } catch (_: Exception) { 9 }
                                val initialMinute = try {
                                    if (timeString.isNotBlank()) timeString.substringAfter(":").toInt() else 0
                                } catch (_: Exception) { 0 }
                                val timePickerState = rememberTimePickerState(
                                    initialHour = initialHour.coerceIn(0, 23),
                                    initialMinute = initialMinute.coerceIn(0, 59),
                                    is24Hour = false
                                )
                                AlertDialog(
                                    onDismissRequest = { showTimePicker = false },
                                    title = { Text(stringResource(R.string.select_time_title), fontWeight = FontWeight.Bold) },
                                    text = { TimePicker(state = timePickerState) },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            timeString = String.format(Locale.US, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                                            showTimePicker = false
                                        }) { Text(stringResource(R.string.ok)) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
                                    }
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text(stringResource(R.string.appointment_notes)) },
                                placeholder = { Text(stringResource(R.string.hint_appointment_notes)) },
                                modifier = Modifier.fillMaxWidth().testTag("schedule_form_notes_input"),
                                maxLines = 4
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // ── Associate / Clinic ──
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HomeWork, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.select_associate_clinic),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(8.dp))

                            val selectedClinicName = clinics.find { it.id == selectedClinicId }?.name ?: stringResource(R.string.dropdown_select_clinic)
                            ExposedDropdownMenuBox(
                                expanded = clinicExpanded,
                                onExpandedChange = { clinicExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedClinicName,
                                    onValueChange = {},
                                    label = { Text(stringResource(R.string.select_associate_clinic)) },
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .testTag("schedule_form_clinic_dropdown"),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = clinicExpanded)
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = clinicExpanded,
                                    onDismissRequest = { clinicExpanded = false }
                                ) {
                                    clinics.forEach { clinic ->
                                        DropdownMenuItem(
                                            text = { Text(clinic.name) },
                                            onClick = {
                                                selectedClinicId = clinic.id
                                                clinicExpanded = false
                                            },
                                            modifier = Modifier.testTag("schedule_clinic_item_${clinic.id}")
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // ── Additional Details ──
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.additional_details),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { addOtherDetails = !addOtherDetails }
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Checkbox(
                                    checked = addOtherDetails,
                                    onCheckedChange = { addOtherDetails = it },
                                    modifier = Modifier.testTag("schedule_form_checkbox")
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.add_other_details_now),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.add_clinical_procedure),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        if (addOtherDetails) {
                            Spacer(Modifier.height(24.dp))

                            // ── Medical Alerts ──
                            AppCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MedicalInformation, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.medical_alerts),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = systemic,
                                    onValueChange = { systemic = it },
                                    label = { Text(stringResource(R.string.systemic_conditions_field)) },
                                    placeholder = { Text(stringResource(R.string.hint_systemic)) },
                                    modifier = Modifier.fillMaxWidth().testTag("schedule_form_systemic_input")
                                )

                                Spacer(Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = allergies,
                                    onValueChange = { allergies = it },
                                    label = { Text(stringResource(R.string.drug_allergies_field)) },
                                    placeholder = { Text(stringResource(R.string.hint_allergies)) },
                                    modifier = Modifier.fillMaxWidth().testTag("schedule_form_allergies_input")
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            // Procedure Dropdown
                            ExposedDropdownMenuBox(
                                expanded = procedureExpanded,
                                onExpandedChange = { procedureExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = localizedProcedureName(selectedProcedure?.name ?: stringResource(R.string.select_procedure)),
                                    onValueChange = {},
                                    label = { Text(stringResource(R.string.clinical_procedure_field)) },
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .testTag("schedule_form_procedure_dropdown"),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = procedureExpanded)
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = procedureExpanded,
                                    onDismissRequest = { procedureExpanded = false }
                                ) {
                                    clinicalProcedures.forEach { cp ->
                                        DropdownMenuItem(
                                            text = { Text(localizedProcedureName(cp.name)) },
                                            onClick = {
                                                selectedProcedure = cp
                                                procedureExpanded = false
                                            },
                                            modifier = Modifier.testTag("schedule_procedure_item_${cp.id}")
                                        )
                                    }
                                }
                            }

                            // Type (if applicable)
                            if (selectedProcedure?.hasTypes == true && typesList.isNotEmpty()) {
                                Spacer(Modifier.height(16.dp))
                                ExposedDropdownMenuBox(
                                    expanded = typeExpanded,
                                    onExpandedChange = { typeExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedType?.name ?: stringResource(R.string.select_type),
                                        onValueChange = {},
                                        label = { Text(stringResource(R.string.type)) },
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .testTag("schedule_form_type_dropdown"),
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                                        }
                                    )
                                    ExposedDropdownMenu(
                                        expanded = typeExpanded,
                                        onDismissRequest = { typeExpanded = false }
                                    ) {
                                        typesList.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type.name) },
                                                onClick = {
                                                    selectedType = type
                                                    typeExpanded = false
                                                },
                                                modifier = Modifier.testTag("schedule_type_item_${type.id}")
                                            )
                                        }
                                    }
                                }
                            }

                            // Material (if applicable)
                            if (materialsList.isNotEmpty()) {
                                Spacer(Modifier.height(16.dp))
                                ExposedDropdownMenuBox(
                                    expanded = materialExpanded,
                                    onExpandedChange = { materialExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedMaterial ?: stringResource(R.string.select_material),
                                        onValueChange = {},
                                        label = { Text(stringResource(R.string.material)) },
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .testTag("schedule_form_material_dropdown"),
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = materialExpanded)
                                        }
                                    )
                                    ExposedDropdownMenu(
                                        expanded = materialExpanded,
                                        onDismissRequest = { materialExpanded = false }
                                    ) {
                                        materialsList.forEach { mat ->
                                            DropdownMenuItem(
                                                text = { Text(mat) },
                                                onClick = {
                                                    selectedMaterial = mat
                                                    materialExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            OutlinedTextField(
                                value = toothNumber,
                                onValueChange = { toothNumber = it },
                                label = { Text(stringResource(R.string.tooth_teeth_number)) },
                                placeholder = { Text(stringResource(R.string.hint_tooth)) },
                                modifier = Modifier.fillMaxWidth().testTag("schedule_form_tooth_input"),
                                singleLine = true
                            )

                            // Steps selection
                            if (previewSteps.isNotEmpty()) {
                                Spacer(Modifier.height(16.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = stringResource(R.string.select_steps_to_include),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        previewSteps.forEach { step ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedStepNames = if (step in selectedStepNames) {
                                                            selectedStepNames - step
                                                        } else {
                                                            selectedStepNames + step
                                                        }
                                                    }
                                                    .padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = step in selectedStepNames,
                                                    onCheckedChange = { checked ->
                                                        selectedStepNames = if (checked) {
                                                            selectedStepNames + step
                                                        } else {
                                                            selectedStepNames - step
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = step,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                    }

                    // Footer pinned to bottom
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                        Spacer(Modifier.width(16.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank() && dateString.isNotBlank()) {
                                    scope.launch {
                                        viewModel.scheduleAppointment(
                                            name = name.trim(),
                                            phoneNumber = phoneNumber.trim(),
                                            dateString = dateString.trim(),
                                            timeString = timeString.trim(),
                                            notes = notes.trim(),
                                            addOtherDetails = addOtherDetails,
                                            selectedClinicId = selectedClinicId,
                                            selectedClinicalProcedureId = selectedProcedure?.id,
                                            selectedProcedureTypeId = selectedType?.id,
                                            selectedMaterial = selectedMaterial,
                                            toothNumber = toothNumber.trim(),
                                            systemic = systemic.trim(),
                                            allergies = allergies.trim(),
                                            selectedStepNames = selectedStepNames,
                                            onComplete = { onDismiss() }
                                        )
                                    }
                                }
                            },
                            enabled = name.isNotBlank() && dateString.isNotBlank() && (!addOtherDetails || (selectedClinicId != null && selectedProcedure != null)),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("schedule_form_submit_button"),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(stringResource(R.string.schedule_appointment_btn), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectExistingPatientDialog(
    patients: List<Patient>,
    onPatientSelected: (Patient) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredPatients = remember(patients, searchQuery) {
        if (searchQuery.isBlank()) patients.filter { it.name.isNotBlank() }
        else patients.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_existing_patient), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.search_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                Spacer(Modifier.height(12.dp))
                if (filteredPatients.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_matching_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(filteredPatients) { patient ->
                            ListItem(
                                headlineContent = { Text(patient.name, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.clickable { onPatientSelected(patient) },
                                leadingContent = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                },
                                trailingContent = {
                                    Text(
                                        text = patient.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}