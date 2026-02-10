package com.stripe.android.common.taptoadd

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TapToAddCardDetailsActionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `clicking button calls startPaymentMethodCollection`() = runTest {
        FakeTapToAddHelper.test {
            val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
            val action = TapToAddCardDetailsAction(
                tapToAddHelper = helper,
                paymentMethodMetadata = paymentMethodMetadata,
            )

            composeTestRule.setContent {
                action.Content(enabled = true)
            }

            composeTestRule.onNodeWithText("Tap to add card").performClick()

            assertThat(collectCalls.awaitItem()).isEqualTo(paymentMethodMetadata)
        }
    }

    @Test
    fun `clicking disabled button does not call startPaymentMethodCollection`() = runTest {
        FakeTapToAddHelper.test {
            val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
            val action = TapToAddCardDetailsAction(
                tapToAddHelper = helper,
                paymentMethodMetadata = paymentMethodMetadata,
            )

            composeTestRule.setContent {
                action.Content(enabled = false)
            }

            composeTestRule.onNodeWithText("Tap to add card").performClick()

            // No collect calls should have been made
            collectCalls.expectNoEvents()
        }
    }
}
