package com.example.moneymanager.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.data.local.entity.TransactionEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.moneymanager.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.WindowInsets
import com.example.moneymanager.utils.formatMoneyCompact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onNavigateToAddTransaction: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val drillDownTransactions by viewModel.drillDownTransactions.collectAsState()
    
    var showPeriodSheet by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_stats)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Period Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPeriodSheet = true }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when(uiState.selectedPeriod) {
                            DatePeriod.THIS_MONTH -> stringResource(R.string.stats_this_month)
                            DatePeriod.LAST_MONTH -> stringResource(R.string.stats_last_month)
                            DatePeriod.CUSTOM -> stringResource(R.string.stats_custom_range)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Period")
                }

                // Mode Selector
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = when (uiState.selectedMode) {
                            StatsMode.INCOME -> stringResource(R.string.label_income)
                            StatsMode.EXPENSE -> stringResource(R.string.label_expense)
                            StatsMode.NET -> stringResource(R.string.stats_net)
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expanded
                            )
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        StatsMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    val label = when (mode) {
                                        StatsMode.INCOME -> stringResource(R.string.label_income)
                                        StatsMode.EXPENSE -> stringResource(R.string.label_expense)
                                        StatsMode.NET -> stringResource(R.string.stats_net)
                                    }
                                    Text(text = label)
                                },
                                onClick = {
                                    viewModel.setMode(mode)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            if (uiState.isLoading) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     CircularProgressIndicator()
                 }
            } else if (uiState.totalIncome == 0.0 && uiState.totalExpense == 0.0) {
                 EmptyState(onAddTransaction = onNavigateToAddTransaction)
            } else {
                 LazyColumn(
                     modifier = Modifier.fillMaxSize(),
                     contentPadding = PaddingValues(16.dp),
                     verticalArrangement = Arrangement.spacedBy(16.dp)
                 ) {
                     item {
                         SummaryCards(uiState)
                     }
                     
                     if (uiState.selectedMode != StatsMode.NET && uiState.categoryStats.isNotEmpty()) {
                         item {
                             ChartSection(uiState)
                         }
                     }
                     
                     if (uiState.categoryStats.isNotEmpty()) {
                         item {
                             Text(
                                 text = stringResource(R.string.stats_breakdown),
                                 style = MaterialTheme.typography.titleMedium,
                                 fontWeight = FontWeight.Bold,
                                 modifier = Modifier.padding(vertical = 8.dp)
                             )
                         }
                         
                         items(
                             items = uiState.categoryStats,
                             key = { stat -> stat.categoryId ?: "name_${stat.categoryName}" }
                         ) { stat ->
                             CategoryStatItem(stat, onClick = { viewModel.selectCategory(stat) })
                         }
                     }
                 }
            }
        }
    }
    
    if (showPeriodSheet) {
        ModalBottomSheet(onDismissRequest = { showPeriodSheet = false }) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text(stringResource(R.string.date_cd), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) // Reusing date_cd "Date" or maybe need "Select Period"
                Spacer(modifier = Modifier.height(16.dp))
                DatePeriod.values().forEach { period ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                viewModel.setPeriod(period) 
                                showPeriodSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when(period) {
                                DatePeriod.THIS_MONTH -> stringResource(R.string.stats_this_month)
                                DatePeriod.LAST_MONTH -> stringResource(R.string.stats_last_month)
                                DatePeriod.CUSTOM -> stringResource(R.string.stats_custom_range)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    if (selectedCategory != null) {
        ModalBottomSheet(onDismissRequest = { viewModel.dismissDrillDown() }) {
            DrillDownList(
                category = selectedCategory!!, 
                transactions = drillDownTransactions
            )
        }
    }
}

@Composable
fun SummaryCards(state: StatsUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(
            title = stringResource(R.string.label_income),
            amount = state.totalIncome,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = stringResource(R.string.label_expense),
            amount = state.totalExpense,
            color = Color(0xFFE53935),
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    SummaryCard(
        title = stringResource(R.string.stats_net),
        amount = state.totalNet,
        color = if (state.totalNet >= 0) Color(0xFF2196F3) else Color(0xFFE53935),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SummaryCard(title: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(
                text = "\u0E3F" + formatMoneyCompact(amount, Locale.getDefault()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun ChartSection(state: StatsUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.Center
    ) {
        if (state.categoryStats.isNotEmpty()) {
            DonutChart(state.categoryStats)
        }
    }
}

@Composable
fun DonutChart(stats: List<CategoryStat>) {
    val total = stats.sumOf { it.amount }
    
    Canvas(modifier = Modifier.size(200.dp)) {
        val strokeWidth = 40.dp.toPx()
        val radius = size.minDimension / 2 - strokeWidth / 2
        var startAngle = -90f
        
        stats.forEach { stat ->
            val sweepAngle = (stat.amount / total * 360).toFloat()
            drawArc(
                color = stat.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun CategoryStatItem(stat: CategoryStat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(stat.color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stat.categoryName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${(stat.percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = stat.percentage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = stat.color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "\u0E3F" + formatMoneyCompact(stat.amount, Locale.getDefault()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
fun EmptyState(onAddTransaction: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.stats_empty_msg), style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAddTransaction) {
            Text(stringResource(R.string.nav_add_transaction))
        }
    }
}

@Composable
fun DrillDownList(category: CategoryStat, transactions: List<TransactionEntity>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "${category.categoryName} ${stringResource(R.string.stats_transactions_suffix)}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.stats_no_transactions))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = transactions,
                    key = { tx -> "${tx.id}_${tx.date}" }
                ) { tx ->
                    TransactionRow(tx)
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

@Composable
fun TransactionRow(tx: TransactionEntity) {
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.note.takeIf { !it.isNullOrBlank() } ?: stringResource(R.string.default_transaction_note),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = dateFormat.format(Date(tx.date)), // Assuming date is full timestamp or combined? 
                // TransactionEntity has date: Long and time: Long.
                // Assuming 'date' holds the main timestamp.
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = NumberFormat.getCurrencyInstance(Locale("th", "TH")).format(tx.amount),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (tx.type == "income") Color(0xFF4CAF50) else Color(0xFFE53935)
        )
    }
}
