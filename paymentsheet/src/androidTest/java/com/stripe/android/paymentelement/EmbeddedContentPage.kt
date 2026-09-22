package com.stripe.android.paymentelement

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
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
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
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

    fun assertLpmIsEnabled(code: String, isEnabled: Boolean) {
        waitUntilVisible()

        val row = composeTestRule.onNode(hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$code"))
            .performScrollTo()
        if (isEnabled) {
            row.assertIsEnabled()
        } else {
            row.assertIsNotEnabled()
        }
    }

    fun assertPaymentMethodRowsAreEnabled(isEnabled: Boolean) {
        waitUntilVisible()

        val rowMatcher = SemanticsMatcher("saved or new payment-method row") { node ->
            val tag = node.config.getOrElse(SemanticsProperties.TestTag) { "" }
            tag.startsWith("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_") ||
                tag.startsWith("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_")
        }
        val rowsMatcher = rowMatcher.and(
            hasAnyAncestor(hasTestTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT))
        )
        val enabledMatcher = if (isEnabled) isEnabled() else isNotEnabled()

        composeTestRule.waitUntil {
            val rows = composeTestRule.onAllNodes(rowsMatcher)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
            val rowsWithExpectedState = composeTestRule.onAllNodes(rowsMatcher.and(enabledMatcher))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
            rows.isNotEmpty() && rowsWithExpectedState.size == rows.size
        }

        val rows = composeTestRule.onAllNodes(rowsMatcher)
        assertThat(rows.fetchSemanticsNodes()).isNotEmpty()
        rows.assertAll(enabledMatcher)
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
