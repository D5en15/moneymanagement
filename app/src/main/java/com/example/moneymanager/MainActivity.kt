package com.example.moneymanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.moneymanager.ui.MainScreen
import com.example.moneymanager.ui.theme.MoneyManagerTheme
import com.example.moneymanager.domain.repository.MoneyRepository
import com.example.moneymanager.data.local.PreferenceManager
import com.example.moneymanager.utils.DebugDataSeeder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.ui.MainViewModel
import com.example.moneymanager.ui.screens.auth.PasscodeScreen
import com.example.moneymanager.ui.screens.splash.SplashScreen
import androidx.compose.runtime.LaunchedEffect
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

import androidx.activity.enableEdgeToEdge
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.example.moneymanager.ui.screens.onboarding.OnboardingHostScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: MoneyRepository

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isDebugBuild = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebugBuild) {
            CoroutineScope(Dispatchers.IO).launch {
                DebugDataSeeder.seedIfNeeded(
                    repository = repository,
                    preferenceManager = preferenceManager
                )
            }
        }

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val theme by viewModel.theme.collectAsState()
            
            val darkTheme = when (theme) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MoneyManagerTheme(darkTheme = darkTheme) {
                val isSplashDone by viewModel.isSplashDone.collectAsState()
                val isLocked by viewModel.isLocked.collectAsState()
                val language by viewModel.language.collectAsState()
                val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
                val isSetupCompleted by viewModel.isSetupCompleted.collectAsState()
                
                var bypassOnboardingThisSession by remember { mutableStateOf(false) }

                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshLockStatus()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                LaunchedEffect(language) {
                    val current = context.resources.configuration.locales[0].language
                    if (current != language) {
                        val locale = Locale(language)
                        Locale.setDefault(locale)
                        val config = context.resources.configuration
                        config.setLocale(locale)
                        context.resources.updateConfiguration(config, context.resources.displayMetrics)
                        
                        (context as? android.app.Activity)?.recreate()
                    }
                }

                if (!isSplashDone) {
                    SplashScreen(onFinish = { viewModel.markSplashDone() })
                } else if (!bypassOnboardingThisSession && (!isOnboardingCompleted || !isSetupCompleted)) {
                    OnboardingHostScreen(
                        onFinish = { bypassOnboardingThisSession = true }
                    )
                } else if (isLocked) {
                    PasscodeScreen(
                        isVerification = true,
                        onPasscodeEntered = { viewModel.unlock(it) }
                    )
                } else {
                    MainScreen()
                }
            }
        }
    }
}

