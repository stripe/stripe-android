package com.stripe.android.link.ui.paymentmethod

import android.content.Intent
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.LinkActivity
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.StripeIntentFixtures
import com.stripe.android.link.createAndroidIntentComposeRule
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.progressIndicatorTestTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class PaymentMethodScreenTest {
    @get:Rule
    val composeTestRule = createAndroidIntentComposeRule<LinkActivity> {
        PaymentConfiguration.init(it, "publishable_key")
        Intent(it, LinkActivity::class.java).apply {
            putExtra(
                LinkActivityContract.EXTRA_ARGS,
                LinkActivityContract.Args(
                    StripeIntentFixtures.PI_SUCCEEDED,
                    "Merchant, Inc"
                )
            )
        }
    }

    private val payButtonLabel = "Pay $10.99"
    private val payAnotherWayButtonLabel = "Pay another way"

    @Test
    fun buy_button_shows_progress_indicator_when_processing() {
        setContent(isProcessing = true)
        onProgressIndicator().assertExists()
    }

    @Test
    fun buttons_are_disabled_when_processing() {
        var count = 0
        setContent(
            isProcessing = true,
            onPayButtonClick = {
                count++
            },
            onPayAnotherWayClick = {
                count++
            }
        )

        onPayButton().assertDoesNotExist()
        onPayAnotherWayButton().performClick()

        assertThat(count).isEqualTo(0)
    }

    @Test
    fun pay_button_does_not_trigger_event_when_disabled() {
        var count = 0
        setContent(
            isProcessing = false,
            payButtonEnabled = false,
            onPayButtonClick = {
                count++
            }
        )

        onPayButton().performClick()

        assertThat(count).isEqualTo(0)
    }

    @Test
    fun pay_another_way_click_triggers_action() {
        var count = 0
        setContent(
            onPayAnotherWayClick = {
                count++
            }
        )

        onPayAnotherWayButton().performClick()

        assertThat(count).isEqualTo(1)
    }

    private fun setContent(
        isProcessing: Boolean = false,
        payButtonEnabled: Boolean = false,
        onPayButtonClick: () -> Unit = {},
        onPayAnotherWayClick: () -> Unit = {}
    ) = composeTestRule.setContent {
        DefaultLinkTheme {
            PaymentMethodBody(
                isProcessing = isProcessing,
                payButtonLabel = payButtonLabel,
                payButtonEnabled = payButtonEnabled,
                onPayButtonClick = onPayButtonClick,
                onPayAnotherWayClick = onPayAnotherWayClick,
                formContent = {}
            )
        }
    }

    private fun onPayButton() = composeTestRule.onNodeWithText(payButtonLabel)
    private fun onPayAnotherWayButton() = composeTestRule.onNodeWithText(payAnotherWayButtonLabel)
    private fun onProgressIndicator() = composeTestRule.onNodeWithTag(progressIndicatorTestTag)
}
