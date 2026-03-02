package com.example.moneymanager.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.FileProvider
import com.example.moneymanager.domain.repository.CsvImportRow
import com.example.moneymanager.domain.repository.MoneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: MoneyRepository,
    private val preferenceManager: com.example.moneymanager.data.local.PreferenceManager
) : ViewModel() {

    // --- Codex UI State ---
    sealed interface CsvImportUiState {
        data object Idle : CsvImportUiState
        data object Loading : CsvImportUiState
        data class Success(val result: ImportResult) : CsvImportUiState // Modified to include full result
        data class Error(val message: String) : CsvImportUiState
    }

    private val _csvImportUiState = MutableStateFlow<CsvImportUiState>(CsvImportUiState.Idle)
    val csvImportUiState: StateFlow<CsvImportUiState> = _csvImportUiState.asStateFlow()

    // --- Existing Settings State ---
    val isPasscodeEnabled = preferenceManager.isPasscodeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentLanguage = preferenceManager.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val currentTheme = preferenceManager.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            preferenceManager.setLanguage(lang)
        }
    }

    fun setTheme(theme: Int) {
        viewModelScope.launch {
            preferenceManager.setTheme(theme)
        }
    }

    fun resetData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
        }
    }

    fun setPasscode(passcode: String) {
        viewModelScope.launch {
            preferenceManager.setPasscode(passcode)
            preferenceManager.setPasscodeEnabled(true)
        }
    }

    fun disablePasscode() {
        viewModelScope.launch {
            preferenceManager.setPasscodeEnabled(false)
        }
    }

    // --- CSV Export (Optimized) ---
    fun exportToCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val transactions = repository.getAllTransactions().first()
            val accounts = repository.getAllAccounts().first().associateBy { it.id }
            val categories = repository.getAllCategories().first().associateBy { it.id }

            val csvHeader = "Date,Time,Type,Amount,Account,ToAccount,Category,Note\n"
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
            val uncategorizedLabel = "Uncategorized"
            val unknownAccountLabel = "Unknown"

            val csvContent = StringBuilder(csvHeader)

            transactions.forEach { trans ->
                val dateStr = dateFormat.format(Date(trans.date))
                val timeStr = timeFormat.format(Date(trans.time))
                val type = trans.type
                val amount = trans.amount
                val note = escapeCsv(trans.note ?: "")

                val accName = escapeCsv(accounts[trans.accountId]?.name ?: unknownAccountLabel)
                val toAccName = trans.toAccountId?.let {
                    escapeCsv(accounts[it]?.name ?: unknownAccountLabel)
                } ?: ""
                val catName = trans.categoryId?.let { id ->
                    escapeCsv(categories[id]?.name ?: uncategorizedLabel)
                } ?: ""

                csvContent.append("$dateStr,$timeStr,$type,$amount,$accName,$toAccName,$catName,$note\n")
            }

            val fileName = "transactions_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            file.writeText(csvContent.toString())
            shareFile(context, file)
        }
    }

    private fun escapeCsv(value: String): String {
        var escaped = value.replace("\"", "\"\"")
        if (escaped.contains(",") || escaped.contains("\n")) {
            escaped = "\"$escaped\""
        }
        return escaped
    }

    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export CSV"))
    }


    // --- CSV Import ---
    data class ImportResult(
        val importedCount: Int,
        val skippedCount: Int,
        val skippedReasons: List<String>
    )

    fun importFromCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _csvImportUiState.value = CsvImportUiState.Loading
            try {
                val result = performImport(context, uri)
                _csvImportUiState.value = CsvImportUiState.Success(result)
            } catch (e: Exception) {
                _csvImportUiState.value = CsvImportUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    fun clearImportState() {
        _csvImportUiState.value = CsvImportUiState.Idle
    }

    private suspend fun performImport(context: Context, uri: Uri): ImportResult {
        val skippedReasons = mutableListOf<String>()
        val importRows = mutableListOf<CsvImportRow>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream))

            val headerLine = reader.readLine()
            if (headerLine == null) {
                return ImportResult(0, 0, listOf("File is empty or unreadable"))
            }

            val expectedHeaders = listOf("date", "time", "type", "amount", "account", "toaccount", "category", "note")
            val actualHeaders = headerLine.split(',').map { it.trim().lowercase() }
            if (actualHeaders.take(expectedHeaders.size) != expectedHeaders) {
                return ImportResult(
                    importedCount = 0,
                    skippedCount = 0,
                    skippedReasons = listOf("Invalid CSV header. Expected: ${expectedHeaders.joinToString(",")}")
                )
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
            var lineNumber = 1

            while (true) {
                val line = reader.readLine() ?: break
                lineNumber += 1
                if (line.isBlank()) continue

                try {
                    val row = parseCsvLine(line)
                    if (row.size < 5) {
                        skippedReasons.add("line $lineNumber: missing required columns")
                        continue
                    }

                    val dateStr = row.getOrElse(0) { "" }.trim()
                    val timeStr = row.getOrElse(1) { "" }.trim()
                    val typeStr = row.getOrElse(2) { "" }.trim().lowercase()
                    val amountStr = row.getOrElse(3) { "" }.trim()
                    val accountName = row.getOrElse(4) { "" }.trim()
                    val toAccountName = row.getOrElse(5) { "" }.trim()
                    val categoryName = row.getOrElse(6) { "" }.trim()
                    val note = row.getOrElse(7) { "" }.trim()

                    if (accountName.isEmpty()) {
                        skippedReasons.add("line $lineNumber: account is empty")
                        continue
                    }

                    val rawAmount = amountStr.toDoubleOrNull()
                    if (rawAmount == null || rawAmount == 0.0) {
                        skippedReasons.add("line $lineNumber: invalid amount '$amountStr'")
                        continue
                    }

                    val normalizedType = when {
                        typeStr.contains("income") -> "income"
                        typeStr.contains("expense") -> "expense"
                        typeStr.contains("transfer") -> "transfer"
                        else -> if (rawAmount > 0) "income" else "expense"
                    }

                    if (normalizedType == "transfer" && toAccountName.isEmpty()) {
                        skippedReasons.add("line $lineNumber: transfer missing destination account")
                        continue
                    }

                    val date = dateFormat.parse(dateStr) ?: Date()
                    val time = timeFormat.parse(timeStr) ?: date
                    val combinedDateTime = Date(date.time + (time.time % (24 * 60 * 60 * 1000)))

                    importRows.add(
                        CsvImportRow(
                            lineNumber = lineNumber,
                            amount = rawAmount,
                            dateTimeMillis = combinedDateTime.time,
                            type = normalizedType,
                            accountName = accountName,
                            toAccountName = toAccountName.ifEmpty { null },
                            categoryName = categoryName.ifEmpty { null },
                            note = note
                        )
                    )
                } catch (e: Exception) {
                    skippedReasons.add("line $lineNumber: ${e.message ?: "parse error"}")
                }
            }
        } ?: return ImportResult(0, 0, listOf("Unable to open selected file"))

        val repositorySummary = repository.importTransactionsAtomic(importRows)
        val allReasons = (skippedReasons + repositorySummary.skipReasons).take(20)
        return ImportResult(
            importedCount = repositorySummary.importedCount,
            skippedCount = skippedReasons.size + repositorySummary.skippedCount,
            skippedReasons = allReasons
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        if (line.isBlank()) return tokens

        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        // This is an escaped quote "" inside a quoted field
                        sb.append('"')
                        i++ // Skip the next quote
                    } else {
                        // This is the start or end of a quoted field
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    tokens.add(sb.toString())
                    sb.setLength(0)
                }
                else -> sb.append(char)
            }
            i++
        }
        tokens.add(sb.toString()) // Add the last token
        return tokens
    }
}
