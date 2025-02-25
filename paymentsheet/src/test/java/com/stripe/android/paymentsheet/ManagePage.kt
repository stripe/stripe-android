package com.stripe.android.paymentsheet

import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.stripe.android.common.ui.performClickWithKeyboard
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_ICON_FROM_RES
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_MANAGE_SCREEN_CHEVRON_ICON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON

internal class ManagePage(
    private val composeTestRule: ComposeTestRule
) {
    fun waitUntilVisible(customLast4: String? = null) {
        composeTestRule.waitUntil {
//            composeTestRule
//                .onAllNodes(hasTestTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST))
//                .fetchSemanticsNodes()
//                .isNotEmpty()

            composeTestRule.onAllNodesWithText("···· 4242", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("···· 6789", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() ||
                (customLast4 != null && composeTestRule.onAllNodesWithText("···· $customLast4", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        }
    }

    fun selectPaymentMethod(paymentMethodId: String) {
        composeTestRule.onNode(
            hasTestTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodId")
        ).performScrollTo().performClickWithKeyboard()
    }

    fun selectPaymentMethodWithLast4(last4: String) {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodesWithText("···· $last4", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithText("···· $last4", substring = true, useUnmergedTree = true)
            .performScrollTo()
            .performClickWithKeyboard()
    }

    fun clickEdit() {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodesWithText("EDIT", ignoreCase = true, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size == 1
        }

        composeTestRule.onNodeWithText("EDIT", ignoreCase = true, useUnmergedTree = true)
            .performClickWithKeyboard()

//        composeTestRule.onNodeWithTag(PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG).performClickWithKeyboard()
//        composeTestRule.onNodeWithText("Edit", ignoreCase = true).performClickWithKeyboard()
    }

    fun clickDone() {
        composeTestRule.onNodeWithTag(PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG).performClick()
    }

    fun clickEdit(paymentMethodId: String) {
        composeTestRule.onNode(
            hasAnyDescendant(hasTestTag("${TEST_TAG_MANAGE_SCREEN_CHEVRON_ICON}_$paymentMethodId")).and(isFocusable()),
            useUnmergedTree = true,
        ).performScrollTo().performClickWithKeyboard()
    }

    fun waitUntilGone(paymentMethodId: String) {
        composeTestRule.waitUntil(timeoutMillis = 2_000L) {
            composeTestRule
                .onAllNodes(
                    hasTestTag("${TEST_TAG_MANAGE_SCREEN_CHEVRON_ICON}_$paymentMethodId"),
                    useUnmergedTree = true
                )
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    fun waitUntilGoneWithLast4(last4: String) {
        composeTestRule.waitUntil(timeoutMillis = 2_000L) {
            composeTestRule
                .onAllNodes(
                    hasText("···· $last4", substring = true),
                    useUnmergedTree = true
                )
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    fun assertLpmIsSelected(paymentMethodCode: PaymentMethodCode) {
        composeTestRule.onNode(
            hasTestTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodCode").and(isSelected())
        ).assertExists()
    }

    fun assertLpmIsNotSelected(paymentMethodCode: PaymentMethodCode) {
        composeTestRule.onNode(
            hasTestTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodCode").and(isSelected().not())
        ).assertExists()
    }

    fun assertCardIsVisible(paymentMethodId: String, cardBrand: String) {
        composeTestRule.onNode(
            hasTestTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodId")
                .and(hasAnyDescendant(hasTestTag(TEST_TAG_ICON_FROM_RES).and(hasTestMetadata(cardBrand)))),
            useUnmergedTree = true,
        ).assertExists()
    }
}
