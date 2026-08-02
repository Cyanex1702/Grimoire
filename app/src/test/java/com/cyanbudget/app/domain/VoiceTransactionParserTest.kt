package com.cyanbudget.app.domain

import com.cyanbudget.app.model.Recurrence
import com.cyanbudget.app.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class VoiceTransactionParserTest {
    private val today = LocalDate.of(2026, 8, 1)

    @Test fun `parses lunch expense`() {
        val result = VoiceTransactionParser.parse("Add an expense of 25 dollars for lunch", today)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(2500L, result.amountCents)
        assertEquals("Food & dining", result.category)
        assertEquals("Lunch", result.description)
        assertFalse(result.needsConfirmation)
    }

    @Test fun `parses salary income with comma`() {
        val result = VoiceTransactionParser.parse("Add 2,500 dollars salary income", today)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals(250_000L, result.amountCents)
        assertEquals("Salary", result.category)
    }

    @Test fun `parses recurring subscription`() {
        val result = VoiceTransactionParser.parse("Add a monthly Netflix subscription of 15 dollars", today)
        assertEquals(Recurrence.MONTHLY, result.recurrence)
        assertEquals("Subscriptions", result.category)
        assertEquals(1500L, result.amountCents)
    }

    @Test fun `flags ambiguous command`() {
        val result = VoiceTransactionParser.parse("I paid for something", today)
        assertTrue(result.needsConfirmation)
    }
}
