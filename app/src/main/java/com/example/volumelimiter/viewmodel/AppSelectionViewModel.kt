package com.example.volumelimiter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.volumelimiter.data.datastore.VolumeDataStore
import com.example.volumelimiter.data.model.AppVolumeRule
import com.example.volumelimiter.data.model.InstalledAppInfo
import com.example.volumelimiter.data.repository.InstalledAppRepository
import com.example.volumelimiter.data.repository.VolumeRuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSelectionViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val installedAppRepository = InstalledAppRepository(appContext)
    private val ruleRepository = VolumeRuleRepository(VolumeDataStore(appContext))
    private val allApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val searchQuery = MutableStateFlow("")
    private val isLoading = MutableStateFlow(true)

    val uiState = combine(
        allApps,
        searchQuery,
        ruleRepository.rules,
        isLoading,
    ) { apps, query, rules, loading ->
        val selectedPackages = rules.map { it.packageName }.toSet()
        val filteredApps = if (query.isBlank()) {
            apps
        } else {
            apps.filter { app ->
                app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            }
        }

        AppSelectionUiState(
            apps = filteredApps,
            selectedPackages = selectedPackages,
            searchQuery = query,
            isLoading = loading,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSelectionUiState(isLoading = true),
    )

    init {
        reloadApps()
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun reloadApps() {
        viewModelScope.launch {
            isLoading.value = true
            allApps.value = withContext(Dispatchers.IO) {
                installedAppRepository.loadLaunchableApps()
            }
            isLoading.value = false
        }
    }

    fun addApp(app: InstalledAppInfo) {
        viewModelScope.launch {
            ruleRepository.upsertRule(
                AppVolumeRule(
                    packageName = app.packageName,
                    appName = app.appName,
                    maxVolumePercent = DEFAULT_LIMIT_PERCENT,
                    enabled = true,
                ),
            )
        }
    }

    companion object {
        private const val DEFAULT_LIMIT_PERCENT = 40
    }
}

data class AppSelectionUiState(
    val apps: List<InstalledAppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
)
