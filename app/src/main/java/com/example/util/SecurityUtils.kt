package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom

object SecurityUtils {
    private const val SALT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private val random = SecureRandom()

    fun generateSalt(length: Int = 16): String {
        val sb = StringBuilder(length)
        repeat(length) {
            sb.append(SALT_CHARS[random.nextInt(SALT_CHARS.length)])
        }
        return sb.toString()
    }

    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, salt: String, expectedHash: String): Boolean {
        if (pin.isBlank() || salt.isBlank() || expectedHash.isBlank()) return false
        val actual = hashPin(pin, salt)
        return MessageDigest.isEqual(actual.toByteArray(), expectedHash.toByteArray())
    }

    fun generateReferralCode(uid: String, name: String): String {
        val prefix = name.uppercase().filter { it in 'A'..'Z' }.take(4).ifEmpty { "JISAN" }
        val suffix = uid.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(4).padEnd(4, '9')
        return "JM$prefix$suffix".take(12)
    }

    fun generateOrderId(idempotencyKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(idempotencyKey.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(12)
            .uppercase()
        return "JM-$digest"
    }
}
