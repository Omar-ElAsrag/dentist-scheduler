package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.*
import com.example.ui.DentistViewModel
import com.example.ui.components.AppCard
import com.example.ui.components.SectionHeader
import com.example.ui.localizedProcedureName
import com.example.ui.theme.themeSuccess
import com.example.ui.theme.themeSuccessContainer
import com.example.ui.theme.themeWarning
import com.example.ui.theme.themeWarningContainer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PatientsScreen(viewModel: DentistViewModel) {
    val patients by viewModel.patients.collectAsState()
    val procedureCards by viewModel.procedureCards.collectAsState()
    var searchToken by remember { mutableStateOf("") }
    var selectedPatientForPortal by remember { mutableStateOf<Patient?>(null) }
    var showAddPatientDialog by remember { mutableStateOf(false) }

    val clinics by viewModel.clinics.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterClinicId by remember { mutableStateOf<Int?>(null) }
    var filterStatus by remember { mutableStateOf<String?>(null) }
    var filterDateOption by remember { mutableStateOf<String?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var pickerYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var pickerMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }

    fun matchesDateFilter(createdDate: String, option: String): Boolean {
        if (createdDate.isBlank()) return false
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val currentYearMonth = sdf.format(cal.time).substring(0, 7)
        cal.add(Calendar.MONTH, -1)
        val prevYearMonth = sdf.format(cal.time).substring(0, 7)
        return when (option) {
            "this_month" -> createdDate.startsWith(currentYearMonth)
            "previous_month" -> createdDate.startsWith(prevYearMonth)
            else -> createdDate.startsWith(option)
        }
    }

    val dateOpt = filterDateOption
    val filteredList = patients.filter { p ->
        p.isInProgress &&
        p.name.contains(searchToken, ignoreCase = true) &&
        (filterClinicId == null || procedureCards.any { it.patientId == p.id && it.clinicId == filterClinicId }) &&
        (filterStatus == null || procedureCards.any { it.patientId == p.id && it.status == filterStatus }) &&
        (dateOpt == null || matchesDateFilter(p.createdDate, dateOpt))
    }

    val sortedPatients = filteredList.sortedWith(
        compareBy<Patient> { patient ->
            val hasInProgress = procedureCards.any { it.patientId == patient.id && it.status == "In Progress" }
            if (hasInProgress) 0 else 1
        }.thenByDescending { it.createdDate }
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPatientDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_patient_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_create_patient))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            SectionHeader(
                title = stringResource(R.string.patients_title),
                subtitle = stringResource(R.string.patients_subtitle),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
            )

            OutlinedTextField(
                value = searchToken,
                onValueChange = { searchToken = it },
                label = { Text(stringResource(R.string.search_patients)) },
                placeholder = { Text(stringResource(R.string.hint_search)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp),
                shape = MaterialTheme.shapes.medium,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.cd_filter),
                            tint = if (filterClinicId != null || filterStatus != null || filterDateOption != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = pluralStringResource(R.plurals.patients_count, filteredList.size, filteredList.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (filteredList.isEmpty()) {
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = stringResource(R.string.cd_empty),
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (patients.isEmpty()) stringResource(R.string.no_patients_title) else stringResource(R.string.no_matching_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (patients.isEmpty()) stringResource(R.string.no_patients_subtitle) else stringResource(R.string.no_matching_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedPatients) { patient ->
                        val patientStatus = remember(patient.id, procedureCards) {
                            val pCards = procedureCards.filter { it.patientId == patient.id }
                            if (pCards.isEmpty()) null
                            else if (pCards.any { it.status == "In Progress" }) "In Progress"
                            else if (pCards.any { it.status == "Canceled" }) "Canceled"
                            else "Completed"
                        }
                        PatientRowCard(
                            patient = patient,
                            status = patientStatus,
                            procedureCards = procedureCards.filter { it.patientId == patient.id },
                            onClick = { selectedPatientForPortal = patient }
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.filter_patients), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()
                Text(stringResource(R.string.associate_clinic_filter), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = filterClinicId == null,
                        onClick = { filterClinicId = null },
                        label = { Text(stringResource(R.string.all), style = MaterialTheme.typography.bodySmall) }
                    )
                    clinics.forEach { clinic ->
                        FilterChip(
                            selected = filterClinicId == clinic.id,
                            onClick = { filterClinicId = clinic.id },
                            label = { Text(clinic.name, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
                Text(stringResource(R.string.status_filter), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = filterStatus == null,
                        onClick = { filterStatus = null },
                        label = { Text(stringResource(R.string.all), style = MaterialTheme.typography.bodySmall) }
                    )
                    FilterChip(
                        selected = filterStatus == "In Progress",
                        onClick = { filterStatus = "In Progress" },
                        label = { Text(stringResource(R.string.in_progress), style = MaterialTheme.typography.bodySmall) }
                    )
                    FilterChip(
                        selected = filterStatus == "Completed",
                        onClick = { filterStatus = "Completed" },
                        label = { Text(stringResource(R.string.completed), style = MaterialTheme.typography.bodySmall) }
                    )
                    FilterChip(
                        selected = filterStatus == "Canceled",
                        onClick = { filterStatus = "Canceled" },
                        label = { Text(stringResource(R.string.canceled), style = MaterialTheme.typography.bodySmall) }
                    )
                }
                Text(stringResource(R.string.date_filter), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = filterDateOption == null,
                        onClick = { filterDateOption = null },
                        label = { Text(stringResource(R.string.all), style = MaterialTheme.typography.bodySmall) }
                    )
                    FilterChip(
                        selected = filterDateOption == "this_month",
                        onClick = { filterDateOption = "this_month" },
                        label = { Text(stringResource(R.string.this_month), style = MaterialTheme.typography.bodySmall) }
                    )
                    FilterChip(
                        selected = filterDateOption == "previous_month",
                        onClick = { filterDateOption = "previous_month" },
                        label = { Text(stringResource(R.string.previous_month), style = MaterialTheme.typography.bodySmall) }
                    )
                    FilterChip(
                        selected = filterDateOption != null && filterDateOption !in setOf("this_month", "previous_month"),
                        onClick = { showMonthPicker = true },
                        label = { Text(stringResource(R.string.custom_month), style = MaterialTheme.typography.bodySmall) }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        filterClinicId = null
                        filterStatus = null
                        filterDateOption = null
                        showFilterSheet = false
                    }) {
                        Text(stringResource(R.string.clear_all))
                    }
                }
            }
        }
    }

    if (showMonthPicker) {
        val monthNames = java.text.DateFormatSymbols.getInstance(Locale.getDefault()).months.take(12).toTypedArray()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear downTo currentYear - 5).toList()
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            title = { Text(stringResource(R.string.select_month_title), fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(monthNames.toList()) { month ->
                            val idx = monthNames.indexOf(month)
                            TextButton(
                                onClick = { pickerMonth = idx },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    month,
                                    fontWeight = if (idx == pickerMonth) FontWeight.Bold else FontWeight.Normal,
                                    color = if (idx == pickerMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(years) { year ->
                            TextButton(
                                onClick = { pickerYear = year },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    year.toString(),
                                    fontWeight = if (year == pickerYear) FontWeight.Bold else FontWeight.Normal,
                                    color = if (year == pickerYear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.YEAR, pickerYear)
                    cal.set(Calendar.MONTH, pickerMonth)
                    filterDateOption = sdf.format(cal.time)
                    showMonthPicker = false
                }) {
                    Text(stringResource(R.string.select), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMonthPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddPatientDialog) {
        PatientFormDialog(
            title = stringResource(R.string.new_patient_title),
            viewModel = viewModel,
            onDismiss = { showAddPatientDialog = false }
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
fun PatientRowCard(
    patient: Patient,
    status: String? = null,
    procedureCards: List<ProcedureCardDetail> = emptyList(),
    onClick: () -> Unit
) {
    val holdsAlert = patient.systemicConditions.isNotBlank() || patient.allergies.isNotBlank()

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring())
            .testTag("patient_card_${patient.id}"),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = patient.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patient.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (status != null) {
                    Surface(
                        color = when (status) {
                            "Completed" -> themeSuccessContainer()
                            "Canceled" -> MaterialTheme.colorScheme.errorContainer
                            else -> themeWarningContainer()
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = stringResource(
                                when (status) {
                                    "Completed" -> R.string.completed
                                    "Canceled" -> R.string.canceled
                                    else -> R.string.in_progress
                                }
                            ).uppercase(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (status) {
                                "Completed" -> themeSuccess()
                                "Canceled" -> MaterialTheme.colorScheme.error
                                else -> themeWarning()
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (holdsAlert) {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.medical_alert),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.systemic_condition_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (patient.systemicConditions.isNotBlank()) patient.systemicConditions else stringResource(R.string.none),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (patient.systemicConditions.isNotBlank()) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.allergies_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (patient.allergies.isNotBlank()) patient.allergies else "NKDA",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (patient.allergies.isNotBlank()) themeWarning() else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }

        if (patient.pastDentalTreatments.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.dental_history, patient.pastDentalTreatments),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        if (procedureCards.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.procedures_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            procedureCards.take(3).forEach { pc ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (pc.status == "Completed") themeSuccess() else themeWarning(),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = localizedProcedureName(pc.procedureName) + if (pc.typeName != null) " — ${pc.typeName}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (pc.status == "Completed") themeSuccessContainer() else themeWarningContainer()
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = pc.status.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (pc.status == "Completed") themeSuccess() else themeWarning()
                        )
                    }
                }
            }
            if (procedureCards.size > 3) {
                Text(
                    text = pluralStringResource(R.plurals.more_count, procedureCards.size - 3, procedureCards.size - 3),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientFormDialog(
    title: String,
    viewModel: DentistViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var showDatePicker by remember { mutableStateOf(false) }
    var systemic by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }

    // Procedure card fields
    var selectedClinicId by remember { mutableStateOf<Int?>(null) }
    var selectedProcedure by remember { mutableStateOf<ClinicalProcedure?>(null) }
    var selectedType by remember { mutableStateOf<ProcedureType?>(null) }
    var selectedMaterial by remember { mutableStateOf<String?>(null) }
    var toothNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val clinics by viewModel.clinics.collectAsState()
    val clinicalProcedures by viewModel.clinicalProcedures.collectAsState()

    var typesList by remember { mutableStateOf<List<ProcedureType>>(emptyList()) }
    var materialsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewSteps by remember { mutableStateOf<List<String>>(emptyList()) }
    data class CardStepItem(val id: Int, val name: String, val isFromTemplate: Boolean)
    var cardStepItems by remember { mutableStateOf<List<CardStepItem>>(emptyList()) }
    var insertAtIndex by remember { mutableStateOf<Int?>(null) }
    var insertText by remember { mutableStateOf("") }
    var nextCustomId by remember { mutableStateOf(0) }
    var newCardStepText by remember { mutableStateOf("") }
    var initialFee by remember { mutableStateOf("") }
    var initialLabFee by remember { mutableStateOf("") }

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
        val procDefaultFee = selectedProcedure?.defaultFee ?: 0.0
        val typeDefaultFee = selectedType?.defaultFee ?: 0.0
        initialFee = if (typeDefaultFee > 0) typeDefaultFee.toString() else if (procDefaultFee > 0) procDefaultFee.toString() else ""

        val procDefaultLabFee = selectedProcedure?.defaultLabFee ?: 0.0
        val typeDefaultLabFee = selectedType?.defaultLabFee ?: 0.0
        initialLabFee = if (typeDefaultLabFee > 0) typeDefaultLabFee.toString() else if (procDefaultLabFee > 0) procDefaultLabFee.toString() else ""

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
        cardStepItems = steps.mapIndexed { idx, name -> CardStepItem(id = idx, name = name, isFromTemplate = true) }
        insertAtIndex = null
        insertText = ""
        nextCustomId = steps.size
    }

    var clinicExpanded by remember { mutableStateOf(false) }
    var procedureExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var materialExpanded by remember { mutableStateOf(false) }

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
                                text = stringResource(R.string.new_patient_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.new_patient_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close_dialog))
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
                                modifier = Modifier.fillMaxWidth().testTag("patient_form_name_input"),
                                singleLine = true
                            )

                            Spacer(Modifier.height(16.dp))

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text(stringResource(R.string.phone_number)) },
                                placeholder = { Text(stringResource(R.string.hint_phone)) },
                                modifier = Modifier.fillMaxWidth().testTag("patient_form_phone_input"),
                                singleLine = true
                            )

                            Spacer(Modifier.height(16.dp))

                            Box {
                                OutlinedTextField(
                                    value = date,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.appointment_date)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("patient_form_date_input"),
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
                        }

                        if (showDatePicker) {
                            val datePickerState = rememberDatePickerState(
                                initialSelectedDateMillis = try {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)?.time
                                } catch (_: Exception) { System.currentTimeMillis() }
                            )
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        datePickerState.selectedDateMillis?.let { millis ->
                                            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
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
                                modifier = Modifier.fillMaxWidth().testTag("patient_form_systemic_input")
                            )

                            Spacer(Modifier.height(16.dp))

                            OutlinedTextField(
                                value = allergies,
                                onValueChange = { allergies = it },
                                label = { Text(stringResource(R.string.drug_allergies_field)) },
                                placeholder = { Text(stringResource(R.string.hint_allergies)) },
                                modifier = Modifier.fillMaxWidth().testTag("patient_form_allergies_input")
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // ── Initial Procedure (Optional) ──
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MedicalServices, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.initial_procedure),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(8.dp))

                            // Clinic Dropdown
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
                                        .testTag("patient_form_clinic_dropdown"),
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
                                            modifier = Modifier.testTag("clinic_item_${clinic.id}")
                                        )
                                    }
                                }
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
                                        .testTag("patient_form_procedure_dropdown"),
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
                                            modifier = Modifier.testTag("procedure_item_${cp.id}")
                                        )
                                    }
                                }
                            }

                            // Type dropdown (if applicable)
                            if (selectedProcedure?.hasTypes == true && typesList.isNotEmpty()) {
                                Spacer(Modifier.height(16.dp))
                                ExposedDropdownMenuBox(
                                    expanded = typeExpanded,
                                    onExpandedChange = { typeExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedType?.name ?: stringResource(R.string.dropdown_select_type),
                                        onValueChange = {},
                                        label = { Text(stringResource(R.string.type)) },
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .testTag("patient_form_type_dropdown"),
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
                                                modifier = Modifier.testTag("type_item_${type.id}")
                                            )
                                        }
                                    }
                                }
                            }

                            // Material dropdown (if applicable)
                            if (materialsList.isNotEmpty()) {
                                Spacer(Modifier.height(16.dp))
                                ExposedDropdownMenuBox(
                                    expanded = materialExpanded,
                                    onExpandedChange = { materialExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedMaterial ?: stringResource(R.string.dropdown_select_material),
                                        onValueChange = {},
                                        label = { Text(stringResource(R.string.material)) },
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .testTag("patient_form_material_dropdown"),
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = initialFee,
                                    onValueChange = { initialFee = it.filter { c -> c.isDigit() || c == '.' } },
                                    label = { Text(stringResource(R.string.default_fee)) },
                                    placeholder = { Text("0.00") },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                                OutlinedTextField(
                                    value = initialLabFee,
                                    onValueChange = { initialLabFee = it.filter { c -> c.isDigit() || c == '.' } },
                                    label = { Text(stringResource(R.string.default_lab_fee)) },
                                    placeholder = { Text("0.00") },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                            }

                            // Treatment Steps
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.treatment_steps),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.treatment_steps_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))

                            cardStepItems.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(20.dp)
                                    )
                                    if (item.isFromTemplate) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        OutlinedTextField(
                                            value = item.name,
                                            onValueChange = { newName ->
                                                cardStepItems = cardStepItems.toMutableList().also { it[index] = item.copy(name = newName) }
                                            },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                            placeholder = { Text(stringResource(R.string.hint_step)) }
                                        )
                                        IconButton(
                                            onClick = {
                                                cardStepItems = cardStepItems.toMutableList().also { it.removeAt(index) }
                                                insertAtIndex = null
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove_step), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    if (insertAtIndex == index) {
                                        IconButton(
                                            onClick = { insertAtIndex = null },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_cancel_step), modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                insertAtIndex = index
                                                insertText = ""
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_insert_step), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                if (insertAtIndex == index) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 20.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = insertText,
                                            onValueChange = { insertText = it },
                                            placeholder = { Text(stringResource(R.string.hint_new_step)) },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                if (insertText.isNotBlank()) {
                                                    val newItem = CardStepItem(id = nextCustomId, name = insertText.trim(), isFromTemplate = false)
                                                    nextCustomId++
                                                    val mutableList = cardStepItems.toMutableList()
                                                    mutableList.add(index, newItem)
                                                    cardStepItems = mutableList
                                                    insertAtIndex = null
                                                    insertText = ""
                                                }
                                            },
                                            enabled = insertText.isNotBlank(),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.cd_confirm_step), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newCardStepText,
                                    onValueChange = { newCardStepText = it },
                                    placeholder = { Text(stringResource(R.string.hint_new_step)) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        if (newCardStepText.isNotBlank()) {
                                            val newItem = CardStepItem(id = nextCustomId, name = newCardStepText.trim(), isFromTemplate = false)
                                            nextCustomId++
                                            cardStepItems = cardStepItems + newItem
                                            newCardStepText = ""
                                        }
                                    },
                                    enabled = newCardStepText.isNotBlank(),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_step), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            OutlinedTextField(
                                value = toothNumber,
                                onValueChange = { toothNumber = it },
                                label = { Text(stringResource(R.string.tooth_label)) },
                                placeholder = { Text(stringResource(R.string.hint_tooth)) },
                                modifier = Modifier.fillMaxWidth().testTag("patient_form_tooth_input"),
                                singleLine = true
                            )

                            Spacer(Modifier.height(16.dp))

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text(stringResource(R.string.procedure_notes_field)) },
                                placeholder = { Text(stringResource(R.string.hint_notes)) },
                                modifier = Modifier.fillMaxWidth().testTag("patient_form_notes_input"),
                                maxLines = 4
                            )
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
                                if (name.isNotBlank()) {
                                    viewModel.insertPatient(
                                        name = name.trim(),
                                        phoneNumber = phoneNumber.trim(),
                                        systemicConditions = systemic.trim(),
                                        allergies = allergies.trim(),
                                        notes = notes.trim()
                                    ) { patientId ->
                                        if (selectedClinicId != null && selectedProcedure != null) {
                                            viewModel.createProcedureCard(
                                                patientId = patientId,
                                                clinicId = selectedClinicId!!,
                                                clinicalProcedureId = selectedProcedure!!.id,
                                                procedureTypeId = selectedType?.id,
                                                material = selectedMaterial,
                                                dateCreated = date.trim(),
                                                toothNumber = toothNumber.trim(),
                                                notes = notes.trim(),
                                                selectedStepNames = null,
                                                additionalStepNames = cardStepItems.filter { !it.isFromTemplate }.map { it.name },
                                                treatmentFee = initialFee.toDoubleOrNull() ?: 0.0,
                                                labFees = initialLabFee.toDoubleOrNull() ?: 0.0
                                            )
                                        }
                                    }
                                    onDismiss()
                                }
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("patient_form_submit_button"),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(stringResource(R.string.register_patient), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
