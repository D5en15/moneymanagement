package com.example.moneymanager.ui.screens.accounts

import com.example.moneymanager.utils.IconUtils
import androidx.compose.ui.graphics.toArgb
import com.example.moneymanager.ui.theme.AccountColors
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.entity.AccountEntity
import com.example.moneymanager.domain.repository.MoneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.moneymanager.data.local.entity.CategoryEntity
import com.example.moneymanager.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import java.util.Date
import kotlin.math.abs

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: MoneyRepository
) : ViewModel() {

    // Get all accounts
    val accounts: StateFlow<List<AccountEntity>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Get all categories for adjustment
    private val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculate Total Balance (Net Worth)
    val totalBalance: StateFlow<Double> = accounts.map { list ->
        list.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Used Colors
    val takenColors: StateFlow<List<Int>> = accounts.map { list ->
        list.mapNotNull { it.color }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI State for Add/Edit Account
    private val _newAccountName = MutableStateFlow("")
    val newAccountName: StateFlow<String> = _newAccountName

    private val _newAccountBalance = MutableStateFlow("")
    val newAccountBalance: StateFlow<String> = _newAccountBalance

    private val _selectedColor = MutableStateFlow<Int?>(null)
    val selectedColor: StateFlow<Int?> = _selectedColor

    private val _selectedIcon = MutableStateFlow<String?>(null)
    val selectedIcon: StateFlow<String?> = _selectedIcon

    val accountTypes = listOf(
        "cash", 
        "bank", 
        "credit_card", 
        "e_wallet", 
        "investment", 
        "digital_asset", 
        "loan", 
        "others"
    )
    private val _selectedAccountType = MutableStateFlow("cash")
    val selectedAccountType: StateFlow<String> = _selectedAccountType

    fun onNewAccountNameChanged(name: String) {
        _newAccountName.value = name
    }

    fun onNewAccountBalanceChanged(balance: String) {
        _newAccountBalance.value = balance
    }

    fun onColorSelected(color: Int) {
        _selectedColor.value = color
    }

    fun onIconSelected(iconName: String) {
        _selectedIcon.value = iconName
    }

    fun onAccountTypeSelected(type: String) {
        _selectedAccountType.value = type
    }

    fun resetStateForAdd() {
        _newAccountName.value = ""
        _newAccountBalance.value = ""
        _selectedAccountType.value = "cash"
        
        // Pick first available color
        val allColors = AccountColors.map { it.toArgb() }
        val taken = takenColors.value
        val available = allColors.filter { !taken.contains(it) }
        _selectedColor.value = available.firstOrNull() ?: allColors.random()
        
        // Default Icon
        _selectedIcon.value = "payments"
    }

    fun prepareForEdit(account: AccountEntity) {
        _newAccountName.value = account.name
        // Remove trailing .0 if balance is an integer
        _newAccountBalance.value = if (account.balance % 1.0 == 0.0) {
            account.balance.toLong().toString()
        } else {
            account.balance.toString()
        }
        _selectedColor.value = account.color ?: AccountColors.first().toArgb()
        _selectedIcon.value = account.icon ?: IconUtils.accountIcons.first().first
        _selectedAccountType.value = account.type
    }

    fun addAccount() {
        if (_newAccountName.value.isBlank()) return
        val balance = _newAccountBalance.value.toDoubleOrNull() ?: 0.0
        val color = _selectedColor.value ?: AccountColors.first().toArgb()
        val icon = _selectedIcon.value ?: IconUtils.accountIcons.first().first
        
        viewModelScope.launch {
            val newAccount = AccountEntity(
                name = _newAccountName.value,
                type = _selectedAccountType.value,
                balance = balance,
                icon = icon,
                color = color
            )
            repository.insertAccount(newAccount)
            resetStateForAdd()
        }
    }

    fun updateAccount(account: AccountEntity, recordTransaction: Boolean = false, oldBalance: Double = 0.0) {
        viewModelScope.launch {
            val balance = _newAccountBalance.value.toDoubleOrNull() ?: 0.0
            val color = _selectedColor.value ?: AccountColors.first().toArgb()
            val icon = _selectedIcon.value ?: IconUtils.accountIcons.first().first
            
            val updatedAccount = account.copy(
                name = _newAccountName.value,
                balance = balance,
                type = _selectedAccountType.value,
                color = color,
                icon = icon
            )
            repository.updateAccount(updatedAccount)

            if (recordTransaction && balance != oldBalance) {
                val diff = balance - oldBalance
                val type = if (diff > 0) "income" else "expense"
                val amount = abs(diff)
                
                // Get fresh categories list
                val catList = repository.getAllCategories().first()
                val category = catList.find { it.type == type && (it.name.equals("Others", ignoreCase = true) || it.name.equals("Adjustment", ignoreCase = true)) }
                    ?: catList.find { it.type == type }
                    ?: catList.firstOrNull()

                if (category != null) {
                    val cal = java.util.Calendar.getInstance()
                    val time = cal.timeInMillis
                    // Normalize date to start of day
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    val date = cal.timeInMillis

                    val transaction = TransactionEntity(
                        amount = amount,
                        type = type,
                        categoryId = category.id,
                        accountId = account.id,
                        date = date, // Normalized Date
                        time = time, // Actual Time
                        note = "Balance Adjustment"
                    )
                    repository.insertTransaction(transaction)
                }
            }
        }
    }

    private val _showDeleteBlockedDialog = MutableStateFlow(false)
    val showDeleteBlockedDialog: StateFlow<Boolean> = _showDeleteBlockedDialog

    private val _linkedTransactionCount = MutableStateFlow(0)
    val linkedTransactionCount: StateFlow<Int> = _linkedTransactionCount

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            val count = repository.getTransactionCountByAccount(account.id)
            if (count > 0) {
                _linkedTransactionCount.value = count
                _showDeleteBlockedDialog.value = true
            } else {
                repository.deleteAccount(account)
            }
        }
    }

    fun dismissDeleteBlockedDialog() {
        _showDeleteBlockedDialog.value = false
    }
}
