package com.example.moneymanager.domain.repository

import com.example.moneymanager.data.local.dto.CategorySum
import com.example.moneymanager.data.local.entity.AccountEntity
import com.example.moneymanager.data.local.entity.CategoryEntity
import com.example.moneymanager.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class CsvImportRow(
    val lineNumber: Int,
    val amount: Double,
    val dateTimeMillis: Long,
    val type: String, // income, expense, transfer
    val accountName: String,
    val toAccountName: String?,
    val categoryName: String?,
    val note: String
)

data class CsvImportSummary(
    val importedCount: Int,
    val skippedCount: Int,
    val skipReasons: List<String>
)

data class AccountSeed(
    val name: String,
    val type: String,
    val balance: Double = 0.0,
    val currency: String = "THB",
    val icon: String = "wallet",
    val color: Int = android.graphics.Color.GRAY
)

data class CategorySeed(
    val name: String,
    val type: String,
    val icon: String,
    val color: Int
)

interface MoneyRepository {
    // Accounts
    fun getAllAccounts(): Flow<List<AccountEntity>>
    suspend fun getAccountById(id: Int): AccountEntity?
    suspend fun insertAccount(account: AccountEntity)
    suspend fun updateAccount(account: AccountEntity)
    suspend fun deleteAccount(account: AccountEntity)
    suspend fun getAccountByName(name: String): AccountEntity?

    // Categories
    fun getAllCategories(): Flow<List<CategoryEntity>>
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>
    suspend fun insertCategory(category: CategoryEntity)
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity)
    suspend fun getCategoryByName(name: String): CategoryEntity?

    // Transactions
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    fun getTotalAmountByType(type: String, startDate: Long, endDate: Long): Flow<Double?>
    fun getTransactionCount(startDate: Long, endDate: Long): Flow<Int>
    suspend fun getTransactionCountByAccount(accountId: Int): Int
    fun getCategorySumsByType(type: String, startDate: Long, endDate: Long): Flow<List<CategorySum>>
    fun getTransactionsByCategory(categoryId: Int, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    fun getNetChangeSince(startDate: Long): Flow<Double?>
    suspend fun getTransactionById(id: Int): TransactionEntity?
    suspend fun insertTransaction(transaction: TransactionEntity)
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)

    // Maintenance
    suspend fun clearAllData()
    suspend fun importTransactionsAtomic(rows: List<CsvImportRow>): CsvImportSummary
    suspend fun seedDefaultsAtomic(accounts: List<AccountSeed>, categories: List<CategorySeed>)
}
