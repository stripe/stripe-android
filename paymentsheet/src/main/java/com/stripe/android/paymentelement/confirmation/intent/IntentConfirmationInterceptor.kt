package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.exception.StripeException
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import javax.inject.Inject

internal interface IntentConfirmationInterceptor {

    suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<Args>

    suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<Args>

    interface Factory {
        suspend fun create(initializationMode: PaymentElementLoader.InitializationMode): IntentConfirmationInterceptor
    }

    companion object {
        const val COMPLETE_WITHOUT_CONFIRMING_INTENT = "COMPLETE_WITHOUT_CONFIRMING_INTENT"
    }
}

@OptIn(SharedPaymentTokenSessionPreview::class)
internal class DefaultIntentConfirmationInterceptorFactory @Inject constructor(
    private val deferredIntentCallbackRetriever: DeferredIntentCallbackRetriever,
    private val intentFirstConfirmationInterceptorFactory: IntentFirstConfirmationInterceptor.Factory,
    private val deferredIntentConfirmationInterceptorFactory: DeferredIntentConfirmationInterceptor.Factory,
    private val sharedPaymentTokenConfirmationInterceptorFactory: SharedPaymentTokenConfirmationInterceptor.Factory,
) : IntentConfirmationInterceptor.Factory {
    override suspend fun create(
        initializationMode: PaymentElementLoader.InitializationMode
    ): IntentConfirmationInterceptor {
        return when (initializationMode) {
            is PaymentElementLoader.InitializationMode.DeferredIntent -> {
                when (
                    val deferredIntentCallback = deferredIntentCallbackRetriever.waitForDeferredIntentCallback(
                        initializationMode.intentConfiguration.intentBehavior
                    )
                ) {
                    is DeferredIntentCallback.ConfirmationToken -> TODO()
                    is DeferredIntentCallback.PaymentMethod -> {
                        deferredIntentConfirmationInterceptorFactory.create(
                            initializationMode.intentConfiguration,
                            deferredIntentCallback.callback,
                        )
                    }
                    is DeferredIntentCallback.SharedPaymentToken -> {
                        sharedPaymentTokenConfirmationInterceptorFactory.create(
                            initializationMode.intentConfiguration,
                            deferredIntentCallback.handler,
                        )
                    }
                }
            }
            is PaymentElementLoader.InitializationMode.PaymentIntent,
            is PaymentElementLoader.InitializationMode.SetupIntent -> {
                intentFirstConfirmationInterceptorFactory.create(initializationMode.clientSecret)
            }
        }
    }
}

internal enum class DeferredIntentConfirmationType(val value: String) {
    Client("client"),
    Server("server"),
    None("none")
}

internal class InvalidDeferredIntentUsageException : StripeException() {
    override fun analyticsValue(): String = "invalidDeferredIntentUsage"

    override val message: String = """
        The payment method on the intent doesn't match the one provided in the createIntentCallback. When using deferred
        intent creation, ensure you're either creating a new intent with the correct payment method or updating an
        existing intent with the new payment method ID.
    """.trimIndent()
}

internal class CreateIntentCallbackFailureException(override val cause: Throwable?) : StripeException() {
    override fun analyticsValue(): String = "merchantReturnedCreateIntentCallbackFailure"
}

internal class InvalidClientSecretException(
    val clientSecret: String,
    val intent: StripeIntent,
) : StripeException() {
    private val intentType = when (intent) {
        is PaymentIntent -> "PaymentIntent"
        is SetupIntent -> "SetupIntent"
    }

    override fun analyticsValue(): String = "invalidClientSecretProvided"

    override val message: String = """
        Encountered an invalid client secret "$clientSecret" for intent type "$intentType"
    """.trimIndent()
}
