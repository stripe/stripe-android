package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput

internal class CvcPage(
    private val composeTestRule: ComposeTestRule
) {
    fun setCvc(cvc: String) {
        composeTestRule.onNodeWithTag(TEST_TAG_CVC_FIELD).performTextInput(cvc)
    }

    fun assertCvcField(matcher: SemanticsMatcher) {
        composeTestRule.onNodeWithTag(TEST_TAG_CVC_FIELD).assert(matcher)
    }

    fun displaysLastFour(lastFour: String) {
        composeTestRule.onNodeWithTag(TEST_TAG_CVC_LAST_FOUR).assert(hasText(lastFour, substring = true))
    }

    fun hasLabel(label: String) {
        composeTestRule.onNode(hasTestTag(TEST_TAG_CVC_LABEL), useUnmergedTree = true).assert(hasText(label))
    }

    fun hasTitle(title: String) {
        composeTestRule.onNodeWithTag(TEST_TAG_CONFIRM_CVC).assert(hasText(title))
    }
}
