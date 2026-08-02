package com.cyanbudget.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cyanbudget.app.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "cyan_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding")
        val currency = stringPreferencesKey("currency")
        val theme = stringPreferencesKey("theme")
        val accent = longPreferencesKey("accent")
        val privacy = booleanPreferencesKey("privacy")
        val budget = longPreferencesKey("monthly_budget")
        val compact = booleanPreferencesKey("compact")
        val reminder = booleanPreferencesKey("daily_reminder")
        val alerts = booleanPreferencesKey("budget_alerts")
        val summary = booleanPreferencesKey("weekly_summary")
        val biometric = booleanPreferencesKey("biometric")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            onboardingComplete = p[Keys.onboarding] ?: false,
            currencyCode = p[Keys.currency] ?: "USD",
            theme = p[Keys.theme] ?: "System",
            accent = p[Keys.accent] ?: 0xFF377CF6,
            privacyMode = p[Keys.privacy] ?: false,
            monthlyBudgetCents = p[Keys.budget] ?: 300_000,
            compactMode = p[Keys.compact] ?: false,
            dailyReminder = p[Keys.reminder] ?: false,
            budgetAlerts = p[Keys.alerts] ?: true,
            weeklySummary = p[Keys.summary] ?: true,
            biometricLock = p[Keys.biometric] ?: false
        )
    }

    suspend fun update(value: AppSettings) = context.dataStore.edit { p ->
        p[Keys.onboarding] = value.onboardingComplete; p[Keys.currency] = value.currencyCode
        p[Keys.theme] = value.theme; p[Keys.accent] = value.accent; p[Keys.privacy] = value.privacyMode
        p[Keys.budget] = value.monthlyBudgetCents; p[Keys.compact] = value.compactMode
        p[Keys.reminder] = value.dailyReminder; p[Keys.alerts] = value.budgetAlerts
        p[Keys.summary] = value.weeklySummary; p[Keys.biometric] = value.biometricLock
    }
}
