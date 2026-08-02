package com.cyanbudget.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyanbudget.app.data.FinanceRepository
import com.cyanbudget.app.data.SettingsRepository
import com.cyanbudget.app.model.AppSettings
import com.cyanbudget.app.model.Budget
import com.cyanbudget.app.model.FinanceInsights
import com.cyanbudget.app.model.FinanceTransaction
import com.cyanbudget.app.model.SavingsGoal
import com.cyanbudget.app.widget.BudgetWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FinanceUiState(
    val loading: Boolean = true,
    val transactions: List<FinanceTransaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val goals: List<SavingsGoal> = emptyList(),
    val settings: AppSettings = AppSettings()
) {
    val summary get() = FinanceInsights.summary(transactions)
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CyanBudgetApplication
    private val repository: FinanceRepository = app.financeRepository
    private val settingsRepository: SettingsRepository = app.settingsRepository

    val uiState: StateFlow<FinanceUiState> = combine(
        repository.transactions, repository.budgets, repository.goals, settingsRepository.settings
    ) { transactions, budgets, goals, settings ->
        FinanceUiState(false, transactions, budgets, goals, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceUiState())

    init { viewModelScope.launch { repository.refresh() } }

    fun save(transaction: FinanceTransaction) { viewModelScope.launch { repository.save(transaction) } }
    fun delete(transaction: FinanceTransaction) { viewModelScope.launch { repository.delete(transaction) } }
    fun undoDelete(transaction: FinanceTransaction) = save(transaction)
    fun save(budget: Budget) { viewModelScope.launch { repository.save(budget) } }
    fun delete(budget: Budget) { viewModelScope.launch { repository.delete(budget) } }
    fun save(goal: SavingsGoal) { viewModelScope.launch { repository.save(goal) } }
    fun delete(goal: SavingsGoal) { viewModelScope.launch { repository.delete(goal) } }
    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val updated = transform(uiState.value.settings)
            settingsRepository.update(updated)
            getApplication<Application>().getSharedPreferences("widget", android.content.Context.MODE_PRIVATE).edit()
                .putBoolean("privacy", updated.privacyMode)
                .putString("currency", updated.currencyCode)
                .putLong("budget", updated.monthlyBudgetCents)
                .apply()
            BudgetWidgetProvider.updateAll(getApplication())
        }
    }
}
