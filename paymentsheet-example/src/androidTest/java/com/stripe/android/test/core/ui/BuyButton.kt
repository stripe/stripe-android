package com.stripe.android.test.core.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.stripe.android.paymentelement.embedded.form.EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON
import com.stripe.android.test.core.DEFAULT_UI_TIMEOUT
import kotlin.time.Duration

class BuyButton(
    private val composeTestRule: ComposeTestRule,
    private val processingCompleteTimeout: Duration = DEFAULT_UI_TIMEOUT,
) {

    fun click() {
        composeTestRule.onNode(hasTestTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON))
            .performScrollTo()
            .performClick()
    }

    fun checkEnabled(): Boolean = runCatching {
        composeTestRule.onNode(hasTestTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON))
            .assertIsEnabled()
    }.isSuccess

    fun isEnabled() {
        composeTestRule.onNode(hasTestTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON))
            .assertIsEnabled()
    }

    fun isDisplayed() {
        composeTestRule.onNode(hasTestTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON))
            .assertIsDisplayed()
    }

    fun scrollTo() {
        composeTestRule.onNode(hasTestTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON))
            .performScrollTo()
    }

    fun waitProcessingComplete() {
        composeTestRule.waitUntil(timeoutMillis = processingCompleteTimeout.inWholeMilliseconds) {
            runCatching {
                composeTestRule.onNode(
                    hasTestTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON)
                ).assertIsDisplayed()
            }.isSuccess && checkEnabled()
        }
    }
}
