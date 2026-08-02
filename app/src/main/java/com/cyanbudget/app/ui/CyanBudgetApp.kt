package com.cyanbudget.app.ui

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyanbudget.app.MainViewModel
import com.cyanbudget.app.domain.ParsedVoiceTransaction
import com.cyanbudget.app.domain.VoiceTransactionParser
import com.cyanbudget.app.model.FinanceTransaction
import com.cyanbudget.app.model.TransactionType
import com.cyanbudget.app.ui.theme.CyanBudgetTheme
import kotlinx.coroutines.launch

private enum class MainTab(val label: String) { HOME("Home"), INCOME("Income"), EXPENSES("Expenses"), REPORTS("Reports"), SETTINGS("Settings") }
private enum class Overlay { FORM, BUDGETS, GOALS }

@Composable
fun CyanBudgetApp(startDestination: String?, accessGranted: Boolean = true, onUnlock: () -> Unit = {}, viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    CyanBudgetTheme(state.settings) {
        if (!accessGranted) LockScreen(onUnlock)
        else if (state.loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (!state.settings.onboardingComplete) OnboardingScreen(state.settings) { chosen -> viewModel.updateSettings { chosen } }
        else MainShell(state, startDestination, viewModel)
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp))
        Text("Cyan Budget is locked", style = MaterialTheme.typography.headlineMedium)
        Text("Authenticate to view your financial information.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        Button(onClick = onUnlock) { Text("Unlock") }
    }
}

@Composable
private fun MainShell(state: com.cyanbudget.app.FinanceUiState, startDestination: String?, viewModel: MainViewModel) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(when (startDestination) { "home" -> MainTab.HOME; else -> MainTab.HOME }) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    var formType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editing by remember { mutableStateOf<FinanceTransaction?>(null) }
    var voiceEntry by remember { mutableStateOf<ParsedVoiceTransaction?>(null) }
    var addSheet by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { spoken ->
            voiceEntry = VoiceTransactionParser.parse(spoken)
            formType = voiceEntry!!.type
            editing = null
            overlay = Overlay.FORM
        } ?: scope.launch { snackbar.showSnackbar("Voice entry was cancelled") }
    }
    fun launchVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Try: I spent 25 dollars on lunch")
        }
        runCatching { voiceLauncher.launch(intent) }.onFailure { scope.launch { snackbar.showSnackbar("Speech recognition is not available on this device") } }
    }
    fun openForm(type: TransactionType, transaction: FinanceTransaction? = null) {
        formType = type; editing = transaction; voiceEntry = null; overlay = Overlay.FORM; addSheet = false
    }

    LaunchedEffect(startDestination) {
        when (startDestination) {
            "add" -> addSheet = true
            "voice" -> launchVoice()
        }
    }
    BackHandler(enabled = overlay != null) { overlay = null }

    if (addSheet) ModalBottomSheet(onDismissRequest = { addSheet = false }) {
        ListItem(headlineContent = { Text("Add expense") }, supportingContent = { Text("Record something you paid for") }, leadingContent = { Icon(Icons.Outlined.Payments, null) }, modifier = Modifier.clickable { openForm(TransactionType.EXPENSE) }.padding(horizontal = 8.dp), trailingContent = null)
        androidx.compose.material3.HorizontalDivider()
        ListItem(headlineContent = { Text("Add income") }, supportingContent = { Text("Record money you received") }, leadingContent = { Icon(Icons.Outlined.AttachMoney, null) }, modifier = Modifier.clickable { openForm(TransactionType.INCOME) }.padding(horizontal = 8.dp))
        androidx.compose.material3.HorizontalDivider()
        ListItem(headlineContent = { Text("Use voice") }, supportingContent = { Text("Speak naturally, then review") }, leadingContent = { Icon(Icons.Outlined.Mic, null) }, modifier = Modifier.clickable { addSheet = false; launchVoice() }.padding(horizontal = 8.dp))
        Spacer(Modifier.padding(bottom = 24.dp))
    }

    when (overlay) {
        Overlay.FORM -> TransactionFormScreen(
            initialType = formType, existing = editing, voice = voiceEntry, onBack = { overlay = null },
            onSave = { viewModel.save(it); overlay = null; scope.launch { snackbar.showSnackbar("Transaction saved") } },
            onDelete = if (editing != null) ({ item -> viewModel.delete(item); overlay = null; scope.launch {
                val result = snackbar.showSnackbar("Transaction deleted", "Undo")
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) viewModel.undoDelete(item)
            } }) else null,
            onDuplicate = if (editing != null) ({ item -> viewModel.save(item); overlay = null; scope.launch { snackbar.showSnackbar("Transaction duplicated") } }) else null
        )
        Overlay.BUDGETS -> BudgetsScreen(state, { overlay = null }, viewModel::save, viewModel::delete)
        Overlay.GOALS -> GoalsScreen(state, { overlay = null }, viewModel::save, viewModel::delete)
        null -> Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = { FloatingActionButton(onClick = { addSheet = true }) { Icon(Icons.Filled.Add, "Quick add transaction") } },
            bottomBar = {
                NavigationBar {
                    MainTab.entries.forEach { item ->
                        val icon = when (item) {
                            MainTab.HOME -> Icons.Outlined.Home
                            MainTab.INCOME -> Icons.Outlined.AttachMoney
                            MainTab.EXPENSES -> Icons.Outlined.Payments
                            MainTab.REPORTS -> Icons.Outlined.Assessment
                            MainTab.SETTINGS -> Icons.Outlined.Settings
                        }
                        NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(icon, item.label) }, label = { Text(item.label) })
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding(), top = padding.calculateTopPadding())) {
                when (tab) {
                    MainTab.HOME -> DashboardScreen(state, {
                        context.getSharedPreferences("widget", android.content.Context.MODE_PRIVATE).edit().putBoolean("privacy", !state.settings.privacyMode).apply()
                        viewModel.updateSettings { it.copy(privacyMode = !it.privacyMode) }
                    }, ::openForm, ::launchVoice, { openForm(it.type, it) }, { overlay = Overlay.BUDGETS }, { overlay = Overlay.GOALS }, { tab = MainTab.EXPENSES })
                    MainTab.INCOME -> TransactionListScreen("Income", TransactionType.INCOME, state.transactions, state.settings.currencyCode, state.settings.privacyMode, { openForm(it.type, it) }, { openForm(TransactionType.INCOME) })
                    MainTab.EXPENSES -> TransactionListScreen("Expenses", TransactionType.EXPENSE, state.transactions, state.settings.currencyCode, state.settings.privacyMode, { openForm(it.type, it) }, { openForm(TransactionType.EXPENSE) })
                    MainTab.REPORTS -> ReportsScreen(state)
                    MainTab.SETTINGS -> SettingsScreen(state, viewModel::updateSettings, { overlay = Overlay.BUDGETS }, { overlay = Overlay.GOALS }, { tab = MainTab.REPORTS })
                }
            }
        }
    }
}
