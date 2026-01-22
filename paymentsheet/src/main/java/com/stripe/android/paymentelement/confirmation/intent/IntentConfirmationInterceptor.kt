package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.exception.StripeException
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
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
        suspend fun create(
            integrationMetadata: IntegrationMetadata,
            customerId: String?,
            ephemeralKeySecret: String?,
            clientAttributionMetadata: ClientAttributionMetadata,
        ): IntentConfirmationInterceptor
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
    private val confirmationTokenConfirmationInterceptorFactory: ConfirmationTokenConfirmationInterceptor.Factory,
    private val sharedPaymentTokenConfirmationInterceptorFactory: SharedPaymentTokenConfirmationInterceptor.Factory,
    private val checkoutSessionConfirmationInterceptorFactory: CheckoutSessionConfirmationInterceptor.Factory,
) : IntentConfirmationInterceptor.Factory {
    override suspend fun create(
        integrationMetadata: IntegrationMetadata,
        customerId: String?,
        ephemeralKeySecret: String?,
        clientAttributionMetadata: ClientAttributionMetadata,
    ): IntentConfirmationInterceptor {
        return when (integrationMetadata) {
            is IntegrationMetadata.CustomerSheet -> {
                // CustomerSheet doesn't call confirm with IntegrationMetadata.CustomerSheet.
                // CustomerSheet calls confirm with an IntegrationMetadata.IntentFirst setup intent.
                throw IllegalStateException("CustomerSheet not supported by default confirmation interceptor!")
            }
            IntegrationMetadata.CryptoOnramp -> {
                // CryptoOnRamp doesn't call confirm.
                throw IllegalStateException("No intent confirmation interceptor for CryptoOnramp.")
            }
            is IntegrationMetadata.DeferredIntent.WithConfirmationToken -> {
                confirmationTokenConfirmationInterceptorFactory.create(
                    intentConfiguration = integrationMetadata.intentConfiguration,
                    createIntentCallback = deferredIntentCallbackRetriever.waitForConfirmationTokenCallback(),
                    customerId = customerId,
                    ephemeralKeySecret = ephemeralKeySecret,
                    clientAttributionMetadata = clientAttributionMetadata,
                )
            }
            is IntegrationMetadata.DeferredIntent.WithPaymentMethod -> {
                deferredIntentConfirmationInterceptorFactory.create(
                    intentConfiguration = integrationMetadata.intentConfiguration,
                    createIntentCallback = deferredIntentCallbackRetriever.waitForPaymentMethodCallback(),
                    clientAttributionMetadata = clientAttributionMetadata,
                )
            }
            is IntegrationMetadata.DeferredIntent.WithSharedPaymentToken -> {
                sharedPaymentTokenConfirmationInterceptorFactory.create(
                    intentConfiguration = integrationMetadata.intentConfiguration,
                    handler = deferredIntentCallbackRetriever.waitForSharedPaymentTokenCallback(),
                )
            }
            is IntegrationMetadata.IntentFirst -> {
                intentFirstConfirmationInterceptorFactory.create(
                    clientSecret = integrationMetadata.clientSecret,
                    clientAttributionMetadata = clientAttributionMetadata,
                )
            }
            is IntegrationMetadata.CheckoutSession -> {
                checkoutSessionConfirmationInterceptorFactory.create(
                    checkoutSessionId = integrationMetadata.id,
                    clientAttributionMetadata = clientAttributionMetadata,
                )
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
