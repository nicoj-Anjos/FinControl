package com.example.fincontrol.data

import android.content.Context
import com.example.fincontrol.model.Category
import com.example.fincontrol.model.TransactionType

class CategoryRepository(context: Context) {
    private val dbHelper = FinControlDatabaseHelper(context)

    fun getByType(type: TransactionType): List<Category> {
        val categories = mutableListOf<Category>()
        val cursor = dbHelper.readableDatabase.rawQuery(
            "SELECT id, name, type, is_default FROM categories WHERE type = ? ORDER BY name ASC",
            arrayOf(type.dbValue)
        )
        cursor.use {
            while (it.moveToNext()) {
                categories += Category(
                    id = it.getLong(0),
                    name = it.getString(1),
                    type = TransactionType.fromDb(it.getString(2)),
                    isDefault = it.getInt(3) == 1
                )
            }
        }
        return categories
    }

    fun getById(categoryId: Long): Category? {
        val cursor = dbHelper.readableDatabase.rawQuery(
            "SELECT id, name, type, is_default FROM categories WHERE id = ?",
            arrayOf(categoryId.toString())
        )
        cursor.use {
            if (!it.moveToFirst()) return null
            return Category(it.getLong(0), it.getString(1), TransactionType.fromDb(it.getString(2)), it.getInt(3) == 1)
        }
    }
}
