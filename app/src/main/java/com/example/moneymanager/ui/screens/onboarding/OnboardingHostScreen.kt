package com.example.moneymanager.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingHostScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = uiState.currentStep,
            transitionSpec = { fadeIn(animationSpec = tween(220)) with fadeOut(animationSpec = tween(220)) },
            label = "OnboardingStepTransition"
        ) { step ->
            when (step) {
                OnboardingStep.Language -> LanguageSelectScreen(
                    selectedLanguage = uiState.selectedLanguage,
                    onLanguageSelected = viewModel::selectLanguage,
                    onNext = viewModel::nextStep
                )

                OnboardingStep.Theme -> ThemeSelectScreen(
                    selectedTheme = uiState.selectedTheme,
                    onThemeSelected = viewModel::selectTheme,
                    onNext = viewModel::nextStep,
                    onBack = viewModel::prevStep
                )

                OnboardingStep.Intro -> IntroPagerScreen(
                    pageIndex = uiState.introPageIndex,
                    onPageChanged = viewModel::setIntroPage,
                    onNext = viewModel::nextStep,
                    onBack = viewModel::prevStep,
                    onNextIntro = viewModel::nextIntro,
                    onPrevIntro = viewModel::prevIntro
                )

                OnboardingStep.SetupQuestions -> SetupWizardScreen(
                    answers = uiState.setupAnswers,
                    hasExistingData = uiState.hasExistingData,
                    onStartModeSelected = viewModel::setStartMode,
                    onToggleAccount = viewModel::toggleAccount,
                    onPurposeSelected = viewModel::setPurpose,
                    onStarterCategoriesSelected = viewModel::setAddStarterCategories,
                    onFinish = viewModel::submitSetup,
                    onBack = viewModel::prevStep,
                    isSeeding = uiState.isSeeding,
                    errorMessage = uiState.errorMessage
                )

                OnboardingStep.Done -> DoneScreen(onFinish = onFinish)
            }
        }
    }
}
