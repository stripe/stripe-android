package com.stripe.android.paymentelement.confirmation.epms

import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.runLaunchTest
import com.stripe.android.paymentelement.confirmation.runResultTest
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test

class ExternalPaymentMethodConfirmationFlowTest {
    @Test
    fun `on launch, should persist parameters & launch using launcher as expected`() = runLaunchTest(
        confirmationOption = EPM_CONFIRMATION_OPTION,
        parameters = CONFIRMATION_PARAMETERS,
        definition = ExternalPaymentMethodConfirmationDefinition(
            externalPaymentMethodConfirmHandlerProvider = {
                ExternalPaymentMethodConfirmHandler { _, _ ->
                    error("Not implemented!")
                }
            },
            errorReporter = FakeErrorReporter()
        ),
    )

    @Test
    fun `on result, should return confirmation result as expected`() = runResultTest(
        confirmationOption = EPM_CONFIRMATION_OPTION,
        parameters = CONFIRMATION_PARAMETERS,
        definition = ExternalPaymentMethodConfirmationDefinition(
            externalPaymentMethodConfirmHandlerProvider = {
                ExternalPaymentMethodConfirmHandler { _, _ ->
                    error("Not implemented!")
                }
            },
            errorReporter = FakeErrorReporter()
        ),
        launcherResult = PaymentResult.Completed,
        definitionResult = ConfirmationDefinition.Result.Succeeded(
            intent = PAYMENT_INTENT,
            deferredIntentConfirmationType = null,
        )
    )

    private companion object {
        private val EPM_CONFIRMATION_OPTION = ExternalPaymentMethodConfirmationOption(
            type = "paypal",
            billingDetails = PaymentMethod.BillingDetails(
                name = "John Doe",
                address = Address(
                    line1 = "123 Apple Street",
                    city = "South San Francisco",
                    state = "CA",
                    country = "US",
                ),
            )
        )

        private val PAYMENT_INTENT = PaymentIntentFactory.create()

        private val CONFIRMATION_PARAMETERS = ConfirmationDefinition.Parameters(
            intent = PAYMENT_INTENT,
            appearance = PaymentSheet.Appearance(),
            shippingDetails = AddressDetails(),
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123",
            ),
        )
    }
}
