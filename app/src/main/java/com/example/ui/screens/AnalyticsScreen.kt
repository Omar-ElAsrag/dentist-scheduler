package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ProcedureCardDetail
import androidx.compose.ui.res.stringResource
import com.example.ui.DentistViewModel
import com.example.ui.components.AppCard
import com.example.ui.localizedProcedureName
import com.example.ui.components.MetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.themeOnSuccessContainer
import com.example.ui.theme.themeSuccessContainer
import java.text.SimpleDateFormat
import java.util.*

private val PROCEDURE_COLORS = listOf(
    Color(0xFFE53935), // Deep Red
    Color(0xFF1E88E5), // Blue
    Color(0xFF00897B), // Teal
    Color(0xFFFB8C00), // Orange
    Color(0xFF8E24AA), // Purple
    Color(0xFF43A047), // Green
    Color(0xFFD81B60), // Pink
    Color(0xFFFFB300)  // Amber
)

private data class DonutSlice(
    val procedureName: String,
    val count: Int,
    val color: Color,
    val percentage: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: DentistViewModel) {
    val procedureCards by viewModel.procedureCards.collectAsState()
    val clinics by viewModel.clinics.collectAsState()
    val clinicalProcedures by viewModel.clinicalProcedures.collectAsState()

    val calendar = Calendar.getInstance()
    val rawRecentMonths = remember {
        (0..5).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -offset)
            val yr = cal.get(Calendar.YEAR)
            val mo = cal.get(Calendar.MONTH) + 1
            val yearMonthStr = String.format(Locale.US, "%04d-%02d", yr, mo)
            val displayTitle = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)
            yearMonthStr to displayTitle
        }
    }

    var selectedTabItem by remember { mutableStateOf(0) }

    // Tab 0 - Clinic Traffic
    var tab1SelectedMonthSec by remember { mutableStateOf(rawRecentMonths.firstOrNull()?.first ?: "") }
    var tab1SelectedClinicId by remember { mutableStateOf<Int?>(null) }

    // Tab 1 - Treatment Trends
    var trendsSourceId by remember { mutableStateOf<Int?>(null) }
    var trendsTimeSpan by remember { mutableStateOf(6) }
    var trendsSelectedMonth by remember { mutableStateOf(rawRecentMonths.firstOrNull()?.first ?: "") }
    var trendsProcedureId by remember { mutableStateOf<Int?>(null) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    // --- Tab 0 data ---
    val tab1MatchedCards = procedureCards.filter { card ->
        (card.status == "Completed" || card.amountPaid > 0) &&
        card.dateCreated.startsWith(tab1SelectedMonthSec) &&
                (tab1SelectedClinicId == null || card.clinicId == tab1SelectedClinicId)
    }

    val tab1UniquePatientCount = tab1MatchedCards.map { it.patientId }.toSet().size
    val tab1TotalCardsCount = tab1MatchedCards.size

    val isCurrentMonthSelected = tab1SelectedMonthSec == rawRecentMonths.firstOrNull()?.first

    // --- Tab 1 data ---
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val monthsInYear = remember {
        (0..11).map { monthIndex ->
            val cal = Calendar.getInstance()
            cal.set(currentYear, monthIndex, 1)
            val yearMonth = String.format(Locale.US, "%04d-%02d", currentYear, monthIndex + 1)
            val displayName = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
            yearMonth to displayName
        }
    }

    val trendsMonths = remember(trendsTimeSpan) {
        (0 until trendsTimeSpan).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -offset)
            val yr = cal.get(Calendar.YEAR)
            val mo = cal.get(Calendar.MONTH) + 1
            val yearMonth = String.format(Locale.US, "%04d-%02d", yr, mo)
            val displayName = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)
            yearMonth to displayName
        }
    }

    val relevantYearMonths = if (trendsTimeSpan == 1) listOf(trendsSelectedMonth)
                             else trendsMonths.map { it.first }

    val cardsInPeriod = procedureCards.filter { card ->
        relevantYearMonths.any { card.dateCreated.startsWith(it) } &&
        (trendsSourceId == null || card.clinicId == trendsSourceId)
    }

    val completedInPeriod = cardsInPeriod.filter { it.status == "Completed" }
    val totalCardsCount = cardsInPeriod.size
    val completedCount = completedInPeriod.size
    val inProgressCount = cardsInPeriod.count { it.status == "In Progress" }
    val canceledCount = cardsInPeriod.count { it.status == "Canceled" }

    val totalPatientsInPeriod = cardsInPeriod.map { it.patientId }.distinct().size

    val generalExamProcId = clinicalProcedures.find { it.name == "General Examination" }?.id
    val genExamOnlyCount = if (generalExamProcId != null) {
        cardsInPeriod.groupBy { it.patientId }
            .count { (_, patientCards) ->
                patientCards.all { it.clinicalProcedureId == generalExamProcId }
            }
    } else 0

    // Donut data
    val completedTotal = completedInPeriod.size.toFloat()
    val donutSlices = completedInPeriod.groupBy { it.clinicalProcedureId }
        .map { (procId, cards) ->
            val proc = clinicalProcedures.find { it.id == procId }
            val idx = clinicalProcedures.indexOf(proc).coerceIn(0, PROCEDURE_COLORS.lastIndex)
            DonutSlice(
                procedureName = proc?.name ?: "Unknown",
                count = cards.size,
                color = PROCEDURE_COLORS[idx],
                percentage = if (completedTotal > 0) cards.size / completedTotal * 100 else 0f
            )
        }
        .sortedByDescending { it.count }

    // Bar chart data
    val barChartValues = trendsMonths.map { (yearMonth, displayName) ->
        val matchingCards = procedureCards.filter { card ->
            card.status == "Completed" &&
            card.dateCreated.startsWith(yearMonth) &&
            (trendsSourceId == null || card.clinicId == trendsSourceId) &&
            (trendsProcedureId == null || card.clinicalProcedureId == trendsProcedureId)
        }
        displayName.substringBefore(" ") to matchingCards.size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        SectionHeader(
            title = stringResource(R.string.analytics_title),
            subtitle = stringResource(R.string.analytics_subtitle),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
        )

        TabRow(
            selectedTabIndex = selectedTabItem,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTabItem == 0,
                onClick = { selectedTabItem = 0 },
                text = { Text(stringResource(R.string.clinic_traffic_tab), fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Poll, contentDescription = null) }
            )
            Tab(
                selected = selectedTabItem == 1,
                onClick = { selectedTabItem = 1 },
                text = { Text(stringResource(R.string.treatment_trends_tab), fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.QueryStats, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Crossfade(targetState = selectedTabItem) { tabIndex ->
            if (tabIndex == 0) {
                // ═══ TAB 0: CLINIC TRAFFIC (unchanged) ═══
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = stringResource(R.string.monthly_clinic_activity),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.activity_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.select_month_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rawRecentMonths.forEach { (yearMonthVal, displayName) ->
                                    val isSelected = tab1SelectedMonthSec == yearMonthVal
                                    ElevatedFilterChip(
                                        selected = isSelected,
                                        onClick = { tab1SelectedMonthSec = yearMonthVal },
                                        label = { Text(displayName) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = stringResource(R.string.associate_clinic_analytics),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = tab1SelectedClinicId == null,
                                    onClick = { tab1SelectedClinicId = null },
                                    label = { Text(stringResource(R.string.all_filter_analytics)) }
                                )
                                clinics.forEach { c ->
                                    FilterChip(
                                        selected = tab1SelectedClinicId == c.id,
                                        onClick = { tab1SelectedClinicId = c.id },
                                        label = { Text(c.name) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            icon = Icons.Default.People,
                            label = stringResource(R.string.patients_metric),
                            value = "${tab1UniquePatientCount}",
                            subtitle = stringResource(R.string.patients_metric_subtitle),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            icon = Icons.Default.MedicalServices,
                            label = stringResource(R.string.procedures_metric),
                            value = "${tab1TotalCardsCount}",
                            subtitle = stringResource(R.string.procedures_metric_subtitle),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val totalRevenue = tab1MatchedCards.sumOf { it.amountPaid }
                    val totalLabFees = tab1MatchedCards.sumOf { it.labFees }
                    val totalAssociatePayout = tab1MatchedCards.sumOf { it.calculatedAssociateCut }
                    val totalClinicRevenue = tab1MatchedCards.sumOf { it.calculatedClinicShare }

                    Text(
                        text = stringResource(R.string.financial_summary_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.total_gross_revenue),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(0.8f)
                                )
                                Text(
                                    text = String.format(Locale.US, "%,.2f", totalRevenue),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.total_lab_fees_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(0.8f)
                                )
                                Text(
                                    text = String.format(Locale.US, "%,.2f", totalLabFees),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = themeSuccessContainer())
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.associate_payout),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = themeOnSuccessContainer().copy(0.8f)
                                )
                                Text(
                                    text = String.format(Locale.US, "%,.2f", totalAssociatePayout),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = themeOnSuccessContainer()
                                )
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.clinic_revenue),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.8f)
                                )
                                Text(
                                    text = String.format(Locale.US, "%,.2f", totalClinicRevenue),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    if (isCurrentMonthSelected) {
                        AppCard(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.ongoing_month_message),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                // ═══ TAB 1: TREATMENT TRENDS (redesigned) ═══
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = stringResource(R.string.treatment_trends_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.trends_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Shared Filters ──
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.source_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = trendsSourceId == null,
                                    onClick = { trendsSourceId = null },
                                    label = { Text(stringResource(R.string.all_filter_analytics)) }
                                )
                                clinics.forEach { c ->
                                    FilterChip(
                                        selected = trendsSourceId == c.id,
                                        onClick = { trendsSourceId = c.id },
                                        label = { Text(c.name) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = stringResource(R.string.time_span_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val periods = listOf(
                                    1 to stringResource(R.string.one_month),
                                    3 to stringResource(R.string.three_months),
                                    6 to stringResource(R.string.six_months),
                                    12 to stringResource(R.string.one_year)
                                )
                                periods.forEach { (count, label) ->
                                    FilterChip(
                                        selected = trendsTimeSpan == count,
                                        onClick = { trendsTimeSpan = count },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            if (trendsTimeSpan == 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.select_month_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ExposedDropdownMenuBox(
                                        expanded = monthDropdownExpanded,
                                        onExpandedChange = { monthDropdownExpanded = it }
                                    ) {
                                        val selectedLabel = monthsInYear.find { it.first == trendsSelectedMonth }?.second
                                            ?: monthsInYear.firstOrNull()?.second ?: ""
                                        OutlinedTextField(
                                            value = selectedLabel,
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded) },
                                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                                            label = { Text(stringResource(R.string.select_month_label)) },
                                            singleLine = true
                                        )
                                        ExposedDropdownMenu(
                                            expanded = monthDropdownExpanded,
                                            onDismissRequest = { monthDropdownExpanded = false }
                                        ) {
                                            monthsInYear.forEach { (yearMonth, displayName) ->
                                                DropdownMenuItem(
                                                    text = { Text(displayName) },
                                                    onClick = {
                                                        trendsSelectedMonth = yearMonth
                                                        monthDropdownExpanded = false
                                                    },
                                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── CHART 1: Donut — Treatment Volumes ──
                    AppCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(4.dp, 20.dp)
                                        .background(Color(0xFFE53935), RoundedCornerShape(2.dp))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.treatment_volumes_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = stringResource(R.string.treatment_volumes_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(Modifier.height(16.dp))

                            if (donutSlices.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.no_cards_title),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    DonutChart(
                                        slices = donutSlices,
                                        modifier = Modifier.size(180.dp)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    donutSlices.take(8).forEach { slice ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                Modifier.size(10.dp)
                                                    .background(slice.color, RoundedCornerShape(2.dp))
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = localizedProcedureName(slice.procedureName),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${slice.count}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = " (${String.format(Locale.US, "%.0f", slice.percentage)}%)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── CHART 2: Vertical Bar — Treatment Numbers ──
                    AppCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(4.dp, 20.dp)
                                        .background(Color(0xFF1E88E5), RoundedCornerShape(2.dp))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.treatment_numbers_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = stringResource(R.string.treatment_numbers_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )

                            Text(
                                text = stringResource(R.string.select_procedure_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = trendsProcedureId == null,
                                    onClick = { trendsProcedureId = null },
                                    label = { Text(stringResource(R.string.all_filter_analytics), style = MaterialTheme.typography.labelSmall) }
                                )
                                clinicalProcedures.forEach { cp ->
                                    FilterChip(
                                        selected = trendsProcedureId == cp.id,
                                        onClick = { trendsProcedureId = cp.id },
                                        label = { Text(localizedProcedureName(cp.name), style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            val barColor = if (trendsProcedureId != null) {
                                val idx = clinicalProcedures.indexOfFirst { it.id == trendsProcedureId }
                                    .coerceIn(0, PROCEDURE_COLORS.lastIndex)
                                PROCEDURE_COLORS[idx]
                            } else MaterialTheme.colorScheme.primary

                            AnalyticsBarChart(
                                chartData = barChartValues,
                                accentColor = barColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── CHART 3: Horizontal Bar — Treatment Status ──
                    AppCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(4.dp, 20.dp)
                                        .background(Color(0xFF43A047), RoundedCornerShape(2.dp))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.treatment_status_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = stringResource(R.string.treatment_status_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(Modifier.height(16.dp))

                            val maxCount = maxOf(totalCardsCount, 1)
                            StatusBar(
                                label = stringResource(R.string.cards_created),
                                count = totalCardsCount,
                                max = maxCount,
                                color = Color(0xFF42A5F5)
                            )
                            Spacer(Modifier.height(10.dp))
                            StatusBar(
                                label = stringResource(R.string.completed),
                                count = completedCount,
                                max = maxCount,
                                color = Color(0xFF66BB6A)
                            )
                            Spacer(Modifier.height(10.dp))
                            StatusBar(
                                label = stringResource(R.string.in_progress),
                                count = inProgressCount,
                                max = maxCount,
                                color = Color(0xFFFFCA28)
                            )
                            if (canceledCount > 0) {
                                Spacer(Modifier.height(10.dp))
                                StatusBar(
                                    label = stringResource(R.string.canceled),
                                    count = canceledCount,
                                    max = maxCount,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.general_exam_statement, genExamOnlyCount, totalPatientsInPeriod),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.count }.toFloat()
    Canvas(modifier = modifier) {
        if (total == 0f) return@Canvas
        val strokeWidth = size.minDimension * 0.18f
        val radius = (size.minDimension - strokeWidth) / 2
        val centerX = size.width / 2
        val centerY = size.height / 2
        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = (slice.count / total) * 360f
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun StatusBar(
    label: String,
    count: Int,
    max: Int,
    color: Color
) {
    val fraction = if (max > 0) count.toFloat() / max else 0f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(color, RoundedCornerShape(11.dp))
            )
        }
    }
}

@Composable
fun AnalyticsBarChart(
    chartData: List<Pair<String, Int>>,
    accentColor: Color
) {
    val textLabelColor = MaterialTheme.colorScheme.onBackground.toArgb()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                val horizontalPadding = 48f
                val verticalPadding = 60f

                val usableWidth = width - (horizontalPadding * 2)
                val usableHeight = height - (verticalPadding * 2)

                val maxValue = chartData.maxOfOrNull { it.second } ?: 1
                val safeMaxY = if (maxValue == 0) 5 else maxValue + 1

                val columnsSize = chartData.size
                val colWidth = usableWidth / columnsSize

                val gridSteps = 4
                for (i in 0..gridSteps) {
                    val gridY = verticalPadding + (usableHeight * (1f - (i.toFloat() / gridSteps)))
                    val refLabel = (safeMaxY * i) / gridSteps

                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(horizontalPadding, gridY),
                        end = Offset(width - horizontalPadding, gridY),
                        strokeWidth = 2f
                    )

                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = textLabelColor
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isAntiAlias = true
                        }
                        canvas.nativeCanvas.drawText(
                            "$refLabel",
                            horizontalPadding - 12f,
                            gridY + 8f,
                            paint
                        )
                    }
                }

                chartData.forEachIndexed { idx, (label, value) ->
                    val progressYFraction = value.toFloat() / safeMaxY
                    val blockHeight = usableHeight * progressYFraction

                    val startX = horizontalPadding + (idx * colWidth) + (colWidth * 0.15f)
                    val blockWidth = colWidth * 0.7f
                    val topY = verticalPadding + usableHeight - blockHeight

                    drawRoundRect(
                        color = if (value > 0) accentColor else Color.LightGray.copy(alpha = 0.4f),
                        topLeft = Offset(startX, topY),
                        size = Size(blockWidth, maxOf(10f, blockHeight)),
                        cornerRadius = CornerRadius(12f, 12f)
                    )

                    drawIntoCanvas { canvas ->
                        val textPaint = android.graphics.Paint().apply {
                            color = textLabelColor
                            textSize = 30f
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        canvas.nativeCanvas.drawText(
                            "$value",
                            startX + (blockWidth / 2),
                            topY - 12f,
                            textPaint
                        )
                    }

                    drawIntoCanvas { canvas ->
                        val labelPaint = android.graphics.Paint().apply {
                            color = textLabelColor
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        canvas.nativeCanvas.drawText(
                            label,
                            startX + (blockWidth / 2),
                            height - 12f,
                            labelPaint
                        )
                    }
                }
            }
        }
    }
}
