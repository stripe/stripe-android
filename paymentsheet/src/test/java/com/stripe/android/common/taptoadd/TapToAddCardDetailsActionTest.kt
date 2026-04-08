package com.stripe.android.common.taptoadd

import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class TapToAddCardDetailsActionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

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

            composeTestRule.waitForIdle()

            assertThat(helper.reportButtonShownCalls.awaitItem()).isNotNull()

            composeTestRule.onNodeWithText("Tap to add").performClick()

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

            composeTestRule.waitForIdle()

            assertThat(helper.reportButtonShownCalls.awaitItem()).isNotNull()

            composeTestRule.onNodeWithText("Tap to add").performClick()

            collectCalls.expectNoEvents()
        }
    }

    @Test
    fun `Content does not report button shown again when enabled changes`() = runTest {
        FakeTapToAddHelper.test {
            val action = TapToAddCardDetailsAction(
                tapToAddHelper = helper,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            )

            val enabledHolder = mutableStateOf(true)

            composeTestRule.setContent {
                action.Content(enabled = enabledHolder.value)
            }

            composeTestRule.waitForIdle()

            assertThat(helper.reportButtonShownCalls.awaitItem()).isNotNull()

            composeTestRule.runOnIdle {
                enabledHolder.value = false
            }

            composeTestRule.waitForIdle()
        }
    }
}
