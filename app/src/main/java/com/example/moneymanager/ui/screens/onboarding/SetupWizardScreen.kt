package com.example.moneymanager.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.moneymanager.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SetupWizardScreen(
    answers: SetupAnswers,
    hasExistingData: Boolean,
    onStartModeSelected: (SetupStartMode) -> Unit,
    onToggleAccount: (SetupAccountOption) -> Unit,
    onPurposeSelected: (SetupPurpose) -> Unit,
    onStarterCategoriesSelected: (Boolean) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    isSeeding: Boolean,
    errorMessage: String?
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 4

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            LinearProgressIndicator(
                progress = currentStep.toFloat() / totalSteps.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.setup_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (hasExistingData) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.setup_existing_data_note),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = { fadeIn() with fadeOut() },
                    label = "WizardStepTransition"
                ) { step ->
                    when (step) {
                        1 -> QuestionStart(
                            answers = answers,
                            onStartModeSelected = onStartModeSelected
                        )

                        2 -> QuestionAccounts(
                            answers = answers,
                            onToggleAccount = onToggleAccount
                        )

                        3 -> QuestionPurpose(
                            answers = answers,
                            onPurposeSelected = onPurposeSelected
                        )

                        4 -> QuestionCategories(
                            answers = answers,
                            onStarterCategoriesSelected = onStarterCategoriesSelected
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        if (currentStep == 1) onBack() else currentStep -= 1
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(stringResource(R.string.common_back))
                }

                Button(
                    onClick = {
                        if (currentStep == totalSteps) onFinish() else currentStep += 1
                    },
                    enabled = !isSeeding,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSeeding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = if (currentStep == totalSteps) {
                                stringResource(R.string.common_finish)
                            } else {
                                stringResource(R.string.common_next)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionStart(
    answers: SetupAnswers,
    onStartModeSelected: (SetupStartMode) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.setup_q1_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        ChoiceCard(
            label = stringResource(R.string.setup_q1_a_quick),
            isSelected = answers.startMode == SetupStartMode.QUICK,
            onClick = { onStartModeSelected(SetupStartMode.QUICK) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        ChoiceCard(
            label = stringResource(R.string.setup_q1_b_custom),
            isSelected = answers.startMode == SetupStartMode.CUSTOMIZE,
            onClick = { onStartModeSelected(SetupStartMode.CUSTOMIZE) }
        )
    }
}

@Composable
private fun QuestionAccounts(
    answers: SetupAnswers,
    onToggleAccount: (SetupAccountOption) -> Unit
) {
    val options = listOf(
        SetupAccountOption.CASH to R.string.setup_account_cash,
        SetupAccountOption.BANK to R.string.setup_account_bank,
        SetupAccountOption.E_WALLET to R.string.setup_account_ewallet,
        SetupAccountOption.CREDIT_CARD to R.string.setup_account_credit
    )

    Column {
        Text(
            text = stringResource(R.string.setup_q2_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (answers.startMode == SetupStartMode.QUICK) {
            Text(
                text = stringResource(R.string.setup_quick_mode_account_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        options.forEach { (key, labelRes) ->
            val selected = answers.selectedAccounts.contains(key)
            ChoiceCard(
                label = stringResource(labelRes),
                isSelected = selected,
                enabled = answers.startMode == SetupStartMode.CUSTOMIZE,
                onClick = { onToggleAccount(key) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun QuestionPurpose(
    answers: SetupAnswers,
    onPurposeSelected: (SetupPurpose) -> Unit
) {
    val options = listOf(
        SetupPurpose.PERSONAL to R.string.setup_purpose_personal,
        SetupPurpose.STUDENT to R.string.setup_purpose_student,
        SetupPurpose.FREELANCE to R.string.setup_purpose_freelance,
        SetupPurpose.BUSINESS to R.string.setup_purpose_business
    )

    Column {
        Text(
            text = stringResource(R.string.setup_q3_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(14.dp))
        options.forEach { (option, labelRes) ->
            ChoiceCard(
                label = stringResource(labelRes),
                isSelected = answers.purpose == option,
                onClick = { onPurposeSelected(option) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun QuestionCategories(
    answers: SetupAnswers,
    onStarterCategoriesSelected: (Boolean) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.setup_q4_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (answers.startMode == SetupStartMode.QUICK) {
            Text(
                text = stringResource(R.string.setup_quick_mode_categories_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        ChoiceCard(
            label = stringResource(R.string.setup_seed_yes),
            isSelected = answers.addStarterCategories,
            enabled = answers.startMode == SetupStartMode.CUSTOMIZE,
            onClick = { onStarterCategoriesSelected(true) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        ChoiceCard(
            label = stringResource(R.string.setup_seed_no),
            isSelected = !answers.addStarterCategories,
            enabled = answers.startMode == SetupStartMode.CUSTOMIZE,
            onClick = { onStarterCategoriesSelected(false) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
