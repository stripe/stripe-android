package com.stripe.paymentelementtestpages

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.ui.SHEET_MANDATE_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_ICON_FROM_RES
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_EDIT_SAVED_CARD
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_SAVED_TEXT
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_VIEW_MORE
import com.stripe.android.testing.ScrollBehavior
import com.stripe.android.testing.clickNode
import com.stripe.android.testing.waitForNoNodes
import com.stripe.android.testing.waitForDisplayedNode
import com.stripe.android.testing.waitForNode

@SuppressWarnings("TooManyFunctions")
class VerticalModePage(
    private val composeTestRule: ComposeTestRule
) {
    fun assertIsNotVisible() {
        composeTestRule
            .onNodeWithTag(TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT)
            .assertDoesNotExist()
    }

    fun waitUntilVisible() {
        composeTestRule.waitForNode(
            matcher = hasTestTag(TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT),
            atLeastOneRootRequired = true,
        )
    }

    fun waitUntilMissing() {
        composeTestRule.waitForNoNodes(
            matcher = hasTestTag(TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT),
            atLeastOneRootRequired = false,
        )
    }

    fun clickOnNewLpm(paymentMethodCode: PaymentMethodCode) {
        composeTestRule.clickNode(
            matcher = hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodCode"),
            scrollBehavior = ScrollBehavior.Required,
        )
    }

    fun assertLpmIsSelected(paymentMethodCode: PaymentMethodCode) {
        composeTestRule.onNode(
            hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodCode").and(isSelected())
        ).assertExists()
    }

    fun assertPrimaryButton(matcher: SemanticsMatcher) {
        composeTestRule
            .onNode(hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG).and(matcher))
            .assertExists()
    }

    fun assertMandateExists() {
        composeTestRule
            .onNode(hasTestTag(SHEET_MANDATE_TEST_TAG))
            .assertExists()
    }

    fun assertMandateDoesNotExists() {
        composeTestRule
            .onNode(hasTestTag(SHEET_MANDATE_TEST_TAG))
            .assertDoesNotExist()
    }

    fun assertHasSavedPaymentMethods() {
        composeTestRule.waitForDisplayedNode(
            matcher = hasTestTag(TEST_TAG_SAVED_TEXT),
        )
        composeTestRule.onNodeWithTag(TEST_TAG_SAVED_TEXT).assertExists()
    }

    fun assertDoesNotHaveSavedPaymentMethods() {
        composeTestRule.waitForNoNodes(
            matcher = hasTestTag(TEST_TAG_SAVED_TEXT),
            atLeastOneRootRequired = false,
        )
        composeTestRule.onNodeWithTag(TEST_TAG_SAVED_TEXT).assertDoesNotExist()
    }

    fun assertHasSelectedSavedPaymentMethod(paymentMethodId: String, cardBrand: String? = null) {
        composeTestRule.onNode(
            hasTestTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodId").and(isSelected())
        ).assertExists()

        if (cardBrand != null) {
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

    fun assertHasDisplayedSavedPaymentMethod(paymentMethodId: String) {
        composeTestRule.onNode(
            hasTestTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodId")
        ).assertExists()
    }

    fun clickSavedPaymentMethod(paymentMethodId: String) {
        val matcher = hasTestTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodId")
        val savedPaymentMethod = composeTestRule.onNode(matcher)

        composeTestRule.waitForDisplayedNode(matcher = matcher)
        composeTestRule.clickNode(
            node = savedPaymentMethod,
            scrollBehavior = ScrollBehavior.Never,
        )
    }

    fun clickViewMore() {
        waitUntilVisible()
        composeTestRule.clickNode(
            matcher = hasTestTag(TEST_TAG_VIEW_MORE),
            scrollBehavior = ScrollBehavior.Never,
        )
    }

    fun clickEdit() {
        composeTestRule.clickNode(
            matcher = hasTestTag(TEST_TAG_EDIT_SAVED_CARD),
            scrollBehavior = ScrollBehavior.Never,
        )
    }

    fun clickNewPaymentMethodButton(paymentMethodCode: PaymentMethodCode) {
        composeTestRule.waitForNode(
            matcher = hasTestTag(TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT),
            atLeastOneRootRequired = true,
        )

        val testTag = "${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodCode"

        composeTestRule.waitForNode(
            matcher = hasTestTag(testTag),
            atLeastOneRootRequired = true,
        )

        composeTestRule.clickNode(
            matcher = hasTestTag(testTag),
            scrollBehavior = ScrollBehavior.Never,
        )
    }
}
