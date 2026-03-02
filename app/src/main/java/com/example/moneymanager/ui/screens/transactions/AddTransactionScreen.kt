package com.example.moneymanager.ui.screens.transactions

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.ui.components.SelectionSheet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.ui.res.stringResource
import com.example.moneymanager.R

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val transactionType by viewModel.transactionType.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val note by viewModel.note.collectAsState()
    val date by viewModel.date.collectAsState()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsState()

    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val selectedToAccountId by viewModel.selectedToAccountId.collectAsState()
    val isEditMode = viewModel.isEditMode

    var showAccountSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showToAccountSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    calendar.time = date

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newDate = Calendar.getInstance()
            newDate.set(year, month, dayOfMonth)
            viewModel.onDateChanged(newDate.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val tabs = listOf(stringResource(R.string.label_income), stringResource(R.string.label_expense), stringResource(R.string.label_transfer))
    val selectedTabIndex = when (transactionType) {
        "income" -> 0
        "expense" -> 1
        "transfer" -> 2
        else -> 1
    }

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_transaction_title)) },
            text = { Text(stringResource(R.string.delete_transaction_confirm)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.deleteTransaction(onSuccess = onBackClick)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    val typeName = when (transactionType) {
        "income" -> stringResource(R.string.label_income)
        "expense" -> stringResource(R.string.label_expense)
        "transfer" -> stringResource(R.string.label_transfer)
        else -> ""
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isEditMode) 
                            stringResource(R.string.title_edit_transaction) + " ($typeName)"
                        else 
                            typeName
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.btn_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.saveTransaction(onSuccess = onBackClick) },
                        enabled = isSaveEnabled
                    ) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save_cd))
                    }
                },
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TabRow removed per user request as type is selected before entering the screen

            // Date Picker
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                onClick = { datePickerDialog.show() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.date_cd))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDate(date),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Account & Category Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Source Account
                val selectedAccount = accounts.find { it.id == selectedAccountId }
                val accountName = selectedAccount?.name ?: stringResource(R.string.select_account)
                OutlinedButton(
                    onClick = { showAccountSheet = true },
                    modifier = Modifier.weight(1f),
                    colors = if (selectedAccountId == null) ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.outlinedButtonColors()
                ) {
                    if (selectedAccount != null) {
                        Icon(
                            imageVector = com.example.moneymanager.utils.IconUtils.getIconByName(selectedAccount.icon),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp).padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = if (selectedTabIndex == 2) stringResource(R.string.from_account_prefix, accountName) else accountName,
                        maxLines = 1
                    )
                }

                if (selectedTabIndex == 2) {
                    // Target Account (Transfer)
                    val selectedToAccount = accounts.find { it.id == selectedToAccountId }
                    val toAccountName = selectedToAccount?.name ?: stringResource(R.string.to_account_default)
                    OutlinedButton(
                        onClick = { showToAccountSheet = true },
                        modifier = Modifier.weight(1f),
                        colors = if (selectedToAccountId == null) ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        if (selectedToAccount != null) {
                            Icon(
                                imageVector = com.example.moneymanager.utils.IconUtils.getIconByName(selectedToAccount.icon),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp).padding(end = 4.dp)
                            )
                        }
                        Text(toAccountName, maxLines = 1)
                    }
                } else {
                    // Category
                    val selectedCategory = categories.find { it.id == selectedCategoryId }
                    val categoryName = selectedCategory?.name ?: stringResource(R.string.select_category)
                    OutlinedButton(
                        onClick = { showCategorySheet = true },
                        modifier = Modifier.weight(1f),
                        colors = if (selectedCategoryId == null) ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        if (selectedCategory != null) {
                            Icon(
                                imageVector = com.example.moneymanager.utils.IconUtils.getIconByName(selectedCategory.icon),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp).padding(end = 4.dp)
                            )
                        }
                        Text(categoryName, maxLines = 1)
                    }
                }
            }

            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = viewModel::onAmountChanged,
                label = { Text(stringResource(R.string.label_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineMedium
            )

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = viewModel::onNoteChanged,
                label = { Text(stringResource(R.string.label_note)) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Bottom Sheets
        if (showAccountSheet) {
            SelectionSheet(
                title = stringResource(R.string.select_account),
                items = accounts,
                selectedItem = accounts.find { it.id == selectedAccountId },
                keySelector = { account -> account.id },
                onItemSelected = { viewModel.onAccountSelected(it.id) },
                onDismissRequest = { showAccountSheet = false },
                itemContent = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = com.example.moneymanager.utils.IconUtils.getIconByName(it.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(it.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            )
        }

        if (showToAccountSheet) {
            SelectionSheet(
                title = stringResource(R.string.select_target_account),
                items = accounts.filter { it.id != selectedAccountId }, // Prevent same account transfer
                selectedItem = accounts.find { it.id == selectedToAccountId },
                keySelector = { account -> account.id },
                onItemSelected = { viewModel.onToAccountSelected(it.id) },
                onDismissRequest = { showToAccountSheet = false },
                itemContent = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = com.example.moneymanager.utils.IconUtils.getIconByName(it.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(it.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            )
        }

        if (showCategorySheet) {
            SelectionSheet(
                title = stringResource(R.string.select_category),
                items = categories,
                selectedItem = categories.find { it.id == selectedCategoryId },
                keySelector = { category -> category.id },
                onItemSelected = { viewModel.onCategorySelected(it.id) },
                onDismissRequest = { showCategorySheet = false },
                itemContent = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = com.example.moneymanager.utils.IconUtils.getIconByName(it.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(it.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            )
        }
    }
}


private val DATE_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.getDefault())

fun formatDate(date: Date): String {
    val localDate = date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    return localDate.format(DATE_FORMATTER)
}
