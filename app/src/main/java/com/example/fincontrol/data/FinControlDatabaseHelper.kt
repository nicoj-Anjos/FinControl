package com.example.fincontrol.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.fincontrol.model.TransactionType

class FinControlDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL CHECK(type IN ('receita','despesa')),
                is_default INTEGER NOT NULL DEFAULT 1,
                UNIQUE(name, type)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE recurring_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                category_id INTEGER NOT NULL,
                amount_cents INTEGER NOT NULL,
                type TEXT NOT NULL CHECK(type IN ('receita','despesa')),
                description TEXT,
                start_date TEXT NOT NULL,
                day_of_month INTEGER NOT NULL,
                active INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY(category_id) REFERENCES categories(id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                category_id INTEGER NOT NULL,
                amount_cents INTEGER NOT NULL,
                type TEXT NOT NULL CHECK(type IN ('receita','despesa')),
                transaction_date TEXT NOT NULL,
                description TEXT,
                recurring_rule_id INTEGER,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY(category_id) REFERENCES categories(id),
                FOREIGN KEY(recurring_rule_id) REFERENCES recurring_rules(id) ON DELETE SET NULL,
                UNIQUE(recurring_rule_id, transaction_date)
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX idx_transactions_user_date ON transactions(user_id, transaction_date DESC)")
        db.execSQL("CREATE INDEX idx_transactions_category ON transactions(category_id)")
        seedCategories(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recurring_rules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    category_id INTEGER NOT NULL,
                    amount_cents INTEGER NOT NULL,
                    type TEXT NOT NULL CHECK(type IN ('receita','despesa')),
                    description TEXT,
                    start_date TEXT NOT NULL,
                    day_of_month INTEGER NOT NULL,
                    active INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(category_id) REFERENCES categories(id)
                )
                """.trimIndent()
            )
        }
    }

    private fun seedCategories(db: SQLiteDatabase) {
        val defaults = listOf(
            "Salário" to TransactionType.INCOME,
            "Freelance" to TransactionType.INCOME,
            "Investimentos" to TransactionType.INCOME,
            "Outras receitas" to TransactionType.INCOME,
            "Alimentação" to TransactionType.EXPENSE,
            "Moradia" to TransactionType.EXPENSE,
            "Transporte" to TransactionType.EXPENSE,
            "Saúde" to TransactionType.EXPENSE,
            "Lazer" to TransactionType.EXPENSE,
            "Educação" to TransactionType.EXPENSE,
            "Contas" to TransactionType.EXPENSE,
            "Mercado" to TransactionType.EXPENSE,
            "Outras despesas" to TransactionType.EXPENSE,
        )
        defaults.forEach { (name, type) ->
            val values = ContentValues().apply {
                put("name", name)
                put("type", type.dbValue)
                put("is_default", 1)
            }
            db.insertWithOnConflict("categories", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    companion object {
        private const val DB_NAME = "fincontrol.db"
        private const val DB_VERSION = 2
    }
}
