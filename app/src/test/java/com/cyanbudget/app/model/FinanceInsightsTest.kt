package com.cyanbudget.app.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FinanceInsightsTest {
    private fun date(value: LocalDate) = value.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test fun `summary separates income expense and category totals`() {
        val today = LocalDate.of(2026, 8, 2)
        val items = listOf(
            FinanceTransaction(type = TransactionType.INCOME, amountCents = 100_000, category = "Salary", description = "Pay", occurredAt = date(today)),
            FinanceTransaction(type = TransactionType.EXPENSE, amountCents = 2_500, category = "Food & dining", description = "Lunch", occurredAt = date(today)),
            FinanceTransaction(type = TransactionType.EXPENSE, amountCents = 5_000, category = "Groceries", description = "Shop", occurredAt = date(today.minusDays(1)))
        )
        val summary = FinanceInsights.summary(items, today)
        assertEquals(92_500L, summary.balanceCents)
        assertEquals(7_500L, summary.monthExpenseCents)
        assertEquals(2_500L, summary.todayCents)
        assertEquals(5_000L, summary.categoryTotals["Groceries"])
    }
}
