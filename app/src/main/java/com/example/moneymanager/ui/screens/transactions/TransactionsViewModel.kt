package com.example.moneymanager.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.entity.AccountEntity
import com.example.moneymanager.data.local.entity.CategoryEntity
import com.example.moneymanager.data.local.entity.TransactionEntity
import com.example.moneymanager.domain.repository.MoneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class TransactionDisplayItem(
    val id: Int,
    val amount: Double,
    val date: Long,
    val note: String?,
    val type: String,
    val categoryName: String,
    val accountName: String,
    val iconName: String?,
    val isExpense: Boolean
)

data class TransactionsUiState(
    val transactionsByDate: Map<String, List<TransactionDisplayItem>> = emptyMap(), // Key: Week Range (e.g. "Dec 01 - 07")
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalBalance: Double = 0.0,
    val currentMonth: String = "",
    val currentDate: Date = Date(),
    val surroundingMonths: List<Date> = emptyList(), // For the Month Scroller
    val isLoading: Boolean = false,
    val chartData: List<Float> = emptyList()
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: MoneyRepository
) : ViewModel() {

    private val calendar = Calendar.getInstance()
    private val _currentDate = MutableStateFlow(Date())
    
    // Formatter for Week grouping
    private val weekFormatter = SimpleDateFormat("dd", Locale.getDefault())
    private val monthFormatter = SimpleDateFormat("MMM", Locale.getDefault())

    val uiState: StateFlow<TransactionsUiState> = combine(
        _currentDate,
        repository.getAllAccounts(),
        repository.getAllCategories()
    ) { date, accounts, categories ->
        Triple(date, accounts, categories)
    }.flatMapLatest { (date, accounts, categories) ->
        val startOfMonth = getStartOfMonth(date)
        val endOfMonth = getEndOfMonth(date)
        
        combine(
            repository.getTransactionsByDateRange(startOfMonth, endOfMonth),
            repository.getNetChangeSince(startOfMonth)
        ) { transactions, netChangeSince ->
            val sortedTransactions = transactions.sortedByDescending { it.date }
            val accountMap = accounts.associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }
            
            val displayItems = sortedTransactions.map { trans ->
                TransactionDisplayItem(
                    id = trans.id,
                    amount = trans.amount,
                    date = trans.date,
                    note = trans.note,
                    type = trans.type,
                    categoryName = categoryMap[trans.categoryId]?.name ?: trans.type.replaceFirstChar { it.uppercase() },
                    accountName = accountMap[trans.accountId]?.name ?: "Unknown",
                    iconName = if (trans.type == "transfer") "sync_alt" else categoryMap[trans.categoryId]?.icon,
                    isExpense = trans.type == "expense"
                )
            }

            // Group by Week
            val grouped = displayItems.groupBy { item ->
                getWeekRangeLabel(Date(item.date))
            }
            
            val income = transactions.filter { it.type == "income" }.sumOf { it.amount }
            val expense = transactions.filter { it.type == "expense" }.sumOf { it.amount }

            // Calculate Chart Data (Every Transaction Point)
            val currentTotalBalance = accounts.sumOf { it.balance }
            val netChange = netChangeSince ?: 0.0
            val startBalance = currentTotalBalance - netChange

            val cumulativeData = mutableListOf<Float>()
            var currentSum = startBalance.toFloat()
            
            // Add initial point (Start of Month)
            cumulativeData.add(currentSum)

            // Sort by Date ASCENDING to calculate running balance
            val chronologicalTransactions = transactions.sortedBy { it.date }

            chronologicalTransactions.forEach { trans ->
                val amount = trans.amount.toFloat()
                if (trans.type == "income") {
                    currentSum += amount
                } else if (trans.type == "expense") {
                    currentSum -= amount
                }
                // Add point for this transaction
                cumulativeData.add(currentSum)
            }
            
            TransactionsUiState(
                transactionsByDate = grouped,
                totalIncome = income,
                totalExpense = expense,
                totalBalance = income - expense,
                currentMonth = getMonthYearString(date),
                currentDate = date,
                surroundingMonths = generateSurroundingMonths(date),
                chartData = cumulativeData
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun selectDate(date: Date) {
        _currentDate.value = date
    }

    fun nextMonth() {
        calendar.time = _currentDate.value
        calendar.add(Calendar.MONTH, 1)
        _currentDate.value = calendar.time
    }

    fun previousMonth() {
        calendar.time = _currentDate.value
        calendar.add(Calendar.MONTH, -1)
        _currentDate.value = calendar.time
    }

    private fun generateSurroundingMonths(centerDate: Date): List<Date> {
        val months = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.time = centerDate
        cal.add(Calendar.MONTH, -2) // Start 2 months back
        
        repeat(5) { // Generate 5 months total (-2, -1, 0, +1, +2)
            months.add(cal.time)
            cal.add(Calendar.MONTH, 1)
        }
        return months
    }

    private fun getWeekRangeLabel(date: Date): String {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.firstDayOfWeek = Calendar.SUNDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val start = cal.time
        cal.add(Calendar.DAY_OF_WEEK, 6)
        val end = cal.time
        
        // Format: "MMM dd - dd" (e.g., "Jan 01 - 07")
        return "${monthFormatter.format(start)} ${weekFormatter.format(start)} - ${weekFormatter.format(end)}"
    }

    private fun getStartOfMonth(date: Date): Long {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfMonth(date: Date): Long {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun getMonthYearString(date: Date): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(date)
    }
}