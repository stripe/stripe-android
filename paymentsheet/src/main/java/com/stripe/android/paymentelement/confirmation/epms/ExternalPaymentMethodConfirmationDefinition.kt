package com.stripe.android.paymentelement.confirmation.epms

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.ExternalPaymentMethodContract
import com.stripe.android.paymentsheet.ExternalPaymentMethodInput
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Provider

internal class ExternalPaymentMethodConfirmationDefinition @Inject constructor(
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
    private val externalPaymentMethodConfirmHandlerProvider: Provider<ExternalPaymentMethodConfirmHandler?>,
    private val errorReporter: ErrorReporter,
) : ConfirmationDefinition<
    ExternalPaymentMethodConfirmationOption,
    ActivityResultLauncher<ExternalPaymentMethodInput>,
    Unit,
    PaymentResult
    > {
    override val key: String = "ExternalPaymentMethod"

    override fun option(
        confirmationOption: ConfirmationHandler.Option
    ): ExternalPaymentMethodConfirmationOption? {
        return confirmationOption as? ExternalPaymentMethodConfirmationOption
    }

    override suspend fun action(
        confirmationOption: ExternalPaymentMethodConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
    ): ConfirmationDefinition.Action<Unit> {
        val externalPaymentMethodType = confirmationOption.type
        val externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandlerProvider.get()

        return if (externalPaymentMethodConfirmHandler == null) {
            errorReporter.report(
                ErrorReporter.ExpectedErrorEvent.EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER_NULL,
                additionalNonPiiParams = mapOf("external_payment_method_type" to externalPaymentMethodType)
            )

            val error = IllegalStateException(
                "externalPaymentMethodConfirmHandler is null." +
                    " Cannot process payment for payment selection: $externalPaymentMethodType"
            )

            ConfirmationDefinition.Action.Fail(
                cause = error,
                message = error.stripeErrorMessage(),
                errorType = ConfirmationHandler.Result.Failed.ErrorType.ExternalPaymentMethod,
            )
        } else {
            ConfirmationDefinition.Action.Launch(
                launcherArguments = Unit,
                deferredIntentConfirmationType = null,
                receivesResultInProcess = false,
            )
        }
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (PaymentResult) -> Unit
    ): ActivityResultLauncher<ExternalPaymentMethodInput> {
        return activityResultCaller.registerForActivityResult(
            ExternalPaymentMethodContract(errorReporter),
            onResult
        )
    }

    override fun launch(
        launcher: ActivityResultLauncher<ExternalPaymentMethodInput>,
        arguments: Unit,
        confirmationOption: ExternalPaymentMethodConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
    ) {
        errorReporter.report(
            ErrorReporter.SuccessEvent.EXTERNAL_PAYMENT_METHODS_LAUNCH_SUCCESS,
            additionalNonPiiParams = mapOf("external_payment_method_type" to confirmationOption.type)
        )

        launcher.launch(
            ExternalPaymentMethodInput(
                paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
                type = confirmationOption.type,
                billingDetails = confirmationOption.billingDetails,
            )
        )
    }

    override fun toResult(
        confirmationOption: ExternalPaymentMethodConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        result: PaymentResult
    ): ConfirmationDefinition.Result {
        return when (result) {
            is PaymentResult.Completed -> ConfirmationDefinition.Result.Succeeded(
                intent = confirmationParameters.intent,
                deferredIntentConfirmationType = null,
            )
            is PaymentResult.Failed -> ConfirmationDefinition.Result.Failed(
                cause = result.throwable,
                message = result.throwable.stripeErrorMessage(),
                type = ConfirmationHandler.Result.Failed.ErrorType.ExternalPaymentMethod,
            )
            is PaymentResult.Canceled -> ConfirmationDefinition.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.None,
            )
        }
    }
}
