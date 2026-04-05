package com.example.fincontrol.data

import android.content.ContentValues
import android.content.Context
import com.example.fincontrol.model.User
import com.example.fincontrol.util.PasswordUtils

class UserRepository(context: Context) {
    private val dbHelper = FinControlDatabaseHelper(context)

    fun register(name: String, email: String, rawPassword: String): Result<Long> {
        val normalizedEmail = email.trim().lowercase()
        if (findByEmail(normalizedEmail) != null) {
            return Result.failure(IllegalArgumentException("Já existe uma conta com este e-mail."))
        }
        val values = ContentValues().apply {
            put("name", name.trim())
            put("email", normalizedEmail)
            put("password_hash", PasswordUtils.hash(rawPassword))
        }
        val id = dbHelper.writableDatabase.insert("users", null, values)
        return if (id > 0) Result.success(id) else Result.failure(IllegalStateException("Não foi possível criar o usuário."))
    }

    fun login(identifier: String, rawPassword: String): User? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, name, email, password_hash, created_at FROM users WHERE lower(email) = ? OR lower(name) = ? LIMIT 1",
            arrayOf(identifier.trim().lowercase(), identifier.trim().lowercase())
        )
        cursor.use {
            if (!it.moveToFirst()) return null
            val user = User(
                id = it.getLong(0),
                name = it.getString(1),
                email = it.getString(2),
                passwordHash = it.getString(3),
                createdAt = it.getString(4)
            )
            return if (PasswordUtils.matches(rawPassword, user.passwordHash)) user else null
        }
    }

    fun resetPassword(email: String, name: String, newPassword: String): Boolean {
        val count = dbHelper.writableDatabase.update(
            "users",
            ContentValues().apply { put("password_hash", PasswordUtils.hash(newPassword)) },
            "lower(email) = ? AND lower(name) = ?",
            arrayOf(email.trim().lowercase(), name.trim().lowercase())
        )
        return count > 0
    }

    fun getById(userId: Long): User? {
        val cursor = dbHelper.readableDatabase.rawQuery(
            "SELECT id, name, email, password_hash, created_at FROM users WHERE id = ?",
            arrayOf(userId.toString())
        )
        cursor.use {
            if (!it.moveToFirst()) return null
            return User(it.getLong(0), it.getString(1), it.getString(2), it.getString(3), it.getString(4))
        }
    }

    private fun findByEmail(email: String): User? {
        val cursor = dbHelper.readableDatabase.rawQuery(
            "SELECT id, name, email, password_hash, created_at FROM users WHERE lower(email) = ?",
            arrayOf(email)
        )
        cursor.use {
            if (!it.moveToFirst()) return null
            return User(it.getLong(0), it.getString(1), it.getString(2), it.getString(3), it.getString(4))
        }
    }
}
