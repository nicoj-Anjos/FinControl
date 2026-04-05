package com.example.fincontrol.util

import java.security.MessageDigest

object PasswordUtils {
    fun hash(rawPassword: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(rawPassword.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun matches(rawPassword: String, passwordHash: String): Boolean = hash(rawPassword) == passwordHash
}
