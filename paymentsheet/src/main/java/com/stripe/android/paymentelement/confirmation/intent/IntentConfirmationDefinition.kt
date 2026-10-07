package com.stripe.android.paymentelement.confirmation.intent

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.parcelize.Parcelize

internal class IntentConfirmationDefinition(
    private val intentConfirmationInterceptorFactory: IntentConfirmationInterceptor.Factory,
    private val paymentLauncherFactory:
        (ActivityResultLauncher<PaymentLauncherContract.Args>, Int?, ApiConfiguration.State) -> PaymentLauncher,
    private val checkoutSessionFinalizer: CheckoutSessionConfirmationFinalizer,
) : ConfirmationDefinition<
    ConfirmationHandler.Option,
    ActivityResultLauncher<PaymentLauncherContract.Args>,
    IntentConfirmationDefinition.Args,
    InternalPaymentResult
    > {
    override val key: String = "IntentConfirmation"

    override fun option(confirmationOption: ConfirmationHandler.Option): ConfirmationHandler.Option? {
        return confirmationOption.takeIf {
            it is PaymentMethodConfirmationOption || it is CheckoutSessionConfirmationOption
        }
    }

    override fun canConfirm(
        confirmationOption: ConfirmationHandler.Option,
        confirmationArgs: ConfirmationHandler.Args,
    ): Boolean = confirmationOption !is CheckoutSessionConfirmationOption ||
        confirmationArgs.paymentMethodMetadata.integrationMetadata is IntegrationMetadata.CheckoutSession

    override suspend fun action(
        confirmationOption: ConfirmationHandler.Option,
        confirmationArgs: ConfirmationHandler.Args,
    ): ConfirmationDefinition.Action<Args> {
        val paymentMethodMetadata = confirmationArgs.paymentMethodMetadata
        if (confirmationOption is CheckoutSessionConfirmationOption.Finalize) {
            return checkoutSessionFinalizer.finalize(confirmationOption.response, confirmationOption.intent)
        }
        val interceptor: IntentConfirmationInterceptor
        try {
            interceptor = intentConfirmationInterceptorFactory.create(
                integrationMetadata = paymentMethodMetadata.integrationMetadata,
                customerMetadata = paymentMethodMetadata.customerMetadata,
                clientAttributionMetadata = paymentMethodMetadata.clientAttributionMetadata,
                isLiveMode = paymentMethodMetadata.apiConfiguration.isLiveMode(),
            )
        } catch (e: CallbackNotFoundException) {
            return ConfirmationDefinition.Action.Fail(
                cause = IllegalStateException(e.message),
                message = e.resolvableError,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        }
        val shippingValues = paymentMethodMetadata.shippingDetails?.toConfirmPaymentIntentShipping()
        return when (confirmationOption) {
            is PaymentMethodConfirmationOption.New ->
                interceptor.intercept(
                    intent = confirmationArgs.intent,
                    confirmationOption = confirmationOption,
                    shippingValues = shippingValues,
                )
            is PaymentMethodConfirmationOption.Saved ->
                interceptor.intercept(
                    intent = confirmationArgs.intent,
                    confirmationOption = confirmationOption,
                    shippingValues = shippingValues,
                )
            is CheckoutSessionConfirmationOption.WithoutPaymentMethod -> interceptor.intercept(
                confirmationOption = confirmationOption,
                shippingValues = shippingValues,
            )
            else -> error("Unsupported Intent confirmation option: $confirmationOption")
        }
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
        onResult: (InternalPaymentResult) -> Unit
    ): ActivityResultLauncher<PaymentLauncherContract.Args> {
        return activityResultCaller.registerForActivityResult(
            PaymentLauncherContract(),
            onResult
        )
    }

    override fun unregister(launcher: ActivityResultLauncher<PaymentLauncherContract.Args>) {
        launcher.unregister()
    }

    override fun launch(
        launcher: ActivityResultLauncher<PaymentLauncherContract.Args>,
        arguments: Args,
        confirmationOption: ConfirmationHandler.Option,
        confirmationArgs: ConfirmationHandler.Args,
    ) {
        val paymentLauncher = paymentLauncherFactory(
            launcher,
            confirmationArgs.statusBarColor,
            confirmationArgs.paymentMethodMetadata.apiConfiguration,
        )
        when (arguments) {
            is Args.Confirm -> launchConfirm(paymentLauncher, arguments.confirmNextParams)
            is Args.NextAction -> paymentLauncher.handleNextActionForStripeIntent(arguments.intent)
            is Args.CheckoutNextAction -> paymentLauncher.handleNextActionForStripeIntent(arguments.intent)
        }
    }

    override fun toResult(
        confirmationOption: ConfirmationHandler.Option,
        confirmationArgs: ConfirmationHandler.Args,
        launcherArgs: Args,
        result: InternalPaymentResult
    ): ConfirmationDefinition.Result {
        if (launcherArgs is Args.CheckoutNextAction) {
            val metadata = CheckoutSessionConfirmationFinalizer.metadata(launcherArgs.response)
            return when (result) {
                is InternalPaymentResult.Completed -> ConfirmationDefinition.Result.NextStep(
                    confirmationOption = CheckoutSessionConfirmationOption.Finalize(
                        launcherArgs.response,
                        result.intent,
                    ),
                    arguments = confirmationArgs,
                )
                is InternalPaymentResult.Failed -> ConfirmationDefinition.Result.Failed(
                    cause = result.throwable,
                    message = result.throwable.stripeErrorMessage(),
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                    metadata = metadata,
                )
                is InternalPaymentResult.Canceled -> ConfirmationDefinition.Result.Canceled(
                    action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
                    metadata = metadata,
                )
            }
        }
        return when (result) {
            is InternalPaymentResult.Completed -> ConfirmationDefinition.Result.Succeeded(
                intent = result.intent,
                metadata = MutableConfirmationMetadata().apply {
                    launcherArgs.deferredIntentConfirmationType?.let {
                        set(DeferredIntentConfirmationTypeKey, it)
                    }
                }
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
        data class CheckoutNextAction(
            val intent: StripeIntent,
            val response: CheckoutSessionResponse,
        ) : Args {
            override val deferredIntentConfirmationType: DeferredIntentConfirmationType
                get() = DeferredIntentConfirmationType.Server
        }

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
