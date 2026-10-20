package com.stripe.android.checkout

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.analytics.CheckoutConfirmationContext
import com.stripe.android.checkout.analytics.CheckoutConfirmationEvent
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.defaultAnalyticsParams
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationTypeKey
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.utils.toConfirmationError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
internal class CheckoutAnalyticsPerformer @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val savedStateHandle: SavedStateHandle,
    context: Context,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val durationProvider: DurationProvider,
    private val paymentMethodMetadataProvider: Provider<PaymentMethodMetadata?>,
    private val apiConfigurationProvider: Provider<ApiConfiguration.State>,
    @IOContext private val workContext: CoroutineContext,
) {
    private val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
        context = context,
        defaultProductUsageTokens = setOf("checkout_elements"),
        apiConfigurationProvider = apiConfigurationProvider,
    )

    fun onPaymentElementConfirmationStarted(paymentSelection: PaymentSelection) {
        savedStateHandle[CONFIRMATION_CONTEXT_KEY] = CheckoutConfirmationContext(
            paymentSelection = paymentSelection,
        )
    }

    fun onExpressCheckoutElementConfirmationStarted(paymentSelection: PaymentSelection) {
        savedStateHandle[CONFIRMATION_CONTEXT_KEY] = CheckoutConfirmationContext(
            paymentSelection = paymentSelection,
        )
    }

    suspend fun reportConfirmationResults() {
        confirmationHandler.state.collect { state ->
            if (state is ConfirmationHandler.State.Complete) {
                val confirmationContext = savedStateHandle.get<CheckoutConfirmationContext>(CONFIRMATION_CONTEXT_KEY)

                if (confirmationContext != null) {
                    reportConfirmationResult(state.result, confirmationContext)
                }

                savedStateHandle.remove<CheckoutConfirmationContext>(CONFIRMATION_CONTEXT_KEY)
            }
        }
    }

    private fun reportConfirmationResult(
        result: ConfirmationHandler.Result,
        confirmationContext: CheckoutConfirmationContext,
    ) {
        when (result) {
            is ConfirmationHandler.Result.Succeeded -> reportEvent(
                CheckoutConfirmationEvent.Succeeded(
                    confirmationContext = confirmationContext,
                    duration = durationProvider.end(DurationProvider.Key.Checkout),
                    deferredIntentConfirmationType = result.metadata[DeferredIntentConfirmationTypeKey],
                    intentId = result.intent.id,
                )
            )
            is ConfirmationHandler.Result.Failed -> result.toConfirmationError()?.let { error ->
                reportEvent(
                    CheckoutConfirmationEvent.Failed(
                        confirmationContext = confirmationContext,
                        duration = durationProvider.end(DurationProvider.Key.Checkout),
                        error = error,
                    )
                )
            }
            is ConfirmationHandler.Result.Canceled -> Unit
        }
    }

    private fun reportEvent(event: CheckoutConfirmationEvent) {
        val metadata = paymentMethodMetadataProvider.get()
        val publishableKey = metadata?.apiConfiguration?.publishableKey
            ?: apiConfigurationProvider.get().publishableKey
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = metadata.defaultAnalyticsParams() + event.params,
                    publishableKeyOverride = publishableKey,
                )
            )
        }
    }

    private companion object {
        const val CONFIRMATION_CONTEXT_KEY = "CheckoutAnalyticsPerformer.confirmationContext"
    }
}
