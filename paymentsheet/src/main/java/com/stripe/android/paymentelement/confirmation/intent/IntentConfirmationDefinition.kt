package com.stripe.android.paymentelement.confirmation.intent

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayDisplayItemsFactory
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping
import com.stripe.android.paymentsheet.paymentdatacollection.updatedtax.UpdatedTaxAmountContract
import com.stripe.android.paymentsheet.paymentdatacollection.updatedtax.UpdatedTaxAmountLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.updatedtax.UpdatedTaxAmountResult
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.parcelize.Parcelize

internal class IntentConfirmationDefinition(
    private val intentConfirmationInterceptorFactory: IntentConfirmationInterceptor.Factory,
    private val paymentLauncherFactory:
        (ActivityResultLauncher<PaymentLauncherContract.Args>, Int?) -> PaymentLauncher,
    private val updatedTaxAmountLauncherFactory: UpdatedTaxAmountLauncherFactory,
) : ConfirmationDefinition<
    PaymentMethodConfirmationOption,
    IntentConfirmationLauncher,
    IntentConfirmationDefinition.Args,
    IntentConfirmationLauncherResult
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
                integrationMetadata = paymentMethodMetadata.integrationMetadata,
                customerMetadata = paymentMethodMetadata.customerMetadata,
                clientAttributionMetadata = paymentMethodMetadata.clientAttributionMetadata,
                checkoutSessionResponse = paymentMethodMetadata.checkoutSessionResponse,
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
        }
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
        onResult: (IntentConfirmationLauncherResult) -> Unit
    ): IntentConfirmationLauncher {
        val paymentLauncher = activityResultCaller.registerForActivityResult(
            PaymentLauncherContract(),
        ) { result ->
            onResult(IntentConfirmationLauncherResult.Payment(result))
        }
        val updatedTaxAmountActivityLauncher = activityResultCaller.registerForActivityResult(
            UpdatedTaxAmountContract(),
        ) { result ->
            onResult(IntentConfirmationLauncherResult.UpdatedTaxAmount(result))
        }
        return IntentConfirmationLauncher(
            paymentLauncher = paymentLauncher,
            updatedTaxAmountActivityLauncher = updatedTaxAmountActivityLauncher,
            updatedTaxAmountLauncher = updatedTaxAmountLauncherFactory.create(updatedTaxAmountActivityLauncher),
        )
    }

    override fun unregister(launcher: IntentConfirmationLauncher) {
        launcher.paymentLauncher.unregister()
        launcher.updatedTaxAmountActivityLauncher.unregister()
    }

    override fun launch(
        launcher: IntentConfirmationLauncher,
        arguments: Args,
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
    ) {
        when (arguments) {
            is Args.Confirm -> {
                val paymentLauncher = paymentLauncherFactory(launcher.paymentLauncher, confirmationArgs.statusBarColor)
                launchConfirm(paymentLauncher, arguments.confirmNextParams)
            }
            is Args.NextAction -> {
                val paymentLauncher = paymentLauncherFactory(launcher.paymentLauncher, confirmationArgs.statusBarColor)
                paymentLauncher.handleNextActionForStripeIntent(arguments.intent)
            }
            is Args.ConfirmUpdatedTax -> {
                launcher.updatedTaxAmountLauncher.launch(
                    UpdatedTaxAmountContract.Args(
                        displayItems = GooglePayDisplayItemsFactory.create(arguments.checkoutSessionResponse),
                        currency = arguments.checkoutSessionResponse.currency,
                        appearance = confirmationArgs.paymentMethodMetadata.appearance,
                    )
                )
            }
        }
    }

    override fun toResult(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
        launcherArgs: Args,
        result: IntentConfirmationLauncherResult
    ): ConfirmationDefinition.Result {
        return when (result) {
            is IntentConfirmationLauncherResult.Payment -> toPaymentResult(launcherArgs, result.result)
            is IntentConfirmationLauncherResult.UpdatedTaxAmount -> toUpdatedTaxAmountResult(
                confirmationOption = confirmationOption,
                confirmationArgs = confirmationArgs,
                launcherArgs = launcherArgs,
                result = result.result,
            )
        }
    }

    private fun toPaymentResult(
        launcherArgs: Args,
        result: InternalPaymentResult,
    ): ConfirmationDefinition.Result {
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

    private fun toUpdatedTaxAmountResult(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
        launcherArgs: Args,
        result: UpdatedTaxAmountResult,
    ): ConfirmationDefinition.Result {
        val updatedCheckoutSessionResponse = (launcherArgs as? Args.ConfirmUpdatedTax)?.checkoutSessionResponse
            ?: run {
                val error = IllegalStateException("Missing updated checkout session response.")
                return ConfirmationDefinition.Result.Failed(
                    cause = error,
                    message = error.stripeErrorMessage(),
                    type = ConfirmationHandler.Result.Failed.ErrorType.Internal,
                )
            }
        return when (result) {
            UpdatedTaxAmountResult.Confirmed -> ConfirmationDefinition.Result.NextStep(
                confirmationOption = confirmationOption,
                arguments = confirmationArgs.copy(
                    paymentMethodMetadata = confirmationArgs.paymentMethodMetadata.copy(
                        checkoutSessionResponse = updatedCheckoutSessionResponse,
                    ),
                ),
            )
            UpdatedTaxAmountResult.Cancelled -> ConfirmationDefinition.Result.Canceled(
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

        @Parcelize
        data class ConfirmUpdatedTax(
            val checkoutSessionResponse: CheckoutSessionResponse,
        ) : Args {
            override val deferredIntentConfirmationType: DeferredIntentConfirmationType? = null
        }
    }
}
