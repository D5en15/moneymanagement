package com.example.moneymanager.ui.screens.stats

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.domain.repository.MoneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.moneymanager.data.local.entity.TransactionEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.Calendar
import javax.inject.Inject

enum class DatePeriod { THIS_MONTH, LAST_MONTH, CUSTOM }
enum class StatsMode { EXPENSE, INCOME, NET }

data class CategoryStat(
    val categoryName: String,
    val amount: Double,
    val color: Color,
    val percentage: Float,
    val categoryId: Int?
)

data class StatsUiState(
    val isLoading: Boolean = true,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalNet: Double = 0.0,
    val categoryStats: List<CategoryStat> = emptyList(),
    val selectedPeriod: DatePeriod = DatePeriod.THIS_MONTH,
    val selectedMode: StatsMode = StatsMode.EXPENSE,
    val startDate: Long = 0L,
    val endDate: Long = 0L
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: MoneyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    
    private val _selectedCategory = MutableStateFlow<CategoryStat?>(null)
    val selectedCategory: StateFlow<CategoryStat?> = _selectedCategory.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val drillDownTransactions: StateFlow<List<TransactionEntity>> = combine(
        _selectedCategory,
        _state
    ) { category, state ->
        Pair(category, state)
    }.flatMapLatest { (category, state) ->
        if (category == null || category.categoryId == null) {
            flowOf(emptyList())
        } else {
            repository.getTransactionsByCategory(category.categoryId, state.startDate, state.endDate)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = combine(
        _state,
        repository.getAllCategories()
    ) { state, allCategories ->
        Pair(state, allCategories)
    }.flatMapLatest { (state, allCategories) ->
        val mode = state.selectedMode
        val start = state.startDate
        val end = state.endDate

        combine(
            repository.getTotalAmountByType("income", start, end),
            repository.getTotalAmountByType("expense", start, end),
            // For breakdown, we switch based on mode
            if (mode == StatsMode.NET) {
                repository.getCategorySumsByType("expense", start, end)
            } else {
                repository.getCategorySumsByType(mode.name.lowercase(), start, end)
            }
        ) { incomeSum, expenseSum, categorySums ->
            val income = incomeSum ?: 0.0
            val expense = expenseSum ?: 0.0
            val net = income - expense
            
            // Calculate total for percentage based on mode
            val totalForPercentage = if (mode == StatsMode.INCOME) income else expense
            
            val statsList = categorySums.mapNotNull { catSum ->
                val category = allCategories.find { it.id == catSum.categoryId }
                if (category != null) {
                    val colorInt = category.color ?: (0xFF000000.toInt() or category.name.hashCode())
                    CategoryStat(
                        categoryName = category.name,
                        amount = catSum.total,
                        color = Color(colorInt).copy(alpha = 1f),
                        percentage = if (totalForPercentage > 0) (catSum.total / totalForPercentage).toFloat() else 0f,
                        categoryId = category.id
                    )
                } else null
            }

            state.copy(
                isLoading = false,
                totalIncome = income,
                totalExpense = expense,
                totalNet = net,
                categoryStats = statsList
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    init {
        setPeriod(DatePeriod.THIS_MONTH)
    }

    fun setPeriod(period: DatePeriod, customStart: Long? = null, customEnd: Long? = null) {
        val (start, end) = when (period) {
            DatePeriod.THIS_MONTH -> getStartAndEndOfMonth(0)
            DatePeriod.LAST_MONTH -> getStartAndEndOfMonth(-1)
            DatePeriod.CUSTOM -> (customStart ?: 0L) to (customEnd ?: 0L)
        }
        
        _state.update { 
            it.copy(
                selectedPeriod = period, 
                startDate = start, 
                endDate = end,
                isLoading = true
            ) 
        }
    }

    fun setMode(mode: StatsMode) {
        _state.update { it.copy(selectedMode = mode, isLoading = true) }
        dismissDrillDown()
    }

    fun selectCategory(category: CategoryStat) {
        _selectedCategory.value = category
    }

    fun dismissDrillDown() {
        _selectedCategory.value = null
    }

    private fun getStartAndEndOfMonth(offset: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, offset)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis
        return start to end
    }
}