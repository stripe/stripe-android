package com.stripe.android.paymentelement.confirmation

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.ExternalPaymentMethodContract
import com.stripe.android.paymentsheet.ExternalPaymentMethodInput
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor
import java.lang.IllegalStateException

internal class ExternalPaymentMethodConfirmationDefinition(
    private val errorReporter: ErrorReporter,
) : ConfirmationDefinition<
    ConfirmationHandler.Option.ExternalPaymentMethod,
    ActivityResultLauncher<ExternalPaymentMethodInput>,
    Unit,
    PaymentResult
    > {
    override val key: String = "IntentConfirmation"

    override val confirmationFlow: ConfirmationDefinition.ConfirmationFlow =
        ConfirmationDefinition.ConfirmationFlow.Confirm

    override fun option(
        confirmationOption: ConfirmationHandler.Option
    ): ConfirmationHandler.Option.ExternalPaymentMethod? {
        return confirmationOption as? ConfirmationHandler.Option.ExternalPaymentMethod
    }

    override fun canConfirm(
        confirmationOption: ConfirmationHandler.Option.ExternalPaymentMethod,
        intent: StripeIntent
    ): Boolean = true

    override suspend fun action(
        confirmationOption: ConfirmationHandler.Option.ExternalPaymentMethod,
        intent: StripeIntent
    ): ConfirmationDefinition.ConfirmationAction<Unit> {
        val externalPaymentMethodType = confirmationOption.type
        val externalPaymentMethodConfirmHandler = ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler

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
        confirmationOption: ConfirmationHandler.Option.ExternalPaymentMethod,
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

    override fun toResult(
        confirmationOption: ConfirmationHandler.Option.ExternalPaymentMethod,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intent: StripeIntent,
        result: PaymentResult,
    ): ConfirmationDefinition.Result {
        return when (result) {
            is PaymentResult.Completed -> ConfirmationDefinition.Result.Succeeded(
                intent = intent,
                confirmationOption = confirmationOption,
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
