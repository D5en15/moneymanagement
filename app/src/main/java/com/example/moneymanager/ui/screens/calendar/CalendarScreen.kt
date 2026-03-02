package com.example.moneymanager.ui.screens.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.R
import com.example.moneymanager.ui.theme.ErrorRed
import com.example.moneymanager.ui.theme.SuccessGreen
import com.example.moneymanager.utils.formatMoneyCompact
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    onDayLongPress: (Date) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val locale = remember { Locale.getDefault() }
    val sheetDateFormatter = remember(locale) { SimpleDateFormat("EEEE, d MMMM yyyy", locale) }

    var showDatePicker by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val baseMonth = remember { YearMonth.now() }
    val startPage = remember { Int.MAX_VALUE / 2 }
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { Int.MAX_VALUE })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val monthDiff = page - startPage
                val targetMonth = baseMonth.plusMonths(monthDiff.toLong())
                viewModel.prefetchMonth(targetMonth)
            }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val monthDiff = page - startPage
                val targetMonth = baseMonth.plusMonths(monthDiff.toLong())
                viewModel.setMonth(targetMonth)
            }
    }

    LaunchedEffect(uiState.month) {
        val totalMonthDiff = ChronoUnit.MONTHS.between(baseMonth, uiState.month).toInt()
        val targetPage = startPage + totalMonthDiff
        if (pagerState.settledPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(uiState.isSheetVisible) {
        if (uiState.isSheetVisible) {
            coroutineScope.launch { bottomSheetState.show() }
        } else {
            coroutineScope.launch { bottomSheetState.hide() }
        }
    }

    val selectedDayDate by remember(uiState.selectedDayMillis) {
        derivedStateOf { uiState.selectedDayMillis?.let(::Date) }
    }

    if (uiState.isSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearSelectedDate() },
            sheetState = bottomSheetState,
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            val sheetTitle = if (uiState.selectionMode == CalendarSelectionMode.MULTI) {
                stringResource(R.string.calendar_days_selected, uiState.selectedDaysCount)
            } else {
                selectedDayDate?.let(sheetDateFormatter::format).orEmpty()
            }

            DailyTransactionsSheet(
                title = sheetTitle,
                transactions = uiState.selectedDayTransactions,
                summary = uiState.selectedDaySummary,
                onDismiss = { viewModel.clearSelectedDate() }
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                CalendarHeader(
                    currentMonth = uiState.month,
                    totalBalance = uiState.totalBalance,
                    selectedDaysCount = uiState.selectedDaysCount,
                    isSelectionMode = uiState.selectionMode == CalendarSelectionMode.MULTI,
                    onHeaderClick = { showDatePicker = true },
                    onGoToToday = { viewModel.onGoToToday() },
                    onToggleSelectionMode = { viewModel.onToggleSelectionMode() },
                    onClearSelection = { viewModel.onClearSelection() }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val hasSelection = uiState.selectedDaysCount > 0
            val summaryIncome = if (hasSelection) uiState.selectedDaySummary.income else uiState.totalIncome
            val summaryExpense = if (hasSelection) uiState.selectedDaySummary.expense else uiState.totalExpense
            val summaryBalance = if (hasSelection) uiState.selectedDaySummary.balance else uiState.totalBalance

            CalendarSummaryRow(
                income = summaryIncome,
                expense = summaryExpense,
                balance = summaryBalance,
                hasSelection = hasSelection,
                onClearSelection = viewModel::onClearSelection
            )

            Divider(modifier = Modifier.padding(top = 4.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                val days = listOf(
                    R.string.day_sun, R.string.day_mon, R.string.day_tue,
                    R.string.day_wed, R.string.day_thu, R.string.day_fri, R.string.day_sat
                )
                days.forEach { dayRes ->
                    Text(
                        text = stringResource(dayRes),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                beyondBoundsPageCount = 1,
                modifier = Modifier.fillMaxSize()
            ) {
                CalendarMonthPage(
                    days = uiState.days,
                    selectionMode = uiState.selectionMode,
                    selectedDays = uiState.selectedDayMillisSet,
                    onDayClick = viewModel::onDayClick,
                    onDayLongPress = { dayStart ->
                        if (uiState.selectionMode == CalendarSelectionMode.MULTI) {
                            viewModel.onDayLongPress(dayStart)
                        } else {
                            onDayLongPress(Date(dayStart))
                        }
                    }
                )
            }
        }
    }

    if (showDatePicker) {
        MonthYearPickerDialog(
            currentMonth = uiState.month,
            onDateSelected = { year, month ->
                viewModel.setMonth(YearMonth.of(year, month + 1))
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false }
        )
    }
}

@Composable
private fun CalendarSummaryRow(
    income: Double,
    expense: Double,
    balance: Double,
    hasSelection: Boolean,
    onClearSelection: () -> Unit
) {
    val locale = remember { Locale.getDefault() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryColumn(
                label = stringResource(R.string.label_income),
                valueText = "\u0E3F" + formatMoneyCompact(income, locale),
                valueStyle = MaterialTheme.typography.titleSmall,
                valueColor = SuccessGreen
            )
            SummaryColumn(
                label = stringResource(R.string.label_expense),
                valueText = "\u0E3F" + formatMoneyCompact(expense, locale),
                valueStyle = MaterialTheme.typography.titleSmall,
                valueColor = ErrorRed
            )
            SummaryColumn(
                label = stringResource(R.string.stats_net),
                valueText = "\u0E3F" + formatMoneyCompact(balance, locale),
                valueStyle = MaterialTheme.typography.titleMedium,
                valueColor = if (balance >= 0) SuccessGreen else ErrorRed
            )
        }
        if (hasSelection) {
            Text(
                text = stringResource(R.string.calendar_clear_selection),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onClearSelection)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RowScope.SummaryColumn(
    label: String,
    valueText: String,
    valueStyle: androidx.compose.ui.text.TextStyle,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valueText,
            style = valueStyle,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
fun CalendarMonthPage(
    days: List<CalendarDayUi>,
    selectionMode: CalendarSelectionMode,
    selectedDays: Set<Long>,
    onDayClick: (Long) -> Unit,
    onDayLongPress: (Long) -> Unit
) {
    if (days.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items = days, key = { _, day -> day.dayStartMillis }) { index, day ->
                val col = index % 7
                val row = index / 7
                CalendarDayCell(
                    day = day,
                    isSelectionMode = selectionMode == CalendarSelectionMode.MULTI,
                    isSelected = selectedDays.contains(day.dayStartMillis),
                    isLastCol = col == 6,
                    isLastRow = row == 5,
                    onDayClick = onDayClick,
                    onDayLongPress = onDayLongPress
                )
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    totalBalance: Double,
    selectedDaysCount: Int,
    isSelectionMode: Boolean,
    onHeaderClick: () -> Unit,
    onGoToToday: () -> Unit,
    onToggleSelectionMode: () -> Unit,
    onClearSelection: () -> Unit
) {
    val locale = remember { Locale.getDefault() }
    val dateFormat = remember(locale) { java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", locale) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onHeaderClick)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentMonth.format(dateFormat),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.calendar_today),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onGoToToday)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onToggleSelectionMode) {
                Icon(
                    imageVector = if (isSelectionMode) Icons.Default.CheckCircle else Icons.Default.Checklist,
                    contentDescription = stringResource(if (isSelectionMode) R.string.calendar_done else R.string.calendar_multi_select),
                    tint = if (isSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarDayCell(
    day: CalendarDayUi,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isLastCol: Boolean,
    isLastRow: Boolean,
    onDayClick: (Long) -> Unit,
    onDayLongPress: (Long) -> Unit
) {
    val canOpen = true
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

    val cellModifier = Modifier
        .aspectRatio(0.8f)
        .drawBehind {
            val strokeWidth = 0.5.dp.toPx()
            if (!isLastCol) {
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )
            }
            if (!isLastRow) {
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )
            }
        }
        .combinedClickable(
            onClick = {
                if (canOpen) {
                    onDayClick(day.dayStartMillis)
                }
            },
            onLongClick = {
                if (day.isInMonth) {
                    onDayLongPress(day.dayStartMillis)
                }
            }
        )
        .let {
            when {
                day.isToday && !isSelected -> it.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                else -> it
            }
        }
        .padding(4.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = cellModifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = if (isSelected) {
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            } else Modifier.size(24.dp)
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    day.isToday -> MaterialTheme.colorScheme.primary
                    day.isInMonth -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                },
                fontWeight = if (day.isInMonth) FontWeight.Bold else FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(46.dp))
        
        if (day.isInMonth) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.height(4.dp)
            ) {
                if (day.summary.income > 0) {
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(SuccessGreen))
                }
                if (day.summary.expense > 0) {
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(ErrorRed))
                }
            }
        }
    }
}

@Composable
fun DailyTransactionsSheet(
    title: String,
    transactions: List<CalendarTransactionDisplay>,
    summary: DaySummary,
    onDismiss: () -> Unit
) {
    val locale = remember { Locale.getDefault() }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.label_income), style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "\u0E3F" + formatMoneyCompact(summary.income, locale),
                    color = SuccessGreen,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.label_expense), style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "\u0E3F" + formatMoneyCompact(summary.expense, locale),
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.info_no_transactions_on_this_day), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn {
                items(items = transactions, key = { it.id }) { transaction ->
                    TransactionRow(transaction = transaction)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.btn_close))
        }
    }
}

@Composable
fun MonthYearPickerDialog(
    currentMonth: YearMonth,
    onDateSelected: (Int, Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedYear by remember(currentMonth) { mutableStateOf(currentMonth.year) }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedYear-- }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous Year")
                }
                Text(
                    text = selectedYear.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { selectedYear++ }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next Year")
                }
            }
        },
        text = {
            Column {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val months = java.text.DateFormatSymbols().shortMonths
                    items(items = months.indices.toList(), key = { monthIndex -> monthIndex }) { monthIndex ->
                        val isSelected = monthIndex == (currentMonth.monthValue - 1) && selectedYear == currentMonth.year
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onDateSelected(selectedYear, monthIndex) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = months[monthIndex],
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
