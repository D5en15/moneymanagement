package com.example.moneymanager.ui.screens.calendar

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.entity.TransactionEntity
import com.example.moneymanager.domain.repository.MoneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.util.Calendar
import java.util.Date
import java.util.LinkedHashMap
import javax.inject.Inject

data class DaySummary(
    val income: Double,
    val expense: Double,
    val balance: Double
)

data class CalendarDayUi(
    val dayStartMillis: Long,
    val dayOfMonth: Int,
    val isInMonth: Boolean,
    val isToday: Boolean,
    val hasTx: Boolean,
    val summary: DaySummary
)

data class CalendarTransactionDisplay(
    val id: Int,
    val amount: Double,
    val type: String,
    val note: String?,
    val categoryId: Int?,
    val timeMillis: Long
)

enum class CalendarSelectionMode {
    NONE,
    MULTI
}

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val days: List<CalendarDayUi> = emptyList(),
    val selectionMode: CalendarSelectionMode = CalendarSelectionMode.NONE,
    val selectedDayMillis: Long? = null,
    val selectedDayMillisSet: Set<Long> = emptySet(),
    val selectedDaysCount: Int = 0,
    val isSheetVisible: Boolean = false,
    val selectedDayTransactions: List<CalendarTransactionDisplay> = emptyList(),
    val selectedDaySummary: DaySummary = DaySummary(0.0, 0.0, 0.0),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalBalance: Double = 0.0,
    val monthBuildMs: Long = 0L
)

private data class TransactionsSignature(
    val size: Int,
    val checksum: Long
)

