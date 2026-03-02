package com.example.moneymanager.ui.screens.accounts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.R
import com.example.moneymanager.data.local.entity.AccountEntity
import com.example.moneymanager.ui.theme.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.moneymanager.utils.IconUtils
import com.example.moneymanager.utils.formatMoneyCompact
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val accountList by viewModel.accounts.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    
    // Edit/Add States from ViewModel
    val newAccountName by viewModel.newAccountName.collectAsState()
    val newAccountBalance by viewModel.newAccountBalance.collectAsState()
    val selectedColor by viewModel.selectedColor.collectAsState()
    val selectedIcon by viewModel.selectedIcon.collectAsState()
    val selectedAccountType by viewModel.selectedAccountType.collectAsState()
    val takenColors by viewModel.takenColors.collectAsState()
    val showDeleteBlockedDialog by viewModel.showDeleteBlockedDialog.collectAsState()
    val linkedTransactionCount by viewModel.linkedTransactionCount.collectAsState()

    val sortedList = remember(accountList) { accountList.sortedByDescending { it.balance } }
    // Determine theme based on active background color, respecting app settings
    val isDark = MaterialTheme.colorScheme.background == DarkBackground

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAdjustmentDialog by remember { mutableStateOf(false) }
    
    // We need to know which account is being edited to exclude its own color from "taken" list
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var oldBalance by remember { mutableStateOf(0.0) }

    // Background Gradient (Hero area)
    val bgBrush = if (isDark) {
        Brush.verticalGradient(DarkTopGradientColors, startY = 0f, endY = 800f)
    } else {
        Brush.verticalGradient(LightTopGradientColors, startY = 0f, endY = 800f)
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0) 
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .background(bgBrush)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                AccountsHeader(onAddAccount = { 
                    viewModel.resetStateForAdd()
                    showAddDialog = true 
                })

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Donut Chart
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DonutChart(
                                accounts = sortedList,
                                totalValue = totalBalance,
                                isDark = isDark
                            )
                        }
                    }

                    // "My Accounts" Section Title
                    item {
                        Text(
                            text = stringResource(R.string.assets_section),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }

                    // List Items
                    items(items = sortedList, key = { account -> account.id }) { account ->
                        AccountRow(
                            account = account,
                            onClick = { 
                                editingAccount = account
                                oldBalance = account.balance
                                viewModel.prepareForEdit(account)
                                showEditDialog = true
                            },
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }

    // --- ADD DIALOG ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
                            title = { Text(stringResource(R.string.title_add_account)) },            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                OutlinedTextField(
                    value = newAccountName,
                    onValueChange = { viewModel.onNewAccountNameChanged(it) },
                    label = { Text(stringResource(R.string.label_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newAccountBalance,
                    onValueChange = { viewModel.onNewAccountBalanceChanged(it) },
                    label = { Text(stringResource(R.string.label_initial_balance)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                    
                    
                    AccountTypeDropdown(
                        selectedType = selectedAccountType,
                        onTypeSelected = viewModel::onAccountTypeSelected,
                        accountTypes = viewModel.accountTypes
                    )

                    Text(stringResource(R.string.select_icon), style = MaterialTheme.typography.labelMedium)
                    val accountColor = selectedColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                    // Reduce height to prevent dialog overflow on small screens
                    Box(modifier = Modifier.height(140.dp)) {
                        IconPicker(
                            selectedIcon = selectedIcon,
                            tint = accountColor,
                            onIconSelected = viewModel::onIconSelected
                        )
                    }

                    Text(stringResource(R.string.select_color), style = MaterialTheme.typography.labelMedium)
                    Box(modifier = Modifier.height(140.dp)) {
                        ColorPicker(
                            selectedColor = selectedColor,
                            takenColors = takenColors,
                            onColorSelected = viewModel::onColorSelected
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addAccount()
                        showAddDialog = false
                    }
                ) {
                    Text(stringResource(R.string.btn_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // --- EDIT DIALOG ---
    if (showEditDialog && editingAccount != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.title_edit_account)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newAccountName,
                        onValueChange = { viewModel.onNewAccountNameChanged(it) },
                        label = { Text(stringResource(R.string.label_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newAccountBalance,
                        onValueChange = { viewModel.onNewAccountBalanceChanged(it) },
                        label = { Text(stringResource(R.string.label_amount)) }, // Use label_amount for Balance
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    AccountTypeDropdown(
                        selectedType = selectedAccountType,
                        onTypeSelected = viewModel::onAccountTypeSelected,
                        accountTypes = viewModel.accountTypes
                    )

                    Text(stringResource(R.string.select_icon), style = MaterialTheme.typography.labelMedium)
                    val accountColor = selectedColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                    Box(modifier = Modifier.height(140.dp)) {
                        IconPicker(
                            selectedIcon = selectedIcon,
                            tint = accountColor,
                            onIconSelected = viewModel::onIconSelected
                        )
                    }

                    Text(stringResource(R.string.select_color), style = MaterialTheme.typography.labelMedium)
                    // When editing, the account's own color should NOT be considered "taken" (so we can keep it)
                    val currentTaken = remember(takenColors, editingAccount) {
                        takenColors.filter { it != editingAccount?.color }
                    }
                    Box(modifier = Modifier.height(140.dp)) {
                        ColorPicker(
                            selectedColor = selectedColor,
                            takenColors = currentTaken,
                            onColorSelected = viewModel::onColorSelected
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            editingAccount?.let { viewModel.deleteAccount(it) }
                            showEditDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.title_delete_account))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentEditingAccount = editingAccount
                        if (currentEditingAccount != null) {
                            val newBalanceVal = newAccountBalance.toDoubleOrNull() ?: 0.0
                            if (newBalanceVal != oldBalance) {
                                showEditDialog = false
                                showAdjustmentDialog = true
                            } else {
                                viewModel.updateAccount(currentEditingAccount)
                                showEditDialog = false
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
    // --- ADJUSTMENT CONFIRMATION DIALOG ---
    if (showAdjustmentDialog && editingAccount != null) {
        AlertDialog(
            onDismissRequest = { 
                // If dismissed, just update without transaction? Or cancel update?
                // Let's assume cancel update to be safe, or maybe just update. 
                // User clicked "Save" already. So let's default to "No, Just Update" behavior if dismissed?
                // Better to force choice. But for dismissal, let's just update.
                viewModel.updateAccount(editingAccount!!, false, oldBalance)
                showAdjustmentDialog = false 
            },
            title = { Text(stringResource(R.string.dialog_adjustment_title)) },
            text = { Text(stringResource(R.string.dialog_adjustment_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateAccount(editingAccount!!, true, oldBalance)
                        showAdjustmentDialog = false
                    }
                ) {
                    Text(stringResource(R.string.btn_record_transaction))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.updateAccount(editingAccount!!, false, oldBalance)
                        showAdjustmentDialog = false
                    }
                ) {
                    Text(stringResource(R.string.btn_update_only))
                }
            }
        )
    }

    if (showDeleteBlockedDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteBlockedDialog,
            title = { Text(stringResource(R.string.account_delete_blocked_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.account_delete_blocked_message,
                        linkedTransactionCount
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDeleteBlockedDialog) {
                    Text(stringResource(R.string.common_ok))
                }
            }
        )
    }
}

@Composable
fun ColorPicker(
    selectedColor: Int?,
    takenColors: List<Int>,
    onColorSelected: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 48.dp), // Slightly larger touch target
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize() // Use parent size
    ) {
        items(items = AccountColors, key = { color -> color.toArgb() }) { color ->
            val colorInt = color.toArgb()
            val isTaken = takenColors.contains(colorInt)
            val isSelected = selectedColor == colorInt
            
            Box(
                modifier = Modifier
                    .size(48.dp) // Fixed size
                    .clip(CircleShape)
                    .background(color)
                    .alpha(if (isTaken && !isSelected) 0.3f else 1f) // Fade if taken
                    .clickable(enabled = !isTaken) { onColorSelected(colorInt) }
                    .then(
                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isTaken) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Taken",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountsHeader(onAddAccount: () -> Unit) {
    // Top Row: Title + Add Button
    // Padded for Status Bar here only
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding() // The ONE source of top padding
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.nav_accounts),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Pill Button
        Surface(
            onClick = onAddAccount,
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant, // Pill background
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.title_add_account),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_add),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DonutChart(
    accounts: List<AccountEntity>,
    totalValue: Double,
    isDark: Boolean
) {
    val chartSize = 260.dp
    val strokeWidth = 30.dp
    
    // Default colors if account has no color (fallback)
    val fallbackColors = remember { AccountColors }

    // Track Color (Low opacity)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(chartSize)
    ) {
        Canvas(modifier = Modifier.size(chartSize - strokeWidth)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
            
            // 1. Background Ring
            drawCircle(
                color = trackColor,
                style = stroke
            )

            // 2. Data Segments
            if (totalValue > 0) {
                var startAngle = -90f
                accounts.forEachIndexed { index, account ->
                    val sweepAngle = ((account.balance / totalValue) * 360f).toFloat()
                    
                    // Use Account Color OR Fallback
                    val color = if (account.color != null) {
                        Color(account.color)
                    } else {
                        fallbackColors[index % fallbackColors.size]
                    }
                    
                    if (sweepAngle > 0) {
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = stroke
                        )
                        startAngle += sweepAngle
                    }
                }
            }
        }

        // Center Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Net Worth Pill
            Surface(
                color = if(isDark) Color(0xFF1E293B) else Color(0xFFE0E7FF),
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.net_worth),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            
            // Amount
            Text(
                text = "฿" + formatMoneyCompact(totalValue, Locale.getDefault()),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun AccountRow(
    account: AccountEntity,
    onClick: () -> Unit,
    isDark: Boolean
) {
    // Card Specs: 22dp radius, white/glassy
    val shape = RoundedCornerShape(22.dp)
    val cardColor = if (isDark) DarkCardGlass else LightSurface
    val shadowColor = if (isDark) DarkShadow else LightShadow
    
    // Visual Dot - USE ACCOUNT COLOR if available
    val dotColor = if (account.color != null) {
        Color(account.color)
    } else {
        // Fallback logic
        when(account.type.lowercase()) {
            "bank" -> BrandPurple
            "cash" -> SuccessGreen
            else -> BrandBlue
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(
                elevation = if(isDark) 0.dp else 10.dp,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .background(cardColor, shape)
            .then(
                if (isDark) Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), shape) 
                else Modifier
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Icon + Text
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(dotColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .semantics(mergeDescendants = true) { contentDescription = account.icon ?: "" },
                    contentAlignment = Alignment.Center
                ) {
                    val iconVector = IconUtils.getIconByName(account.icon)
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = dotColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when(account.type) {
                            "cash" -> stringResource(R.string.type_cash)
                            "bank" -> stringResource(R.string.type_bank)
                            "credit_card" -> stringResource(R.string.type_credit_card)
                            "e_wallet" -> stringResource(R.string.type_e_wallet)
                            "investment" -> stringResource(R.string.type_investment)
                            "digital_asset" -> stringResource(R.string.type_digital_asset)
                            "loan" -> stringResource(R.string.type_loan)
                            "others" -> stringResource(R.string.type_others)
                            else -> account.type.replaceFirstChar { it.uppercase() }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: Amount
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "\u0E3F" + formatMoneyCompact(account.balance, Locale.getDefault()),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTypeDropdown(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    accountTypes: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = when(selectedType) {
                "cash" -> stringResource(R.string.type_cash)
                "bank" -> stringResource(R.string.type_bank)
                "credit_card" -> stringResource(R.string.type_credit_card)
                "e_wallet" -> stringResource(R.string.type_e_wallet)
                "investment" -> stringResource(R.string.type_investment)
                "digital_asset" -> stringResource(R.string.type_digital_asset)
                "loan" -> stringResource(R.string.type_loan)
                "others" -> stringResource(R.string.type_others)
                else -> selectedType
            },
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.label_account_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accountTypes.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(text = when(type) {
                            "cash" -> stringResource(R.string.type_cash)
                            "bank" -> stringResource(R.string.type_bank)
                            "credit_card" -> stringResource(R.string.type_credit_card)
                            "e_wallet" -> stringResource(R.string.type_e_wallet)
                            "investment" -> stringResource(R.string.type_investment)
                            "digital_asset" -> stringResource(R.string.type_digital_asset)
                            "loan" -> stringResource(R.string.type_loan)
                            "others" -> stringResource(R.string.type_others)
                            else -> type
                        })
                    },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun IconPicker(
    selectedIcon: String?,
    tint: Color,
    onIconSelected: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize() // Use parent size
    ) {
        items(items = IconUtils.accountIcons, key = { (name, _) -> name }) { (name, iconVector) ->
            val isSelected = selectedIcon == name
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) tint.copy(alpha = 0.1f) else Color.Transparent)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp, 
                        color = if (isSelected) tint else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onIconSelected(name) }
                    .semantics(mergeDescendants = true) { contentDescription = name },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = if (isSelected) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
