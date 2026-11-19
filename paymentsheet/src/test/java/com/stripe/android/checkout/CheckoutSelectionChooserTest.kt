package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.Test

internal class CheckoutSelectionChooserTest {
    @Test
    fun `uses the new selection when there is no previous selection`() = runScenario {
        val selection = choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(isGooglePayReady = true),
            previousSelection = null,
            newSelection = PaymentSelection.GooglePay,
        )

        assertThat(selection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `keeps a new card selection when the card form gains a save option`() = runScenario {
        // A session that gains a customer adds a save checkbox to the card form.
        val selection = choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                hasCustomerConfiguration = true,
                isGooglePayReady = true,
            ),
            previousSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            newSelection = PaymentSelection.GooglePay,
        )

        assertThat(selection).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `uses the new selection when the previous new selection's type is no longer supported`() = runScenario {
        val selection = choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card"),
                ),
                isGooglePayReady = true,
            ),
            previousSelection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            newSelection = PaymentSelection.GooglePay,
        )

        assertThat(selection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `keeps a saved selection that is still in the customer's payment methods`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.createCard()

        val selection = choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                hasCustomerConfiguration = true,
                isGooglePayReady = true,
            ),
            paymentMethods = PaymentMethodFixtures.createCards(2) + paymentMethod,
            previousSelection = PaymentSelection.Saved(paymentMethod),
            newSelection = PaymentSelection.GooglePay,
        )

        assertThat(selection).isEqualTo(PaymentSelection.Saved(paymentMethod))
    }

    @Test
    fun `uses the new selection when the saved payment method was removed`() = runScenario {
        val selection = choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                hasCustomerConfiguration = true,
                isGooglePayReady = true,
            ),
            paymentMethods = PaymentMethodFixtures.createCards(2),
            previousSelection = PaymentSelection.Saved(PaymentMethodFixtures.createCard()),
            newSelection = PaymentSelection.GooglePay,
        )

        assertThat(selection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `uses the new selection when Google Pay is no longer ready`() = runScenario {
        val selection = choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(isGooglePayReady = false),
            previousSelection = PaymentSelection.GooglePay,
            newSelection = null,
        )

        assertThat(selection).isNull()
    }

    @Test
    fun `keeps the previous selection instead of the loader default when set as default is enabled`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val selection = choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                hasCustomerConfiguration = true,
                isPaymentMethodSetAsDefaultEnabled = true,
                isGooglePayReady = true,
            ),
            paymentMethods = listOf(paymentMethod),
            previousSelection = PaymentSelection.GooglePay,
            newSelection = PaymentSelection.Saved(paymentMethod),
        )

        assertThat(selection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `returns no selection when rows confirm immediately and the form sheet confirms`() = runScenario(
        internalRowSelectionCallback = {},
    ) {
        val selection = choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(isGooglePayReady = true),
            previousSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            newSelection = PaymentSelection.GooglePay,
            formSheetAction = EmbeddedPaymentElement.FormSheetAction.Confirm,
        )

        assertThat(selection).isNull()
    }

    private fun runScenario(
        internalRowSelectionCallback: InternalRowSelectionCallback? = null,
        block: Scenario.() -> Unit,
    ) {
        Scenario(
            chooser = CheckoutSelectionChooser(
                internalRowSelectionCallback = { internalRowSelectionCallback },
            ),
        ).block()
    }

    private class Scenario(
        private val chooser: CheckoutSelectionChooser,
    ) {
        fun choose(
            paymentMethodMetadata: PaymentMethodMetadata,
            previousSelection: PaymentSelection?,
            newSelection: PaymentSelection?,
            paymentMethods: List<PaymentMethod>? = null,
            formSheetAction: EmbeddedPaymentElement.FormSheetAction = EmbeddedPaymentElement.FormSheetAction.Continue,
        ): PaymentSelection? = chooser.choose(
            paymentMethodMetadata = paymentMethodMetadata,
            paymentMethods = paymentMethods,
            previousSelection = previousSelection,
            newSelection = newSelection,
            newConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .build()
                .asCommonConfiguration(),
            formSheetAction = formSheetAction,
        )
    }
}
