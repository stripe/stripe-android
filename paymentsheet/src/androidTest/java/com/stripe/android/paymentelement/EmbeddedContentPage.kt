package com.stripe.android.paymentelement

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.stripe.android.paymentsheet.ui.TEST_TAG_ICON_FROM_RES
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_EDIT_SAVED_CARD
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_VIEW_MORE
import com.stripe.paymentelementtestpages.hasTestMetadata

internal class EmbeddedContentPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun waitUntilVisible() {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodes(hasTestTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    fun clickOnLpm(code: String) {
        waitUntilVisible()

        composeTestRule.onNode(hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$code"))
            .performScrollTo()
            .performClick()
    }

    fun assertHasSelectedLpm(code: String) {
        waitUntilVisible()

        composeTestRule.onNode(hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$code"))
            .assertIsSelected()
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

    fun clickOnSavedPM(paymentMethodId: String) {
        waitUntilVisible()

        composeTestRule.onNode(hasTestTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodId"))
            .performScrollTo()
            .performClick()
    }

    fun clickViewMore() {
        waitUntilVisible()

        composeTestRule.onNodeWithTag(TEST_TAG_VIEW_MORE).performClick()
    }

    fun clickEdit() {
        waitUntilVisible()

        composeTestRule.onNodeWithTag(TEST_TAG_EDIT_SAVED_CARD).performClick()
    }
}
