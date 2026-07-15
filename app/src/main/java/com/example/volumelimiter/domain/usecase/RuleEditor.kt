package com.example.volumelimiter.domain.usecase

import com.example.volumelimiter.data.model.AppVolumeRule

object RuleEditor {
    fun upsertRule(rules: List<AppVolumeRule>, rule: AppVolumeRule): List<AppVolumeRule> {
        val sanitized = rule.copy(maxVolumePercent = rule.maxVolumePercent.coerceIn(0, 100))
        val withoutExisting = rules.filterNot { it.packageName == sanitized.packageName }
        return (withoutExisting + sanitized).sortedBy { it.appName.lowercase() }
    }

    fun removeRule(rules: List<AppVolumeRule>, packageName: String): List<AppVolumeRule> =
        rules.filterNot { it.packageName == packageName }

    fun setRuleEnabled(
        rules: List<AppVolumeRule>,
        packageName: String,
        enabled: Boolean,
    ): List<AppVolumeRule> = rules.map { rule ->
        if (rule.packageName == packageName) rule.copy(enabled = enabled) else rule
    }

    fun setRuleLimit(
        rules: List<AppVolumeRule>,
        packageName: String,
        percent: Int,
    ): List<AppVolumeRule> = rules.map { rule ->
        if (rule.packageName == packageName) {
            rule.copy(maxVolumePercent = percent.coerceIn(0, 100))
        } else {
            rule
        }
    }

    fun findEnabledRule(
        rules: List<AppVolumeRule>,
        packageName: String?,
    ): AppVolumeRule? = rules.firstOrNull { rule ->
        rule.enabled && rule.packageName == packageName
    }
}
