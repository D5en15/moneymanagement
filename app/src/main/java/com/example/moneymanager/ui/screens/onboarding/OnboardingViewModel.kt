package com.example.moneymanager.ui.screens.onboarding

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.R
import com.example.moneymanager.data.local.PreferenceManager
import com.example.moneymanager.domain.repository.AccountSeed
import com.example.moneymanager.domain.repository.CategorySeed
import com.example.moneymanager.domain.repository.MoneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep { Language, Theme, Intro, SetupQuestions, Done }

enum class SetupStartMode { QUICK, CUSTOMIZE }

enum class SetupAccountOption { CASH, BANK, E_WALLET, CREDIT_CARD }

enum class SetupPurpose { PERSONAL, STUDENT, FREELANCE, BUSINESS }

data class SetupAnswers(
    val startMode: SetupStartMode = SetupStartMode.QUICK,
    val selectedAccounts: Set<SetupAccountOption> = setOf(SetupAccountOption.CASH),
    val purpose: SetupPurpose = SetupPurpose.PERSONAL,
    val addStarterCategories: Boolean = true
)

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.Language,
    val selectedLanguage: String = "en",
    val selectedTheme: Int = 0,
    val introPageIndex: Int = 0,
    val setupAnswers: SetupAnswers = SetupAnswers(),
    val isSeeding: Boolean = false,
    val hasExistingData: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager,
    private val repository: MoneyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        observeInputs()
    }

    private fun observeInputs() {
        viewModelScope.launch {
            combine(
                preferenceManager.language,
                preferenceManager.theme,
                repository.getAllAccounts(),
                repository.getAllCategories()
            ) { language, theme, accounts, categories ->
                Triple(language, theme, accounts.isNotEmpty() || categories.isNotEmpty())
            }.collect { (language, theme, hasExistingData) ->
                _uiState.update {
                    it.copy(
                        selectedLanguage = language,
                        selectedTheme = theme,
                        hasExistingData = hasExistingData
                    )
                }
            }
        }
    }

    fun selectLanguage(languageCode: String) {
        viewModelScope.launch {
            preferenceManager.setLanguage(languageCode)
            _uiState.update { it.copy(selectedLanguage = languageCode) }
        }
    }

    fun selectTheme(themeValue: Int) {
        viewModelScope.launch {
            preferenceManager.setTheme(themeValue)
            _uiState.update { it.copy(selectedTheme = themeValue) }
        }
    }

    fun nextStep() {
        _uiState.update {
            val next = when (it.currentStep) {
                OnboardingStep.Language -> OnboardingStep.Theme
                OnboardingStep.Theme -> OnboardingStep.Intro
                OnboardingStep.Intro -> OnboardingStep.SetupQuestions
                OnboardingStep.SetupQuestions -> OnboardingStep.Done
                OnboardingStep.Done -> OnboardingStep.Done
            }
            it.copy(currentStep = next)
        }
    }

    fun prevStep() {
        _uiState.update {
            val prev = when (it.currentStep) {
                OnboardingStep.Language -> OnboardingStep.Language
                OnboardingStep.Theme -> OnboardingStep.Language
                OnboardingStep.Intro -> OnboardingStep.Theme
                OnboardingStep.SetupQuestions -> OnboardingStep.Intro
                OnboardingStep.Done -> OnboardingStep.SetupQuestions
            }
            it.copy(currentStep = prev)
        }
    }

    fun nextIntro() {
        val page = _uiState.value.introPageIndex
        if (page < INTRO_PAGE_COUNT - 1) {
            setIntroPage(page + 1)
        }
    }

    fun prevIntro() {
        val page = _uiState.value.introPageIndex
        if (page > 0) {
            setIntroPage(page - 1)
        }
    }

    fun setIntroPage(index: Int) {
        _uiState.update { it.copy(introPageIndex = index.coerceIn(0, INTRO_PAGE_COUNT - 1)) }
    }

    fun setStartMode(mode: SetupStartMode) {
        _uiState.update { state ->
            val nextAnswers = if (mode == SetupStartMode.QUICK) {
                state.setupAnswers.copy(
                    startMode = mode,
                    selectedAccounts = setOf(SetupAccountOption.CASH),
                    addStarterCategories = true
                )
            } else {
                state.setupAnswers.copy(startMode = mode)
            }
            state.copy(setupAnswers = nextAnswers)
        }
    }

    fun toggleAccount(option: SetupAccountOption) {
        _uiState.update { state ->
            if (state.setupAnswers.startMode == SetupStartMode.QUICK) {
                return@update state
            }
            val current = state.setupAnswers.selectedAccounts
            val next = if (current.contains(option)) current - option else current + option
            val safe = if (next.isEmpty()) setOf(SetupAccountOption.CASH) else next
            state.copy(setupAnswers = state.setupAnswers.copy(selectedAccounts = safe))
        }
    }

    fun setPurpose(purpose: SetupPurpose) {
        _uiState.update { it.copy(setupAnswers = it.setupAnswers.copy(purpose = purpose)) }
    }

    fun setAddStarterCategories(enabled: Boolean) {
        _uiState.update { state ->
            if (state.setupAnswers.startMode == SetupStartMode.QUICK) {
                state
            } else {
                state.copy(setupAnswers = state.setupAnswers.copy(addStarterCategories = enabled))
            }
        }
    }

    fun submitSetup() {
        if (_uiState.value.isSeeding) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSeeding = true, errorMessage = null) }
            runCatching {
                val answers = _uiState.value.setupAnswers
                val defaults = buildSeedPayload(answers)
                repository.seedDefaultsAtomic(defaults.accounts, defaults.categories)

                preferenceManager.setSetupProfile(answers.purpose.profileValue)
                preferenceManager.setDefaultCurrency("THB")
                preferenceManager.setSetupCompleted(true)
                preferenceManager.setOnboardingCompleted(true)
            }.onSuccess {
                _uiState.update { it.copy(isSeeding = false, currentStep = OnboardingStep.Done) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSeeding = false,
                        errorMessage = error.message ?: context.getString(R.string.setup_error_generic)
                    )
                }
            }
        }
    }

    private fun buildSeedPayload(answers: SetupAnswers): SeedPayload {
        val accountOptions = if (answers.startMode == SetupStartMode.QUICK) {
            setOf(SetupAccountOption.CASH)
        } else {
            answers.selectedAccounts.ifEmpty { setOf(SetupAccountOption.CASH) }
        }

        val accounts = accountOptions.map { accountOption ->
            when (accountOption) {
                SetupAccountOption.CASH -> AccountSeed(
                    name = context.getString(R.string.setup_account_cash),
                    type = "cash",
                    icon = "account_balance_wallet",
                    color = Color.parseColor("#4CAF50")
                )
                SetupAccountOption.BANK -> AccountSeed(
                    name = context.getString(R.string.setup_account_bank),
                    type = "bank",
                    icon = "account_balance",
                    color = Color.parseColor("#1E88E5")
                )
                SetupAccountOption.E_WALLET -> AccountSeed(
                    name = context.getString(R.string.setup_account_ewallet),
                    type = "e-wallet",
                    icon = "payments",
                    color = Color.parseColor("#FB8C00")
                )
                SetupAccountOption.CREDIT_CARD -> AccountSeed(
                    name = context.getString(R.string.setup_account_credit),
                    type = "credit_card",
                    icon = "credit_card",
                    color = Color.parseColor("#E53935")
                )
            }
        }

        val shouldAddStarter = answers.startMode == SetupStartMode.QUICK || answers.addStarterCategories
        val categories = if (shouldAddStarter) {
            starterCategoriesForPurpose(answers.purpose)
        } else {
            listOf(
                CategorySeed(
                    name = context.getString(R.string.other_cat_name),
                    type = "expense",
                    icon = "more_horiz",
                    color = Color.parseColor("#9E9E9E")
                ),
                CategorySeed(
                    name = context.getString(R.string.other_cat_name),
                    type = "income",
                    icon = "more_horiz",
                    color = Color.parseColor("#9E9E9E")
                )
            )
        }

        return SeedPayload(accounts = accounts, categories = categories)
    }

    private fun starterCategoriesForPurpose(purpose: SetupPurpose): List<CategorySeed> {
        val baseExpenses = mutableListOf(
            CategorySeed(context.getString(R.string.food_cat_name), "expense", "restaurant", Color.parseColor("#FF7043")),
            CategorySeed(context.getString(R.string.transport_cat_name), "expense", "directions_car", Color.parseColor("#42A5F5")),
            CategorySeed(context.getString(R.string.shopping_cat_name), "expense", "shopping_cart", Color.parseColor("#EC407A")),
            CategorySeed(context.getString(R.string.bills_cat_name), "expense", "receipt", Color.parseColor("#78909C")),
            CategorySeed(context.getString(R.string.ent_cat_name), "expense", "movie", Color.parseColor("#AB47BC")),
            CategorySeed(context.getString(R.string.health_cat_name), "expense", "medical_services", Color.parseColor("#EF5350")),
            CategorySeed(context.getString(R.string.edu_cat_name), "expense", "school", Color.parseColor("#5C6BC0")),
            CategorySeed(context.getString(R.string.other_cat_name), "expense", "more_horiz", Color.parseColor("#9E9E9E"))
        )

        val baseIncome = mutableListOf(
            CategorySeed(context.getString(R.string.salary_cat_name), "income", "payments", Color.parseColor("#66BB6A")),
            CategorySeed(context.getString(R.string.freelance_cat_name), "income", "laptop", Color.parseColor("#FFCA28")),
            CategorySeed(context.getString(R.string.gift_cat_name), "income", "card_giftcard", Color.parseColor("#EC407A")),
            CategorySeed(context.getString(R.string.invest_cat_name), "income", "trending_up", Color.parseColor("#26C6DA")),
            CategorySeed(context.getString(R.string.other_cat_name), "income", "more_horiz", Color.parseColor("#9E9E9E"))
        )

        when (purpose) {
            SetupPurpose.STUDENT -> {
                baseExpenses += CategorySeed(
                    name = context.getString(R.string.fastfood_cat_name),
                    type = "expense",
                    icon = "fastfood",
                    color = Color.parseColor("#FFA726")
                )
            }
            SetupPurpose.FREELANCE -> {
                baseIncome += CategorySeed(
                    name = context.getString(R.string.client_payments_cat_name),
                    type = "income",
                    icon = "laptop",
                    color = Color.parseColor("#7CB342")
                )
                baseExpenses += CategorySeed(
                    name = context.getString(R.string.tools_cat_name),
                    type = "expense",
                    icon = "build",
                    color = Color.parseColor("#8D6E63")
                )
            }
            SetupPurpose.BUSINESS -> {
                baseIncome += CategorySeed(
                    name = context.getString(R.string.sales_cat_name),
                    type = "income",
                    icon = "storefront",
                    color = Color.parseColor("#43A047")
                )
                baseExpenses += CategorySeed(
                    name = context.getString(R.string.supplies_cat_name),
                    type = "expense",
                    icon = "inventory",
                    color = Color.parseColor("#FF7043")
                )
            }
            SetupPurpose.PERSONAL -> Unit
        }

        return baseExpenses + baseIncome
    }

    data class SeedPayload(
        val accounts: List<AccountSeed>,
        val categories: List<CategorySeed>
    )

    private companion object {
        const val INTRO_PAGE_COUNT = 5
    }
}

private val SetupPurpose.profileValue: String
    get() = when (this) {
        SetupPurpose.PERSONAL -> "simple"
        SetupPurpose.STUDENT -> "student"
        SetupPurpose.FREELANCE -> "freelancer"
        SetupPurpose.BUSINESS -> "business"
    }
