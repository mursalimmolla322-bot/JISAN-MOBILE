package com.example

import com.example.util.ImageUtils
import com.example.util.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun pinHashing_usesSaltAndVerifiesCorrectly() {
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPin("S3cretPin!", salt)
        assertTrue(SecurityUtils.verifyPin("S3cretPin!", salt, hash))
        assertFalse(SecurityUtils.verifyPin("WrongPin!", salt, hash))
        assertNotEquals("S3cretPin!", hash)
    }

    @Test
    fun orderIdGeneration_isDeterministicPerIdempotencyKey() {
        val id1 = SecurityUtils.generateOrderId("idem-abc-123")
        val id2 = SecurityUtils.generateOrderId("idem-abc-123")
        val id3 = SecurityUtils.generateOrderId("idem-xyz-999")
        assertTrue(id1.startsWith("JM-"))
        assertEquals(id1, id2)
        assertNotEquals(id1, id3)
    }

    @Test
    fun referralCode_hasExpectedPrefixAndLength() {
        val code = SecurityUtils.generateReferralCode("uid_123456", "Jisan")
        assertTrue(code.startsWith("JMJISA"))
        assertTrue(code.length >= 8)
    }

    @Test
    fun currencyFormatter_formatsInrWithRupeeSymbol() {
        val formatted = ImageUtils.formatInr(14999.0)
        assertTrue(formatted.contains("₹"))
        assertTrue(formatted.contains("14,999") || formatted.contains("14999"))
    }
}
