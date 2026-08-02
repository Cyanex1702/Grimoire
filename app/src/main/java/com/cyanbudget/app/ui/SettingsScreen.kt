package com.cyanbudget.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cyanbudget.app.FinanceUiState
import com.cyanbudget.app.model.AppSettings

@Composable
fun SettingsScreen(
    state: FinanceUiState,
    update: ((AppSettings) -> AppSettings) -> Unit,
    onBudgets: () -> Unit,
    onGoals: () -> Unit,
    onExport: () -> Unit
) {
    var choiceDialog by remember { mutableStateOf<String?>(null) }
    val settings = state.settings
    choiceDialog?.let { choice ->
        val options = if (choice == "Theme") listOf("System", "Light", "Dark") else listOf("USD", "EUR", "GBP", "PKR", "INR", "CAD", "AUD", "JPY")
        val current = if (choice == "Theme") settings.theme else settings.currencyCode
        AlertDialog(onDismissRequest = { choiceDialog = null }, title = { Text(choice) }, text = { Column {
            options.forEach { option -> Row(Modifier.fillMaxWidth().clickable {
                update { if (choice == "Theme") it.copy(theme = option) else it.copy(currencyCode = option) }; choiceDialog = null
            }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = current == option, onClick = null); Text(option, Modifier.padding(start = 8.dp)) } }
        } }, confirmButton = { TextButton(onClick = { choiceDialog = null }) { Text("Close") } })
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 12.dp, 20.dp, 120.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineLarge); Text("Make Cyan Budget yours", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            SettingsGroup("Money") {
                SettingsRow(Icons.Outlined.AccountBalanceWallet, "Budgets", "Monthly and category limits", onClick = onBudgets)
                SettingsRow(Icons.Outlined.Savings, "Savings goals", "Track milestones and contributions", onClick = onGoals)
                SettingsRow(Icons.Outlined.CurrencyExchange, "Currency", settings.currencyCode, onClick = { choiceDialog = "Currency" })
            }
        }
        item {
            SettingsGroup("Appearance") {
                SettingsRow(Icons.Outlined.DarkMode, "Theme", settings.theme, onClick = { choiceDialog = "Theme" })
                SettingsRow(Icons.Outlined.Palette, "Compact mode", "Reduce spacing for more detail", trailing = { Switch(settings.compactMode, { update { s -> s.copy(compactMode = it) } }) })
                SettingsRow(Icons.Outlined.Security, "Privacy mode", "Hide all financial amounts", trailing = { Switch(settings.privacyMode, { update { s -> s.copy(privacyMode = it) } }) })
            }
        }
        item {
            SettingsGroup("Notifications") {
                SettingsRow(Icons.Outlined.Notifications, "Daily reminder", "Remember to record spending", trailing = { Switch(settings.dailyReminder, { update { s -> s.copy(dailyReminder = it) } }) })
                SettingsRow(Icons.Outlined.Notifications, "Budget alerts", "Threshold and limit warnings", trailing = { Switch(settings.budgetAlerts, { update { s -> s.copy(budgetAlerts = it) } }) })
                SettingsRow(Icons.Outlined.Notifications, "Weekly summary", "A calm financial check-in", trailing = { Switch(settings.weeklySummary, { update { s -> s.copy(weeklySummary = it) } }) })
            }
        }
        item {
            SettingsGroup("Privacy & data") {
                SettingsRow(Icons.Outlined.Fingerprint, "Biometric lock", "Require device authentication", trailing = { Switch(settings.biometricLock, { update { s -> s.copy(biometricLock = it) } }) })
                SettingsRow(Icons.Outlined.Download, "Export & reports", "CSV and PDF with a date range", onClick = onExport)
                SettingsRow(Icons.Outlined.CloudOff, "Offline first", "Financial data stays on this device")
            }
        }
        item { Text("Cyan Budget 1.0.0 · Built for privacy", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp)) }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp), content = { content() }) }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clickable(enabled = onClick != null) { onClick?.invoke() }.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        when { trailing != null -> trailing(); onClick != null -> Icon(Icons.Outlined.ArrowForwardIos, null, modifier = Modifier.padding(4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
