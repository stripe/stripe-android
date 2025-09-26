package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

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
        fun create(initializationMode: PaymentElementLoader.InitializationMode): IntentConfirmationInterceptor
    }

    companion object {
        var createIntentCallback: CreateIntentCallback? = null

        const val COMPLETE_WITHOUT_CONFIRMING_INTENT = "COMPLETE_WITHOUT_CONFIRMING_INTENT"
        const val PROVIDER_FETCH_TIMEOUT = 2
        const val PROVIDER_FETCH_INTERVAL = 5L
    }
}

@OptIn(SharedPaymentTokenSessionPreview::class)
internal class DefaultIntentConfirmationInterceptorFactory @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val errorReporter: ErrorReporter,
    private val intentCreationCallbackProvider: Provider<CreateIntentCallback?>,
    private val preparePaymentMethodHandlerProvider: Provider<PreparePaymentMethodHandler?>,
    @Named(ALLOWS_MANUAL_CONFIRMATION) private val allowsManualConfirmation: Boolean,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
) : IntentConfirmationInterceptor.Factory {
    override fun create(initializationMode: PaymentElementLoader.InitializationMode): IntentConfirmationInterceptor {
        return when (initializationMode) {
            is PaymentElementLoader.InitializationMode.DeferredIntent -> {
                /*
                 * We don't pass the created intent in `DeferredIntent` mode because we rely on the created intent
                 * from the merchant through `CreateIntentCallback`. The intent passed through here is created from
                 * `PaymentSheet.Configuration` in order to populate `Payment Element` data.
                 */
                if (initializationMode.intentConfiguration.intentBehavior is
                        PaymentSheet.IntentConfiguration.IntentBehavior.SharedPaymentToken
                ) {
                    SharedPaymentTokenConfirmationInterceptor(
                        initializationMode = initializationMode,
                        stripeRepository = stripeRepository,
                        errorReporter = errorReporter,
                        preparePaymentMethodHandlerProvider = preparePaymentMethodHandlerProvider,
                        publishableKeyProvider = publishableKeyProvider,
                        stripeAccountIdProvider = stripeAccountIdProvider,
                    )
                } else {
                    DeferredIntentConfirmationInterceptor(
                        intentConfiguration = initializationMode.intentConfiguration,
                        stripeRepository = stripeRepository,
                        errorReporter = errorReporter,
                        intentCreationCallbackProvider = intentCreationCallbackProvider,
                        allowsManualConfirmation = allowsManualConfirmation,
                        publishableKeyProvider = publishableKeyProvider,
                        stripeAccountIdProvider = stripeAccountIdProvider,
                    )
                }
            }
            is PaymentElementLoader.InitializationMode.PaymentIntent,
            is PaymentElementLoader.InitializationMode.SetupIntent -> {
                IntentFirstConfirmationInterceptor(
                    clientSecret = initializationMode.clientSecret,
                    publishableKeyProvider = publishableKeyProvider,
                    stripeAccountIdProvider = stripeAccountIdProvider,
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
