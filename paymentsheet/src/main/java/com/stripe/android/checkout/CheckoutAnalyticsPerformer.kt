@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.analytics.checkoutAnalyticsParams
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.analytics.linkContext
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.isSaved
import com.stripe.android.paymentsheet.state.LinkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@Singleton
internal class CheckoutAnalyticsPerformer @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val savedStateHandle: SavedStateHandle,
    private val stateHolder: CheckoutControllerStateHolder,
    private val linkAccountHolder: LinkAccountHolder,
    context: Context,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val apiConfigurationProvider: Provider<ApiConfiguration.State>,
    private val timeSource: TimeSource,
    @IOContext private val workContext: CoroutineContext,
) {
    private val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
        context = context,
        defaultProductUsageTokens = setOf("checkout_elements"),
        apiConfigurationProvider = apiConfigurationProvider,
    )
    private var confirmationStartedAt: TimeMark? = null

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
                    reportConfirmationResult(state.result, integrationType, paymentSelection)
                }
                clearConfirmation()
            }
        }
    }

    private fun reportConfirmationResult(
        result: ConfirmationHandler.Result,
        integrationType: IntegrationType,
        selection: PaymentSelection,
    ) {
        val confirmationResponse = (result as? ConfirmationHandler.Result.Succeeded)
            ?.metadata?.get(CheckoutSessionResponseKey)
        val params = initialConfirmationParams(integrationType, selection) +
            (confirmationResponse ?: stateHolder.state?.checkoutSessionResponse)?.checkoutAnalyticsParams().orEmpty()
        val duration = confirmationStartedAt?.elapsedNow()

        when (result) {
            is ConfirmationHandler.Result.Succeeded -> reportEvent(
                event = ConfirmationEvent.Succeeded,
                params = params + terminalParams(confirmationResponse?.requestId, duration),
            )
            is ConfirmationHandler.Result.Failed -> {
                val stripeException = result.cause as? StripeException
                val errorParams = buildMap {
                    put("error_type", stripeException?.stripeError?.type ?: result.cause.javaClass.simpleName)
                    stripeException?.stripeError?.code?.let { put("error_code", it) }
                }
                reportEvent(
                    event = ConfirmationEvent.Failed,
                    params = params + errorParams + terminalParams(stripeException?.requestId, duration),
                )
            }
            is ConfirmationHandler.Result.Canceled -> reportEvent(
                event = ConfirmationEvent.Canceled,
                params = params + terminalParams(requestId = null, duration = duration),
            )
        }
    }

    private fun reportEvent(
        event: ConfirmationEvent,
        params: Map<String, Any>,
    ) {
        val publishableKey = stateHolder.state?.paymentMethodMetadata?.apiConfiguration?.publishableKey
            ?: apiConfigurationProvider.get().publishableKey
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = params,
                    publishableKeyOverride = publishableKey,
                )
            )
        }
    }

    private fun terminalParams(
        requestId: String?,
        duration: Duration?,
    ): Map<String, Any> = buildMap {
        requestId?.let { put("request_id", it) }
        duration?.let { put("duration", it.toDouble(DurationUnit.SECONDS).toFloat()) }
    }

    private fun saveConfirmation(
        integrationType: IntegrationType,
        paymentSelection: PaymentSelection,
    ) {
        savedStateHandle[INTEGRATION_TYPE_KEY] = integrationType
        savedStateHandle[PAYMENT_SELECTION_KEY] = paymentSelection
        val params = initialParams(integrationType, paymentSelection)
        savedStateHandle[PARAMS_KEY] = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        confirmationStartedAt = timeSource.markNow()
        reportEvent(event = ConfirmationEvent.Started, params = params)
    }

    private fun initialConfirmationParams(
        integrationType: IntegrationType,
        selection: PaymentSelection,
    ): Map<String, Any> {
        val restored = savedStateHandle.get<Bundle>(PARAMS_KEY) ?: return initialParams(integrationType, selection)
        return buildMap {
            restored.keySet().forEach { key ->
                @Suppress("DEPRECATION")
                restored.get(key)?.let { put(key, it) }
            }
        }
    }

    private fun initialParams(
        integrationType: IntegrationType,
        selection: PaymentSelection,
    ): Map<String, Any> = stateHolder.state?.checkoutSessionResponse?.checkoutAnalyticsParams().orEmpty() +
        confirmationParams(integrationType, selection)

    private fun confirmationParams(
        integrationType: IntegrationType,
        selection: PaymentSelection,
    ): Map<String, Any> {
        val metadata = when (integrationType) {
            IntegrationType.PaymentElement -> stateHolder.state?.paymentMethodMetadata
            IntegrationType.ExpressCheckoutElement -> stateHolder.state?.expressCheckoutElementPaymentMethodMetadata
        }
        return buildMap {
            put("selected_lpm", selection.code())
            put("is_saved_payment_method", selection.isSaved)
            selection.linkContext()?.let { put("link_context", it) }
            put(
                "active_link_session",
                linkAccountHolder.linkAccountInfo.value.account?.isVerified
                    ?: (metadata?.linkState?.loginState == LinkState.LoginState.LoggedIn)
            )
            put(
                "invocation_source",
                when (integrationType) {
                    IntegrationType.PaymentElement -> "merchant"
                    IntegrationType.ExpressCheckoutElement -> "express_checkout_element"
                },
            )
        }
    }

    private fun clearConfirmation() {
        confirmationStartedAt = null
        savedStateHandle.remove<IntegrationType>(INTEGRATION_TYPE_KEY)
        savedStateHandle.remove<PaymentSelection>(PAYMENT_SELECTION_KEY)
        savedStateHandle.remove<Bundle>(PARAMS_KEY)
    }

    private enum class ConfirmationEvent(override val eventName: String) : AnalyticsEvent {
        Started("checkout_elements.confirm.started"),
        Succeeded("checkout_elements.confirm.succeeded"),
        Failed("checkout_elements.confirm.failed"),
        Canceled("checkout_elements.confirm.canceled"),
    }

    private enum class IntegrationType {
        PaymentElement,
        ExpressCheckoutElement,
    }

    private companion object {
        const val INTEGRATION_TYPE_KEY = "CheckoutAnalyticsPerformer.integrationType"
        const val PAYMENT_SELECTION_KEY = "CheckoutAnalyticsPerformer.paymentSelection"
        const val PARAMS_KEY = "CheckoutAnalyticsPerformer.params"
    }
}
