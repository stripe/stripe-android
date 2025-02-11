package com.stripe.android.paymentsheet.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import org.junit.Rule
import org.junit.Test

class GooglePayButtonTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun handlesPressWhenEnabled() {
        val testTag = GOOGLE_PAY_BUTTON_TEST_TAG
        var didCallOnPressed = false

        composeTestRule.setContent {
            GooglePayButton(
                state = null,
                isEnabled = true,
                allowCreditCards = true,
                billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(),
                onPressed = { didCallOnPressed = true },
                modifier = Modifier.testTag(testTag),
                buttonType = GooglePayButtonType.Pay,
                cardBrandFilter = DefaultCardBrandFilter
            )
        }

        composeTestRule
            .onNodeWithTag(testTag)
            .performClick()

        composeTestRule.waitForIdle()

        assertThat(didCallOnPressed).isTrue()
    }

    @Test
    fun ignoresPressWhenDisabled() {
        val testTag = GOOGLE_PAY_BUTTON_TEST_TAG
        var didCallOnPressed = false

        composeTestRule.setContent {
            GooglePayButton(
                state = null,
                isEnabled = false,
                allowCreditCards = true,
                billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(),
                onPressed = { didCallOnPressed = true },
                modifier = Modifier.testTag(testTag),
                buttonType = GooglePayButtonType.Pay,
                cardBrandFilter = DefaultCardBrandFilter
            )
        }

        composeTestRule
            .onNodeWithTag(testTag)
            .performClick()

        composeTestRule.waitForIdle()

        assertThat(didCallOnPressed).isFalse()
    }
}
