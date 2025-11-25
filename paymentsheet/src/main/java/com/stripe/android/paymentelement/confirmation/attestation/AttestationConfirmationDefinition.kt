package com.stripe.android.paymentelement.confirmation.attestation

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.attestation.AttestationActivityContract
import com.stripe.android.attestation.AttestationActivityResult
import com.stripe.android.attestation.analytics.AttestationAnalyticsEventsReporter
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
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
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

internal class AttestationConfirmationDefinition @Inject constructor(
    private val errorReporter: ErrorReporter,
    private val integrityRequestManager: IntegrityRequestManager,
    @AttestationScope private val coroutineScope: CoroutineScope,
    @IOContext private val workContext: CoroutineContext,
    private val attestationAnalyticsEventsReporter: AttestationAnalyticsEventsReporter,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>
) : ConfirmationDefinition<
    PaymentMethodConfirmationOption.New,
    ActivityResultLauncher<AttestationActivityContract.Args>,
    AttestationActivityContract.Args,
    AttestationActivityResult
    > {
    override val key = "Attestation"

    override fun option(confirmationOption: ConfirmationHandler.Option): PaymentMethodConfirmationOption.New? {
        return confirmationOption as? PaymentMethodConfirmationOption.New
    }

    override fun bootstrap(paymentMethodMetadata: PaymentMethodMetadata) {
        if (paymentMethodMetadata.attestOnIntentConfirmation.not()) return
        coroutineScope.launch(workContext) {
            attestationAnalyticsEventsReporter.prepare()
            integrityRequestManager.prepare()
                .onSuccess {
                    attestationAnalyticsEventsReporter.prepareSucceeded()
                }.onFailure { error ->
                    attestationAnalyticsEventsReporter.prepareFailed(error)
                    errorReporter.report(
                        ErrorReporter.UnexpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_ATTESTATION_FAILED_TO_PREPARE,
                        stripeException = StripeException.create(error)
                    )
                }
        }
    }

    override fun canConfirm(
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationArgs: ConfirmationHandler.Args
    ): Boolean {
        return confirmationOption.createParams.typeCode == "card" &&
            confirmationArgs.paymentMethodMetadata.attestOnIntentConfirmation &&
            confirmationOption.attestationComplete.not() &&
            confirmationOption.hasToken().not()
    }

    override fun toResult(
        confirmationOption: PaymentMethodConfirmationOption.New,
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
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationArgs: ConfirmationHandler.Args
    ) {
        launcher.launch(arguments)
    }

    override suspend fun action(
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationArgs: ConfirmationHandler.Args
    ): ConfirmationDefinition.Action<AttestationActivityContract.Args> {
        if (confirmationArgs.paymentMethodMetadata.attestOnIntentConfirmation) {
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

    private fun PaymentMethodConfirmationOption.New.attachToken(token: String?): PaymentMethodConfirmationOption {
        return copy(
            createParams = createParams.copy(
                radarOptions = token?.let {
                    RadarOptions(
                        hCaptchaToken = null,
                        androidVerificationObject = AndroidVerificationObject(
                            androidVerificationToken = it
                        )
                    )
                }
            ),
            attestationComplete = true
        )
    }

    private fun PaymentMethodConfirmationOption.hasToken(): Boolean {
        return when (this) {
            is PaymentMethodConfirmationOption.New -> {
                createParams.radarOptions?.androidVerificationObject?.androidVerificationToken != null
            }
            is PaymentMethodConfirmationOption.Saved -> {
                attestationToken != null
            }
        }
    }
}
