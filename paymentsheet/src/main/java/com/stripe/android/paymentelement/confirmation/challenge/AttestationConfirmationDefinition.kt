package com.stripe.android.paymentelement.confirmation.challenge

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.core.exception.StripeException
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Named

@Parcelize
object AttestationResult : Parcelable

internal class AttestationConfirmationDefinition @Inject constructor(
    private val errorReporter: ErrorReporter,
    private val integrityRequestManager: IntegrityRequestManager,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>
) : ConfirmationDefinition<
    PaymentMethodConfirmationOption,
    Unit,
    Unit,
    AttestationResult
    > {
    override val key = "Attestation"

    override fun option(confirmationOption: ConfirmationHandler.Option): PaymentMethodConfirmationOption? {
        return confirmationOption as? PaymentMethodConfirmationOption
    }

    override fun bootstrap(paymentMethodMetadata: PaymentMethodMetadata) {
        if (paymentMethodMetadata.attestationRequired) {
            // Warm up IntegrityRequestManager
            GlobalScope.launch {
                integrityRequestManager.prepare()
            }
        }
    }

    override fun canConfirm(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters
    ): Boolean {
        return confirmationOption.attestationRequired
    }

    override fun toResult(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        result: AttestationResult
    ): ConfirmationDefinition.Result {
        // This should never be called since we don't use launchers
        return ConfirmationDefinition.Result.NextStep(
            confirmationOption = confirmationOption,
            parameters = confirmationParameters
        )
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (AttestationResult) -> Unit
    ): Unit {
        // No launcher needed for attestation
    }

    override fun launch(
        launcher: Unit,
        arguments: Unit,
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters
    ) {
        // No-op since we don't use a launcher
    }

    override suspend fun action(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters
    ): ConfirmationDefinition.Action<Unit> {
        if (!confirmationOption.attestationRequired) {
            return ConfirmationDefinition.Action.Complete(
                intent = confirmationParameters.intent,
                confirmationOption = confirmationOption,
                deferredIntentConfirmationType = null,
                completedFullPaymentFlow = false
            )
        }

        return try {
            val result = withContext(Dispatchers.IO) {
                integrityRequestManager.requestToken()
            }

            result.fold(
                onSuccess = { token ->
                    ConfirmationDefinition.Action.Complete(
                        intent = confirmationParameters.intent,
                        confirmationOption = confirmationOption.attachAttestationToken(token),
                        deferredIntentConfirmationType = null,
                        completedFullPaymentFlow = false
                    )
                },
                onFailure = { error ->
                    ConfirmationDefinition.Action.Complete(
                        intent = confirmationParameters.intent,
                        confirmationOption = confirmationOption.attachAttestationToken(null),
                        deferredIntentConfirmationType = null,
                        completedFullPaymentFlow = false
                    )
                }
            )
        } catch (error: Exception) {
            ConfirmationDefinition.Action.Complete(
                intent = confirmationParameters.intent,
                confirmationOption = confirmationOption.attachAttestationToken(null),
                deferredIntentConfirmationType = null,
                completedFullPaymentFlow = false
            )
        }
    }

    private fun PaymentMethodConfirmationOption.attachAttestationToken(token: String?): PaymentMethodConfirmationOption {
        return when (this) {
            is PaymentMethodConfirmationOption.New -> {
                // For new payment methods, we don't attach the attestation token
                // It should be handled differently based on your requirements
                copy(attestationRequired = false)
            }
            is PaymentMethodConfirmationOption.Saved -> {
                copy(
                    attestationToken = token,
                    attestationRequired = false
                )
            }
        }
    }
}