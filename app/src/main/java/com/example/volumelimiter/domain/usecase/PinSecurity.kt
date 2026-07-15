package com.example.volumelimiter.domain.usecase

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PinSecurity {
    private const val SALT_SIZE_BYTES = 32

    fun createHash(pin: String): PinHash {
        val salt = ByteArray(SALT_SIZE_BYTES)
        SecureRandom().nextBytes(salt)
        return PinHash(
            hash = hash(pin, salt),
            salt = Base64.getEncoder().encodeToString(salt),
        )
    }

    fun verify(
        pin: String,
        storedHash: String?,
        storedSalt: String?,
    ): Boolean {
        if (pin.isBlank() || storedHash.isNullOrBlank() || storedSalt.isNullOrBlank()) return false
        val salt = runCatching { Base64.getDecoder().decode(storedSalt) }.getOrNull()
            ?: return false
        val candidate = hash(pin, salt)
        return MessageDigest.isEqual(
            candidate.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8),
        )
    }

    private fun hash(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest.digest())
    }
}

data class PinHash(
    val hash: String,
    val salt: String,
)

object PinPolicy {
    fun validateNewPin(pin: String, confirmation: String): PinValidationResult {
        if (pin.isBlank()) return PinValidationResult.Error("Informe um PIN.")
        if (!pin.all { it.isDigit() }) {
            return PinValidationResult.Error("Use apenas números.")
        }
        if (pin.length !in 4..6) {
            return PinValidationResult.Error("O PIN deve ter entre 4 e 6 dígitos.")
        }
        if (pin != confirmation) {
            return PinValidationResult.Error("A confirmação não confere.")
        }
        return if (isSimplePin(pin)) {
            PinValidationResult.Warning(
                "Este PIN é simples. Ele será aceito, mas escolha outro se possível.",
            )
        } else {
            PinValidationResult.Valid
        }
    }

    private fun isSimplePin(pin: String): Boolean {
        if (pin.all { it == pin.first() }) return true
        val ascending = pin.zipWithNext().all { (left, right) -> right == left + 1 }
        val descending = pin.zipWithNext().all { (left, right) -> right == left - 1 }
        return ascending || descending
    }
}

sealed interface PinValidationResult {
    data object Valid : PinValidationResult
    data class Warning(val message: String) : PinValidationResult
    data class Error(val message: String) : PinValidationResult
}
