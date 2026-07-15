package com.example.volumelimiter.domain

import com.example.volumelimiter.data.model.AppVolumeRule
import com.example.volumelimiter.domain.usecase.RuleEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuleEditorTest {
    @Test
    fun findEnabledRule_returnsTheMatchingActiveRule() {
        val rules = listOf(
            rule("com.video", enabled = true, percent = 40),
            rule("com.social", enabled = false, percent = 30),
        )

        val result = RuleEditor.findEnabledRule(rules, "com.video")

        assertEquals("com.video", result?.packageName)
        assertEquals(40, result?.maxVolumePercent)
    }

    @Test
    fun findEnabledRule_ignoresDisabledRules() {
        val rules = listOf(rule("com.social", enabled = false, percent = 30))

        assertNull(RuleEditor.findEnabledRule(rules, "com.social"))
    }

    @Test
    fun setRuleEnabled_updatesOnlyTheRequestedRule() {
        val rules = listOf(
            rule("com.video", enabled = true),
            rule("com.social", enabled = false),
        )

        val result = RuleEditor.setRuleEnabled(rules, "com.social", true)

        assertEquals(true, result.first { it.packageName == "com.video" }.enabled)
        assertEquals(true, result.first { it.packageName == "com.social" }.enabled)
    }

    @Test
    fun setRuleLimit_clampsTheSavedValue() {
        val result = RuleEditor.setRuleLimit(
            rules = listOf(rule("com.video", percent = 40)),
            packageName = "com.video",
            percent = 120,
        )

        assertEquals(100, result.single().maxVolumePercent)
    }

    private fun rule(
        packageName: String,
        enabled: Boolean = true,
        percent: Int = 40,
    ): AppVolumeRule =
        AppVolumeRule(
            packageName = packageName,
            appName = packageName,
            maxVolumePercent = percent,
            enabled = enabled,
        )
}
