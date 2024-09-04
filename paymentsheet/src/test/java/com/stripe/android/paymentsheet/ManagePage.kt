package com.stripe.android.paymentsheet

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_MANAGE_SCREEN_DELETE_ICON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_MANAGE_SCREEN_EDIT_ICON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_CONFIRM_BUTTON

internal class ManagePage(
    private val composeTestRule: ComposeTestRule
) {
    fun waitUntilVisible() {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodes(hasTestTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    fun selectPaymentMethod(paymentMethodId: String) {
        composeTestRule.onNode(
            hasTestTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodId")
        ).performScrollTo().performClick()
    }

    fun clickEdit() {
        composeTestRule.onNodeWithTag(PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG).performClick()
    }

    fun clickDone() {
        composeTestRule.onNodeWithTag(PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG).performClick()
    }

    fun waitUntilRemoveVisible(paymentMethodId: String) {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodes(hasTestTag("${TEST_TAG_MANAGE_SCREEN_DELETE_ICON}_$paymentMethodId"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    fun clickRemove(paymentMethodId: String) {
        composeTestRule.onNode(
            hasTestTag("${TEST_TAG_MANAGE_SCREEN_DELETE_ICON}_$paymentMethodId")
        ).performScrollTo().performClick()

        composeTestRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).performClick()
    }

    fun clickEdit(paymentMethodId: String) {
        composeTestRule.onNode(
            hasTestTag("${TEST_TAG_MANAGE_SCREEN_EDIT_ICON}_$paymentMethodId")
        ).performScrollTo().performClick()
    }
}
