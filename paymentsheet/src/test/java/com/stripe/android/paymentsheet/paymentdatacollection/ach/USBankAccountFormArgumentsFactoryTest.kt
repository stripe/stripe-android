package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import org.junit.Test

internal class USBankAccountFormArgumentsFactoryTest {

    @Test
    fun `create combines metadata and host arguments`() {
        val incentive = PaymentMethodIncentive(
            identifier = "link_instant_debits",
            displayText = "$5",
        )
        val metadata = PaymentMethodMetadataFactory.create(
            paymentMethodIncentive = incentive,
            onBehalfOf = "acct_123",
            sellerBusinessName = "Rocket Rides",
            forceSetupFutureUseBehaviorAndNewMandate = true,
        )
        val draftPaymentSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        val autocompleteAddressInteractorFactory = TestAutocompleteAddressInteractor.noOpFactory()

        val arguments = USBankAccountFormArgumentsFactory.create(
            paymentMethodMetadata = metadata,
            selectedPaymentMethodCode = PaymentMethod.Type.Link.code,
            hostedSurface = "test_surface",
            host = USBankAccountFormArgumentsFactory.Host(
                isCompleteFlow = true,
                shippingDetails = null,
                draftPaymentSelection = draftPaymentSelection,
                autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
                setAsDefaultMatchesSaveForFutureUse = true,
                termsDisplay = PaymentSheet.TermsDisplay.NEVER,
                onAnalyticsEvent = {},
                onMandateTextChanged = { _, _ -> },
                onLinkedBankAccountChanged = {},
                onUpdatePrimaryButtonUIState = { it },
                onUpdatePrimaryButtonState = {},
                onError = {},
                onFormCompleted = {},
            ),
        )

        assertThat(arguments.hostedSurface).isEqualTo("test_surface")
        assertThat(arguments.instantDebits).isTrue()
        assertThat(arguments.showCheckbox).isFalse()
        assertThat(arguments.isCompleteFlow).isTrue()
        assertThat(arguments.isPaymentFlow).isTrue()
        assertThat(arguments.stripeIntentId).isEqualTo(metadata.stripeIntent.id)
        assertThat(arguments.clientSecret).isEqualTo(metadata.stripeIntent.clientSecret)
        assertThat(arguments.draftPaymentSelection).isSameInstanceAs(draftPaymentSelection)
        assertThat(arguments.autocompleteAddressInteractorFactory)
            .isSameInstanceAs(autocompleteAddressInteractorFactory)
        assertThat(arguments.incentive).isSameInstanceAs(incentive)
        assertThat(arguments.setAsDefaultMatchesSaveForFutureUse).isTrue()
        assertThat(arguments.termsDisplay).isEqualTo(PaymentSheet.TermsDisplay.NEVER)
        assertThat(arguments.onBehalfOf).isEqualTo("acct_123")
        assertThat(arguments.sellerBusinessName).isEqualTo("Rocket Rides")
        assertThat(arguments.forceSetupFutureUseBehavior).isTrue()
    }
}
