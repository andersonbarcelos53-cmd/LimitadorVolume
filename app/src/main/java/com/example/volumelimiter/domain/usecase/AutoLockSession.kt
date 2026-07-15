package com.example.volumelimiter.domain.usecase

class AutoLockSession(
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) {
    private var unlocked = false
    private var backgroundAtMillis: Long? = null

    fun isUnlocked(): Boolean = unlocked

    fun unlock() {
        unlocked = true
        backgroundAtMillis = null
    }

    fun lockNow() {
        unlocked = false
        backgroundAtMillis = null
    }

    fun onAppBackgrounded() {
        if (unlocked) {
            backgroundAtMillis = clockMillis()
        }
    }

    fun onAppForegrounded(timeoutSeconds: Int): Boolean {
        val backgroundedAt = backgroundAtMillis
        backgroundAtMillis = null
        if (unlocked && backgroundedAt != null) {
            val elapsedMillis = clockMillis() - backgroundedAt
            if (elapsedMillis >= timeoutSeconds.coerceAtLeast(0) * 1_000L) {
                lockNow()
            }
        }
        return unlocked
    }
}
