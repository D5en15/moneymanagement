package com.example.moneymanager.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.entity.AccountEntity
import com.example.moneymanager.domain.repository.MoneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageAccountsViewModel @Inject constructor(
    private val repository: MoneyRepository
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _newAccountName = MutableStateFlow("")
    val newAccountName: StateFlow<String> = _newAccountName

    private val _newAccountBalance = MutableStateFlow("")
    val newAccountBalance: StateFlow<String> = _newAccountBalance

    fun onNewAccountNameChanged(name: String) {
        _newAccountName.value = name
    }

    fun onNewAccountBalanceChanged(balance: String) {
        _newAccountBalance.value = balance
    }

    fun addAccount() {
        if (_newAccountName.value.isBlank()) return
        val balance = _newAccountBalance.value.toDoubleOrNull() ?: 0.0
        
        viewModelScope.launch {
            val newAccount = AccountEntity(
                name = _newAccountName.value,
                type = "cash", // Default for now
                balance = balance,
                icon = "account_balance_wallet"
            )
            repository.insertAccount(newAccount)
            _newAccountName.value = ""
            _newAccountBalance.value = ""
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }
}
