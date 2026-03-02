package com.example.moneymanager.utils

import com.example.moneymanager.data.local.entity.AccountEntity
import com.example.moneymanager.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

import androidx.compose.ui.graphics.toArgb
import com.example.moneymanager.ui.theme.AccountColors

object DummyData {
    fun populateDatabase(
        insertAccount: suspend (AccountEntity) -> Unit,
        insertCategory: suspend (CategoryEntity) -> Unit
    ): Flow<Boolean> = flow {
        // Categories
        val categories = listOf(
            CategoryEntity(name = "Food", type = "expense", icon = "restaurant"),
            CategoryEntity(name = "Transport", type = "expense", icon = "directions_bus"),
            CategoryEntity(name = "Salary", type = "income", icon = "attach_money"),
            CategoryEntity(name = "Others", type = "expense", icon = "more_horiz"),
            CategoryEntity(name = "Others", type = "income", icon = "more_horiz")
        )
        categories.forEach { insertCategory(it) }

        // Accounts
        val accounts = listOf(
            AccountEntity(name = "Cash", type = "cash", balance = 0.0, icon = "payments", color = AccountColors[9].toArgb()),
            AccountEntity(name = "Bank", type = "bank", balance = 0.0, icon = "account_balance", color = AccountColors[5].toArgb())
        )
        accounts.forEach { insertAccount(it) }

        emit(true)
    }
}
