package com.example.fincontrol.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.example.fincontrol.model.CategoryExpenseSummary
import com.example.fincontrol.model.DashboardSummary
import com.example.fincontrol.model.FinanceTransaction
import com.example.fincontrol.model.RecurringRule
import com.example.fincontrol.model.TransactionType
import com.example.fincontrol.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

class TransactionRepository(context: Context) {
    private val dbHelper = FinControlDatabaseHelper(context)

    data class SaveTransactionRequest(
        val userId: Long,
        val categoryId: Long,
        val amountCents: Long,
        val type: TransactionType,
        val date: LocalDate,
        val description: String?,
        val recurring: Boolean
    )

    fun saveTransaction(request: SaveTransactionRequest): Result<Long> {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        return try {
            val transactionId = insertTransaction(db, request.userId, request.categoryId, request.amountCents, request.type, request.date, request.description, null)
            if (request.recurring) {
                val ruleValues = ContentValues().apply {
                    put("user_id", request.userId)
                    put("category_id", request.categoryId)
                    put("amount_cents", request.amountCents)
                    put("type", request.type.dbValue)
                    put("description", request.description)
                    put("start_date", request.date.toString())
                    put("day_of_month", request.date.dayOfMonth)
                    put("active", 1)
                }
                val ruleId = db.insertOrThrow("recurring_rules", null, ruleValues)
                val updateValues = ContentValues().apply { put("recurring_rule_id", ruleId) }
                db.update("transactions", updateValues, "id = ?", arrayOf(transactionId.toString()))
            }
            db.setTransactionSuccessful()
            Result.success(transactionId)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            db.endTransaction()
        }
    }

    private fun insertTransaction(
        db: android.database.sqlite.SQLiteDatabase,
        userId: Long,
        categoryId: Long,
        amountCents: Long,
        type: TransactionType,
        date: LocalDate,
        description: String?,
        recurringRuleId: Long?
    ): Long {
        val values = ContentValues().apply {
            put("user_id", userId)
            put("category_id", categoryId)
            put("amount_cents", amountCents)
            put("type", type.dbValue)
            put("transaction_date", date.toString())
            put("description", description)
            if (recurringRuleId != null) put("recurring_rule_id", recurringRuleId)
        }
        return db.insertOrThrow("transactions", null, values)
    }

