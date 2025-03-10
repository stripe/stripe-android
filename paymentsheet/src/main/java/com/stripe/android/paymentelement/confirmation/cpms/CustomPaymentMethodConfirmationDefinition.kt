package com.stripe.android.paymentelement.confirmation.cpms

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.paymentelement.CustomPaymentMethodConfirmHandler
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentResult
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Provider

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
internal class CustomPaymentMethodConfirmationDefinition @Inject constructor(
    private val customPaymentMethodConfirmHandlerProvider: Provider<CustomPaymentMethodConfirmHandler?>,
    private val errorReporter: ErrorReporter,
) : ConfirmationDefinition<
    CustomPaymentMethodConfirmationOption,
    ActivityResultLauncher<CustomPaymentMethodInput>,
    Unit,
    PaymentResult
    > {
    override val key: String = "CustomPaymentMethod"

    override fun option(
        confirmationOption: ConfirmationHandler.Option
    ): CustomPaymentMethodConfirmationOption? {
        return confirmationOption as? CustomPaymentMethodConfirmationOption
    }

    override suspend fun action(
        confirmationOption: CustomPaymentMethodConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
    ): ConfirmationDefinition.Action<Unit> {
        val customPaymentMethodId = confirmationOption.customPaymentMethodType.id
        val customPaymentMethodConfirmHandler = customPaymentMethodConfirmHandlerProvider.get()

        return if (customPaymentMethodConfirmHandler == null) {
            errorReporter.report(
                ErrorReporter.ExpectedErrorEvent.CUSTOM_PAYMENT_METHOD_CONFIRM_HANDLER_NULL,
                additionalNonPiiParams = mapOf("custom_payment_method_type" to customPaymentMethodId)
            )

            val error = IllegalStateException(
                "customPaymentMethodConfirmHandler is null." +
                    " Cannot process payment for payment selection: $customPaymentMethodId"
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
    ): ActivityResultLauncher<CustomPaymentMethodInput> {
        return activityResultCaller.registerForActivityResult(
            CustomPaymentMethodContract(errorReporter),
            onResult
        )
    }

    override fun launch(
        launcher: ActivityResultLauncher<CustomPaymentMethodInput>,
        arguments: Unit,
        confirmationOption: CustomPaymentMethodConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
    ) {
        errorReporter.report(
            ErrorReporter.SuccessEvent.CUSTOM_PAYMENT_METHODS_LAUNCH_SUCCESS,
            additionalNonPiiParams = mapOf(
                "custom_payment_method_type" to confirmationOption.customPaymentMethodType.id
            )
        )

        launcher.launch(
            CustomPaymentMethodInput(
                type = confirmationOption.customPaymentMethodType,
                billingDetails = confirmationOption.billingDetails,
            )
        )
    }

    override fun toResult(
        confirmationOption: CustomPaymentMethodConfirmationOption,
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
