package com.example.fincontrol.data

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("fincontrol_session", Context.MODE_PRIVATE)

    fun saveUser(userId: Long) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply()
    }

    fun getLoggedUserId(): Long? {
        val value = prefs.getLong(KEY_USER_ID, -1L)
        return if (value > 0) value else null
    }

    fun clear() {
        prefs.edit().remove(KEY_USER_ID).apply()
    }

    companion object {
        private const val KEY_USER_ID = "logged_user_id"
    }
}
