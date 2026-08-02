package com.cyanbudget.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.FragmentActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cyanbudget.app.ui.CyanBudgetApp
import com.cyanbudget.app.work.FinanceReminderWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity() {
    private var accessGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "cyan_widget_refresh",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<FinanceReminderWorker>(12, TimeUnit.HOURS).build()
        )
        setContent { CyanBudgetApp(startDestination = intent.getStringExtra("destination"), accessGranted = accessGranted, onUnlock = ::authenticate) }
        lifecycleScope.launch {
            val enabled = (application as CyanBudgetApplication).settingsRepository.settings.first().biometricLock
            if (enabled) authenticate() else accessGranted = true
        }
    }

    private fun authenticate() {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            accessGranted = true
            return
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { accessGranted = true }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Cyan Budget")
            .setSubtitle("Your financial data stays private")
            .setAllowedAuthenticators(authenticators)
            .build())
    }
}
