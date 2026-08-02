package com.cyanbudget.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cyanbudget.app.FinanceUiState
import com.cyanbudget.app.model.TransactionType
import com.cyanbudget.app.model.asMoney
import com.cyanbudget.app.ui.theme.Coral
import com.cyanbudget.app.ui.theme.Emerald
import com.cyanbudget.app.ui.theme.Navy

@Composable
fun DashboardScreen(
    state: FinanceUiState,
    onPrivacyToggle: () -> Unit,
    onAdd: (TransactionType) -> Unit,
    onVoice: () -> Unit,
    onTransaction: (com.cyanbudget.app.model.FinanceTransaction) -> Unit,
    onBudgets: () -> Unit,
    onGoals: () -> Unit,
    onAllTransactions: () -> Unit
) {
    val settings = state.settings
    val summary = state.summary
    val hidden = settings.privacyMode
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Good ${greeting()},", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Your money, simplified", style = MaterialTheme.typography.headlineMedium)
                }
                IconButton(onClick = onPrivacyToggle, modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)) {
                    Icon(if (hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, if (hidden) "Show financial amounts" else "Hide financial amounts")
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
                        .background(Brush.linearGradient(listOf(Navy, Color(0xFF193F64)))).padding(22.dp)
                ) {
                    Column {
                        Text("TOTAL BALANCE", color = Color(0xFFB9C9DA), style = MaterialTheme.typography.labelMedium)
                        Text(summary.balanceCents.asMoney(settings.currencyCode, hidden), color = Color.White, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.height(22.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                            BalanceMini("Income", summary.incomeCents.asMoney(settings.currencyCode, hidden), Emerald, Icons.Outlined.ArrowDownward)
                            BalanceMini("Expenses", summary.expenseCents.asMoney(settings.currencyCode, hidden), Coral, Icons.Outlined.ArrowUpward)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { onAdd(TransactionType.EXPENSE) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Coral)) {
                    Icon(Icons.Outlined.Add, null); Text("Expense", Modifier.padding(start = 6.dp))
                }
                FilledTonalButton(onClick = { onAdd(TransactionType.INCOME) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Add, null); Text("Income", Modifier.padding(start = 6.dp))
                }
                FilledTonalButton(onClick = onVoice, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp), modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Mic, "Add transaction by voice")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Today", summary.todayCents.asMoney(settings.currencyCode, hidden), "spent so far", Coral, Modifier.weight(1f))
                MetricCard("This week", summary.weekCents.asMoney(settings.currencyCode, hidden), "last 7 days", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            }
        }
        item {
            SectionHeader("Monthly budget", "Manage", onBudgets)
            Spacer(Modifier.height(10.dp))
            BudgetProgress("Overall budget", summary.monthExpenseCents, settings.monthlyBudgetCents, settings.currencyCode, hidden)
        }
        if (state.goals.isNotEmpty()) item {
            val goal = state.goals.first()
            SectionHeader("Savings goal", "View all", onGoals)
            Spacer(Modifier.height(10.dp))
            BudgetProgress(goal.name, goal.savedCents, goal.targetCents, settings.currencyCode, hidden)
        }
        if (summary.categoryTotals.isNotEmpty()) item {
            SectionHeader("Spending by category")
            Spacer(Modifier.height(10.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                val values = summary.categoryTotals.entries.sortedByDescending { it.value }.take(5)
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(values.map { it.key to it.value }, listOf(Coral, Emerald, MaterialTheme.colorScheme.primary, Color(0xFFF0B54A), Color(0xFFA27AE7)), Modifier.size(110.dp))
                    Column(Modifier.padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        values.take(4).forEachIndexed { index, entry ->
                            val colors = listOf(Coral, Emerald, MaterialTheme.colorScheme.primary, Color(0xFFF0B54A))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(colors[index], CircleShape))
                                Text(entry.key, Modifier.padding(start = 8.dp).weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                Text(entry.value.asMoney(settings.currencyCode, hidden), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionHeader("Recent transactions", "See all", onAllTransactions)
            Card(Modifier.padding(top = 10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    if (state.transactions.isEmpty()) EmptyState("No transactions yet", "Use + to record your first expense or income.")
                    state.transactions.take(5).forEach { transaction ->
                        TransactionRow(transaction, settings.currencyCode, hidden, { onTransaction(transaction) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceMini(label: String, amount: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).background(color.copy(alpha = .18f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.padding(start = 8.dp)) {
            Text(label, color = Color(0xFFB9C9DA), style = MaterialTheme.typography.bodyMedium)
            Text(amount, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "morning"
    in 12..16 -> "afternoon"
    else -> "evening"
}
