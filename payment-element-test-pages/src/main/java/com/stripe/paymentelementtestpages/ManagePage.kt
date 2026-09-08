package com.stripe.paymentelementtestpages

import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_ICON_FROM_RES
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_MANAGE_SCREEN_CHEVRON_ICON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.testing.ScrollBehavior
import com.stripe.android.testing.clickNode
import com.stripe.android.testing.waitForNoNodes
import com.stripe.android.testing.waitForNode

@Suppress("TooManyFunctions")
class ManagePage(
    private val composeTestRule: ComposeTestRule,
) {
    fun waitUntilVisible() {
        composeTestRule.waitForNode(
            matcher = hasTestTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST),
            atLeastOneRootRequired = false,
        )
    }

    fun assertNotVisible() {
        composeTestRule
            .onNode(hasTestTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST))
            .assertDoesNotExist()
    }

    fun waitUntilNotVisible() {
        composeTestRule.waitForNoNodes(
            matcher = hasTestTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST),
            atLeastOneRootRequired = true,
        )
    }

    fun selectPaymentMethod(paymentMethodId: String) {
        composeTestRule.clickNode(
            matcher = hasTestTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodId"),
            scrollBehavior = ScrollBehavior.Required,
        )
    }

    fun clickEdit() {
        composeTestRule.clickNode(
            matcher = hasTestTag(PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG),
            scrollBehavior = ScrollBehavior.Never,
        )
    }

    fun clickDone() {
        composeTestRule.clickNode(
            matcher = hasTestTag(PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG),
            scrollBehavior = ScrollBehavior.Never,
        )
    }

    fun clickEdit(paymentMethodId: String) {
        composeTestRule.clickNode(
            matcher = hasTestTag("${TEST_TAG_MANAGE_SCREEN_CHEVRON_ICON}_$paymentMethodId"),
            scrollBehavior = ScrollBehavior.Required,
            useUnmergedTree = true,
        )
    }

    fun waitUntilGone(paymentMethodId: String) {
        composeTestRule.waitForNoNodes(
            matcher = hasTestTag("${TEST_TAG_MANAGE_SCREEN_CHEVRON_ICON}_$paymentMethodId"),
            atLeastOneRootRequired = true,
            useUnmergedTree = true,
        )
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
                .and(
                    hasAnyDescendant(
                        hasTestTag(TEST_TAG_ICON_FROM_RES).and(
                            hasTestMetadata(
                                cardBrand
                            )
                        )
                    )
                ),
            useUnmergedTree = true,
        ).assertExists()
    }
}
