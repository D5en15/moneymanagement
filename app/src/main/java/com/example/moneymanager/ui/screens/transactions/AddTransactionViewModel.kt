package com.example.moneymanager.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.entity.AccountEntity
import com.example.moneymanager.data.local.entity.CategoryEntity
import com.example.moneymanager.data.local.entity.TransactionEntity
import com.example.moneymanager.domain.repository.MoneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repository: MoneyRepository,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val transactionId: Int = savedStateHandle.get<Int>("transactionId") ?: -1
    val isEditMode: Boolean = transactionId != -1
    private val initialType: String? = savedStateHandle.get<String>("type")
    private val initialDate: Long? = savedStateHandle.get<Long>("date")

    // UI State
    private val _transactionType = MutableStateFlow(initialType ?: "expense") // expense, income, transfer
    val transactionType: StateFlow<String> = _transactionType

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId

    private val _selectedAccountId = MutableStateFlow<Int?>(null)
    val selectedAccountId: StateFlow<Int?> = _selectedAccountId

    private val _selectedToAccountId = MutableStateFlow<Int?>(null) // For transfer
    val selectedToAccountId: StateFlow<Int?> = _selectedToAccountId

    private val _date = MutableStateFlow(Date())
    val date: StateFlow<Date> = _date

    // Data from Repository
    val accounts: StateFlow<List<AccountEntity>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = combine(_transactionType, repository.getAllCategories()) { type, allCategories ->
        allCategories.filter { it.type == type }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSaveEnabled: StateFlow<Boolean> = combine(
        _amount,
        _selectedAccountId,
        _selectedCategoryId,
        _transactionType,
        _selectedToAccountId
    ) { amount, accountId, categoryId, type, toAccountId ->
        val isAmountValid = amount.toDoubleOrNull() != null && amount.toDouble() > 0
        val isAccountValid = accountId != null
        val isCategoryValid = if (type == "transfer") toAccountId != null else categoryId != null
        
        isAmountValid && isAccountValid && isCategoryValid
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        println("AddTransactionViewModel init: transactionId=$transactionId, isEditMode=$isEditMode")
        if (isEditMode) {
            loadTransaction(transactionId)
        } else if (initialDate != null && initialDate > 0) {
            _date.value = Date(initialDate)
        }
    }

    private fun loadTransaction(id: Int) {
        viewModelScope.launch {
            val transaction = repository.getTransactionById(id)
            if (transaction != null) {
                _transactionType.value = transaction.type
                _amount.value = transaction.amount.toString()
                _note.value = transaction.note ?: ""
                _date.value = Date(transaction.date)
                _selectedAccountId.value = transaction.accountId
                _selectedCategoryId.value = transaction.categoryId
                _selectedToAccountId.value = transaction.toAccountId
            }
        }
    }

    // Actions
    fun onTransactionTypeChanged(type: String) {
        _transactionType.value = type
        _selectedCategoryId.value = null // Reset category when type changes
    }

    fun onAmountChanged(newAmount: String) {
        _amount.value = newAmount
    }

    fun onNoteChanged(newNote: String) {
        _note.value = newNote
    }
    
    fun onDateChanged(newDate: Date) {
        _date.value = newDate
    }

    fun onCategorySelected(categoryId: Int) {
        _selectedCategoryId.value = categoryId
    }

    fun onAccountSelected(accountId: Int) {
        _selectedAccountId.value = accountId
    }

    fun onToAccountSelected(accountId: Int) {
        _selectedToAccountId.value = accountId
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val amountValue = _amount.value.toDoubleOrNull() ?: return
        val accountId = _selectedAccountId.value ?: return
        val type = _transactionType.value
        val dateVal = _date.value.time

        viewModelScope.launch {
            // Handle Edit: Revert old transaction impact
            if (isEditMode) {
                val oldTransaction = repository.getTransactionById(transactionId)
                if (oldTransaction != null) {
                    revertTransactionBalance(oldTransaction)
                }
            }

            val transaction = TransactionEntity(
                id = if (isEditMode) transactionId else 0, // 0 for Insert, ID for Update
                amount = amountValue,
                date = dateVal,
                time = dateVal,
                note = _note.value,
                type = type,
                categoryId = if (type != "transfer") _selectedCategoryId.value else null,
                accountId = accountId,
                toAccountId = if (type == "transfer") _selectedToAccountId.value else null
            )

            if (isEditMode) {
                repository.updateTransaction(transaction)
            } else {
                repository.insertTransaction(transaction)
            }

            // Apply new transaction impact
            applyTransactionBalance(transaction)

            onSuccess()
        }
    }

    fun deleteTransaction(onSuccess: () -> Unit) {
        if (!isEditMode) return
        viewModelScope.launch {
            val transaction = repository.getTransactionById(transactionId)
            if (transaction != null) {
                revertTransactionBalance(transaction)
                repository.deleteTransaction(transaction)
            }
            onSuccess()
        }
    }

    private suspend fun applyTransactionBalance(transaction: TransactionEntity) {
        val account = repository.getAccountById(transaction.accountId)
        if (account != null) {
            val newBalance = when (transaction.type) {
                "expense" -> account.balance - transaction.amount
                "income" -> account.balance + transaction.amount
                "transfer" -> account.balance - transaction.amount
                else -> account.balance
            }
            repository.updateAccount(account.copy(balance = newBalance))
        }

        if (transaction.type == "transfer" && transaction.toAccountId != null) {
            val toAccount = repository.getAccountById(transaction.toAccountId)
            if (toAccount != null) {
                repository.updateAccount(toAccount.copy(balance = toAccount.balance + transaction.amount))
            }
        }
    }

    private suspend fun revertTransactionBalance(transaction: TransactionEntity) {
        val account = repository.getAccountById(transaction.accountId)
        if (account != null) {
            val newBalance = when (transaction.type) {
                "expense" -> account.balance + transaction.amount // Revert expense (add back)
                "income" -> account.balance - transaction.amount // Revert income (subtract)
                "transfer" -> account.balance + transaction.amount // Revert transfer source (add back)
                else -> account.balance
            }
            repository.updateAccount(account.copy(balance = newBalance))
        }

        if (transaction.type == "transfer" && transaction.toAccountId != null) {
            val toAccount = repository.getAccountById(transaction.toAccountId)
            if (toAccount != null) {
                repository.updateAccount(toAccount.copy(balance = toAccount.balance - transaction.amount)) // Revert transfer target (subtract)
            }
        }
    }
}
