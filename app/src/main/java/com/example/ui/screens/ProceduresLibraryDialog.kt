package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ClinicalProcedure
import com.example.data.ProcedureType
import com.example.data.ClinicalProcedureStep
import kotlinx.coroutines.launch
import com.example.R
import androidx.compose.ui.res.stringResource
import com.example.ui.DentistViewModel
import com.example.ui.localizedProcedureName
import com.example.ui.components.RoleGate

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProceduresLibraryDialog(
    viewModel: DentistViewModel,
    onDismiss: () -> Unit
) {
    val clinicalProcedures by viewModel.clinicalProcedures.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    var showCreatorForm by remember { mutableStateOf(false) }
    var editingProcedure by remember { mutableStateOf<ClinicalProcedure?>(null) }

    var nameInput by remember { mutableStateOf("") }
    var hasTypesInput by remember { mutableStateOf(false) }
    var typesInput by remember { mutableStateOf("") }
    var stepsInput by remember { mutableStateOf("") }
    var typeStepsInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf("") }

    data class MaterialFeeDetail(val material: String, val fee: String, val labFee: String)
data class TypeFormItem(val id: Int, val name: String, val materials: String, val steps: List<String>, val fee: String = "", val labFee: String = "", val materialFees: List<MaterialFeeDetail> = emptyList())
    var typeItems by remember { mutableStateOf<List<TypeFormItem>>(emptyList()) }
    var sharedStepItems by remember { mutableStateOf<List<String>>(emptyList()) }
    var nextTypeId by remember { mutableStateOf(0) }
    var nextTypeStepId by remember { mutableStateOf(0) }
    var newSharedStepText by remember { mutableStateOf("") }
