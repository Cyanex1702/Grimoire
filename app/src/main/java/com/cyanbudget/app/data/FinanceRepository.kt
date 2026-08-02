package com.cyanbudget.app.data

import com.cyanbudget.app.model.Budget
import com.cyanbudget.app.model.FinanceTransaction
import com.cyanbudget.app.model.SavingsGoal
import com.cyanbudget.app.widget.BudgetWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class FinanceRepository(private val database: FinanceDatabase, private val updateWidget: () -> Unit) {
    private val _transactions = MutableStateFlow<List<FinanceTransaction>>(emptyList())
    val transactions: StateFlow<List<FinanceTransaction>> = _transactions.asStateFlow()
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()
    private val _goals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val goals: StateFlow<List<SavingsGoal>> = _goals.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _transactions.value = database.transactions()
        _budgets.value = database.budgets()
        _goals.value = database.goals()
    }

    suspend fun save(transaction: FinanceTransaction) = withContext(Dispatchers.IO) {
        database.upsert(transaction); _transactions.value = database.transactions(); updateWidget()
    }

    suspend fun delete(transaction: FinanceTransaction) = withContext(Dispatchers.IO) {
        database.deleteTransaction(transaction.id); _transactions.value = database.transactions(); updateWidget()
    }

    suspend fun save(budget: Budget) = withContext(Dispatchers.IO) {
        database.upsert(budget); _budgets.value = database.budgets()
    }

    suspend fun delete(budget: Budget) = withContext(Dispatchers.IO) {
        database.deleteBudget(budget.id); _budgets.value = database.budgets()
    }

    suspend fun save(goal: SavingsGoal) = withContext(Dispatchers.IO) {
        database.upsert(goal); _goals.value = database.goals()
    }

    suspend fun delete(goal: SavingsGoal) = withContext(Dispatchers.IO) {
        database.deleteGoal(goal.id); _goals.value = database.goals()
    }
}
