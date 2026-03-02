package com.example.moneymanager.ui.screens.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.R
import com.example.moneymanager.ui.components.timeline.TimelineHeader
import com.example.moneymanager.ui.theme.ErrorRed
import com.example.moneymanager.ui.theme.SuccessGreen
import com.example.moneymanager.utils.IconUtils

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight

import androidx.compose.ui.graphics.Brush
import com.example.moneymanager.ui.components.LineChart
import com.example.moneymanager.ui.theme.DarkBackground

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onAddTransactionClick: (String) -> Unit,
    onTransactionClick: (Int) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    var showAddMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (showAddMenu) {
        ModalBottomSheet(
            onDismissRequest = { showAddMenu = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.title_add_transaction),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                
                ListItem(
                    headlineContent = { Text(stringResource(R.string.label_income)) },
                    leadingContent = { 
                        Icon(
                            IconUtils.getIconByName("trending_up"), 
                            contentDescription = null,
                            tint = SuccessGreen
                        ) 
                    },
                    modifier = Modifier.clickable { 
                        showAddMenu = false
                        onAddTransactionClick("income") 
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.label_expense)) },
                    leadingContent = { 
                        Icon(
                            IconUtils.getIconByName("trending_down"), 
                            contentDescription = null,
                            tint = ErrorRed
                        ) 
                    },
                    modifier = Modifier.clickable { 
                        showAddMenu = false
                        onAddTransactionClick("expense") 
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.label_transfer)) },
                    leadingContent = { 
                        Icon(
                            IconUtils.getIconByName("sync_alt"), 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    modifier = Modifier.clickable { 
                        showAddMenu = false
                        onAddTransactionClick("transfer") 
                    }
                )
            }
        }
    }

    // Transparent Scaffold background to let the MainScreen gradient show through
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddMenu = true },
                containerColor = MaterialTheme.colorScheme.primary, // Primary Gradient base
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Add, stringResource(R.string.add_transaction_cd)) },
                text = { Text(text = stringResource(R.string.add_btn)) },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp) // Add horizontal padding for cards
        ) {
            // Spacer for Status Bar (Manual handling)
            item {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            }

            // 1. Header Section (Not Sticky)
            item {
                TimelineHeader(
                    currentMonth = uiState.currentMonth,
                    totalBalance = uiState.totalBalance,
                    surroundingMonths = uiState.surroundingMonths,
                    selectedDate = uiState.currentDate,
                    onMonthSelected = viewModel::selectDate,
                    income = uiState.totalIncome,
                    expense = uiState.totalExpense
                )
            }

            // 1.5 Line Chart (Trend)
            if (uiState.chartData.isNotEmpty() && uiState.chartData.any { it != 0f }) {
                item {
                    val lineColor = if (isDark) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary
                    val fillBrush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(vertical = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(16.dp)
                            .semantics { contentDescription = "Trend Line Chart" }
                    ) {
                        LineChart(
                            data = uiState.chartData,
                            modifier = Modifier.fillMaxSize(),
                            lineColor = lineColor,
                            fillBrush = fillBrush
                        )
                    }
                }
            }

            // 2. Transactions grouped by Week (Sticky Headers)
            if (uiState.transactionsByDate.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_transactions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                uiState.transactionsByDate.forEach { (weekLabel, transactions) ->
                    stickyHeader {
                        WeekHeader(label = weekLabel)
                    }
                    items(
                        items = transactions,
                        key = { transaction -> "${transaction.id}_${transaction.date}" }
                    ) { transaction ->
                        TransactionItem(
                            id = transaction.id,
                            title = transaction.note?.takeIf { it.isNotBlank() } ?: transaction.categoryName,
                            account = transaction.accountName,
                            amount = (if (transaction.isExpense) "-" else "+") + formatAmount(transaction.amount),
                            iconName = transaction.iconName,
                            isExpense = transaction.isExpense,
                            onClick = { onTransactionClick(transaction.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeekHeader(label: String) {
    // "AppBar/Top bar: Use surface = transparent... OR #F7F8FF"
    // For sticky headers, we want a slight blur or solid color to obscure scrolled content.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)) 
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun TransactionItem(
    id: Int,
    title: String,
    account: String,
    amount: String,
    iconName: String?,
    isExpense: Boolean,
    onClick: () -> Unit
) {
    // "Cards (stat cards / list items): Use card surface rgba(255,255,255,0.78)"
    ListItem(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface) // Glassy surface from theme
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f), shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "transaction_$id" },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent // We set background above
        ),
        headlineContent = { 
            Text(
                title, 
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
        supportingContent = { 
            Text(
                account,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        },
        leadingContent = {
            // Icon background bubble
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = IconUtils.getIconByName(iconName),
                    contentDescription = "Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        trailingContent = {
            Text(
                text = amount,
                color = if (isExpense) ErrorRed else SuccessGreen,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    )
}

fun formatAmount(amount: Double): String {
    return "\u0E3F" + String.format("%,.2f", amount)
}
