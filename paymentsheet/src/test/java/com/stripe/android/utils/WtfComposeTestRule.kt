package com.stripe.android.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Using this TestRule to fix tests that we're hanging for inexplicable reasons.
 */
class WtfComposeTestRule : TestRule, TestWatcher() {

    val composeTestRule = createComposeRule()

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

    fun setContent(composable: @Composable () -> Unit) {
        composeTestRule.setContent(composable)
        composeTestRule.mainClock.advanceTimeBy(300)
    }

    fun onNodeWithTag(
        testTag: String,
        useUnmergedTree: Boolean = false,
    ): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithTag(testTag, useUnmergedTree)
    }
}
