package com.example.moneymanager.utils

import androidx.compose.ui.graphics.toArgb
import com.example.moneymanager.data.local.PreferenceManager
import com.example.moneymanager.data.local.entity.AccountEntity
import com.example.moneymanager.data.local.entity.CategoryEntity
import com.example.moneymanager.domain.repository.CsvImportRow
import com.example.moneymanager.domain.repository.MoneyRepository
import com.example.moneymanager.ui.theme.AccountColors
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date
import java.util.Random

object DebugDataSeeder {
    private const val SEED_VERSION = 1
    private const val DAYS_BACK = 90
    private const val MIN_TRANSACTION_COUNT = 120

    suspend fun seedIfNeeded(
        repository: MoneyRepository,
        preferenceManager: PreferenceManager
    ) {
        val currentSeedVersion = preferenceManager.debugSeedVersion.first()
        if (currentSeedVersion >= SEED_VERSION) return

        val hasAccounts = repository.getAllAccounts().first().isNotEmpty()
        val hasTransactions = repository.getAllTransactions().first().isNotEmpty()
        if (hasAccounts || hasTransactions) {
            preferenceManager.setDebugSeedVersion(SEED_VERSION)
            return
        }

        seedCategories(repository)
        seedAccounts(repository)

        val rows = buildSeedRows()
        repository.importTransactionsAtomic(rows)

        preferenceManager.setDebugSeedVersion(SEED_VERSION)
    }

    private suspend fun seedAccounts(repository: MoneyRepository) {
        val accounts = listOf(
            AccountEntity(name = "Cash Wallet", type = "cash", balance = 0.0, icon = "payments", color = AccountColors[9].toArgb()),
            AccountEntity(name = "Main Bank", type = "bank", balance = 0.0, icon = "account_balance", color = AccountColors[5].toArgb()),
            AccountEntity(name = "Everyday E-Wallet", type = "e_wallet", balance = 0.0, icon = "account_balance_wallet", color = AccountColors[1].toArgb()),
            AccountEntity(name = "Credit Card", type = "credit_card", balance = 0.0, icon = "credit_card", color = AccountColors[8].toArgb()),
            AccountEntity(name = "Savings Account", type = "savings", balance = 0.0, icon = "savings", color = AccountColors[3].toArgb()),
            AccountEntity(name = "Investment Fund", type = "investment", balance = 0.0, icon = "trending_up", color = AccountColors[0].toArgb())
        )
        accounts.forEach { repository.insertAccount(it) }
    }

    private suspend fun seedCategories(repository: MoneyRepository) {
        val incomeCategories = listOf("Salary", "Freelance", "Gift", "Interest")
        val expenseCategories = listOf(
            "Food", "Transport", "Coffee", "Shopping",
            "Bills", "Entertainment", "Health", "Education"
        )

        incomeCategories.forEach {
            repository.insertCategory(CategoryEntity(name = it, type = "income", icon = "attach_money"))
        }
        expenseCategories.forEach {
            repository.insertCategory(CategoryEntity(name = it, type = "expense", icon = "category"))
        }
    }

