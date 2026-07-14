package com.stripe.android.checkout

import androidx.compose.material.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.flow.MutableStateFlow
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

    @Test
    fun `content updates when resolved state becomes available after initial composition`() {
        val state = MutableStateFlow<ExpressCheckoutElement.ResolvedState?>(null)
        val element = ExpressCheckoutElement(state = state) { resolvedState ->
            Text(resolvedState.paymentMethodMetadata.availableWallets.joinToString { it.code })
        }

        composeRule.setContent {
            element.Content()
        }

        composeRule.onNodeWithText("link").assertDoesNotExist()

        composeRule.runOnIdle {
            state.value = ExpressCheckoutElement.ResolvedState(
                commonConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example Merchant")
                    .build()
                    .asCommonConfiguration(),
                configuration = ExpressCheckoutElement.Configuration().build(),
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    availableWallets = listOf(WalletType.Link),
                ),
            )
        }

        composeRule.onNodeWithText("link").assertExists()
    }
}
