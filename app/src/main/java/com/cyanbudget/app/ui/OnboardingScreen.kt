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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cyanbudget.app.model.AppSettings

@Composable
fun OnboardingScreen(current: AppSettings, onComplete: (AppSettings) -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    var currency by remember { mutableStateOf(current.currencyCode) }
    var budget by remember { mutableStateOf("3000") }
    var theme by remember { mutableStateOf(current.theme) }
    var reminders by remember { mutableStateOf(false) }
    val features = listOf(
        Triple(Icons.Outlined.Assessment, "Know where money goes", "Record income and expenses in seconds, then see clear trends without spreadsheet work."),
        Triple(Icons.Outlined.Mic, "Speak naturally", "Say “I spent 25 dollars on lunch” and review the details before anything is saved."),
        Triple(Icons.Outlined.Savings, "Plan with confidence", "Set budgets and savings goals, with gentle milestones that keep you moving."),
        Triple(Icons.Outlined.Security, "Private by design", "Your data works offline. Hide amounts anytime and control every notification.")
    )
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text("CYAN BUDGET", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) }
        Spacer(Modifier.weight(.5f))
        if (page < features.size) {
            val feature = features[page]
            Box(Modifier.size(112.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) { Icon(feature.first, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary) }
            Text(feature.second, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 28.dp))
            Text(feature.third, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
        } else {
            Text("Set up your space", style = MaterialTheme.typography.headlineLarge)
            Text("You can change these anytime.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp))
            Text("Currency", Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("USD", "EUR", "GBP", "PKR").forEach { value -> FilterChip(selected = currency == value, onClick = { currency = value }, label = { Text(value) }) } }
            OutlinedTextField(budget, { budget = it.filter(Char::isDigit) }, label = { Text("Monthly budget") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            Text("Theme", Modifier.fillMaxWidth().padding(top = 16.dp), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("System", "Light", "Dark").forEach { value -> FilterChip(selected = theme == value, onClick = { theme = value }, label = { Text(value) }) } }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Daily reminder", style = MaterialTheme.typography.titleMedium); Text("A nudge to log spending", color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(reminders, { reminders = it }) }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(bottom = 22.dp)) { repeat(features.size + 1) { index -> Box(Modifier.size(if (index == page) 22.dp else 7.dp, 7.dp).background(if (index == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = .4f), CircleShape)) } }
        Button(onClick = {
            if (page < features.size) page++ else onComplete(current.copy(onboardingComplete = true, currencyCode = currency, monthlyBudgetCents = (budget.toLongOrNull() ?: 3000) * 100, theme = theme, dailyReminder = reminders))
        }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(if (page < features.size) "Continue" else "Start budgeting") }
        if (page < features.size) OutlinedButton(onClick = { onComplete(current.copy(onboardingComplete = true)) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Skip for now") }
    }
}
