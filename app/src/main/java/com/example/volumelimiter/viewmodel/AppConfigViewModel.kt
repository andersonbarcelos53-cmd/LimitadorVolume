package com.example.volumelimiter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.volumelimiter.data.datastore.VolumeDataStore
import com.example.volumelimiter.data.model.AppVolumeRule
import com.example.volumelimiter.data.repository.VolumeRuleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppConfigViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = VolumeRuleRepository(VolumeDataStore(application.applicationContext))
    private val packageName = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)

    val uiState = combine(
        packageName,
        repository.rules,
        message,
    ) { selectedPackageName, rules, currentMessage ->
        val rule = rules.firstOrNull { it.packageName == selectedPackageName }
        AppConfigUiState(
            packageName = selectedPackageName,
            rule = rule,
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppConfigUiState(),
    )

    fun setPackageName(value: String) {
        if (packageName.value != value) {
            packageName.value = value
            message.value = null
        }
    }

    fun setEnabled(enabled: Boolean) {
        val current = uiState.value.rule ?: return
        viewModelScope.launch {
            repository.setRuleEnabled(current.packageName, enabled)
            message.value = "Configuração salva."
        }
    }

    fun setLimit(percent: Int) {
        val current = uiState.value.rule ?: return
        viewModelScope.launch {
            repository.setRuleLimit(current.packageName, percent)
            message.value = "Configuração salva."
        }
    }

    fun save() {
        message.value = "Configuração salva."
    }

    fun remove() {
        val current = uiState.value.rule ?: return
        viewModelScope.launch {
            repository.removeRule(current.packageName)
            message.value = "Aplicativo removido."
        }
    }
}

data class AppConfigUiState(
    val packageName: String? = null,
    val rule: AppVolumeRule? = null,
    val message: String? = null,
)
