package com.stripe.android.paymentelement.confirmation.attestation

import com.stripe.android.attestation.AttestationActivityResult
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.AndroidVerificationObject
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.RadarOptions
import com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.PAYMENT_INTENT
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.runLaunchTest
import com.stripe.android.paymentelement.confirmation.runResultTest
import com.stripe.android.testing.FakeErrorReporter
import org.junit.Test

internal class AttestationConfirmationFlowTest {
    @Test
    fun `on launch with New option, should persist parameters & launch using launcher as expected`() = runLaunchTest(
        confirmationOption = NEW_CONFIRMATION_OPTION,
        parameters = CONFIRMATION_PARAMETERS,
        definition = confirmationDefinition().apply {
            bootstrap(PaymentMethodMetadataFactory.create(attestOnIntentConfirmation = true))
        },
    )

    @Test
    fun `on launch with Saved option, should persist parameters & launch using launcher as expected`() = runLaunchTest(
        confirmationOption = SAVED_CONFIRMATION_OPTION,
        parameters = CONFIRMATION_PARAMETERS,
        definition = confirmationDefinition().apply {
            bootstrap(PaymentMethodMetadataFactory.create(attestOnIntentConfirmation = true))
        },
    )

    @Test
    fun `on result with Success, should return NextStep result with androidVerificationObject for New option`() =
        runResultTest(
            confirmationOption = NEW_CONFIRMATION_OPTION,
            definition = confirmationDefinition(),
            launcherResult = AttestationActivityResult.Success("test_token"),
            parameters = CONFIRMATION_PARAMETERS,
            definitionResult = ConfirmationDefinition.Result.NextStep(
                confirmationOption = PaymentMethodConfirmationOption.New(
                    createParams = NEW_CONFIRMATION_OPTION.createParams.copy(
                        radarOptions = RadarOptions(
                            hCaptchaToken = null,
                            androidVerificationObject = AndroidVerificationObject(
                                androidVerificationToken = "test_token"
                            )
                        )
                    ),
                    optionsParams = NEW_CONFIRMATION_OPTION.optionsParams,
                    shouldSave = false,
                    extraParams = null,
                    passiveCaptchaParams = null
                ),
                arguments = CONFIRMATION_PARAMETERS,
            ),
        )

    @Test
    fun `on result with Success, should return NextStep result unchanged for Saved option`() = runResultTest(
        confirmationOption = SAVED_CONFIRMATION_OPTION,
        definition = confirmationDefinition(),
        launcherResult = AttestationActivityResult.Success("test_token"),
        parameters = CONFIRMATION_PARAMETERS,
        definitionResult = ConfirmationDefinition.Result.NextStep(
            confirmationOption = SAVED_CONFIRMATION_OPTION,
            arguments = CONFIRMATION_PARAMETERS,
        ),
    )

    @Test
    fun `on result with Failed, should return NextStep result with no token for New option`() = runResultTest(
        confirmationOption = NEW_CONFIRMATION_OPTION,
        definition = confirmationDefinition(),
        launcherResult = AttestationActivityResult.Failed(RuntimeException("Attestation failed")),
        parameters = CONFIRMATION_PARAMETERS,
        definitionResult = ConfirmationDefinition.Result.NextStep(
            confirmationOption = NEW_CONFIRMATION_OPTION,
            arguments = CONFIRMATION_PARAMETERS,
        ),
    )

    @Test
    fun `on result with Failed, should return NextStep result with no token for Saved option`() = runResultTest(
        confirmationOption = SAVED_CONFIRMATION_OPTION,
        definition = confirmationDefinition(),
        launcherResult = AttestationActivityResult.Failed(RuntimeException("Attestation failed")),
        parameters = CONFIRMATION_PARAMETERS,
        definitionResult = ConfirmationDefinition.Result.NextStep(
            confirmationOption = SAVED_CONFIRMATION_OPTION,
            arguments = CONFIRMATION_PARAMETERS,
        ),
    )

    private companion object {
        private val NEW_CONFIRMATION_OPTION = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
            passiveCaptchaParams = null,
        )

        private val SAVED_CONFIRMATION_OPTION = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PAYMENT_INTENT.paymentMethod!!,
            optionsParams = null,
            originatedFromWallet = false,
            passiveCaptchaParams = null,
            hCaptchaToken = null,
        )
    }

    private fun confirmationDefinition(
        errorReporter: FakeErrorReporter = FakeErrorReporter(),
        publishableKey: String = "pk_123",
        productUsage: Set<String> = setOf("PaymentSheet")
    ) = AttestationConfirmationDefinition(
        errorReporter = errorReporter,
        publishableKeyProvider = { publishableKey },
        productUsage = productUsage,
    )
}
