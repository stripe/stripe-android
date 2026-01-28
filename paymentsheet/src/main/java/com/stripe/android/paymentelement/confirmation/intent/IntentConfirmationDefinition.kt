package com.stripe.android.paymentelement.confirmation.intent

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping
import kotlinx.parcelize.Parcelize

internal class IntentConfirmationDefinition(
    private val intentConfirmationInterceptorFactory: IntentConfirmationInterceptor.Factory,
    private val paymentLauncherFactory: (ActivityResultLauncher<PaymentLauncherContract.Args>) -> PaymentLauncher,
) : ConfirmationDefinition<
    PaymentMethodConfirmationOption,
    PaymentLauncher,
    IntentConfirmationDefinition.Args,
    InternalPaymentResult
    > {
    override val key: String = "IntentConfirmation"

    override fun option(confirmationOption: ConfirmationHandler.Option): PaymentMethodConfirmationOption? {
        return confirmationOption as? PaymentMethodConfirmationOption
    }

    override suspend fun action(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
    ): ConfirmationDefinition.Action<Args> {
        val paymentMethodMetadata = confirmationArgs.paymentMethodMetadata
        val interceptor: IntentConfirmationInterceptor
        try {
            interceptor = intentConfirmationInterceptorFactory.create(
                integrationMetadata = confirmationArgs.paymentMethodMetadata.integrationMetadata,
                customerId = paymentMethodMetadata.customerMetadata?.id,
                ephemeralKeySecret = paymentMethodMetadata.customerMetadata?.ephemeralKeySecret,
                clientAttributionMetadata = paymentMethodMetadata.clientAttributionMetadata,
            )
        } catch (e: CallbackNotFoundException) {
            return ConfirmationDefinition.Action.Fail(
                cause = IllegalStateException(e.message),
                message = e.resolvableError,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        }
        return when (confirmationOption) {
            is PaymentMethodConfirmationOption.New ->
                interceptor.intercept(
                    intent = confirmationArgs.intent,
                    confirmationOption = confirmationOption,
                    shippingValues = confirmationArgs.paymentMethodMetadata
                        .shippingDetails?.toConfirmPaymentIntentShipping(),
                )
            is PaymentMethodConfirmationOption.Saved ->
                interceptor.intercept(
                    intent = confirmationArgs.intent,
                    confirmationOption = confirmationOption,
                    shippingValues = confirmationArgs.paymentMethodMetadata
                        .shippingDetails?.toConfirmPaymentIntentShipping(),
                )
        }
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (InternalPaymentResult) -> Unit
    ): PaymentLauncher {
        return paymentLauncherFactory(
            activityResultCaller.registerForActivityResult(
                PaymentLauncherContract(),
                onResult
            )
        )
    }

    override fun launch(
        launcher: PaymentLauncher,
        arguments: Args,
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
    ) {
        when (arguments) {
            is Args.Confirm -> launchConfirm(launcher, arguments.confirmNextParams)
            is Args.NextAction -> launcher.handleNextActionForStripeIntent(arguments.intent)
        }
    }

    override fun toResult(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
        launcherArgs: Args,
        result: InternalPaymentResult
    ): ConfirmationDefinition.Result {
        return when (result) {
            is InternalPaymentResult.Completed -> ConfirmationDefinition.Result.Succeeded(
                intent = result.intent,
                deferredIntentConfirmationType = launcherArgs.deferredIntentConfirmationType,
            )
            is InternalPaymentResult.Failed -> ConfirmationDefinition.Result.Failed(
                cause = result.throwable,
                message = result.throwable.stripeErrorMessage(),
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
            is InternalPaymentResult.Canceled -> ConfirmationDefinition.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
            )
        }
    }

    private fun launchConfirm(
        launcher: PaymentLauncher,
        confirmStripeIntentParams: ConfirmStripeIntentParams
    ) {
        when (confirmStripeIntentParams) {
            is ConfirmPaymentIntentParams -> {
                launcher.confirm(confirmStripeIntentParams)
            }
            is ConfirmSetupIntentParams -> {
                launcher.confirm(confirmStripeIntentParams)
            }
        }
    }

    sealed interface Args : Parcelable {
        val deferredIntentConfirmationType: DeferredIntentConfirmationType?

        @Parcelize
        data class NextAction(
            val intent: StripeIntent,
            override val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        ) : Args

        @Parcelize
        data class Confirm(
            val confirmNextParams: ConfirmStripeIntentParams,
            override val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        ) : Args
    }
}
