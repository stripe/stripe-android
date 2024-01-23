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
