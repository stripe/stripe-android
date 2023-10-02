package com.stripe.android.test.core.ui

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import kotlin.time.Duration.Companion.seconds

internal class ComposeButton(
    private val composeTestRule: ComposeTestRule,
    private val matcher: SemanticsMatcher,
) {
    fun click() {
        composeTestRule.onNode(matcher).performScrollTo().performClick()
    }

    fun waitForEnabled() {
        composeTestRule.waitUntil(timeoutMillis = 5.seconds.inWholeMilliseconds) {
            val combinedMatcher = matcher.and(isEnabled()).and(hasClickAction())
            composeTestRule.onAllNodes(combinedMatcher).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
