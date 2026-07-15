package com.example.volumelimiter.data.model

data class ParentalControlPreferences(
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val autoStartOnBoot: Boolean = true,
    val autoLockTimeoutSeconds: Int = DEFAULT_AUTO_LOCK_TIMEOUT_SECONDS,
    val showNotificationDetails: Boolean = true,
) {
    val isPinConfigured: Boolean
        get() = !pinHash.isNullOrBlank() && !pinSalt.isNullOrBlank()

    companion object {
        const val DEFAULT_AUTO_LOCK_TIMEOUT_SECONDS = 30
        const val MIN_AUTO_LOCK_TIMEOUT_SECONDS = 5
        const val MAX_AUTO_LOCK_TIMEOUT_SECONDS = 300
    }
}
