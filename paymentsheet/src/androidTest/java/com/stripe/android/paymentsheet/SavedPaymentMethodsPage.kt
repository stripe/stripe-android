package com.stripe.android.paymentsheet

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_MODIFY_BADGE

class SavedPaymentMethodsPage(private val composeTestRule: ComposeTestRule) {
    fun waitUntilVisible() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTestTag(SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    fun onEditButton(): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithTag(PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG)
    }

    fun onModifyBadgeFor(last4: String): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag(TEST_TAG_MODIFY_BADGE).and(hasAnyAncestor(savedPaymentMethodMatcher(last4)))
        )
    }

    private fun savedPaymentMethodMatcher(last4: String): SemanticsMatcher {
        return hasTestTag(SAVED_PAYMENT_OPTION_TEST_TAG).and(hasText(last4, substring = true))
    }
}
