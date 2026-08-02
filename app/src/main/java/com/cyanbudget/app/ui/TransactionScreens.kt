package com.cyanbudget.app.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cyanbudget.app.domain.ParsedVoiceTransaction
import com.cyanbudget.app.domain.TransactionValidator
import com.cyanbudget.app.model.FinanceTransaction
import com.cyanbudget.app.model.Recurrence
import com.cyanbudget.app.model.TransactionType
import com.cyanbudget.app.model.expenseCategories
import com.cyanbudget.app.model.incomeCategories
import com.cyanbudget.app.model.toLocalDate
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TransactionListScreen(
    title: String,
    type: TransactionType,
    transactions: List<FinanceTransaction>,
    currency: String,
    hidden: Boolean,
    onOpen: (FinanceTransaction) -> Unit,
    onAdd: () -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    var sort by rememberSaveable { mutableStateOf("Newest") }
    val categories = (if (type == TransactionType.EXPENSE) expenseCategories else incomeCategories).map { it.name }
    val filtered = transactions.filter { it.type == type }
        .filter { category == "All" || it.category == category }
        .filter { search.isBlank() || it.description.contains(search, true) || it.notes.contains(search, true) || it.tags.any { tag -> tag.contains(search, true) } }
        .let { list -> when (sort) {
            "Oldest" -> list.sortedBy { it.occurredAt }
            "Highest" -> list.sortedByDescending { it.amountCents }
            "Lowest" -> list.sortedBy { it.amountCents }
            "Category" -> list.sortedBy { it.category }
            else -> list.sortedByDescending { it.occurredAt }
        }}

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 12.dp, 20.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineLarge)
                    Text("${filtered.size} entries", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onAdd) { Text("Add") }
            }
        }
        item {
            OutlinedTextField(
                value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Search, null) }, placeholder = { Text("Search description, notes or tags") }, singleLine = true
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = category == "All", onClick = { category = "All" }, label = { Text("All") }) }
                items(categories) { value -> FilterChip(selected = category == value, onClick = { category = value }, label = { Text(value) }) }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SwapVert, null)
                Text("Sort", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Newest", "Oldest", "Highest", "Lowest", "Category")) { value ->
                        FilterChip(selected = sort == value, onClick = { sort = value }, label = { Text(value) })
                    }
                }
            }
        }
        if (filtered.isEmpty()) item { EmptyState("Nothing found", "Adjust your filters or add a new ${title.lowercase().dropLast(1)}.") }
        items(filtered, key = { it.id }) { item ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                TransactionRow(item, currency, hidden, { onOpen(item) }, Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun TransactionFormScreen(
    initialType: TransactionType,
    existing: FinanceTransaction?,
    voice: ParsedVoiceTransaction?,
    onBack: () -> Unit,
    onSave: (FinanceTransaction) -> Unit,
    onDelete: ((FinanceTransaction) -> Unit)?,
    onDuplicate: ((FinanceTransaction) -> Unit)?
) {
    val context = LocalContext.current
    var type by remember(existing, voice) { mutableStateOf(existing?.type ?: voice?.type ?: initialType) }
    var amount by remember(existing, voice) { mutableStateOf(existing?.let { "%.2f".format(Locale.US, it.amountCents / 100.0) } ?: voice?.amountCents?.let { "%.2f".format(Locale.US, it / 100.0) } ?: "") }
    var category by remember(existing, voice, type) { mutableStateOf(existing?.category ?: voice?.category ?: "") }
    var description by remember(existing, voice) { mutableStateOf(existing?.description ?: voice?.description ?: "") }
    var notes by remember(existing) { mutableStateOf(existing?.notes ?: "") }
    var tags by remember(existing) { mutableStateOf(existing?.tags?.joinToString(", ") ?: "") }
    var paymentMethod by remember(existing) { mutableStateOf(existing?.paymentMethod ?: "Card") }
    var recurrence by remember(existing, voice) { mutableStateOf(existing?.recurrence ?: voice?.recurrence ?: Recurrence.NONE) }
    var occurredAt by remember(existing, voice) { mutableStateOf(existing?.occurredAt ?: voice?.occurredAt ?: System.currentTimeMillis()) }
    var receiptUri by remember(existing) { mutableStateOf(existing?.receiptUri) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showDelete by remember { mutableStateOf(false) }
    val categories = if (type == TransactionType.EXPENSE) expenseCategories.map { it.name } else incomeCategories.map { it.name }

    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> receiptUri = uri?.toString() }
    val descriptionSpeech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { description = it }
    }
    val notesSpeech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { notes = it }
    }
    fun speechIntent(prompt: String) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    }

    if (showDelete && existing != null) AlertDialog(
        onDismissRequest = { showDelete = false },
        title = { Text("Delete transaction?") },
        text = { Text("This removes ${existing.description}. You can undo immediately afterward.") },
        confirmButton = { TextButton(onClick = { showDelete = false; onDelete?.invoke(existing) }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
    )

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (existing == null) "New transaction" else "Edit transaction") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Go back") } },
            actions = {
                if (existing != null) {
                    IconButton(onClick = { onDuplicate?.invoke(existing.copy(id = java.util.UUID.randomUUID().toString(), description = "${existing.description} copy", createdAt = System.currentTimeMillis(), modifiedAt = System.currentTimeMillis())) }) { Icon(Icons.Outlined.ContentCopy, "Duplicate transaction") }
                    IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.DeleteOutline, "Delete transaction", tint = MaterialTheme.colorScheme.error) }
                }
            }
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 8.dp, 20.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (voice != null) item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(if (voice.needsConfirmation) "Review missing details" else "Voice entry ready", style = MaterialTheme.typography.titleMedium)
                        Text("“${voice.originalText}”", Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == TransactionType.EXPENSE, onClick = { type = TransactionType.EXPENSE; category = "" }, label = { Text("Expense") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = type == TransactionType.INCOME, onClick = { type = TransactionType.INCOME; category = "" }, label = { Text("Income") }, modifier = Modifier.weight(1f))
                }
            }
            item {
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' }; errors = errors - "amount" },
                    label = { Text("Amount") }, prefix = { Text("$") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                    isError = errors.containsKey("amount"), supportingText = errors["amount"]?.let { { Text(it) } }
                )
            }
            item {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    items(categories) { value -> FilterChip(selected = category == value, onClick = { category = value; errors = errors - "category" }, label = { Text(value) }) }
                }
                errors["category"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
            item {
                OutlinedTextField(
                    value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { IconButton(onClick = { descriptionSpeech.launch(speechIntent("Describe this transaction")) }) { Icon(Icons.Outlined.Mic, "Dictate description") } },
                    supportingText = errors["description"]?.let { { Text(it) } }, isError = errors.containsKey("description"), singleLine = true
                )
            }
            item {
                OutlinedButton(onClick = {
                    val date = occurredAt.toLocalDate()
                    DatePickerDialog(context, { _, year, month, day -> occurredAt = LocalDate.of(year, month + 1, day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.CalendarMonth, null); Text(occurredAt.toLocalDate().format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")), Modifier.padding(start = 8.dp))
                }
            }
            item { SelectField("Payment method", paymentMethod, listOf("Card", "Cash", "Bank transfer", "Digital wallet", "Other")) { paymentMethod = it } }
            item {
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2,
                    trailingIcon = { IconButton(onClick = { notesSpeech.launch(speechIntent("Add a note")) }) { Icon(Icons.Outlined.Mic, "Dictate notes") } }
                )
            }
            item { OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("Tags (comma separated)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Recurring", style = MaterialTheme.typography.titleMedium); Text("Schedule future entries", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(checked = recurrence != Recurrence.NONE, onCheckedChange = { recurrence = if (it) Recurrence.MONTHLY else Recurrence.NONE })
                }
                if (recurrence != Recurrence.NONE) Box(Modifier.padding(top = 8.dp)) { SelectField("Frequency", recurrence.label, Recurrence.entries.filter { it != Recurrence.NONE }.map { it.label }) { selected -> recurrence = Recurrence.entries.first { it.label == selected } } }
            }
            item {
                OutlinedButton(onClick = { receiptPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Image, null); Text(if (receiptUri == null) "Attach receipt" else "Receipt attached", Modifier.padding(start = 8.dp))
                }
            }
            item {
                Button(onClick = {
                    errors = TransactionValidator.validate(amount, category, description)
                    if (errors.isEmpty()) {
                        val now = System.currentTimeMillis()
                        onSave(FinanceTransaction(
                            id = existing?.id ?: java.util.UUID.randomUUID().toString(), type = type,
                            amountCents = (amount.toDouble() * 100).toLong(), category = category,
                            description = description.ifBlank { category }, occurredAt = occurredAt, notes = notes,
                            tags = tags.split(',').map(String::trim).filter(String::isNotBlank), paymentMethod = paymentMethod,
                            recurrence = recurrence, receiptUri = receiptUri, createdAt = existing?.createdAt ?: now, modifiedAt = now
                        ))
                    }
                }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(if (existing == null) "Save transaction" else "Save changes") }
            }
        }
    }
}

@Composable
private fun SelectField(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: $value", modifier = Modifier.weight(1f))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}