    private fun buildSeedRows(): List<CsvImportRow> {
        val random = Random(20260216L)
        val rows = mutableListOf<CsvImportRow>()
        var line = 1

        val incomeAccounts = listOf("Main Bank", "Savings Account")
        val spendAccounts = listOf("Cash Wallet", "Main Bank", "Everyday E-Wallet", "Credit Card")
        val expenseCategories = listOf("Food", "Transport", "Coffee", "Shopping", "Bills", "Entertainment", "Health", "Education")
        val expenseNotes = listOf(
            "Lunch", "Dinner with friends", "Coffee break", "Supermarket",
            "Taxi fare", "Monthly subscription", "Medicine", "Online course"
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (offset in DAYS_BACK downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -offset)
            val dayStartMillis = calendar.timeInMillis
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

            if (dayOfMonth == 1 || dayOfMonth == 28) {
                rows += CsvImportRow(
                    lineNumber = line++,
                    amount = 48000.0,
                    dateTimeMillis = dayStartMillis + randomTimeOffset(random),
                    type = "income",
                    accountName = "Main Bank",
                    toAccountName = null,
                    categoryName = "Salary",
                    note = "Monthly salary"
                )
            }

            if (dayOfMonth == 5) {
                rows += CsvImportRow(
                    lineNumber = line++,
                    amount = 12000.0,
                    dateTimeMillis = dayStartMillis + randomTimeOffset(random),
                    type = "expense",
                    accountName = "Main Bank",
                    toAccountName = null,
                    categoryName = "Bills",
                    note = "Rent"
                )
            }
            if (dayOfMonth == 10) {
                rows += CsvImportRow(
                    lineNumber = line++,
                    amount = 899.0,
                    dateTimeMillis = dayStartMillis + randomTimeOffset(random),
                    type = "expense",
                    accountName = "Main Bank",
                    toAccountName = null,
                    categoryName = "Bills",
                    note = "Internet & utilities"
                )
            }

            val dailyCount = when (random.nextInt(100)) {
                in 0..34 -> 0
                in 35..69 -> 1
                in 70..89 -> 2
                else -> 3
            }

            repeat(dailyCount) {
                val category = expenseCategories[random.nextInt(expenseCategories.size)]
                val amount = when (category) {
                    "Food" -> 60 + random.nextInt(220)
                    "Transport" -> 30 + random.nextInt(120)
                    "Coffee" -> 45 + random.nextInt(90)
                    "Shopping" -> 150 + random.nextInt(1200)
                    "Bills" -> 200 + random.nextInt(1500)
                    "Entertainment" -> 120 + random.nextInt(800)
                    "Health" -> 100 + random.nextInt(1400)
                    else -> 120 + random.nextInt(1800)
                }.toDouble()

                rows += CsvImportRow(
                    lineNumber = line++,
                    amount = amount,
                    dateTimeMillis = dayStartMillis + randomTimeOffset(random),
                    type = "expense",
                    accountName = spendAccounts[random.nextInt(spendAccounts.size)],
                    toAccountName = null,
                    categoryName = category,
                    note = if (random.nextBoolean()) expenseNotes[random.nextInt(expenseNotes.size)] else ""
                )
            }

            if (offset % 11 == 0) {
                rows += CsvImportRow(
                    lineNumber = line++,
                    amount = (900 + random.nextInt(5200)).toDouble(),
                    dateTimeMillis = dayStartMillis + randomTimeOffset(random),
                    type = "income",
                    accountName = incomeAccounts[random.nextInt(incomeAccounts.size)],
                    toAccountName = null,
                    categoryName = if (random.nextBoolean()) "Freelance" else "Gift",
                    note = if (random.nextBoolean()) "Side project payout" else ""
                )
            }

            if (offset % 14 == 0) {
                rows += CsvImportRow(
                    lineNumber = line++,
                    amount = (800 + random.nextInt(2800)).toDouble(),
                    dateTimeMillis = dayStartMillis + randomTimeOffset(random),
                    type = "transfer",
                    accountName = "Cash Wallet",
                    toAccountName = "Main Bank",
                    categoryName = null,
                    note = "Cash deposit"
                )
            }

            if (offset % 21 == 0) {
                rows += CsvImportRow(
                    lineNumber = line++,
                    amount = (1500 + random.nextInt(4500)).toDouble(),
                    dateTimeMillis = dayStartMillis + randomTimeOffset(random),
                    type = "transfer",
                    accountName = "Main Bank",
                    toAccountName = "Savings Account",
                    categoryName = null,
                    note = "Save for emergency fund"
                )
            }

            if (dayOfMonth == 18) {
                rows += CsvImportRow(
                    lineNumber = line++,
                    amount = (2000 + random.nextInt(7000)).toDouble(),
                    dateTimeMillis = dayStartMillis + randomTimeOffset(random),
                    type = "transfer",
                    accountName = "Main Bank",
                    toAccountName = "Investment Fund",
                    categoryName = null,
                    note = "Monthly investment contribution"
                )
            }

            if (dayOfMonth == 25) {
                rows += CsvImportRow(
                    lineNumber = line++,
                    amount = (120 + random.nextInt(380)).toDouble(),
                    dateTimeMillis = dayStartMillis + randomTimeOffset(random),
                    type = "income",
                    accountName = "Savings Account",
                    toAccountName = null,
                    categoryName = "Interest",
                    note = "Savings interest"
                )
            }
        }

        while (rows.size < MIN_TRANSACTION_COUNT) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -random.nextInt(DAYS_BACK))
            val baseMillis = calendar.timeInMillis
            rows += CsvImportRow(
                lineNumber = line++,
                amount = (55 + random.nextInt(120)).toDouble(),
                dateTimeMillis = baseMillis + randomTimeOffset(random),
                type = "expense",
                accountName = "Everyday E-Wallet",
                toAccountName = null,
                categoryName = "Coffee",
                note = "Top-up snack run"
            )
        }

        return rows.sortedBy { it.dateTimeMillis }
    }

    private fun randomTimeOffset(random: Random): Long {
        val hour = 7 + random.nextInt(15) // 07:00 - 21:59
        val minute = random.nextInt(60)
        return ((hour * 60L) + minute) * 60_000L
    }
}
