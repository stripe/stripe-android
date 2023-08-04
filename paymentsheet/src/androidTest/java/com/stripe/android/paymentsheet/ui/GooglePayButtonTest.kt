package com.stripe.android.paymentsheet.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.paymentsheet.MainActivity
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class GooglePayButtonTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Ignore("Re-enable once we have refactored the Google Pay button handling")
    @Test
    fun handlesPressWhenEnabled() {
        val testTag = GOOGLE_PAY_BUTTON_TEST_TAG
        var didCallOnPressed = false

        composeTestRule.setContent {
            GooglePayButton(
                state = PrimaryButton.State.Ready,
                isEnabled = true,
                allowCreditCards = true,
                billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(),
                onPressed = { didCallOnPressed = true },
                modifier = Modifier.testTag(testTag),
            )
        }

        composeTestRule
            .onNodeWithTag(testTag)
            .performClick()

        composeTestRule.waitForIdle()

        assertThat(didCallOnPressed).isTrue()
    }

    @Ignore("Re-enable once we have refactored the Google Pay button handling")
    @Test
    fun ignoresPressWhenDisabled() {
        val testTag = GOOGLE_PAY_BUTTON_TEST_TAG
        var didCallOnPressed = false

        composeTestRule.setContent {
            GooglePayButton(
                state = PrimaryButton.State.Ready,
                isEnabled = false,
                allowCreditCards = true,
                billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(),
                onPressed = { didCallOnPressed = true },
                modifier = Modifier.testTag(testTag),
            )
        }

        composeTestRule
            .onNodeWithTag(testTag)
            .performClick()

        composeTestRule.waitForIdle()

        assertThat(didCallOnPressed).isFalse()
    }
}
