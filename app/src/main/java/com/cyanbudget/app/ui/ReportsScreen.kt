package com.cyanbudget.app.ui

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cyanbudget.app.FinanceUiState
import com.cyanbudget.app.model.FinanceTransaction
import com.cyanbudget.app.model.TransactionType
import com.cyanbudget.app.model.asMoney
import com.cyanbudget.app.model.toLocalDate
import com.cyanbudget.app.ui.theme.Coral
import com.cyanbudget.app.ui.theme.Emerald
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit

@Composable
fun ReportsScreen(state: FinanceUiState) {
    var period by rememberSaveable { mutableStateOf("Month") }
    var exportMenu by remember { mutableStateOf(false) }
    var exportType by remember { mutableStateOf("CSV") }
    val context = LocalContext.current
    val today = LocalDate.now()
    val filtered = remember(state.transactions, period) { state.transactions.filter { it.occurredAt.toLocalDate() in rangeFor(period, today) } }
    val income = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
    val expenseItems = filtered.filter { it.type == TransactionType.EXPENSE }
    val expense = expenseItems.sumOf { it.amountCents }
    val categories = expenseItems.groupBy { it.category }.mapValues { it.value.sumOf(FinanceTransaction::amountCents) }.entries.sortedByDescending { it.value }
    val dailyValues = (6 downTo 0).map { offset -> expenseItems.filter { it.occurredAt.toLocalDate() == today.minusDays(offset.toLong()) }.sumOf { it.amountCents } }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.use { output ->
            if (exportType == "PDF") writePdf(output, filtered, state.settings.currencyCode)
            else output.write(toCsv(filtered).toByteArray())
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 12.dp, 20.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Reports", style = MaterialTheme.typography.headlineLarge); Text("See where your money goes", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Box {
                    Button(onClick = { exportMenu = true }) { Icon(Icons.Outlined.Download, null); Text("Export", Modifier.padding(start = 6.dp)) }
                    DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                        listOf("CSV", "PDF").forEach { type -> DropdownMenuItem(text = { Text("Export $type") }, onClick = {
                            exportType = type; exportMenu = false; exporter.launch("cyan-budget-${today}.${if (type == "PDF") "pdf" else "csv"}")
                        }) }
                    }
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Today", "Week", "Month", "Year", "All time")) { value -> FilterChip(selected = period == value, onClick = { period = value }, label = { Text(value) }) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Income", income.asMoney(state.settings.currencyCode, state.settings.privacyMode), "in this period", Emerald, Modifier.weight(1f))
                MetricCard("Expenses", expense.asMoney(state.settings.currencyCode, state.settings.privacyMode), "in this period", Coral, Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Income vs expenses", style = MaterialTheme.typography.titleLarge)
                    val maximum = maxOf(income, expense, 1)
                    ComparisonBar("Income", income, maximum, Emerald, state.settings.currencyCode, state.settings.privacyMode)
                    ComparisonBar("Expenses", expense, maximum, Coral, state.settings.currencyCode, state.settings.privacyMode)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Daily spending", style = MaterialTheme.typography.titleLarge)
                    Bars(dailyValues, MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth().height(150.dp).padding(top = 20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Top categories", style = MaterialTheme.typography.titleLarge)
                    if (categories.isEmpty()) Text("No expenses in this period", Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    categories.take(6).forEachIndexed { index, entry ->
                        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(chartColors[index % chartColors.size], CircleShape))
                            Text(entry.key, Modifier.padding(start = 10.dp).weight(1f))
                            Text(entry.value.asMoney(state.settings.currencyCode, state.settings.privacyMode), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val range = rangeFor(period, today)
                val days = (ChronoUnit.DAYS.between(range.start, range.endInclusive) + 1).coerceIn(1, 3650)
                MetricCard("Daily average", (expense / days).asMoney(state.settings.currencyCode, state.settings.privacyMode), "per active range", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MetricCard("Recurring", expenseItems.count { it.recurrence != com.cyanbudget.app.model.Recurrence.NONE }.toString(), "scheduled expenses", Color(0xFFA27AE7), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ComparisonBar(label: String, value: Long, max: Long, color: Color, currency: String, hidden: Boolean) {
    Column(Modifier.padding(top = 16.dp)) {
        Row { Text(label, Modifier.weight(1f)); Text(value.asMoney(currency, hidden), style = MaterialTheme.typography.labelLarge) }
        androidx.compose.material3.LinearProgressIndicator(
            progress = { value.toFloat() / max }, modifier = Modifier.fillMaxWidth().height(9.dp).padding(top = 4.dp),
            color = color, trackColor = color.copy(alpha = .13f)
        )
    }
}

private val chartColors = listOf(Coral, Emerald, Color(0xFF377CF6), Color(0xFFF0B54A), Color(0xFFA27AE7))

private fun rangeFor(period: String, today: LocalDate): ClosedRange<LocalDate> = when (period) {
    "Today" -> today..today
    "Week" -> today.minusDays(6)..today
    "Month" -> today.with(TemporalAdjusters.firstDayOfMonth())..today.with(TemporalAdjusters.lastDayOfMonth())
    "Year" -> LocalDate.of(today.year, 1, 1)..LocalDate.of(today.year, 12, 31)
    else -> LocalDate.of(1970, 1, 1)..LocalDate.of(2100, 1, 1)
}

private fun toCsv(items: List<FinanceTransaction>): String = buildString {
    appendLine("id,type,amount,category,description,date,payment_method,recurrence,notes,tags,receipt")
    items.forEach { t -> appendLine(listOf(t.id, t.type.name, t.amountCents / 100.0, t.category, t.description, t.occurredAt.toLocalDate(), t.paymentMethod, t.recurrence.name, t.notes, t.tags.joinToString("|"), t.receiptUri.orEmpty()).joinToString(",") { "\"${it.toString().replace("\"", "\"\"")}\"" }) }
}

private fun writePdf(output: java.io.OutputStream, items: List<FinanceTransaction>, currency: String) {
    val document = PdfDocument()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var pageNumber = 1
    var index = 0
    while (index < items.size.coerceAtLeast(1)) {
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create())
        paint.color = Color(0xFF0D213A).toArgb(); paint.textSize = 24f; paint.isFakeBoldText = true
        page.canvas.drawText("Cyan Budget report", 40f, 55f, paint)
        paint.textSize = 11f; paint.isFakeBoldText = false
        if (items.isEmpty()) page.canvas.drawText("No transactions in this date range.", 40f, 95f, paint)
        var y = 95f
        while (index < items.size && y < 800f) {
            val t = items[index++]
            page.canvas.drawText("${t.occurredAt.toLocalDate()}  ${t.description.take(42)}", 40f, y, paint)
            page.canvas.drawText((if (t.type == TransactionType.EXPENSE) "−" else "+") + t.amountCents.asMoney(currency), 450f, y, paint)
            y += 25f
        }
        document.finishPage(page)
    }
    document.writeTo(output); document.close()
}
