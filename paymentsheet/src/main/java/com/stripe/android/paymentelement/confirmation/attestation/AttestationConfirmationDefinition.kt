package com.stripe.android.paymentelement.confirmation.attestation

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.attestation.AttestationActivityContract
import com.stripe.android.attestation.AttestationActivityResult
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.AndroidVerificationObject
import com.stripe.android.model.RadarOptions
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import javax.inject.Inject
import javax.inject.Named

internal class AttestationConfirmationDefinition @Inject constructor(
    private val errorReporter: ErrorReporter,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>
) : ConfirmationDefinition<
    PaymentMethodConfirmationOption,
    ActivityResultLauncher<AttestationActivityContract.Args>,
    AttestationActivityContract.Args,
    AttestationActivityResult
    > {
    override val key = "Attestation"

    private var attestOnIntentConfirmation: Boolean = false

    override fun option(confirmationOption: ConfirmationHandler.Option): PaymentMethodConfirmationOption? {
        return confirmationOption as? PaymentMethodConfirmationOption
    }

    override fun bootstrap(paymentMethodMetadata: PaymentMethodMetadata) {
        attestOnIntentConfirmation = paymentMethodMetadata.attestOnIntentConfirmation
    }

    override fun canConfirm(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args
    ): Boolean {
        return attestOnIntentConfirmation
    }

    override fun toResult(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        result: AttestationActivityResult
    ): ConfirmationDefinition.Result {
        return when (result) {
            is AttestationActivityResult.Failed -> {
                ConfirmationDefinition.Result.NextStep(
                    confirmationOption = confirmationOption.attachToken(null),
                    arguments = confirmationArgs
                )
            }
            is AttestationActivityResult.Success -> {
                ConfirmationDefinition.Result.NextStep(
                    confirmationOption = confirmationOption.attachToken(result.token),
                    arguments = confirmationArgs
                )
            }
        }
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (AttestationActivityResult) -> Unit
    ): ActivityResultLauncher<AttestationActivityContract.Args> {
        return activityResultCaller.registerForActivityResult(AttestationActivityContract(), onResult)
    }

    override fun launch(
        launcher: ActivityResultLauncher<AttestationActivityContract.Args>,
        arguments: AttestationActivityContract.Args,
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args
    ) {
        launcher.launch(arguments)
    }

    override suspend fun action(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args
    ): ConfirmationDefinition.Action<AttestationActivityContract.Args> {
        if (attestOnIntentConfirmation) {
            // Disable attestation after first call to prevent multiple attestations
            attestOnIntentConfirmation = false
            return ConfirmationDefinition.Action.Launch(
                launcherArguments = AttestationActivityContract.Args(
                    publishableKey = publishableKeyProvider(),
                    productUsage = productUsage
                ),
                receivesResultInProcess = false,
                deferredIntentConfirmationType = null,
            )
        }

        val error = IllegalArgumentException("Attestation is not enabled on intent confirmation")
        errorReporter.report(
            ErrorReporter.UnexpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_ATTESTATION_INVOKED_WHEN_DISABLED,
            stripeException = StripeException.create(error)
        )

        return ConfirmationDefinition.Action.Fail(
            cause = error,
            message = "Attestation is not enabled on intent confirmation".resolvableString,
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal
        )
    }

    private fun PaymentMethodConfirmationOption.attachToken(token: String?): PaymentMethodConfirmationOption {
        return when (this) {
            is PaymentMethodConfirmationOption.New -> {
                copy(
                    createParams = createParams.copy(
                        radarOptions = token?.let {
                            RadarOptions(
                                hCaptchaToken = null,
                                androidVerificationObject = AndroidVerificationObject(
                                    androidVerificationToken = it
                                )
                            )
                        }
                    )
                )
            }
            is PaymentMethodConfirmationOption.Saved -> {
                // For saved payment methods, we need to attach the token via radar options
                // This will be handled in the confirmation interceptors
                copy()
            }
        }
    }
}
