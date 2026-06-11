package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.Clinic
import androidx.compose.ui.res.stringResource
import com.example.ui.DentistViewModel
import com.example.ui.components.RoleGate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicsScreen(viewModel: DentistViewModel) {
    val clinics by viewModel.clinics.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newClinicName by remember { mutableStateOf("") }
    var newClinicPercentage by remember { mutableStateOf("") }
    var newClinicDeduct by remember { mutableStateOf(false) }

    var editClinic by remember { mutableStateOf<Clinic?>(null) }
    var editClinicName by remember { mutableStateOf("") }
    var editClinicPercentage by remember { mutableStateOf("") }
    var editClinicDeduct by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            RoleGate(allowedRoles = setOf("admin")) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_clinic_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_associate))
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
                    text = stringResource(R.string.associates_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.associates_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (clinics.isEmpty()) {
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
                            imageVector = Icons.Default.HomeWork,
                            contentDescription = stringResource(R.string.cd_no_entries),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_associates_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.no_associates_subtitle),
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
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(clinics) { clinic ->
                        ClinicItemRow(
                            clinic = clinic,
                            onEdit = {
                                editClinic = clinic
                                editClinicName = clinic.name
                                editClinicPercentage = clinic.defaultPercentage?.toString() ?: ""
                                editClinicDeduct = clinic.deductLabFeesDefault ?: false
                            },
                            onDelete = { viewModel.deleteClinic(clinic) }
                        )
                    }
                }
            }
        }
    }

    // ADD CLINIC DIALOG
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.add_associate_clinic),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showAddDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close_dialog))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newClinicName,
                        onValueChange = { newClinicName = it },
                        label = { Text(stringResource(R.string.associate_clinic_name)) },
                        placeholder = { Text(stringResource(R.string.hint_clinic_name_input)) },
                        modifier = Modifier.fillMaxWidth().testTag("clinic_name_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newClinicPercentage,
                        onValueChange = { newClinicPercentage = it },
                        label = { Text(stringResource(R.string.default_share_percentage)) },
                        placeholder = { Text(stringResource(R.string.hint_percentage_input)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.deduct_lab_fees_first),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = newClinicDeduct,
                            onCheckedChange = { newClinicDeduct = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text(stringResource(R.string.reject))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (newClinicName.isNotBlank()) {
                                    viewModel.insertClinic(
                                        name = newClinicName,
                                        defaultPercentage = newClinicPercentage.toFloatOrNull(),
                                        deductLabFeesDefault = if (newClinicPercentage.isNotBlank()) newClinicDeduct else null
                                    )
                                    newClinicName = ""
                                    newClinicPercentage = ""
                                    newClinicDeduct = false
                                    showAddDialog = false
                                }
                            },
                            enabled = newClinicName.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("clinic_form_submit")
                        ) {
                            Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // EDIT CLINIC DIALOG
    editClinic?.let { clinic ->
        Dialog(onDismissRequest = { editClinic = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.edit_associate_clinic),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { editClinic = null }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close_dialog))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editClinicName,
                        onValueChange = { editClinicName = it },
                        label = { Text(stringResource(R.string.associate_clinic_name)) },
                        placeholder = { Text(stringResource(R.string.hint_clinic_name_input)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editClinicPercentage,
                        onValueChange = { editClinicPercentage = it },
                        label = { Text(stringResource(R.string.default_share_percentage)) },
                        placeholder = { Text(stringResource(R.string.hint_percentage_input)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.deduct_lab_fees_first),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = editClinicDeduct,
                            onCheckedChange = { editClinicDeduct = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editClinic = null }) {
                            Text(stringResource(R.string.reject))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (editClinicName.isNotBlank()) {
                                    viewModel.updateClinic(
                                        clinic.copy(
                                            name = editClinicName.trim(),
                                            defaultPercentage = editClinicPercentage.toFloatOrNull(),
                                            deductLabFeesDefault = if (editClinicPercentage.isNotBlank()) editClinicDeduct else null
                                        )
                                    )
                                    editClinic = null
                                }
                            },
                            enabled = editClinicName.isNotBlank(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClinicItemRow(
    clinic: Clinic,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("clinic_card_${clinic.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HomeWork,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = clinic.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            clinic.defaultPercentage != null && clinic.deductLabFeesDefault != null ->
                                "${clinic.defaultPercentage}% · ${if (clinic.deductLabFeesDefault) "Deduct lab fees first" else "Gross share"}"
                            clinic.defaultPercentage != null ->
                                "${clinic.defaultPercentage}% · Manual fee handling"
                            else -> "Manual configuration"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            Row {
                RoleGate(allowedRoles = setOf("admin"), fallbackContent = {
                    Text(
                        text = "Admin access required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.cd_edit_template),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_delete_item),
                            tint = MaterialTheme.colorScheme.error.copy(0.8f)
                        )
                    }
                }
            }
        }
    }
}
