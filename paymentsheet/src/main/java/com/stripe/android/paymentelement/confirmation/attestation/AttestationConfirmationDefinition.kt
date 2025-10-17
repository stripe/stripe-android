package com.stripe.android.paymentelement.confirmation.attestation

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.attestation.AttestationActivityContract
import com.stripe.android.attestation.AttestationActivityResult
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

internal class AttestationConfirmationDefinition @Inject constructor(
    private val errorReporter: ErrorReporter,
    private val integrityRequestManager: IntegrityRequestManager,
    @IOContext private val workContext: CoroutineContext,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>
) : ConfirmationDefinition<
    PaymentMethodConfirmationOption,
    ActivityResultLauncher<AttestationActivityContract.Args>,
    AttestationActivityContract.Args,
    AttestationActivityResult
    > {
    override val key = "Attestation"

    private var shouldAttestOnConfirm = false

    override fun option(confirmationOption: ConfirmationHandler.Option): PaymentMethodConfirmationOption? {
        return confirmationOption as? PaymentMethodConfirmationOption
    }

    override fun bootstrap(paymentMethodMetadata: PaymentMethodMetadata) {
        shouldAttestOnConfirm = paymentMethodMetadata.attestOnIntentConfirmation
        if (shouldAttestOnConfirm) {
            GlobalScope.launch(workContext) {
                integrityRequestManager.prepare()
            }
        }
    }

    override fun canConfirm(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args
    ): Boolean {
        return shouldAttestOnConfirm
    }

    override fun toResult(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        result: AttestationActivityResult
    ): ConfirmationDefinition.Result {
        return ConfirmationDefinition.Result.NextStep(
            confirmationOption = confirmationOption,
            arguments = confirmationArgs
        )
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
        if (shouldAttestOnConfirm) {
            return ConfirmationDefinition.Action.Launch(
                launcherArguments = AttestationActivityContract.Args(
                    publishableKey = publishableKeyProvider(),
                    productUsage = productUsage
                ),
                receivesResultInProcess = false,
                deferredIntentConfirmationType = null,
            )
        }

        val error = IllegalStateException("Tried to perform attestation when not required")
        errorReporter.report(
            ErrorReporter.UnexpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_UNEXPECTED_ATTESTATION,
            stripeException = StripeException.create(error)
        )

        return ConfirmationDefinition.Action.Fail(
            cause = error,
            message = "Tried to perform attestation when not required".resolvableString,
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal
        )
    }
}
