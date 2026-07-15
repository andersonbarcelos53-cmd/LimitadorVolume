package com.example.volumelimiter.domain

import com.example.volumelimiter.domain.usecase.PinPolicy
import com.example.volumelimiter.domain.usecase.PinSecurity
import com.example.volumelimiter.domain.usecase.PinValidationResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityTest {
    @Test
    fun createHash_doesNotStorePlainPin() {
        val pinHash = PinSecurity.createHash("4931")

        assertNotEquals("4931", pinHash.hash)
        assertTrue(pinHash.hash.isNotBlank())
        assertTrue(pinHash.salt.isNotBlank())
    }

    @Test
    fun verify_acceptsCorrectPinAndRejectsWrongPin() {
        val pinHash = PinSecurity.createHash("4931")

        assertTrue(PinSecurity.verify("4931", pinHash.hash, pinHash.salt))
        assertFalse(PinSecurity.verify("4932", pinHash.hash, pinHash.salt))
    }

    @Test
    fun validateNewPin_rejectsDifferentConfirmation() {
        val result = PinPolicy.validateNewPin("4931", "4932")

        assertTrue(result is PinValidationResult.Error)
    }

    @Test
    fun validateNewPin_warnsButAllowsSimplePin() {
        val result = PinPolicy.validateNewPin("1234", "1234")

        assertTrue(result is PinValidationResult.Warning)
    }
}
