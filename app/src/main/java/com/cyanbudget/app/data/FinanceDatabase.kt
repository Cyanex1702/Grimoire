package com.cyanbudget.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.cyanbudget.app.BuildConfig
import com.cyanbudget.app.model.Budget
import com.cyanbudget.app.model.FinanceTransaction
import com.cyanbudget.app.model.Recurrence
import com.cyanbudget.app.model.SavingsGoal
import com.cyanbudget.app.model.TransactionType
import java.time.LocalDateTime
import java.time.ZoneId

class FinanceDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE transactions (
                id TEXT PRIMARY KEY NOT NULL,
                type TEXT NOT NULL,
                amount_cents INTEGER NOT NULL CHECK(amount_cents > 0),
                category TEXT NOT NULL,
                description TEXT NOT NULL,
                occurred_at INTEGER NOT NULL,
                notes TEXT NOT NULL DEFAULT '',
                tags TEXT NOT NULL DEFAULT '',
                payment_method TEXT NOT NULL DEFAULT 'Card',
                recurrence TEXT NOT NULL DEFAULT 'NONE',
                receipt_uri TEXT,
                created_at INTEGER NOT NULL,
                modified_at INTEGER NOT NULL
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX idx_transactions_date ON transactions(occurred_at DESC)")
        db.execSQL("CREATE INDEX idx_transactions_type_category ON transactions(type, category)")
        db.execSQL(
            """CREATE TABLE budgets (
                id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, limit_cents INTEGER NOT NULL,
                category TEXT, period TEXT NOT NULL, alerts_enabled INTEGER NOT NULL DEFAULT 1
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE savings_goals (
                id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, target_cents INTEGER NOT NULL,
                saved_cents INTEGER NOT NULL DEFAULT 0, target_epoch_day INTEGER, color INTEGER NOT NULL
            )""".trimIndent()
        )
        if (BuildConfig.DEBUG) seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun transactions(): List<FinanceTransaction> = readableDatabase.query(
        "transactions", null, null, null, null, null, "occurred_at DESC"
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(
                FinanceTransaction(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    type = TransactionType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("type"))),
                    amountCents = cursor.getLong(cursor.getColumnIndexOrThrow("amount_cents")),
                    category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    occurredAt = cursor.getLong(cursor.getColumnIndexOrThrow("occurred_at")),
                    notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")),
                    tags = cursor.getString(cursor.getColumnIndexOrThrow("tags")).split('|').filter(String::isNotBlank),
                    paymentMethod = cursor.getString(cursor.getColumnIndexOrThrow("payment_method")),
                    recurrence = Recurrence.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("recurrence"))),
                    receiptUri = cursor.getString(cursor.getColumnIndexOrThrow("receipt_uri")),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                    modifiedAt = cursor.getLong(cursor.getColumnIndexOrThrow("modified_at"))
                )
            )
        }
    }

    fun upsert(transaction: FinanceTransaction) {
        writableDatabase.insertWithOnConflict("transactions", null, transaction.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteTransaction(id: String) {
        writableDatabase.delete("transactions", "id = ?", arrayOf(id))
    }

    fun budgets(): List<Budget> = readableDatabase.query("budgets", null, null, null, null, null, "name").use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(Budget(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                limitCents = cursor.getLong(cursor.getColumnIndexOrThrow("limit_cents")),
                category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                period = cursor.getString(cursor.getColumnIndexOrThrow("period")),
                alertsEnabled = cursor.getInt(cursor.getColumnIndexOrThrow("alerts_enabled")) == 1
            ))
        }
    }

    fun upsert(budget: Budget) {
        val values = ContentValues().apply {
            put("id", budget.id); put("name", budget.name); put("limit_cents", budget.limitCents)
            put("category", budget.category); put("period", budget.period); put("alerts_enabled", if (budget.alertsEnabled) 1 else 0)
        }
        writableDatabase.insertWithOnConflict("budgets", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteBudget(id: String) { writableDatabase.delete("budgets", "id = ?", arrayOf(id)) }

    fun goals(): List<SavingsGoal> = readableDatabase.query("savings_goals", null, null, null, null, null, "name").use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(SavingsGoal(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                targetCents = cursor.getLong(cursor.getColumnIndexOrThrow("target_cents")),
                savedCents = cursor.getLong(cursor.getColumnIndexOrThrow("saved_cents")),
                targetEpochDay = if (cursor.isNull(cursor.getColumnIndexOrThrow("target_epoch_day"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("target_epoch_day")),
                color = cursor.getLong(cursor.getColumnIndexOrThrow("color"))
            ))
        }
    }

    fun upsert(goal: SavingsGoal) {
        val values = ContentValues().apply {
            put("id", goal.id); put("name", goal.name); put("target_cents", goal.targetCents)
            put("saved_cents", goal.savedCents); put("target_epoch_day", goal.targetEpochDay); put("color", goal.color)
        }
        writableDatabase.insertWithOnConflict("savings_goals", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteGoal(id: String) { writableDatabase.delete("savings_goals", "id = ?", arrayOf(id)) }

    private fun FinanceTransaction.toValues() = ContentValues().apply {
        put("id", id); put("type", type.name); put("amount_cents", amountCents); put("category", category)
        put("description", description); put("occurred_at", occurredAt); put("notes", notes)
        put("tags", tags.joinToString("|")); put("payment_method", paymentMethod); put("recurrence", recurrence.name)
        put("receipt_uri", receiptUri); put("created_at", createdAt); put("modified_at", modifiedAt)
    }

    private fun seed(db: SQLiteDatabase) {
        val now = LocalDateTime.now()
        val sample = listOf(
            FinanceTransaction(type = TransactionType.INCOME, amountCents = 420_000, category = "Salary", description = "Monthly salary", occurredAt = now.minusDays(8).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), paymentMethod = "Bank transfer"),
            FinanceTransaction(type = TransactionType.EXPENSE, amountCents = 8_400, category = "Groceries", description = "Weekly groceries", occurredAt = now.minusDays(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()),
            FinanceTransaction(type = TransactionType.EXPENSE, amountCents = 2_450, category = "Food & dining", description = "Lunch with Sam", occurredAt = now.minusHours(3).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()),
            FinanceTransaction(type = TransactionType.EXPENSE, amountCents = 1_500, category = "Subscriptions", description = "Netflix", occurredAt = now.minusDays(4).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), recurrence = Recurrence.MONTHLY),
            FinanceTransaction(type = TransactionType.EXPENSE, amountCents = 12_500, category = "Bills & utilities", description = "Electricity bill", occurredAt = now.minusDays(6).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()),
            FinanceTransaction(type = TransactionType.INCOME, amountCents = 56_000, category = "Freelance", description = "Brand project", occurredAt = now.minusDays(15).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        )
        sample.forEach { db.insert("transactions", null, it.toValues()) }
        val budget = Budget(name = "Monthly essentials", limitCents = 180_000)
        val b = ContentValues().apply { put("id", budget.id); put("name", budget.name); put("limit_cents", budget.limitCents); putNull("category"); put("period", budget.period); put("alerts_enabled", 1) }
        db.insert("budgets", null, b)
        val goal = SavingsGoal(name = "Emergency fund", targetCents = 600_000, savedCents = 215_000)
        val g = ContentValues().apply { put("id", goal.id); put("name", goal.name); put("target_cents", goal.targetCents); put("saved_cents", goal.savedCents); putNull("target_epoch_day"); put("color", goal.color) }
        db.insert("savings_goals", null, g)
    }

    companion object {
        private const val DATABASE_NAME = "cyan_budget.db"
        private const val DATABASE_VERSION = 1
        @Volatile private var instance: FinanceDatabase? = null
        fun get(context: Context): FinanceDatabase = instance ?: synchronized(this) {
            instance ?: FinanceDatabase(context.applicationContext).also { instance = it }
        }
    }
}
