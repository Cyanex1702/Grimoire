package com.cyanbudget.app.domain

import com.cyanbudget.app.model.Recurrence
import com.cyanbudget.app.model.TransactionType
import java.time.LocalDate
import java.time.ZoneId

data class ParsedVoiceTransaction(
    val type: TransactionType,
    val amountCents: Long?,
    val category: String?,
    val description: String,
    val occurredAt: Long,
    val recurrence: Recurrence,
    val originalText: String
) {
    val needsConfirmation: Boolean get() = amountCents == null || category == null
}

object VoiceTransactionParser {
    private val amountRegex = Regex("(?:[$£€]|usd\\s*)?([0-9]+(?:[,][0-9]{3})*(?:[.][0-9]{1,2})?)", RegexOption.IGNORE_CASE)

    fun parse(text: String, today: LocalDate = LocalDate.now()): ParsedVoiceTransaction {
        val lower = text.lowercase()
        val type = if (listOf("income", "salary", "earned", "received", "refund", "bonus").any(lower::contains)) {
            TransactionType.INCOME
        } else TransactionType.EXPENSE
        val amount = amountRegex.find(lower)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        val category = detectCategory(lower, type)
        val date = when {
            "yesterday" in lower -> today.minusDays(1)
            else -> today
        }
        val recurrence = when {
            "every two weeks" in lower || "biweekly" in lower -> Recurrence.BIWEEKLY
            "daily" in lower -> Recurrence.DAILY
            "weekly" in lower -> Recurrence.WEEKLY
            "monthly" in lower || "subscription" in lower -> Recurrence.MONTHLY
            "quarterly" in lower -> Recurrence.QUARTERLY
            "yearly" in lower || "annual" in lower -> Recurrence.YEARLY
            else -> Recurrence.NONE
        }
        val description = when {
            "lunch" in lower -> "Lunch"
            "grocer" in lower -> "Groceries"
            "fuel" in lower || "gas" in lower -> "Fuel"
            "netflix" in lower -> "Netflix"
            "salary" in lower -> "Salary"
            else -> text.trim().replaceFirstChar { it.uppercase() }
        }
        return ParsedVoiceTransaction(
            type = type,
            amountCents = amount?.times(100)?.toLong(),
            category = category,
            description = description,
            occurredAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            recurrence = recurrence,
            originalText = text
        )
    }

    private fun detectCategory(text: String, type: TransactionType): String? {
        if (type == TransactionType.INCOME) return when {
            "salary" in text -> "Salary"
            "freelance" in text -> "Freelance"
            "business" in text -> "Business"
            "investment" in text -> "Investment"
            "refund" in text -> "Refund"
            "bonus" in text -> "Bonus"
            "gift" in text -> "Gift"
            else -> null
        }
        return when {
            listOf("lunch", "dinner", "breakfast", "restaurant", "coffee").any(text::contains) -> "Food & dining"
            "grocer" in text -> "Groceries"
            listOf("fuel", "gas", "taxi", "uber", "bus", "transport").any(text::contains) -> "Transportation"
            listOf("netflix", "spotify", "subscription").any(text::contains) -> "Subscriptions"
            listOf("rent", "landlord").any(text::contains) -> "Rent"
            listOf("electric", "water", "internet", "bill").any(text::contains) -> "Bills & utilities"
            listOf("doctor", "medicine", "health").any(text::contains) -> "Health"
            listOf("school", "course", "book", "education").any(text::contains) -> "Education"
            listOf("flight", "hotel", "travel").any(text::contains) -> "Travel"
            listOf("movie", "game", "concert").any(text::contains) -> "Entertainment"
            listOf("shop", "clothes", "shoes").any(text::contains) -> "Shopping"
            else -> null
        }
    }
}

object TransactionValidator {
    fun validate(amountText: String, category: String, description: String): Map<String, String> = buildMap {
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) put("amount", "Enter an amount greater than zero")
        if (amount != null && amount > 999_999_999) put("amount", "Amount is too large")
        if (category.isBlank()) put("category", "Choose a category")
        if (description.length > 120) put("description", "Keep the description under 120 characters")
    }
}
