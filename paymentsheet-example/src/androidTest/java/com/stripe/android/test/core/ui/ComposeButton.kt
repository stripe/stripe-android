package com.stripe.android.test.core.ui

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.stripe.android.test.core.DEFAULT_UI_TIMEOUT

internal class ComposeButton(
    private val composeTestRule: ComposeTestRule,
    private val matcher: SemanticsMatcher,
) {
    fun click() {
        composeTestRule.onNode(matcher).tryPerformScrollTo().performClick()
    }

    fun waitForEnabled(): ComposeButton {
        composeTestRule.waitUntil(timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
            val combinedMatcher = matcher.and(isEnabled()).and(hasClickAction())
            composeTestRule.onAllNodes(combinedMatcher).fetchSemanticsNodes().isNotEmpty()
        }
        return this
    }

    fun waitFor(semanticsMatcher: SemanticsMatcher): ComposeButton {
        composeTestRule.waitUntil(timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
            val combinedMatcher = matcher.and(semanticsMatcher)
            composeTestRule
                .onAllNodes(combinedMatcher)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        return this
    }

    private fun SemanticsNodeInteraction.tryPerformScrollTo(): SemanticsNodeInteraction {
        fetchSemanticsNode().findClosestParentNode {
            hasScrollAction().matches(it)
        } ?: return this
        performScrollTo()
        return this
    }

    private fun SemanticsNode.findClosestParentNode(
        includeSelf: Boolean = false,
        selector: (SemanticsNode) -> Boolean
    ): SemanticsNode? {
        var currentParent = if (includeSelf) this else parent
        while (currentParent != null) {
            if (selector(currentParent)) {
                return currentParent
            } else {
                currentParent = currentParent.parent
            }
        }

        return null
    }
}
