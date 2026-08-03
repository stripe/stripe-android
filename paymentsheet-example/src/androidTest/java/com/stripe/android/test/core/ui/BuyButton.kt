package com.stripe.android.test.core.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.test.core.DEFAULT_UI_TIMEOUT
import kotlin.time.Duration

class BuyButton(
    private val composeTestRule: ComposeTestRule,
    private val processingCompleteTimeout: Duration = DEFAULT_UI_TIMEOUT,
) {

    fun click() {
        composeTestRule.onNode(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG))
            .performScrollTo()
            .performClick()
    }

    fun checkEnabled(): Boolean = runCatching {
        composeTestRule.onNode(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG))
            .assertIsEnabled()
    }.isSuccess

    /**
     * Waits until the button is displayed and enabled before returning. Callers should invoke this
     * before [click] so we never click a button that has not composed or is still disabled (e.g.
     * while the form is validating or an intent is being created), which otherwise races.
     */
    fun waitForEnabled(): BuyButton {
        composeTestRule.waitUntil(timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
            runCatching {
                composeTestRule.onNode(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG))
                    .assertIsDisplayed()
            }.isSuccess && checkEnabled()
        }
        return this
    }

    fun isEnabled() {
        composeTestRule.onNode(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG))
            .assertIsEnabled()
    }

    fun isDisplayed() {
        composeTestRule.onNode(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG))
            .assertIsDisplayed()
    }

    fun scrollTo() {
        composeTestRule.onNode(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG))
            .performScrollTo()
    }

    fun waitProcessingComplete() {
        composeTestRule.waitUntil(timeoutMillis = processingCompleteTimeout.inWholeMilliseconds) {
            runCatching {
                composeTestRule.onNode(
                    hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG)
                ).assertIsDisplayed()
            }.isSuccess && checkEnabled()
        }
    }
}
