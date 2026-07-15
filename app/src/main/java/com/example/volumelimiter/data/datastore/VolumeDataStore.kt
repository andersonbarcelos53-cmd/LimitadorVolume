package com.example.volumelimiter.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.volumelimiter.data.model.AppVolumeRule
import com.example.volumelimiter.data.model.ParentalControlPreferences
import com.example.volumelimiter.data.model.VolumeLimiterPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.volumeLimiterDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "volume_limiter_preferences",
)

class VolumeDataStore(
    private val context: Context,
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    val preferencesFlow: Flow<VolumeLimiterPreferences> = context.volumeLimiterDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            VolumeLimiterPreferences(
                rules = preferences[Keys.RULES_JSON].decodeRulesSafely(),
                monitoringEnabled = preferences[Keys.MONITORING_ENABLED] ?: false,
                parentalControls = ParentalControlPreferences(
                    pinHash = preferences[Keys.PIN_HASH],
                    pinSalt = preferences[Keys.PIN_SALT],
                    autoStartOnBoot = preferences[Keys.AUTO_START_ON_BOOT] ?: true,
                    autoLockTimeoutSeconds = (
                        preferences[Keys.AUTO_LOCK_TIMEOUT_SECONDS]
                            ?: ParentalControlPreferences.DEFAULT_AUTO_LOCK_TIMEOUT_SECONDS
                        ).coerceIn(
                            ParentalControlPreferences.MIN_AUTO_LOCK_TIMEOUT_SECONDS,
                            ParentalControlPreferences.MAX_AUTO_LOCK_TIMEOUT_SECONDS,
                        ),
                    showNotificationDetails = preferences[Keys.SHOW_NOTIFICATION_DETAILS] ?: true,
                ),
            )
        }

    suspend fun saveRules(rules: List<AppVolumeRule>) {
        context.volumeLimiterDataStore.edit { preferences ->
            preferences[Keys.RULES_JSON] = json.encodeToString(
                ListSerializer(AppVolumeRule.serializer()),
                rules,
            )
        }
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.volumeLimiterDataStore.edit { preferences ->
            preferences[Keys.MONITORING_ENABLED] = enabled
        }
    }

    suspend fun savePinHash(hash: String, salt: String) {
        context.volumeLimiterDataStore.edit { preferences ->
            preferences[Keys.PIN_HASH] = hash
            preferences[Keys.PIN_SALT] = salt
        }
    }

    suspend fun setAutoStartOnBoot(enabled: Boolean) {
        context.volumeLimiterDataStore.edit { preferences ->
            preferences[Keys.AUTO_START_ON_BOOT] = enabled
        }
    }

    suspend fun setAutoLockTimeoutSeconds(seconds: Int) {
        context.volumeLimiterDataStore.edit { preferences ->
            preferences[Keys.AUTO_LOCK_TIMEOUT_SECONDS] = seconds.coerceIn(
                ParentalControlPreferences.MIN_AUTO_LOCK_TIMEOUT_SECONDS,
                ParentalControlPreferences.MAX_AUTO_LOCK_TIMEOUT_SECONDS,
            )
        }
    }

    suspend fun setShowNotificationDetails(show: Boolean) {
        context.volumeLimiterDataStore.edit { preferences ->
            preferences[Keys.SHOW_NOTIFICATION_DETAILS] = show
        }
    }

    private fun String?.decodeRulesSafely(): List<AppVolumeRule> {
        if (isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(ListSerializer(AppVolumeRule.serializer()), this)
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    private object Keys {
        val RULES_JSON = stringPreferencesKey("rules_json")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")
        val AUTO_LOCK_TIMEOUT_SECONDS = intPreferencesKey("auto_lock_timeout_seconds")
        val SHOW_NOTIFICATION_DETAILS = booleanPreferencesKey("show_notification_details")
    }
}
