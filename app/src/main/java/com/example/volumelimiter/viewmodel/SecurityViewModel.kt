package com.example.volumelimiter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.volumelimiter.data.datastore.VolumeDataStore
import com.example.volumelimiter.data.model.ParentalControlPreferences
import com.example.volumelimiter.data.repository.VolumeRuleRepository
import com.example.volumelimiter.domain.usecase.AutoLockSession
import com.example.volumelimiter.domain.usecase.PinPolicy
import com.example.volumelimiter.domain.usecase.PinSecurity
import com.example.volumelimiter.domain.usecase.PinValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SecurityViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = VolumeRuleRepository(VolumeDataStore(application.applicationContext))
    private val session = AutoLockSession()
    private val unlocked = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState = combine(
        repository.preferences,
        unlocked,
        message,
    ) { preferences, isUnlocked, currentMessage ->
        SecurityUiState(
            parentalControls = preferences.parentalControls,
            isUnlocked = isUnlocked,
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SecurityUiState(),
    )

    fun createPin(pin: String, confirmation: String) {
        when (val validation = PinPolicy.validateNewPin(pin, confirmation)) {
            is PinValidationResult.Error -> {
                message.value = validation.message
                return
            }
            is PinValidationResult.Warning -> {
                savePinAndUnlock(pin, validation.message)
            }
            PinValidationResult.Valid -> {
                savePinAndUnlock(pin, "PIN criado. Proteção ativa.")
            }
        }
    }

    fun authenticate(pin: String) {
        val controls = uiState.value.parentalControls
        if (PinSecurity.verify(pin, controls.pinHash, controls.pinSalt)) {
            session.unlock()
            unlocked.value = true
            message.value = null
        } else {
            message.value = "PIN incorreto."
        }
    }

    fun changePin(
        currentPin: String,
        newPin: String,
        confirmation: String,
    ) {
        val controls = uiState.value.parentalControls
        if (!PinSecurity.verify(currentPin, controls.pinHash, controls.pinSalt)) {
            message.value = "PIN atual incorreto."
            return
        }
        when (val validation = PinPolicy.validateNewPin(newPin, confirmation)) {
            is PinValidationResult.Error -> message.value = validation.message
            is PinValidationResult.Warning -> savePinAndKeepUnlocked(newPin, validation.message)
            PinValidationResult.Valid -> savePinAndKeepUnlocked(newPin, "PIN alterado.")
        }
    }

    fun lockNow() {
        session.lockNow()
        unlocked.value = false
        message.value = null
    }

    fun onAppBackgrounded() {
        session.onAppBackgrounded()
    }

    fun onAppForegrounded() {
        val timeout = uiState.value.parentalControls.autoLockTimeoutSeconds
        val stillUnlocked = session.onAppForegrounded(timeout)
        unlocked.value = stillUnlocked
    }

    private fun savePinAndUnlock(pin: String, successMessage: String) {
        viewModelScope.launch {
            val pinHash = PinSecurity.createHash(pin)
            repository.savePinHash(pinHash.hash, pinHash.salt)
            session.unlock()
            unlocked.value = true
            message.value = successMessage
        }
    }

    private fun savePinAndKeepUnlocked(pin: String, successMessage: String) {
        viewModelScope.launch {
            val pinHash = PinSecurity.createHash(pin)
            repository.savePinHash(pinHash.hash, pinHash.salt)
            session.unlock()
            unlocked.value = true
            message.value = successMessage
        }
    }
}

data class SecurityUiState(
    val parentalControls: ParentalControlPreferences = ParentalControlPreferences(),
    val isUnlocked: Boolean = false,
    val message: String? = null,
) {
    val isPinConfigured: Boolean
        get() = parentalControls.isPinConfigured
}
