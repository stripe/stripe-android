package com.stripe.android.paymentsheet

import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_ICON_FROM_RES
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_MANAGE_SCREEN_CHEVRON_ICON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON

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

    fun assertNotVisible() {
        composeTestRule
            .onNode(hasTestTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST))
            .assertDoesNotExist()
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

    fun clickEdit(paymentMethodId: String) {
        composeTestRule.onNode(
            hasTestTag("${TEST_TAG_MANAGE_SCREEN_CHEVRON_ICON}_$paymentMethodId"),
            useUnmergedTree = true,
        ).performScrollTo().performClick()
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
