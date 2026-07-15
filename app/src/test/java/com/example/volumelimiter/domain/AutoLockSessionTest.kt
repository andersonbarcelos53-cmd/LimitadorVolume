package com.example.volumelimiter.domain

import com.example.volumelimiter.domain.usecase.AutoLockSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLockSessionTest {
    @Test
    fun onForegroundBeforeTimeout_keepsSessionUnlocked() {
        var now = 1_000L
        val session = AutoLockSession { now }
        session.unlock()

        session.onAppBackgrounded()
        now += 29_000L

        assertTrue(session.onAppForegrounded(timeoutSeconds = 30))
    }

    @Test
    fun onForegroundAfterTimeout_locksSession() {
        var now = 1_000L
        val session = AutoLockSession { now }
        session.unlock()

        session.onAppBackgrounded()
        now += 30_000L

        assertFalse(session.onAppForegrounded(timeoutSeconds = 30))
    }

    @Test
    fun lockNow_locksImmediately() {
        val session = AutoLockSession()
        session.unlock()

        session.lockNow()

        assertFalse(session.isUnlocked())
    }
}