    fun syncRecurringTransactions(userId: Long) {
        val db = dbHelper.writableDatabase
        val rules = mutableListOf<RecurringRule>()
        val cursor = db.rawQuery(
            "SELECT id, user_id, category_id, amount_cents, type, description, start_date, day_of_month, active FROM recurring_rules WHERE user_id = ? AND active = 1",
            arrayOf(userId.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                rules += RecurringRule(
                    id = it.getLong(0),
                    userId = it.getLong(1),
                    categoryId = it.getLong(2),
                    amountCents = it.getLong(3),
                    type = TransactionType.fromDb(it.getString(4)),
                    description = it.getString(5),
                    startDate = LocalDate.parse(it.getString(6)),
                    dayOfMonth = it.getInt(7),
                    active = it.getInt(8) == 1
                )
            }
        }

        val currentMonth = YearMonth.now()
        db.beginTransaction()
        try {
            rules.forEach { rule ->
                var month = YearMonth.from(rule.startDate)
                while (!month.isAfter(currentMonth)) {
                    val targetDate = DateUtils.safeDayForMonth(rule.dayOfMonth, month)
                    if (!targetDate.isBefore(rule.startDate)) {
                        try {
                            insertTransaction(db, rule.userId, rule.categoryId, rule.amountCents, rule.type, targetDate, rule.description, rule.id)
                        } catch (_: SQLiteConstraintException) {
                            // Já existe a transação recorrente desse mês.
                        }
                    }
                    month = month.plusMonths(1)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getTransactions(
        userId: Long,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        categoryId: Long? = null,
        type: TransactionType? = null
    ): List<FinanceTransaction> {
        val args = mutableListOf(userId.toString())
        val conditions = mutableListOf("t.user_id = ?")
        if (startDate != null) {
            conditions += "t.transaction_date >= ?"
            args += startDate.toString()
        }
        if (endDate != null) {
            conditions += "t.transaction_date <= ?"
            args += endDate.toString()
        }
        if (categoryId != null) {
            conditions += "t.category_id = ?"
            args += categoryId.toString()
        }
        if (type != null) {
            conditions += "t.type = ?"
            args += type.dbValue
        }
        val query = """
            SELECT t.id, t.user_id, t.category_id, c.name, t.amount_cents, t.type, t.transaction_date, t.description, t.recurring_rule_id
            FROM transactions t
            INNER JOIN categories c ON c.id = t.category_id
            WHERE ${conditions.joinToString(" AND ")}
            ORDER BY t.transaction_date DESC, t.id DESC
        """.trimIndent()

        val items = mutableListOf<FinanceTransaction>()
        val cursor = dbHelper.readableDatabase.rawQuery(query, args.toTypedArray())
        cursor.use {
            while (it.moveToNext()) {
                items += FinanceTransaction(
                    id = it.getLong(0),
                    userId = it.getLong(1),
                    categoryId = it.getLong(2),
                    categoryName = it.getString(3),
                    amountCents = it.getLong(4),
                    type = TransactionType.fromDb(it.getString(5)),
                    transactionDate = LocalDate.parse(it.getString(6)),
                    description = it.getString(7),
                    recurringRuleId = if (it.isNull(8)) null else it.getLong(8)
                )
            }
        }
        return items
    }

    fun updateTransaction(transaction: FinanceTransaction): Boolean {
        val values = ContentValues().apply {
            put("category_id", transaction.categoryId)
            put("amount_cents", transaction.amountCents)
            put("type", transaction.type.dbValue)
            put("transaction_date", transaction.transactionDate.toString())
            put("description", transaction.description)
        }
        return dbHelper.writableDatabase.update(
            "transactions",
            values,
            "id = ? AND user_id = ?",
            arrayOf(transaction.id.toString(), transaction.userId.toString())
        ) > 0
    }

    fun deleteTransaction(userId: Long, transactionId: Long): Boolean {
        return dbHelper.writableDatabase.delete(
            "transactions",
            "id = ? AND user_id = ?",
            arrayOf(transactionId.toString(), userId.toString())
        ) > 0
    }

    fun getDashboardSummary(userId: Long): DashboardSummary {
        val db = dbHelper.readableDatabase
        val currentMonth = YearMonth.now()
        val monthStart = currentMonth.atDay(1).toString()
        val monthEnd = currentMonth.atEndOfMonth().toString()

        fun sum(type: TransactionType? = null, monthOnly: Boolean = false): Long {
            val args = mutableListOf(userId.toString())
            val conditions = mutableListOf("user_id = ?")
            if (type != null) {
                conditions += "type = ?"
                args += type.dbValue
            }
            if (monthOnly) {
                conditions += "transaction_date BETWEEN ? AND ?"
                args += monthStart
                args += monthEnd
            }
            val cursor = db.rawQuery("SELECT COALESCE(SUM(amount_cents), 0) FROM transactions WHERE ${conditions.joinToString(" AND ")}", args.toTypedArray())
            cursor.use {
                it.moveToFirst()
                return it.getLong(0)
            }
        }

        val topExpenseCategories = mutableListOf<CategoryExpenseSummary>()
        val cursor = db.rawQuery(
            """
            SELECT c.name, COALESCE(SUM(t.amount_cents), 0) AS total
            FROM transactions t
            INNER JOIN categories c ON c.id = t.category_id
            WHERE t.user_id = ? AND t.type = 'despesa' AND t.transaction_date BETWEEN ? AND ?
            GROUP BY c.name
            ORDER BY total DESC, c.name ASC
            LIMIT 5
            """.trimIndent(),
            arrayOf(userId.toString(), monthStart, monthEnd)
        )
        cursor.use {
            while (it.moveToNext()) {
                topExpenseCategories += CategoryExpenseSummary(it.getString(0), it.getLong(1))
            }
        }

        val totalIncome = sum(TransactionType.INCOME, monthOnly = false)
        val totalExpense = sum(TransactionType.EXPENSE, monthOnly = false)
        return DashboardSummary(
            balanceCents = totalIncome - totalExpense,
            incomeMonthCents = sum(TransactionType.INCOME, monthOnly = true),
            expenseMonthCents = sum(TransactionType.EXPENSE, monthOnly = true),
            topExpenseCategories = topExpenseCategories
        )
    }
}
