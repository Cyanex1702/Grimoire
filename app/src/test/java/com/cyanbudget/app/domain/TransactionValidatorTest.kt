package com.cyanbudget.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionValidatorTest {
    @Test fun `accepts valid transaction`() {
        assertTrue(TransactionValidator.validate("25.50", "Groceries", "Weekly shop").isEmpty())
    }

    @Test fun `rejects zero amount and missing category`() {
        val errors = TransactionValidator.validate("0", "", "")
        assertTrue(errors.containsKey("amount"))
        assertTrue(errors.containsKey("category"))
    }

    @Test fun `rejects implausibly large amount`() {
        assertFalse(TransactionValidator.validate("1000000000", "Other", "Large").isEmpty())
    }
}
