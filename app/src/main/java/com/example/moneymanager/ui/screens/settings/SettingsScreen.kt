package com.example.moneymanager.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.ui.screens.auth.PasscodeScreen
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import com.example.moneymanager.R
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun SettingsScreen(
    onManageCategoriesClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }
    var showPasscodeSetup by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    
    val isPasscodeEnabled by viewModel.isPasscodeEnabled.collectAsState()
    val csvImportState by viewModel.csvImportUiState.collectAsState()
    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            viewModel.importFromCsv(context, uri)
        }
    }

    if (showPasscodeSetup) {
        PasscodeScreen(
            isVerification = false,
            onPasscodeEntered = {
                viewModel.setPasscode(it)
                showPasscodeSetup = false
            },
            onCancel = { showPasscodeSetup = false }
        )
        return
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.theme_title)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.theme_system)) },
                        modifier = Modifier.clickable {
                            viewModel.setTheme(0)
                            showThemeDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.theme_light)) },
                        modifier = Modifier.clickable {
                            viewModel.setTheme(1)
                            showThemeDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.theme_dark)) },
                        modifier = Modifier.clickable {
                            viewModel.setTheme(2)
                            showThemeDialog = false
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language_title)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.language_english)) },
                        modifier = Modifier.clickable {
                            viewModel.setLanguage("en")
                            showLanguageDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.language_thai)) },
                        modifier = Modifier.clickable {
                            viewModel.setLanguage("th")
                            showLanguageDialog = false
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_data_title)) },
            text = { Text(stringResource(R.string.reset_data_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.resetData()
                            showResetDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.btn_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    when (val state = csvImportState) {
        is SettingsViewModel.CsvImportUiState.Idle -> Unit
        is SettingsViewModel.CsvImportUiState.Loading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.settings_import_csv)) },
                text = { CircularProgressIndicator() },
                confirmButton = {}
            )
        }
        is SettingsViewModel.CsvImportUiState.Success -> {
            val skippedReasonText = if (state.result.skippedReasons.isEmpty()) {
                ""
            } else {
                "\n\nSkipped reasons:\n- " + state.result.skippedReasons.joinToString("\n- ")
            }
            AlertDialog(
                onDismissRequest = viewModel::clearImportState,
                title = { Text(stringResource(R.string.import_success_title)) },
                text = {
                    Text(
                        "Imported: ${state.result.importedCount}\nSkipped: ${state.result.skippedCount}$skippedReasonText"
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::clearImportState) {
                        Text(stringResource(R.string.common_ok))
                    }
                }
            )
        }
        is SettingsViewModel.CsvImportUiState.Error -> {
            AlertDialog(
                onDismissRequest = viewModel::clearImportState,
                title = { Text(stringResource(R.string.import_error_title)) },
                text = { Text(state.message.ifBlank { stringResource(R.string.import_error_message) }) },
                confirmButton = {
                    TextButton(onClick = viewModel::clearImportState) {
                        Text(stringResource(R.string.common_ok))
                    }
                }
            )
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            SettingsItem(
                icon = Icons.Default.Category,
                title = stringResource(R.string.cat_mgmt_title),
                subtitle = stringResource(R.string.cat_mgmt_desc),
                onClick = onManageCategoriesClick
            )
            Divider()
            SettingsItem(
                icon = Icons.Default.FileDownload,
                title = stringResource(R.string.export_csv_title),
                subtitle = stringResource(R.string.export_csv_desc),
                onClick = { viewModel.exportToCsv(context) }
            )
            Divider()
            SettingsItem(
                icon = Icons.Default.FileUpload,
                title = stringResource(R.string.settings_import_csv),
                subtitle = stringResource(R.string.settings_import_csv_desc),
                onClick = { csvPickerLauncher.launch(arrayOf("text/csv", "text/plain")) }
            )
            Divider()
            SettingsItem(
                icon = Icons.Default.Language,
                title = stringResource(R.string.language_title),
                subtitle = stringResource(R.string.language_desc),
                onClick = { showLanguageDialog = true }
            )
            Divider()
            SettingsItem(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.theme_title),
                subtitle = stringResource(R.string.theme_desc),
                onClick = { showThemeDialog = true }
            )
            Divider()
            ListItem(
                modifier = Modifier.clickable { 
                    if (isPasscodeEnabled) viewModel.disablePasscode() else showPasscodeSetup = true 
                },
                leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.passcode_lock_title)) },
                supportingContent = { Text(stringResource(R.string.passcode_lock_desc)) },
                trailingContent = {
                    Switch(
                        checked = isPasscodeEnabled,
                        onCheckedChange = { 
                            if (it) showPasscodeSetup = true else viewModel.disablePasscode() 
                        }
                    )
                }
            )
            Divider()
            SettingsItem(
                icon = Icons.Default.DeleteForever,
                title = stringResource(R.string.reset_data_title),
                subtitle = stringResource(R.string.reset_data_desc),
                onClick = { showResetDialog = true }
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) }
    )
}


