package com.cyanbudget.app

import android.app.Application
import com.cyanbudget.app.data.FinanceDatabase
import com.cyanbudget.app.data.FinanceRepository
import com.cyanbudget.app.data.SettingsRepository
import com.cyanbudget.app.widget.BudgetWidgetProvider

class CyanBudgetApplication : Application() {
    val database by lazy { FinanceDatabase.get(this) }
    val financeRepository by lazy { FinanceRepository(database) { BudgetWidgetProvider.updateAll(this) } }
    val settingsRepository by lazy { SettingsRepository(this) }
}