private data class MonthComputedState(
    val month: YearMonth,
    val signature: TransactionsSignature,
    val days: List<CalendarDayUi>,
    val transactionsByDay: Map<Long, List<TransactionEntity>>,
    val summaryByDay: Map<Long, DaySummary>,
    val totalIncome: Double,
    val totalExpense: Double,
    val monthBuildMs: Long
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: MoneyRepository
) : ViewModel() {
    private val monthStateCache = MonthStateLruCache(maxEntries = 16)
    private var prefetchJob: Job? = null

    private val _currentMonth = MutableStateFlow(currentYearMonth())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    private val _selectionMode = MutableStateFlow(CalendarSelectionMode.NONE)
    private val _selectedDayMillis = MutableStateFlow<Long?>(null)
    private val _selectedDayMillisSet = MutableStateFlow<Set<Long>>(emptySet())
    private val _isSheetOpen = MutableStateFlow(false)

    private val monthComputedState: StateFlow<MonthComputedState> = _currentMonth.flatMapLatest { month ->
        val monthRange = monthRangeForGrid(month)
        repository.getTransactionsByDateRange(monthRange.gridStartMillis, monthRange.gridEndMillis)
            .mapLatest { transactions ->
                withContext(Dispatchers.Default) {
                    buildMonthComputedState(month, monthRange, transactions)
                }
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialMonthComputedState()
    )

    val uiState: StateFlow<CalendarUiState> = combine(
        monthComputedState,
        _selectionMode,
        _selectedDayMillis,
        _selectedDayMillisSet,
        _isSheetOpen
    ) { monthState, selectionMode, selectedDayMillis, selectedDayMillisSet, isSheetOpen ->
        val effectiveSelectedDays = when (selectionMode) {
            CalendarSelectionMode.NONE -> selectedDayMillis?.let { setOf(it) }.orEmpty()
            CalendarSelectionMode.MULTI -> selectedDayMillisSet
        }

        val selectedTransactions = effectiveSelectedDays
            .flatMap { dayStart -> monthState.transactionsByDay[dayStart].orEmpty() }
            .sortedByDescending { it.date }
            .map { tx ->
                CalendarTransactionDisplay(
                    id = tx.id,
                    amount = tx.amount,
                    type = tx.type,
                    note = tx.note,
                    categoryId = tx.categoryId,
                    timeMillis = tx.time
                )
            }

        val selectedSummary = effectiveSelectedDays.fold(DaySummary(0.0, 0.0, 0.0)) { acc, dayStart ->
            val daySummary = monthState.summaryByDay[dayStart] ?: DaySummary(0.0, 0.0, 0.0)
            DaySummary(
                income = acc.income + daySummary.income,
                expense = acc.expense + daySummary.expense,
                balance = acc.balance + daySummary.balance
            )
        }

        CalendarUiState(
            month = monthState.month,
            days = monthState.days,
            selectionMode = selectionMode,
            selectedDayMillis = selectedDayMillis,
            selectedDayMillisSet = selectedDayMillisSet,
            selectedDaysCount = effectiveSelectedDays.size,
            isSheetVisible = isSheetOpen && effectiveSelectedDays.isNotEmpty(),
            selectedDayTransactions = selectedTransactions,
            selectedDaySummary = selectedSummary,
            totalIncome = monthState.totalIncome,
            totalExpense = monthState.totalExpense,
            totalBalance = monthState.totalIncome - monthState.totalExpense,
            monthBuildMs = monthState.monthBuildMs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun setDate(date: Date) {
        setMonth(yearMonthFromDate(date))
    }

    fun setMonth(month: YearMonth) {
        if (_currentMonth.value != month) {
            _currentMonth.value = month
            _selectedDayMillis.value = null
            _selectedDayMillisSet.value = emptySet()
        }
    }

    fun selectDate(date: Date) {
        onDayClick(startOfDayMillis(date.time))
    }

    fun onGoToToday() {
        val now = System.currentTimeMillis()
        _currentMonth.value = yearMonthFromDate(Date(now))
        _selectionMode.value = CalendarSelectionMode.NONE
        _selectedDayMillisSet.value = emptySet()
        _selectedDayMillis.value = startOfDayMillis(now)
        _isSheetOpen.value = false
    }

    fun onToggleSelectionMode() {
        _selectionMode.value = if (_selectionMode.value == CalendarSelectionMode.MULTI) {
            _selectedDayMillisSet.value = emptySet()
            _selectedDayMillis.value = null
            _isSheetOpen.value = false
            CalendarSelectionMode.NONE
        } else {
            _selectedDayMillis.value = null
            _isSheetOpen.value = false
            CalendarSelectionMode.MULTI
        }
    }

    fun onDayClick(dayStartMillis: Long) {
        val targetMonth = yearMonthFromDate(Date(dayStartMillis))
        val isMonthJump = targetMonth != _currentMonth.value
        if (isMonthJump) {
            setMonth(targetMonth)
            _selectedDayMillis.value = dayStartMillis
            _isSheetOpen.value = false
            return
        }

        when (_selectionMode.value) {
            CalendarSelectionMode.NONE -> {
                _selectedDayMillis.value = dayStartMillis
                _isSheetOpen.value = true
            }
            CalendarSelectionMode.MULTI -> {
                _selectedDayMillisSet.value = _selectedDayMillisSet.value.toMutableSet().apply {
                    if (contains(dayStartMillis)) remove(dayStartMillis) else add(dayStartMillis)
                }
                _isSheetOpen.value = false
            }
        }
    }

    fun onDayLongPress(dayStartMillis: Long) {
        if (_selectionMode.value == CalendarSelectionMode.MULTI) {
            onDayClick(dayStartMillis)
        }
    }

    fun clearSelectedDate() {
        when (_selectionMode.value) {
            CalendarSelectionMode.NONE -> _selectedDayMillis.value = null
            CalendarSelectionMode.MULTI -> _selectedDayMillisSet.value = emptySet()
        }
        _isSheetOpen.value = false
    }

    fun onClearSelection() {
        _selectionMode.value = CalendarSelectionMode.NONE
        _selectedDayMillis.value = null
        _selectedDayMillisSet.value = emptySet()
        _isSheetOpen.value = false
    }

    private fun buildMonthComputedState(
        month: YearMonth,
        range: MonthGridRange,
        transactions: List<TransactionEntity>
    ): MonthComputedState {
        val signature = buildSignature(transactions)
        monthStateCache.get(month, signature)?.let { cached ->
            return cached
        }

        val startTime = SystemClock.elapsedRealtime()
        val todayStart = startOfDayMillis(System.currentTimeMillis())

        val transactionsByDay = transactions.groupBy { tx ->
            startOfDayMillis(tx.date)
        }

        val summaryByDay = HashMap<Long, DaySummary>(transactionsByDay.size)
        for ((dayStartMillis, txs) in transactionsByDay) {
            var income = 0.0
            var expense = 0.0
            for (tx in txs) {
                when (tx.type) {
                    "income" -> income += tx.amount
                    "expense" -> expense += tx.amount
                }
            }
            summaryByDay[dayStartMillis] = DaySummary(
                income = income,
                expense = expense,
                balance = income - expense
            )
        }

        val hasTxByDay = transactionsByDay.keys
        val days = ArrayList<CalendarDayUi>(42)
        var totalIncome = 0.0
        var totalExpense = 0.0

        val cal = Calendar.getInstance().apply {
            timeInMillis = range.gridStartMillis
        }

        while (cal.timeInMillis <= range.gridEndMillis) {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStartMillis = cal.timeInMillis
            val isInMonth = cal.get(Calendar.YEAR) == month.year &&
                (cal.get(Calendar.MONTH) + 1) == month.monthValue
            val summary = summaryByDay[dayStartMillis] ?: DaySummary(0.0, 0.0, 0.0)

            if (isInMonth) {
                totalIncome += summary.income
                totalExpense += summary.expense
            }

            days.add(
                CalendarDayUi(
                    dayStartMillis = dayStartMillis,
                    dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                    isInMonth = isInMonth,
                    isToday = dayStartMillis == todayStart,
                    hasTx = hasTxByDay.contains(dayStartMillis),
                    summary = summary
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }

        val elapsed = SystemClock.elapsedRealtime() - startTime
        if (elapsed > 8L) {
            Log.d("CalendarViewModel", "monthBuildMs=$elapsed, month=$month, tx=${transactions.size}")
        }

        val computed = MonthComputedState(
            month = month,
            signature = signature,
            days = days,
            transactionsByDay = transactionsByDay,
            summaryByDay = summaryByDay,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            monthBuildMs = elapsed
        )
        monthStateCache.put(month, computed)
        return computed
    }

    private fun initialMonthComputedState(): MonthComputedState {
        val month = currentYearMonth()
        return MonthComputedState(
            month = month,
            signature = TransactionsSignature(size = 0, checksum = 0L),
            days = emptyList(),
            transactionsByDay = emptyMap(),
            summaryByDay = emptyMap(),
            totalIncome = 0.0,
            totalExpense = 0.0,
            monthBuildMs = 0L
        )
    }

    private fun currentYearMonth(): YearMonth {
        val cal = Calendar.getInstance()
        return YearMonth.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    private fun yearMonthFromDate(date: Date): YearMonth {
        val cal = Calendar.getInstance().apply { time = date }
        return YearMonth.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    private data class MonthGridRange(
        val gridStartMillis: Long,
        val gridEndMillis: Long
    )

    private fun monthRangeForGrid(month: YearMonth): MonthGridRange {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.YEAR, month.year)
            set(Calendar.MONTH, month.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                add(Calendar.DAY_OF_MONTH, -1)
            }
        }

        val gridStartMillis = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 41)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)

        return MonthGridRange(
            gridStartMillis = gridStartMillis,
            gridEndMillis = cal.timeInMillis
        )
    }

    private fun startOfDayMillis(timeMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun buildSignature(transactions: List<TransactionEntity>): TransactionsSignature {
        var checksum = 1125899906842597L
        transactions.forEach { tx ->
            checksum = (checksum * 31) xor tx.id.toLong()
            checksum = (checksum * 31) xor tx.date
            checksum = (checksum * 31) xor tx.time
            checksum = (checksum * 31) xor java.lang.Double.doubleToLongBits(tx.amount)
            checksum = (checksum * 31) xor tx.type.hashCode().toLong()
            checksum = (checksum * 31) xor (tx.categoryId ?: -1).toLong()
        }
        return TransactionsSignature(size = transactions.size, checksum = checksum)
    }

    fun prefetchMonth(month: YearMonth) {
        if (month == _currentMonth.value) return
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val range = monthRangeForGrid(month)
                repository
                    .getTransactionsByDateRange(range.gridStartMillis, range.gridEndMillis)
                    .take(1)
                    .collect { transactions ->
                        withContext(Dispatchers.Default) {
                            buildMonthComputedState(month, range, transactions)
                        }
                    }
            } catch (t: Throwable) {
                Log.d("CalendarViewModel", "prefetch failed month=$month", t)
            }
        }
    }
}

private class MonthStateLruCache(
    private val maxEntries: Int
) {
    private val cache = object : LinkedHashMap<YearMonth, MonthComputedState>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<YearMonth, MonthComputedState>?): Boolean {
            return size > maxEntries
        }
    }

    fun get(month: YearMonth, signature: TransactionsSignature): MonthComputedState? {
        val cached = cache[month] ?: return null
        return if (cached.signature == signature) cached else null
    }

    fun put(month: YearMonth, state: MonthComputedState) {
        cache[month] = state
    }
}
