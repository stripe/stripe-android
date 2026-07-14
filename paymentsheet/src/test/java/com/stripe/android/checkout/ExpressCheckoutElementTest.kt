package com.stripe.android.checkout

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.ece.ExpressCheckoutElementInteractor
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class ExpressCheckoutElementTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `content creates interactor and renders content`() {
        val factory = FakeExpressCheckoutElementInteractorFactory(
            interactor = FakeExpressCheckoutElementInteractor(
                state = stateFlowOf(
                    ExpressCheckoutElementInteractor.State(
                        walletButtons = listOf(ExpressCheckoutElementInteractor.ExpressButton.GooglePay),
                    )
                )
            )
        )
        val element = ExpressCheckoutElement(interactorFactory = factory)

        composeRule.setContent {
            element.Content()
        }

        composeRule.onNodeWithText("Google Pay Button").assertExists()
        composeRule.onNodeWithText("Link Button").assertDoesNotExist()
        assertThat(factory.createCalls).isEqualTo(1)
    }

    private class FakeExpressCheckoutElementInteractorFactory(
        private val interactor: ExpressCheckoutElementInteractor,
    ) : ExpressCheckoutElementInteractor.Factory {
        var createCalls = 0
            private set

        override fun create(): ExpressCheckoutElementInteractor {
            createCalls++
            return interactor
        }
    }

    private class FakeExpressCheckoutElementInteractor(
        override val state: StateFlow<ExpressCheckoutElementInteractor.State>,
    ) : ExpressCheckoutElementInteractor
}
