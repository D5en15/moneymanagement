package com.example.moneymanager.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.entity.CategoryEntity
import com.example.moneymanager.domain.repository.MoneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageCategoriesViewModel @Inject constructor(
    private val repository: MoneyRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0) // 0: Income, 1: Expense
    val selectedTab: StateFlow<Int> = _selectedTab

    val categories: StateFlow<List<CategoryEntity>> = combine(_selectedTab, repository.getAllCategories()) { tab, all ->
        val type = if (tab == 0) "income" else "expense"
        all.filter { it.type == type }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // For adding new category
    private val _newCategoryName = MutableStateFlow("")
    val newCategoryName: StateFlow<String> = _newCategoryName

    private val _newCategoryIcon = MutableStateFlow("category") // Default icon
    val newCategoryIcon: StateFlow<String> = _newCategoryIcon

    fun onTabSelected(index: Int) {
        _selectedTab.value = index
    }

    fun onNewCategoryNameChanged(name: String) {
        _newCategoryName.value = name
    }

    fun addCategory() {
        if (_newCategoryName.value.isBlank()) return
        
        viewModelScope.launch {
            val type = if (_selectedTab.value == 0) "income" else "expense"
            val newCategory = CategoryEntity(
                name = _newCategoryName.value,
                type = type,
                icon = _newCategoryIcon.value
            )
            repository.insertCategory(newCategory)
            _newCategoryName.value = "" // Reset input
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }
}
