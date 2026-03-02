package com.example.moneymanager.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.platform.LocalContext

@Composable
fun PasscodeScreen(
    isVerification: Boolean,
    onPasscodeEntered: (String) -> Unit,
    onCancel: () -> Unit = {}
) {
    val context = LocalContext.current
    var passcode by remember { mutableStateOf("") }
    var firstPasscode by remember { mutableStateOf("") }
    var isConfirmMode by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val shakeEffect = remember { androidx.compose.animation.core.Animatable(0f) }

    // Constants for headers
    val titleText = when {
        isVerification -> stringResource(com.example.moneymanager.R.string.passcode_enter_title)
        isConfirmMode -> stringResource(com.example.moneymanager.R.string.passcode_confirm_title)
        else -> stringResource(com.example.moneymanager.R.string.passcode_setup_title)
    }
    
    val descText = when {
        isVerification -> stringResource(com.example.moneymanager.R.string.passcode_enter_desc)
        isConfirmMode -> stringResource(com.example.moneymanager.R.string.passcode_confirm_desc)
        else -> stringResource(com.example.moneymanager.R.string.passcode_setup_desc)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = descText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Dots Indicator
            Row(
                modifier = Modifier.padding(vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(4) { index ->
                    val isFilled = index < passcode.length
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            // Error Message
            if (error != null) {
                Text(
                    text = error!!, 
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp)) // Placeholder to prevent jump
            }

            // Pin Pad
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val numbers = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "BACK")
                )

                numbers.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            PinButton(
                                text = key,
                                onClick = {
                                    error = null // Clear error on type
                                    when (key) {
                                        "BACK" -> {
                                            if (passcode.isNotEmpty()) passcode = passcode.dropLast(1)
                                        }
                                        "" -> {} // Empty cell
                                        else -> {
                                            if (passcode.length < 4) {
                                                passcode += key
                                                if (passcode.length == 4) {
                                                    if (isVerification) {
                                                        onPasscodeEntered(passcode)
                                                        passcode = ""
                                                    } else {
                                                        // Setup Mode Flow
                                                        if (!isConfirmMode) {
                                                            // Switch to Confirm
                                                            firstPasscode = passcode
                                                            passcode = ""
                                                            isConfirmMode = true
                                                        } else {
                                                            // Verify Confirmation
                                                            if (passcode == firstPasscode) {
                                                                onPasscodeEntered(passcode)
                                                            } else {
                                                                error = context.getString(com.example.moneymanager.R.string.passcode_mismatch_error)
                                                                passcode = "" 
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Bottom Actions
            if (!isVerification) {
                TextButton(
                    onClick = {
                        if (isConfirmMode) {
                            // Back to Enter Step
                            isConfirmMode = false
                            passcode = ""
                            firstPasscode = ""
                            error = null
                        } else {
                            onCancel()
                        }
                    }
                ) {
                    Text(
                        text = if (isConfirmMode) stringResource(com.example.moneymanager.R.string.btn_start_over) else stringResource(com.example.moneymanager.R.string.btn_cancel),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun PinButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick, enabled = text.isNotEmpty())
            .background(if (text.isNotEmpty()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (text == "BACK") {
            Icon(Icons.Default.Backspace, contentDescription = "Delete")
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
