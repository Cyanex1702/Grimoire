package com.cyanbudget.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cyanbudget.app.model.FinanceTransaction
import com.cyanbudget.app.model.TransactionType
import com.cyanbudget.app.model.asMoney
import com.cyanbudget.app.model.expenseCategories
import com.cyanbudget.app.model.toLocalDate
import com.cyanbudget.app.ui.theme.Coral
import com.cyanbudget.app.ui.theme.Emerald
import java.time.format.DateTimeFormatter

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) Text(
            action,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable(onClick = onAction).padding(8.dp)
        )
    }
}

@Composable
fun MetricCard(label: String, value: String, supporting: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(color, CircleShape))
                Text(label, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TransactionRow(
    transaction: FinanceTransaction,
    currency: String,
    hidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (transaction.type == TransactionType.INCOME) Emerald else Coral
    val category = expenseCategories.firstOrNull { it.name == transaction.category }
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(Modifier.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(category?.let { Color(it.color) }?.copy(alpha = .14f) ?: color.copy(alpha = .14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(transaction.category.take(1), fontWeight = FontWeight.Bold, color = category?.let { Color(it.color) } ?: color)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(transaction.description.ifBlank { transaction.category }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                Text("${transaction.category} · ${transaction.occurredAt.toLocalDate().format(DateTimeFormatter.ofPattern("MMM d"))}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                (if (transaction.type == TransactionType.INCOME) "+" else "−") + transaction.amountCents.asMoney(currency, hidden),
                color = color,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { contentDescription = if (transaction.type == TransactionType.INCOME) "Income" else "Expense" }
            )
        }
    }
}

@Composable
fun BudgetProgress(
    title: String,
    spent: Long,
    limit: Long,
    currency: String,
    hidden: Boolean,
    modifier: Modifier = Modifier
) {
    val ratio = if (limit == 0L) 0f else (spent.toFloat() / limit).coerceIn(0f, 1f)
    val color = when {
        ratio >= .9f -> Coral
        ratio >= .75f -> Color(0xFFF0A63B)
        else -> MaterialTheme.colorScheme.primary
    }
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${(ratio * 100).toInt()}%", color = color, style = MaterialTheme.typography.labelLarge)
            }
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(8.dp),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            Text("${spent.asMoney(currency, hidden)} of ${limit.asMoney(currency, hidden)}", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DonutChart(values: List<Pair<String, Long>>, colors: List<Color>, modifier: Modifier = Modifier) {
    val total = values.sumOf { it.second }.toFloat().coerceAtLeast(1f)
    Canvas(modifier.semantics { contentDescription = "Spending by category donut chart" }) {
        var start = -90f
        values.forEachIndexed { index, value ->
            val sweep = value.second / total * 360f
            drawArc(colors[index % colors.size], start, sweep - 2f, false, style = Stroke(width = size.minDimension * .16f, cap = StrokeCap.Round))
            start += sweep
        }
    }
}

@Composable
fun Bars(values: List<Long>, color: Color, modifier: Modifier = Modifier) {
    val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
    Canvas(modifier.semantics { contentDescription = "Spending bar chart" }) {
        val gap = size.width / (values.size * 3f)
        val barWidth = gap * 1.7f
        values.forEachIndexed { i, value ->
            val height = size.height * value / max
            val x = gap + i * (barWidth + gap)
            drawLine(color.copy(alpha = .16f), Offset(x + barWidth / 2, 0f), Offset(x + barWidth / 2, size.height), barWidth, StrokeCap.Round)
            drawLine(color, Offset(x + barWidth / 2, size.height), Offset(x + barWidth / 2, size.height - height), barWidth, StrokeCap.Round)
        }
    }
}

@Composable
fun EmptyState(title: String, message: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.ReceiptLong, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    }
}
