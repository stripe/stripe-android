package com.stripe.android.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Using this TestRule to fix tests that were hanging for inexplicable reasons.
 */
class HackyComposeTestRule(
    private val composeTestRule: ComposeContentTestRule = createComposeRule()
) : TestRule, TestWatcher(), ComposeContentTestRule by composeTestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                composeTestRule.apply(base, description)
            }
        }
    }

    override fun starting(description: Description) {
        super.starting(description)
        composeTestRule.mainClock.autoAdvance = false
    }

    override fun setContent(composable: @Composable () -> Unit) {
        composeTestRule.setContent(composable)
        composeTestRule.mainClock.advanceTimeBy(300)
    }
}
