package com.example.moneymanager.data.repository

import androidx.room.withTransaction
import com.example.moneymanager.data.local.AppDatabase
import com.example.moneymanager.data.local.dao.AccountDao
import com.example.moneymanager.data.local.dao.CategoryDao
import com.example.moneymanager.data.local.dao.TransactionDao
import com.example.moneymanager.data.local.dto.CategorySum
import com.example.moneymanager.data.local.entity.AccountEntity
import com.example.moneymanager.data.local.entity.CategoryEntity
import com.example.moneymanager.data.local.entity.TransactionEntity
import com.example.moneymanager.domain.repository.CsvImportRow
import com.example.moneymanager.domain.repository.CsvImportSummary
import com.example.moneymanager.domain.repository.MoneyRepository
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs
import javax.inject.Inject

class MoneyRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) : MoneyRepository {

    override fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    override suspend fun getAccountById(id: Int): AccountEntity? = accountDao.getAccountById(id)

    override suspend fun insertAccount(account: AccountEntity) = accountDao.insertAccount(account)
    
    override suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)

    override suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)

    override suspend fun getAccountByName(name: String): AccountEntity? = accountDao.getAccountByName(name)

    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType(type)
    
    override suspend fun insertCategory(category: CategoryEntity) = categoryDao.insertCategory(category)

    override suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)

    override suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    override suspend fun getCategoryByName(name: String): CategoryEntity? = categoryDao.getCategoryByName(name)

    override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> = 
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    override fun getTotalAmountByType(type: String, startDate: Long, endDate: Long): Flow<Double?> =
        transactionDao.getTotalAmountByType(type, startDate, endDate)

    override fun getTransactionCount(startDate: Long, endDate: Long): Flow<Int> =
        transactionDao.getTransactionCount(startDate, endDate)

    override suspend fun getTransactionCountByAccount(accountId: Int): Int =
        transactionDao.getTransactionCountByAccount(accountId)

    override fun getCategorySumsByType(type: String, startDate: Long, endDate: Long): Flow<List<CategorySum>> =
        transactionDao.getCategorySumsByType(type, startDate, endDate)

    override fun getTransactionsByCategory(categoryId: Int, startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByCategory(categoryId, startDate, endDate)

    override fun getNetChangeSince(startDate: Long): Flow<Double?> =
        transactionDao.getNetChangeSince(startDate)

    override suspend fun getTransactionById(id: Int): TransactionEntity? = transactionDao.getTransactionById(id)

    override suspend fun insertTransaction(transaction: TransactionEntity) = transactionDao.insertTransaction(transaction)

    override suspend fun updateTransaction(transaction: TransactionEntity) = transactionDao.updateTransaction(transaction)

    override suspend fun deleteTransaction(transaction: TransactionEntity) = transactionDao.deleteTransaction(transaction)

    override suspend fun clearAllData() {
        transactionDao.deleteAllTransactions()
        accountDao.resetAccountBalances()
    }

    override suspend fun importTransactionsAtomic(rows: List<CsvImportRow>): CsvImportSummary {
        if (rows.isEmpty()) {
            return CsvImportSummary(importedCount = 0, skippedCount = 0, skipReasons = emptyList())
        }

        return database.withTransaction {
            val accountCache = linkedMapOf<String, AccountEntity>()
            val categoryCache = linkedMapOf<String, CategoryEntity>()
            val skipReasons = mutableListOf<String>()
            var imported = 0
            var skipped = 0

            accountDao.getAllAccountsOnce().forEach { account ->
                accountCache.putIfAbsent(normalizeKey(account.name), account)
            }
            categoryDao.getAllCategoriesOnce().forEach { category ->
                categoryCache.putIfAbsent(categoryKey(category.type, category.name), category)
            }

            rows.forEach { row ->
                try {
                    val normalizedType = normalizeType(row.type)
                    if (normalizedType == null) {
                        skipped++
                        skipReasons.add("line ${row.lineNumber}: unsupported type '${row.type}'")
                        return@forEach
                    }

                    val normalizedAmount = abs(row.amount)
                    if (normalizedAmount == 0.0) {
                        skipped++
                        skipReasons.add("line ${row.lineNumber}: amount is zero")
                        return@forEach
                    }

                    val sourceAccount = resolveOrCreateAccount(row.accountName, accountCache)

                    val destinationAccount = if (normalizedType == "transfer") {
                        val toName = row.toAccountName?.trim().orEmpty()
                        if (toName.isEmpty()) {
                            skipped++
                            skipReasons.add("line ${row.lineNumber}: transfer missing destination account")
                            return@forEach
                        }
                        resolveOrCreateAccount(toName, accountCache)
                    } else {
                        null
                    }

                    if (normalizedType == "transfer" && destinationAccount != null && sourceAccount.id == destinationAccount.id) {
                        skipped++
                        skipReasons.add("line ${row.lineNumber}: transfer source and destination are the same account")
                        return@forEach
                    }

                    val categoryId = when (normalizedType) {
                        "income", "expense" -> {
                            val desiredCategoryName = row.categoryName?.trim().takeUnless { it.isNullOrEmpty() } ?: "Uncategorized"
                            resolveOrCreateCategory(desiredCategoryName, normalizedType, categoryCache).id
                        }
                        else -> null
                    }

                    val transaction = TransactionEntity(
                        amount = normalizedAmount,
                        date = row.dateTimeMillis,
                        time = row.dateTimeMillis,
                        note = row.note.trim().takeIf { it.isNotEmpty() },
                        type = normalizedType,
                        categoryId = categoryId,
                        accountId = sourceAccount.id,
                        toAccountId = destinationAccount?.id,
                        photoPath = null
                    )

                    transactionDao.insertTransaction(transaction)
                    applyBalanceImpact(sourceAccount.id, destinationAccount?.id, normalizedType, normalizedAmount)
                    imported++
                } catch (e: Exception) {
                    skipped++
                    val reason = e.message ?: "unknown error"
                    skipReasons.add("line ${row.lineNumber}: $reason")
                }
            }

            CsvImportSummary(
                importedCount = imported,
                skippedCount = skipped,
                skipReasons = skipReasons.take(20)
            )
        }
    }

    override suspend fun seedDefaultsAtomic(
        accounts: List<com.example.moneymanager.domain.repository.AccountSeed>,
        categories: List<com.example.moneymanager.domain.repository.CategorySeed>
    ) {
        database.withTransaction {
            accounts.forEach { seed ->
                val existing = accountDao.getAccountByNameAndType(seed.type, seed.name)
                if (existing == null) {
                    accountDao.insertAccount(
                        AccountEntity(
                            name = seed.name,
                            type = seed.type,
                            balance = seed.balance,
                            currency = seed.currency,
                            icon = seed.icon,
                            color = seed.color
                        )
                    )
                }
            }

            categories.forEach { seed ->
                val existing = categoryDao.getCategoryByNameAndType(seed.type, seed.name)
                if (existing == null) {
                    categoryDao.insertCategory(
                        CategoryEntity(
                            name = seed.name,
                            type = seed.type,
                            icon = seed.icon,
                            color = seed.color,
                            parentId = null
                        )
                    )
                }
            }
        }
    }

    private suspend fun applyBalanceImpact(accountId: Int, toAccountId: Int?, type: String, amount: Double) {
        val sourceAccount = accountDao.getAccountById(accountId)
            ?: throw IllegalStateException("Source account $accountId not found")

        when (type) {
            "income" -> accountDao.updateAccount(sourceAccount.copy(balance = sourceAccount.balance + amount))
            "expense" -> accountDao.updateAccount(sourceAccount.copy(balance = sourceAccount.balance - amount))
            "transfer" -> {
                accountDao.updateAccount(sourceAccount.copy(balance = sourceAccount.balance - amount))
                val destinationId = toAccountId ?: throw IllegalStateException("Transfer destination account missing")
                val destinationAccount = accountDao.getAccountById(destinationId)
                    ?: throw IllegalStateException("Destination account $destinationId not found")
                accountDao.updateAccount(destinationAccount.copy(balance = destinationAccount.balance + amount))
            }
        }
    }

    private suspend fun resolveOrCreateAccount(
        rawName: String,
        accountCache: MutableMap<String, AccountEntity>
    ): AccountEntity {
        val name = rawName.trim()
        if (name.isEmpty()) {
            throw IllegalArgumentException("account name is empty")
        }

        val key = normalizeKey(name)
        accountCache[key]?.let { return it }

        val existing = accountDao.getAccountByName(name)
        if (existing != null) {
            accountCache[key] = existing
            return existing
        }

        val created = AccountEntity(
            name = name,
            type = "cash",
            balance = 0.0,
            currency = "THB",
            icon = "wallet",
            color = android.graphics.Color.GRAY
        )
        accountDao.insertAccount(created)

        val inserted = accountDao.getAccountByName(name)
            ?: throw IllegalStateException("failed to create account '$name'")
        accountCache[key] = inserted
        return inserted
    }

    private suspend fun resolveOrCreateCategory(
        rawName: String,
        type: String,
        categoryCache: MutableMap<String, CategoryEntity>
    ): CategoryEntity {
        val name = rawName.trim()
        if (name.isEmpty()) {
            throw IllegalArgumentException("category name is empty")
        }

        val key = categoryKey(type, name)
        categoryCache[key]?.let { return it }

        val existing = categoryDao.getCategoryByNameAndType(type, name)
        if (existing != null) {
            categoryCache[key] = existing
            return existing
        }

        val created = CategoryEntity(
            name = name,
            type = type,
            icon = "help",
            color = android.graphics.Color.HSVToColor(floatArrayOf((Math.random() * 360).toFloat(), 0.6f, 0.9f)),
            parentId = null
        )
        categoryDao.insertCategory(created)

        val inserted = categoryDao.getCategoryByNameAndType(type, name)
            ?: throw IllegalStateException("failed to create category '$name' [$type]")
        categoryCache[key] = inserted
        return inserted
    }

    private fun normalizeType(type: String): String? {
        return when (type.trim().lowercase()) {
            "income" -> "income"
            "expense" -> "expense"
            "transfer" -> "transfer"
            else -> null
        }
    }

    private fun normalizeKey(name: String): String = name.trim().lowercase()

    private fun categoryKey(type: String, name: String): String = "${type.trim().lowercase()}|${normalizeKey(name)}"
}
