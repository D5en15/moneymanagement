package com.example.moneymanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isSplashDone = MutableStateFlow(false)
    val isSplashDone: StateFlow<Boolean> = _isSplashDone.asStateFlow()
    
    val language = preferenceManager.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val theme = preferenceManager.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isOnboardingCompleted = preferenceManager.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isSetupCompleted = preferenceManager.isSetupCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val lockConfig = combine(
        preferenceManager.isPasscodeEnabled,
        preferenceManager.passcode
    ) { enabled, passcode ->
        LockConfig(enabled = enabled, passcode = passcode)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        LockConfig(enabled = false, passcode = null)
    )

    private val _isUnlockedThisSession = MutableStateFlow(false)

    private var actualPasscode: String? = null

    init {
        observeLockState()
    }

    private fun observeLockState() {
        viewModelScope.launch {
            var previousConfig: LockConfig? = null
            combine(lockConfig, _isUnlockedThisSession) { config, unlocked ->
                config to unlocked
            }.collect { (config, unlocked) ->
                val configChanged = previousConfig != config
                previousConfig = config

                actualPasscode = config.passcode
                val passcodeExists = !config.passcode.isNullOrEmpty()

                if (configChanged && _isUnlockedThisSession.value) {
                    // If passcode settings changed while running, require fresh verification.
                    _isUnlockedThisSession.value = false
                    _isLocked.value = config.enabled && passcodeExists
                } else {
                    _isLocked.value = config.enabled && passcodeExists && !unlocked
                }
            }
        }
    }

    fun refreshLockStatus() {
        _isUnlockedThisSession.value = false
    }

    fun unlock(enteredPasscode: String): Boolean {
        return if (enteredPasscode == actualPasscode) {
            _isUnlockedThisSession.value = true
            true
        } else {
            false
        }
    }

    fun markSplashDone() {
        _isSplashDone.value = true
    }

    private data class LockConfig(
        val enabled: Boolean,
        val passcode: String?
    )
}
