package com.example.volumelimiter.data.repository

import com.example.volumelimiter.data.datastore.VolumeDataStore
import com.example.volumelimiter.data.model.AppVolumeRule
import com.example.volumelimiter.data.model.VolumeLimiterPreferences
import com.example.volumelimiter.domain.usecase.RuleEditor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class VolumeRuleRepository(
    private val dataStore: VolumeDataStore,
) {
    val preferences: Flow<VolumeLimiterPreferences> = dataStore.preferencesFlow
    val rules: Flow<List<AppVolumeRule>> = preferences
        .map { it.rules }
        .distinctUntilChanged()

    val monitoringEnabled: Flow<Boolean> = preferences
        .map { it.monitoringEnabled }
        .distinctUntilChanged()

    suspend fun upsertRule(rule: AppVolumeRule) {
        val currentRules = preferences.first().rules
        dataStore.saveRules(RuleEditor.upsertRule(currentRules, rule))
    }

    suspend fun removeRule(packageName: String) {
        val currentRules = preferences.first().rules
        dataStore.saveRules(RuleEditor.removeRule(currentRules, packageName))
    }

    suspend fun setRuleEnabled(packageName: String, enabled: Boolean) {
        val currentRules = preferences.first().rules
        dataStore.saveRules(RuleEditor.setRuleEnabled(currentRules, packageName, enabled))
    }

    suspend fun setRuleLimit(packageName: String, percent: Int) {
        val currentRules = preferences.first().rules
        dataStore.saveRules(RuleEditor.setRuleLimit(currentRules, packageName, percent))
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        dataStore.setMonitoringEnabled(enabled)
    }

    suspend fun savePinHash(hash: String, salt: String) {
        dataStore.savePinHash(hash, salt)
    }

    suspend fun setAutoStartOnBoot(enabled: Boolean) {
        dataStore.setAutoStartOnBoot(enabled)
    }

    suspend fun setAutoLockTimeoutSeconds(seconds: Int) {
        dataStore.setAutoLockTimeoutSeconds(seconds)
    }

    suspend fun setShowNotificationDetails(show: Boolean) {
        dataStore.setShowNotificationDetails(show)
    }
}
