package com.example.moneymanager.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val PASSCODE = stringPreferencesKey("passcode")
        val IS_PASSCODE_ENABLED = booleanPreferencesKey("is_passcode_enabled")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = intPreferencesKey("theme")
        val DEBUG_SEED_VERSION = intPreferencesKey("debug_seed_version")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
        val SETUP_PROFILE = stringPreferencesKey("setup_profile")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
    }

    val passcode: Flow<String?> = context.dataStore.data.map { it[PASSCODE] }
    val isPasscodeEnabled: Flow<Boolean> = context.dataStore.data.map { it[IS_PASSCODE_ENABLED] ?: false }
    val language: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "en" }
    val theme: Flow<Int> = context.dataStore.data.map { it[THEME] ?: 0 } // 0: System, 1: Light, 2: Dark
    val debugSeedVersion: Flow<Int> = context.dataStore.data.map { it[DEBUG_SEED_VERSION] ?: 0 }
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }
    val isSetupCompleted: Flow<Boolean> = context.dataStore.data.map { it[SETUP_COMPLETED] ?: false }
    val setupProfile: Flow<String?> = context.dataStore.data.map { it[SETUP_PROFILE] }
    val defaultCurrency: Flow<String> = context.dataStore.data.map { it[DEFAULT_CURRENCY] ?: "THB" }

    suspend fun setPasscode(passcode: String) {
        context.dataStore.edit { it[PASSCODE] = passcode }
    }

    suspend fun setPasscodeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_PASSCODE_ENABLED] = enabled }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[LANGUAGE] = lang }
    }

    suspend fun setTheme(theme: Int) {
        context.dataStore.edit { it[THEME] = theme }
    }

    suspend fun setDebugSeedVersion(version: Int) {
        context.dataStore.edit { it[DEBUG_SEED_VERSION] = version }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        context.dataStore.edit { it[SETUP_COMPLETED] = completed }
    }

    suspend fun setSetupProfile(profile: String) {
        context.dataStore.edit { it[SETUP_PROFILE] = profile }
    }

    suspend fun setDefaultCurrency(currency: String) {
        context.dataStore.edit { it[DEFAULT_CURRENCY] = currency }
    }
}
