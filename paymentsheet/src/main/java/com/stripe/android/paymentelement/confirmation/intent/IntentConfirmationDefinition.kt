package com.stripe.android.paymentelement.confirmation.intent

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping

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

    @Volatile
    private var customerId: String? = null

    @Volatile
    private var ephemeralKeySecret: String? = null

    @Volatile
    private var clientAttributionMetadata: ClientAttributionMetadata? = null

    override fun option(confirmationOption: ConfirmationHandler.Option): PaymentMethodConfirmationOption? {
        return confirmationOption as? PaymentMethodConfirmationOption
    }

    override fun bootstrap(paymentMethodMetadata: PaymentMethodMetadata) {
        customerId = paymentMethodMetadata.customerMetadata?.id
        ephemeralKeySecret = paymentMethodMetadata.customerMetadata?.ephemeralKeySecret
        clientAttributionMetadata = paymentMethodMetadata.clientAttributionMetadata
    }

    override suspend fun action(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
    ): ConfirmationDefinition.Action<Args> {
        val interceptor: IntentConfirmationInterceptor
        try {
            interceptor = intentConfirmationInterceptorFactory.create(
                initializationMode = confirmationArgs.initializationMode,
                customerId = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
                clientAttributionMetadata = clientAttributionMetadata,
            )
        } catch (e: DeferredIntentCallbackNotFoundException) {
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
                    shippingValues = confirmationArgs.shippingDetails?.toConfirmPaymentIntentShipping(),
                )
            is PaymentMethodConfirmationOption.Saved ->
                interceptor.intercept(
                    intent = confirmationArgs.intent,
                    confirmationOption = confirmationOption,
                    shippingValues = confirmationArgs.shippingDetails?.toConfirmPaymentIntentShipping(),
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
            is Args.NextAction -> launchNextAction(launcher, arguments.clientSecret, confirmationArgs.intent)
        }
    }

    override fun toResult(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        isConfirmationToken: Boolean,
        result: InternalPaymentResult
    ): ConfirmationDefinition.Result {
        return when (result) {
            is InternalPaymentResult.Completed -> ConfirmationDefinition.Result.Succeeded(
                intent = result.intent,
                deferredIntentConfirmationType = deferredIntentConfirmationType,
                isConfirmationToken = isConfirmationToken,
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

    private fun launchNextAction(
        launcher: PaymentLauncher,
        clientSecret: String,
        intent: StripeIntent,
    ) {
        when (intent) {
            is PaymentIntent -> {
                launcher.handleNextActionForPaymentIntent(clientSecret)
            }
            is SetupIntent -> {
                launcher.handleNextActionForSetupIntent(clientSecret)
            }
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

    sealed interface Args {
        data class NextAction(val clientSecret: String) : Args

        data class Confirm(val confirmNextParams: ConfirmStripeIntentParams) : Args
    }

    internal companion object {
        @Volatile
        private var instance: IntentConfirmationDefinition? = null

        fun getInstance(
            interceptorFactory: IntentConfirmationInterceptor.Factory,
            paymentLauncherFactory: (ActivityResultLauncher<PaymentLauncherContract. Args>) -> PaymentLauncher,
        ): IntentConfirmationDefinition {
            return instance ?: synchronized(this) {
                IntentConfirmationDefinition(
                    intentConfirmationInterceptorFactory = interceptorFactory,
                    paymentLauncherFactory = paymentLauncherFactory,
                ).also {
                    instance = it
                }
            }
        }
    }
}
