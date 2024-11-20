package com.stripe.android.paymentelement.confirmation.epms

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.ExternalPaymentMethodContract
import com.stripe.android.paymentsheet.ExternalPaymentMethodInput
import java.lang.IllegalStateException
import javax.inject.Provider

internal class ExternalPaymentMethodConfirmationDefinition(
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
        intent: StripeIntent
    ): ConfirmationDefinition.ConfirmationAction<Unit> {
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

            ConfirmationDefinition.ConfirmationAction.Fail(
                cause = error,
                message = error.stripeErrorMessage(),
                errorType = ConfirmationHandler.Result.Failed.ErrorType.ExternalPaymentMethod,
            )
        } else {
            ConfirmationDefinition.ConfirmationAction.Launch(
                launcherArguments = Unit,
                deferredIntentConfirmationType = null,
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
        intent: StripeIntent,
    ) {
        errorReporter.report(
            ErrorReporter.SuccessEvent.EXTERNAL_PAYMENT_METHODS_LAUNCH_SUCCESS,
            additionalNonPiiParams = mapOf("external_payment_method_type" to confirmationOption.type)
        )

        launcher.launch(
            ExternalPaymentMethodInput(
                type = confirmationOption.type,
                billingDetails = confirmationOption.billingDetails,
            )
        )
    }

    override fun toPaymentConfirmationResult(
        confirmationOption: ExternalPaymentMethodConfirmationOption,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intent: StripeIntent,
        result: PaymentResult
    ): ConfirmationHandler.Result {
        return when (result) {
            is PaymentResult.Completed -> ConfirmationHandler.Result.Succeeded(
                intent = intent,
                deferredIntentConfirmationType = null,
            )
            is PaymentResult.Failed -> ConfirmationHandler.Result.Failed(
                cause = result.throwable,
                message = result.throwable.stripeErrorMessage(),
                type = ConfirmationHandler.Result.Failed.ErrorType.ExternalPaymentMethod,
            )
            is PaymentResult.Canceled -> ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.None,
            )
        }
    }
}
