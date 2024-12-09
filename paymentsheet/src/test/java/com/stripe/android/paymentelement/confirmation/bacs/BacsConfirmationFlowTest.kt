package com.stripe.android.paymentelement.confirmation.bacs

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.runLaunchTest
import com.stripe.android.paymentelement.confirmation.runResultTest
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.DefaultBacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test

class BacsConfirmationFlowTest {
    @Test
    fun `on launch, should persist parameters & launch using launcher as expected`() = runLaunchTest(
        confirmationOption = BACS_CONFIRMATION_OPTION,
        parameters = CONFIRMATION_PARAMETERS,
        definition = BacsConfirmationDefinition(
            bacsMandateConfirmationLauncherFactory = DefaultBacsMandateConfirmationLauncherFactory,
        ),
    )

    @Test
    fun `on result, should return confirmation result as expected`() = runResultTest(
        confirmationOption = BACS_CONFIRMATION_OPTION,
        definition = BacsConfirmationDefinition(
            bacsMandateConfirmationLauncherFactory = FakeBacsMandateConfirmationLauncherFactory(),
        ),
        launcherResult = BacsMandateConfirmationResult.Confirmed,
        parameters = CONFIRMATION_PARAMETERS,
        definitionResult = ConfirmationDefinition.Result.NextStep(
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = BACS_CONFIRMATION_OPTION.createParams,
                optionsParams = BACS_CONFIRMATION_OPTION.optionsParams,
                shouldSave = false,
            ),
            parameters = CONFIRMATION_PARAMETERS,
        ),
    )

    private companion object {
        private val BACS_CONFIRMATION_OPTION = BacsConfirmationOption(
            createParams = PaymentMethodCreateParams.create(
                bacsDebit = PaymentMethodCreateParams.BacsDebit(
                    accountNumber = "00012345",
                    sortCode = "108800"
                ),
                billingDetails = PaymentMethod.BillingDetails(
                    name = "John Doe",
                    email = "johndoe@email.com",
                )
            ),
            optionsParams = null,
        )

        private val PAYMENT_INTENT = PaymentIntentFactory.create()

        private val CONFIRMATION_PARAMETERS = ConfirmationDefinition.Parameters(
            intent = PAYMENT_INTENT,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123"
            ),
            shippingDetails = null,
            appearance = PaymentSheet.Appearance(),
        )
    }
}