var newTypeName by remember { mutableStateOf("") }
var newTypeMaterials by remember { mutableStateOf("") }
var newTypeStepsText by remember { mutableStateOf("") }
var defaultFeeInput by remember { mutableStateOf("") }
var defaultLabFeeInput by remember { mutableStateOf("") }
var defaultTypeFee by remember { mutableStateOf("") }
var defaultTypeLabFee by remember { mutableStateOf("") }
var materialFeeInputs by remember { mutableStateOf<Map<String, MaterialFeeDetail>>(emptyMap()) }

    var expandedProcId by remember { mutableStateOf<Int?>(null) }
    var expandedTypes by remember { mutableStateOf<Map<Int, List<ProcedureType>>>(emptyMap()) }
    var expandedSteps by remember { mutableStateOf<Map<Int, List<ClinicalProcedureStep>>>(emptyMap()) }
    var expandedTypeSteps by remember { mutableStateOf<Map<Int, List<ClinicalProcedureStep>>>(emptyMap()) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(expandedProcId) {
        val pid = expandedProcId ?: return@LaunchedEffect
        if (!expandedTypes.containsKey(pid)) {
            val cp = clinicalProcedures.find { it.id == pid } ?: return@LaunchedEffect
            if (cp.hasTypes) {
                val types = viewModel.getTypesForProcedureOnce(pid)
                expandedTypes = expandedTypes + (pid to types)
                types.forEach { type ->
                    val typeSteps = viewModel.getStepsForType(type.id)
                    if (typeSteps.isNotEmpty()) {
                        expandedTypeSteps = expandedTypeSteps + (type.id to typeSteps)
                    }
                }
            }
            val steps = viewModel.getStepsForProcedure(pid)
            expandedSteps = expandedSteps + (pid to steps)
        }
    }

    val categories = remember(clinicalProcedures) {
        clinicalProcedures.map { it.name }.distinct().sorted()
    }

    val filteredProcedures = clinicalProcedures.filter { proc ->
        (selectedCategoryFilter == null || proc.name == selectedCategoryFilter) &&
                (searchQuery.isBlank() || proc.name.contains(searchQuery, ignoreCase = true))
    }

fun resetForm() {
    nameInput = ""
    hasTypesInput = false
    typesInput = ""
    stepsInput = ""
    typeStepsInput = ""
    validationError = ""
    editingProcedure = null
    typeItems = emptyList()
    sharedStepItems = emptyList()
    nextTypeId = 0
    nextTypeStepId = 0
    newSharedStepText = ""
    newTypeName = ""
    newTypeMaterials = ""
    newTypeStepsText = ""
    defaultFeeInput = ""
    defaultLabFeeInput = ""
    defaultTypeFee = ""
    defaultTypeLabFee = ""
    materialFeeInputs = emptyMap()
}

    fun saveProcedure() {
        if (nameInput.isBlank()) {
            validationError = "Procedure name cannot be blank."
            return
        }
        val procDefaultFee = defaultFeeInput.toDoubleOrNull()
        val procDefaultLabFee = defaultLabFeeInput.toDoubleOrNull()
        val serializedTypes = typeItems.joinToString("\n") { t ->
            val matFeesPart = t.materialFees.joinToString(",") { m ->
                "${m.material}:${m.fee}:${m.labFee}"
            }
            "${t.name}|${t.materials}|${t.fee}|${t.labFee}|$matFeesPart"
        }
        val serializedTypeSteps = typeItems.mapNotNull { t ->
            if (t.steps.isNotEmpty()) t.name + "::" + t.steps.joinToString("\n") { it } else null
        }.joinToString("|")
        val serializedSteps = sharedStepItems.joinToString("\n")

        val existing = editingProcedure
        if (existing != null) {
            viewModel.updateClinicalProcedure(
                proc = existing,
                hasTypes = hasTypesInput,
                typesText = serializedTypes,
                stepsText = serializedSteps,
                typeStepsText = serializedTypeSteps,
                defaultFee = procDefaultFee,
                defaultLabFee = procDefaultLabFee
            )
        } else {
            viewModel.insertCustomClinicalProcedure(
                name = nameInput.trim(),
                hasTypes = hasTypesInput,
                typesText = serializedTypes,
                stepsText = serializedSteps,
                typeStepsText = serializedTypeSteps,
                defaultFee = procDefaultFee,
                defaultLabFee = procDefaultLabFee
            )
        }
        showCreatorForm = false
        resetForm()
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.procedures_library_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.procedures_library_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close_library))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                if (!showCreatorForm) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.hint_search_templates)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        RoleGate(allowedRoles = setOf("admin")) {
                            Button(
                                onClick = {
                                    resetForm()
                                    showCreatorForm = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("create_procedure_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.new_template), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ElevatedFilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text(stringResource(R.string.all)) }
                        )

                        categories.forEach { cat ->
                            ElevatedFilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(localizedProcedureName(cat)) }
                            )
                        }
                    }

                    if (filteredProcedures.isEmpty()) {
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
                                    imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.no_templates_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.no_templates_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filteredProcedures) { proc ->
                                ProcedureTemplateCard(
                                    procedure = proc,
                                    isExpanded = expandedProcId == proc.id,
                                    onToggleExpand = {
                                        expandedProcId = if (expandedProcId == proc.id) null else proc.id
                                    },
                                    onDelete = {
                                        if (proc.isCustom) {
                                            viewModel.deleteCustomClinicalProcedure(proc)
                                        }
                                    },
                                    onEdit = {
                                        scope.launch {
                                            val data = viewModel.getProcedureTypesWithStepsOnce(proc.id)
                                            nameInput = proc.name
                                            hasTypesInput = proc.hasTypes
                                            defaultFeeInput = proc.defaultFee?.let { if (it == 0.0) "" else it.toString() } ?: ""
                                            defaultLabFeeInput = proc.defaultLabFee?.let { if (it == 0.0) "" else it.toString() } ?: ""
                                            typeItems = data.types.mapIndexed { idx, t ->
                                                val matFees = if (!t.materialFees.isNullOrBlank()) {
                                                    t.materialFees.split(",").mapNotNull { entry ->
                                                        val mParts = entry.split(":", limit = 3)
                                                        if (mParts.size == 3) MaterialFeeDetail(mParts[0], mParts[1], mParts[2])
                                                        else null
                                                    }
                                                } else emptyList()
                                                TypeFormItem(
                                                    id = idx, name = t.name, materials = t.materials ?: "",
                                                    steps = data.typeStepsMap[t.name] ?: emptyList(),
                                                    fee = t.defaultFee?.let { if (it == 0.0) "" else it.toString() } ?: "",
                                                    labFee = t.defaultLabFee?.let { if (it == 0.0) "" else it.toString() } ?: "",
                                                    materialFees = matFees
                                                )
                                            }
                                            nextTypeId = data.types.size
                                            sharedStepItems = data.sharedSteps.map { it.stepName }
                                            editingProcedure = proc
                                            showCreatorForm = true
                                        }
                                    },
                                    expandedTypes = expandedTypes[proc.id] ?: emptyList(),
                                    expandedSteps = expandedSteps[proc.id] ?: emptyList(),
                                    expandedTypeSteps = expandedTypeSteps
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (editingProcedure != null) stringResource(R.string.edit_template_title) else stringResource(R.string.create_template_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (validationError.isNotBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = validationError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text(stringResource(R.string.procedure_name_label)) },
                            placeholder = { Text(stringResource(R.string.hint_template_name)) },
                            modifier = Modifier.fillMaxWidth().testTag("proc_form_name_input"),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = defaultFeeInput,
                                onValueChange = { defaultFeeInput = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text(stringResource(R.string.default_fee)) },
                                placeholder = { Text("0.00") },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = defaultLabFeeInput,
                                onValueChange = { defaultLabFeeInput = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text(stringResource(R.string.default_lab_fee)) },
                                placeholder = { Text("0.00") },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newVal = !hasTypesInput
                                    hasTypesInput = newVal
                                    if (!newVal) typeItems = emptyList()
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = hasTypesInput,
                                onCheckedChange = {
                                    hasTypesInput = it
                                    if (!it) typeItems = emptyList()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.has_types_checkbox),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (hasTypesInput) {
                            Text(
                            text = stringResource(R.string.types_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        typeItems.forEachIndexed { idx, typeItem ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Type ${idx + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            IconButton(onClick = { typeItems = typeItems.toMutableList().also { it.removeAt(idx) } }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove_type), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                        OutlinedTextField(
                                            value = typeItem.name,
                                            onValueChange = { newName -> typeItems = typeItems.toMutableList().also { it[idx] = typeItem.copy(name = newName) } },
                                            label = { Text(stringResource(R.string.name)) },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = typeItem.materials,
                                            onValueChange = { newMat -> typeItems = typeItems.toMutableList().also { it[idx] = typeItem.copy(materials = newMat) } },
                                            label = { Text(stringResource(R.string.materials_label_plural)) },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = typeItem.fee,
                                                onValueChange = { newFee -> typeItems = typeItems.toMutableList().also { it[idx] = typeItem.copy(fee = newFee.filter { c -> c.isDigit() || c == '.' }) } },
                                                label = { Text(stringResource(R.string.fee)) },
                                                placeholder = { Text("0.00") },
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f),
                                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                                            )
                                            OutlinedTextField(
                                                value = typeItem.labFee,
                                                onValueChange = { newLab -> typeItems = typeItems.toMutableList().also { it[idx] = typeItem.copy(labFee = newLab.filter { c -> c.isDigit() || c == '.' }) } },
                                                label = { Text(stringResource(R.string.lab_fee)) },
                                                placeholder = { Text("0.00") },
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f),
                                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                                            )
                                        }
                                        if (typeItem.materials.split(",").any { it.isNotBlank() } && typeItem.materials.contains(",")) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(R.string.material_pricing),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            typeItem.materials.split(",").filter { it.isNotBlank() }.forEach { mat ->
                                                val trimmedMat = mat.trim()
                                                val existingMatFee = typeItem.materialFees.find { it.material == trimmedMat }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = trimmedMat,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.width(80.dp)
                                                    )
                                                    OutlinedTextField(
                                                        value = existingMatFee?.fee ?: "",
                                                        onValueChange = { v ->
                                                            val filtered = v.filter { c -> c.isDigit() || c == '.' }
                                                            val updated = typeItem.materialFees.toMutableList()
                                                            val idx2 = updated.indexOfFirst { it.material == trimmedMat }
                                                            val newDetail = MaterialFeeDetail(trimmedMat, filtered, existingMatFee?.labFee ?: "")
                                                            if (idx2 >= 0) updated[idx2] = newDetail else updated.add(newDetail)
                                                            typeItems = typeItems.toMutableList().also { it[idx] = typeItem.copy(materialFees = updated) }
                                                        },
                                                        label = { Text(stringResource(R.string.fee)) },
                                                        placeholder = { Text("0.00") },
                                                        singleLine = true,
                                                        textStyle = MaterialTheme.typography.bodySmall,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    OutlinedTextField(
                                                        value = existingMatFee?.labFee ?: "",
                                                        onValueChange = { v ->
                                                            val filtered = v.filter { c -> c.isDigit() || c == '.' }
                                                            val updated = typeItem.materialFees.toMutableList()
                                                            val idx2 = updated.indexOfFirst { it.material == trimmedMat }
                                                            val newDetail = MaterialFeeDetail(trimmedMat, existingMatFee?.fee ?: "", filtered)
                                                            if (idx2 >= 0) updated[idx2] = newDetail else updated.add(newDetail)
                                                            typeItems = typeItems.toMutableList().also { it[idx] = typeItem.copy(materialFees = updated) }
                                                        },
                                                        label = { Text(stringResource(R.string.lab_fee)) },
                                                        placeholder = { Text("0.00") },
                                                        singleLine = true,
                                                        textStyle = MaterialTheme.typography.bodySmall,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(stringResource(R.string.type_specific_steps), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        typeItem.steps.forEachIndexed { stepIdx, step ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("• $step", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                                IconButton(onClick = {
                                                    val newSteps = typeItem.steps.toMutableList().also { it.removeAt(stepIdx) }
                                                    typeItems = typeItems.toMutableList().also { it[idx] = typeItem.copy(steps = newSteps) }
                                                }, modifier = Modifier.size(20.dp)) {
                                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove_step_library), modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                                                }
                                                IconButton(onClick = {
                                                    if (stepIdx > 0) {
                                                        val newSteps = typeItem.steps.toMutableList()
                                                        val temp = newSteps[stepIdx]
                                                        newSteps[stepIdx] = newSteps[stepIdx - 1]
                                                        newSteps[stepIdx - 1] = temp
                                                        typeItems = typeItems.toMutableList().also { it[idx] = typeItem.copy(steps = newSteps) }
                                                    }
                                                }, modifier = Modifier.size(18.dp)) {
                                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.cd_move_up), modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(onClick = {
                                                    if (stepIdx < typeItem.steps.size - 1) {
                                                        val newSteps = typeItem.steps.toMutableList()
                                                        val temp = newSteps[stepIdx]
                                                        newSteps[stepIdx] = newSteps[stepIdx + 1]
                                                        newSteps[stepIdx + 1] = temp
                                                        typeItems = typeItems.toMutableList().also { it[idx] = typeItem.copy(steps = newSteps) }
                                                    }
                                                }, modifier = Modifier.size(18.dp)) {
                                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.cd_move_down), modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = if (idx == typeItems.size - 1 && newTypeStepsText.isNotBlank()) newTypeStepsText else "",
                                                onValueChange = { if (idx == typeItems.size - 1) newTypeStepsText = it },
                                                placeholder = { Text(stringResource(R.string.hint_type_step)) },
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = {
                                                val text = if (idx == typeItems.size - 1) newTypeStepsText else ""
                                                if (text.isNotBlank()) {
                                                    val newSteps = typeItem.steps + text.trim()
                                                    typeItems = typeItems.toMutableList().also { it[idx] = typeItem.copy(steps = newSteps) }
                                                    if (idx == typeItems.size - 1) newTypeStepsText = ""
                                                }
                                            }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_step_library), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newTypeName,
                                    onValueChange = { newTypeName = it },
                                    placeholder = { Text(stringResource(R.string.hint_type_name)) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = newTypeMaterials,
                                    onValueChange = { newTypeMaterials = it },
                                    placeholder = { Text(stringResource(R.string.hint_type_materials)) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        if (newTypeName.isNotBlank()) {
                                            typeItems = typeItems + TypeFormItem(id = nextTypeId, name = newTypeName.trim(), materials = newTypeMaterials.trim(), steps = emptyList())
                                            nextTypeId++
                                            newTypeName = ""
                                            newTypeMaterials = ""
                                        }
                                    },
                                    enabled = newTypeName.isNotBlank(),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_type), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text(
                        text = if (hasTypesInput) stringResource(R.string.shared_steps_label) else stringResource(R.string.steps_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    sharedStepItems.forEachIndexed { idx, step ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedTextField(
                                    value = step,
                                    onValueChange = { newVal -> sharedStepItems = sharedStepItems.toMutableList().also { it[idx] = newVal } },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text(stringResource(R.string.hint_shared_step)) }
                                )
                                IconButton(onClick = { sharedStepItems = sharedStepItems.toMutableList().also { it.removeAt(idx) } }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove_step_library), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                }
                                IconButton(onClick = {
                                    if (idx > 0) {
                                        val mutable = sharedStepItems.toMutableList()
                                        val temp = mutable[idx]
                                        mutable[idx] = mutable[idx - 1]
                                        mutable[idx - 1] = temp
                                        sharedStepItems = mutable
                                    }
                                }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.cd_move_up), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    if (idx < sharedStepItems.size - 1) {
                                        val mutable = sharedStepItems.toMutableList()
                                        val temp = mutable[idx]
                                        mutable[idx] = mutable[idx + 1]
                                        mutable[idx + 1] = temp
                                        sharedStepItems = mutable
                                    }
                                }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.cd_move_down), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newSharedStepText,
                                onValueChange = { newSharedStepText = it },
                                placeholder = { Text(stringResource(R.string.hint_shared_step_add)) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (newSharedStepText.isNotBlank()) {
                                        sharedStepItems = sharedStepItems + newSharedStepText.trim()
                                        newSharedStepText = ""
                                    }
                                },
                                enabled = newSharedStepText.isNotBlank(),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_step_library), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                showCreatorForm = false
                                resetForm()
                            }) {
                                Text(stringResource(R.string.cancel))
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = { saveProcedure() },
                                shape = RoundedCornerShape(10.dp),
                                enabled = nameInput.isNotBlank(),
                                modifier = Modifier.testTag("proc_form_save_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (editingProcedure != null) stringResource(R.string.update_template) else stringResource(R.string.save_template), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProcedureTemplateCard(
    procedure: ClinicalProcedure,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    expandedTypes: List<ProcedureType>,
    expandedSteps: List<ClinicalProcedureStep>,
    expandedTypeSteps: Map<Int, List<ClinicalProcedureStep>>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring()),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = localizedProcedureName(procedure.name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (procedure.isCustom) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.custom_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (procedure.hasTypes) stringResource(R.string.has_types_label) else stringResource(R.string.steps_only_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    )
                }

                Row {
                    RoleGate(allowedRoles = setOf("admin")) {
                        if (procedure.isCustom) {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.cd_delete_template),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.cd_edit_template),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = stringResource(R.string.cd_expand),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()

                if (procedure.hasTypes && expandedTypes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.types_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    expandedTypes.forEach { type ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.padding(start = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = type.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!type.materials.isNullOrBlank()) {
                                    Text(
                                        text = stringResource(R.string.procedure_materials_label, type.materials ?: ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val typeSteps = expandedTypeSteps[type.id] ?: emptyList()
                                if (typeSteps.isNotEmpty()) {
                                    Text(
                                    text = stringResource(R.string.steps_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(0.7f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    typeSteps.forEach { step ->
                                        Row(modifier = Modifier.padding(start = 8.dp, top = 1.dp)) {
                                            Text(
                                                text = "• ${step.stepName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (expandedSteps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (procedure.hasTypes) stringResource(R.string.shared_steps_label) else stringResource(R.string.steps_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    expandedSteps.forEach { step ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.padding(start = 8.dp)) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = step.stepName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!procedure.hasTypes && expandedSteps.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.no_steps_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                    )
                }
            }
        }
    }
}
