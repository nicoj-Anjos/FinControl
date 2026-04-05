package com.example.fincontrol.model

import java.time.LocalDate

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val passwordHash: String,
    val createdAt: String
)

enum class TransactionType(val dbValue: String) {
    INCOME("receita"),
    EXPENSE("despesa");

    companion object {
        fun fromDb(value: String): TransactionType = if (value == EXPENSE.dbValue) EXPENSE else INCOME
    }
}

data class Category(
    val id: Long,
    val name: String,
    val type: TransactionType,
    val isDefault: Boolean = true
)

data class RecurringRule(
    val id: Long,
    val userId: Long,
    val categoryId: Long,
    val amountCents: Long,
    val type: TransactionType,
    val description: String?,
    val startDate: LocalDate,
    val dayOfMonth: Int,
    val active: Boolean
)

data class FinanceTransaction(
    val id: Long,
    val userId: Long,
    val categoryId: Long,
    val categoryName: String,
    val amountCents: Long,
    val type: TransactionType,
    val transactionDate: LocalDate,
    val description: String?,
    val recurringRuleId: Long? = null
)

data class CategoryExpenseSummary(
    val categoryName: String,
    val totalCents: Long
)

data class DashboardSummary(
    val balanceCents: Long,
    val incomeMonthCents: Long,
    val expenseMonthCents: Long,
    val topExpenseCategories: List<CategoryExpenseSummary>
)
