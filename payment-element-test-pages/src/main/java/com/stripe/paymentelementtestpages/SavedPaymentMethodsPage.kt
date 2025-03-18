package com.stripe.paymentelementtestpages

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_MODIFY_BADGE
import com.stripe.android.paymentsheet.ui.UPDATE_PM_REMOVE_BUTTON_TEST_TAG

class SavedPaymentMethodsPage(private val composeTestRule: ComposeTestRule) {
    fun waitUntilVisible() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTestTag(SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    fun waitForSavedPaymentMethodToBeRemoved(last4: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(
                savedPaymentMethodMatcher(last4 = last4).and(
                    SemanticsMatcher("is_placed_in_layout") { node ->
                        node.layoutInfo.isPlaced
                    }
                )
            )
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    fun onSavedPaymentMethod(last4: String): SemanticsNodeInteraction {
        return composeTestRule.onNode(savedPaymentMethodMatcher(last4))
    }

    fun onEditButton(): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithTag(PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG)
    }

    fun onModifyBadgeFor(last4: String): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag(TEST_TAG_MODIFY_BADGE).and(hasAnyAncestor(savedPaymentMethodMatcher(last4)))
        )
    }

    fun clickRemoveButton() {
        composeTestRule.onNodeWithTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG).performClick()
    }

    fun clickNewCardButton() {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodes(hasTestTag(SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        // AddCard is the value of PaymentOptionItems.ViewType.AddCard.name()
        val testTag = "AddCard"

        composeTestRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeTestRule
                .onAllNodes(hasTestTag(testTag))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithTag(testTag, true).performClick()
    }

    private fun savedPaymentMethodMatcher(last4: String): SemanticsMatcher {
        return hasTestTag(SAVED_PAYMENT_OPTION_TEST_TAG).and(hasText(last4, substring = true))
    }

    companion object {
        fun SemanticsNodeInteraction.assertHasModifyBadge() {
            assert(hasAnyDescendant(hasTestTag(TEST_TAG_MODIFY_BADGE)))
        }
    }
}
