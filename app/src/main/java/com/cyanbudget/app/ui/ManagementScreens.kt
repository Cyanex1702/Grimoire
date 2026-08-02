package com.cyanbudget.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.cyanbudget.app.FinanceUiState
import com.cyanbudget.app.model.Budget
import com.cyanbudget.app.model.SavingsGoal
import com.cyanbudget.app.model.TransactionType
import com.cyanbudget.app.model.asMoney

@Composable
fun BudgetsScreen(state: FinanceUiState, onBack: () -> Unit, onSave: (Budget) -> Unit, onDelete: (Budget) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    if (showAdd) BudgetDialog(onDismiss = { showAdd = false }) { onSave(it); showAdd = false }
    Scaffold(topBar = { TopAppBar(
        title = { Text("Budgets") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Go back") } },
        actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Outlined.Add, "Create budget") } }
    ) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Stay ahead of your limits", style = MaterialTheme.typography.headlineMedium); Text("Alerts can notify you at 50%, 75%, 90%, and 100%.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { BudgetProgress("Overall monthly", state.summary.monthExpenseCents, state.settings.monthlyBudgetCents, state.settings.currencyCode, state.settings.privacyMode) }
            if (state.budgets.isEmpty()) item { EmptyState("No custom budgets", "Create a weekly, category, or custom spending limit.") }
            items(state.budgets.size, key = { state.budgets[it].id }) { index ->
                val budget = state.budgets[index]
                val spent = state.transactions.filter { it.type == TransactionType.EXPENSE && (budget.category == null || it.category == budget.category) }.sumOf { it.amountCents }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(budget.name, style = MaterialTheme.typography.titleMedium); Text("${budget.period}${budget.category?.let { " · $it" }.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            IconButton(onClick = { onDelete(budget) }) { Icon(Icons.Outlined.DeleteOutline, "Delete ${budget.name}") }
                        }
                        BudgetProgress("Progress", spent, budget.limitCents, state.settings.currencyCode, state.settings.privacyMode)
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetDialog(onDismiss: () -> Unit, onSave: (Budget) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create budget") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Limit") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(category, { category = it }, label = { Text("Category (optional)") }, singleLine = true)
        } },
        confirmButton = { Button(enabled = name.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0, onClick = { onSave(Budget(name = name, limitCents = (amount.toDouble() * 100).toLong(), category = category.ifBlank { null })) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun GoalsScreen(state: FinanceUiState, onBack: () -> Unit, onSave: (SavingsGoal) -> Unit, onDelete: (SavingsGoal) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var contributing by remember { mutableStateOf<SavingsGoal?>(null) }
    if (showAdd) GoalDialog(onDismiss = { showAdd = false }) { onSave(it); showAdd = false }
    contributing?.let { goal -> ContributionDialog(goal, { contributing = null }) { amount -> onSave(goal.copy(savedCents = goal.savedCents + amount)); contributing = null } }
    Scaffold(topBar = { TopAppBar(
        title = { Text("Savings goals") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Go back") } },
        actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Outlined.Add, "Create goal") } }
    ) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Make progress visible", style = MaterialTheme.typography.headlineMedium); Text("Small contributions add up. Celebrate every milestone.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (state.goals.isEmpty()) item { EmptyState("Start your first goal", "Build an emergency fund, plan a trip, or save for something special.") }
            items(state.goals.size, key = { state.goals[it].id }) { index ->
                val goal = state.goals[index]
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Savings, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(goal.name, style = MaterialTheme.typography.titleLarge); Text("${goal.savedCents.asMoney(state.settings.currencyCode, state.settings.privacyMode)} saved", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            IconButton(onClick = { onDelete(goal) }) { Icon(Icons.Outlined.DeleteOutline, "Delete ${goal.name}") }
                        }
                        BudgetProgress("Goal progress", goal.savedCents, goal.targetCents, state.settings.currencyCode, state.settings.privacyMode, Modifier.padding(top = 12.dp))
                        Button(onClick = { contributing = goal }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Add money") }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalDialog(onDismiss: () -> Unit, onSave: (SavingsGoal) -> Unit) {
    var name by remember { mutableStateOf("") }; var target by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("New savings goal") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Goal name") }, singleLine = true)
        OutlinedTextField(target, { target = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Target amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
    } }, confirmButton = { Button(enabled = name.isNotBlank() && (target.toDoubleOrNull() ?: 0.0) > 0, onClick = { onSave(SavingsGoal(name = name, targetCents = (target.toDouble() * 100).toLong())) }) { Text("Create") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun ContributionDialog(goal: SavingsGoal, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add to ${goal.name}") }, text = { OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true) }, confirmButton = { Button(enabled = (amount.toDoubleOrNull() ?: 0.0) > 0, onClick = { onSave((amount.toDouble() * 100).toLong()) }) { Text("Add money") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
