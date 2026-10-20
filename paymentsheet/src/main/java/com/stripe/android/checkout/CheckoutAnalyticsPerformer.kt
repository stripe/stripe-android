package com.stripe.android.checkout

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.defaultAnalyticsParams
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationTypeKey
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
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
        saveConfirmation(IntegrationType.PaymentElement, paymentSelection)
    }

    fun onExpressCheckoutElementConfirmationStarted(paymentSelection: PaymentSelection) {
        saveConfirmation(IntegrationType.ExpressCheckoutElement, paymentSelection)
    }

    suspend fun reportConfirmationResults() {
        confirmationHandler.state.collect { state ->
            if (state is ConfirmationHandler.State.Complete) {
                val integrationType = savedStateHandle.get<IntegrationType>(INTEGRATION_TYPE_KEY)
                val paymentSelection = savedStateHandle.get<PaymentSelection>(PAYMENT_SELECTION_KEY)

                if (integrationType != null && paymentSelection != null) {
                    reportConfirmationResult(state.result, paymentSelection)
                }

                clearConfirmation()
            }
        }
    }

    private fun reportConfirmationResult(
        result: ConfirmationHandler.Result,
        paymentSelection: PaymentSelection,
    ) {
        when (result) {
            is ConfirmationHandler.Result.Succeeded -> reportEvent(
                event = ConfirmationEvent.Succeeded,
                paymentEvent = PaymentSheetEvent.Payment(
                    mode = EventReporter.Mode.Embedded,
                    result = PaymentSheetEvent.Payment.Result.Success,
                    duration = durationProvider.end(DurationProvider.Key.Checkout),
                    paymentSelection = paymentSelection,
                    deferredIntentConfirmationType = result.metadata[DeferredIntentConfirmationTypeKey],
                    intentId = result.intent.id,
                ),
            )
            is ConfirmationHandler.Result.Failed -> result.toConfirmationError()?.let { error ->
                reportEvent(
                    event = ConfirmationEvent.Failed,
                    paymentEvent = PaymentSheetEvent.Payment(
                        mode = EventReporter.Mode.Embedded,
                        result = PaymentSheetEvent.Payment.Result.Failure(error),
                        duration = durationProvider.end(DurationProvider.Key.Checkout),
                        paymentSelection = paymentSelection,
                        deferredIntentConfirmationType = null,
                        intentId = null,
                    ),
                )
            }
            is ConfirmationHandler.Result.Canceled -> Unit
        }
    }

    private fun reportEvent(event: ConfirmationEvent, paymentEvent: PaymentSheetEvent.Payment) {
        val metadata = paymentMethodMetadataProvider.get()
        val publishableKey = metadata?.apiConfiguration?.publishableKey
            ?: apiConfigurationProvider.get().publishableKey
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = metadata.defaultAnalyticsParams() + paymentEvent.params,
                    publishableKeyOverride = publishableKey,
                )
            )
        }
    }

    private fun saveConfirmation(
        integrationType: IntegrationType,
        paymentSelection: PaymentSelection,
    ) {
        savedStateHandle[INTEGRATION_TYPE_KEY] = integrationType
        savedStateHandle[PAYMENT_SELECTION_KEY] = paymentSelection
    }

    private fun clearConfirmation() {
        savedStateHandle.remove<IntegrationType>(INTEGRATION_TYPE_KEY)
        savedStateHandle.remove<PaymentSelection>(PAYMENT_SELECTION_KEY)
    }

    private enum class ConfirmationEvent(override val eventName: String) : AnalyticsEvent {
        Succeeded("checkout_elements.confirm.succeeded"),
        Failed("checkout_elements.confirm.failed"),
    }

    private enum class IntegrationType {
        PaymentElement,
        ExpressCheckoutElement,
    }

    private companion object {
        const val INTEGRATION_TYPE_KEY = "CheckoutAnalyticsPerformer.integrationType"
        const val PAYMENT_SELECTION_KEY = "CheckoutAnalyticsPerformer.paymentSelection"
    }
}
