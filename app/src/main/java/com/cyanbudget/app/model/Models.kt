package com.cyanbudget.app.model

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import java.util.UUID

enum class TransactionType { INCOME, EXPENSE }

enum class Recurrence(val label: String) {
    NONE("One time"), DAILY("Daily"), WEEKLY("Weekly"), BIWEEKLY("Every two weeks"),
    MONTHLY("Monthly"), QUARTERLY("Quarterly"), YEARLY("Yearly")
}

data class FinanceTransaction(
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val amountCents: Long,
    val category: String,
    val description: String,
    val occurredAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val paymentMethod: String = "Card",
    val recurrence: Recurrence = Recurrence.NONE,
    val receiptUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

data class Budget(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val limitCents: Long,
    val category: String? = null,
    val period: String = "Monthly",
    val alertsEnabled: Boolean = true
)

data class SavingsGoal(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetCents: Long,
    val savedCents: Long = 0,
    val targetEpochDay: Long? = null,
    val color: Long = 0xFF377CF6
)

data class Category(
    val name: String,
    val icon: String,
    val color: Long,
    val type: TransactionType
)

data class AppSettings(
    val onboardingComplete: Boolean = false,
    val currencyCode: String = "USD",
    val theme: String = "System",
    val accent: Long = 0xFF377CF6,
    val privacyMode: Boolean = false,
    val monthlyBudgetCents: Long = 300_000,
    val compactMode: Boolean = false,
    val dailyReminder: Boolean = false,
    val budgetAlerts: Boolean = true,
    val weeklySummary: Boolean = true,
    val biometricLock: Boolean = false
)

data class DashboardSummary(
    val balanceCents: Long = 0,
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    val todayCents: Long = 0,
    val weekCents: Long = 0,
    val monthExpenseCents: Long = 0,
    val categoryTotals: Map<String, Long> = emptyMap()
)

val expenseCategories = listOf(
    Category("Food & dining", "restaurant", 0xFFFF7A66, TransactionType.EXPENSE),
    Category("Groceries", "cart", 0xFF42B883, TransactionType.EXPENSE),
    Category("Transportation", "car", 0xFF5687F5, TransactionType.EXPENSE),
    Category("Shopping", "bag", 0xFFA57BEA, TransactionType.EXPENSE),
    Category("Bills & utilities", "bolt", 0xFFF2B84B, TransactionType.EXPENSE),
    Category("Rent", "home", 0xFF4E6A86, TransactionType.EXPENSE),
    Category("Entertainment", "movie", 0xFFE368A3, TransactionType.EXPENSE),
    Category("Health", "health", 0xFF29A7A1, TransactionType.EXPENSE),
    Category("Education", "book", 0xFF6B79D6, TransactionType.EXPENSE),
    Category("Travel", "plane", 0xFF38A6D5, TransactionType.EXPENSE),
    Category("Subscriptions", "repeat", 0xFFED706B, TransactionType.EXPENSE),
    Category("Personal care", "person", 0xFFD488BA, TransactionType.EXPENSE),
    Category("Gifts", "gift", 0xFFE0A23B, TransactionType.EXPENSE),
    Category("Other", "more", 0xFF8090A0, TransactionType.EXPENSE)
)

val incomeCategories = listOf(
    Category("Salary", "wallet", 0xFF18A77A, TransactionType.INCOME),
    Category("Freelance", "laptop", 0xFF377CF6, TransactionType.INCOME),
    Category("Business", "store", 0xFF6C79D8, TransactionType.INCOME),
    Category("Investment", "chart", 0xFF12A3A8, TransactionType.INCOME),
    Category("Gift", "gift", 0xFFE0A23B, TransactionType.INCOME),
    Category("Refund", "undo", 0xFF3B9DA5, TransactionType.INCOME),
    Category("Bonus", "star", 0xFFAC78DA, TransactionType.INCOME),
    Category("Other", "more", 0xFF8090A0, TransactionType.INCOME)
)

fun Long.asMoney(currencyCode: String, hidden: Boolean = false): String {
    if (hidden) return "••••••"
    return runCatching {
        NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(currencyCode) }
            .format(this / 100.0)
    }.getOrElse { "${currencyCode.uppercase()} ${"%.2f".format(Locale.US, this / 100.0)}" }
}

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

object FinanceInsights {
    fun summary(items: List<FinanceTransaction>, now: LocalDate = LocalDate.now()): DashboardSummary {
        val income = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
        val expenseItems = items.filter { it.type == TransactionType.EXPENSE }
        val expenses = expenseItems.sumOf { it.amountCents }
        val month = expenseItems.filter {
            val date = it.occurredAt.toLocalDate()
            date.year == now.year && date.month == now.month
        }
        return DashboardSummary(
            balanceCents = income - expenses,
            incomeCents = income,
            expenseCents = expenses,
            todayCents = expenseItems.filter { it.occurredAt.toLocalDate() == now }.sumOf { it.amountCents },
            weekCents = expenseItems.filter { !it.occurredAt.toLocalDate().isBefore(now.minusDays(6)) }.sumOf { it.amountCents },
            monthExpenseCents = month.sumOf { it.amountCents },
            categoryTotals = month.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amountCents } }
        )
    }
}
