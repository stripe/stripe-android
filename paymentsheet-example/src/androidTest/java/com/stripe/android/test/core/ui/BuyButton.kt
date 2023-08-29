package com.stripe.android.test.core.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import com.stripe.android.ui.core.R as StripeUiCoreR
import com.stripe.android.paymentsheet.example.R as StripePaymentSheetExampleR

class BuyButton(
    private val composeTestRule: ComposeTestRule,
    private val processingCompleteTimeout: Duration = 5.seconds,
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

    fun waitProcessingComplete() {
        val expectedText = InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(
            StripeUiCoreR.string.stripe_pay_button_amount
        ).replace("%s", "")
        composeTestRule.waitUntil(timeoutMillis = processingCompleteTimeout.inWholeMilliseconds) {
            runCatching {
                composeTestRule.onNode(hasText(text = expectedText, substring = true))
                    .assertIsDisplayed()
            }.isSuccess && checkEnabled()
        }
    }

    fun waitPollingComplete() {
        val expectedText =
            InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(
                StripePaymentSheetExampleR.string.success
            )
        composeTestRule.waitUntil(timeoutMillis = processingCompleteTimeout.inWholeMilliseconds) {
            runCatching {

                composeTestRule.onNode(hasText(text = expectedText, substring = false))
                    .assertExists()
            }.isSuccess
        }
    }
}
