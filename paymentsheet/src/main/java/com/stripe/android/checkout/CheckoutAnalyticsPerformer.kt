@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.analytics.CheckoutEventReporter
import com.stripe.android.checkout.analytics.checkoutAnalyticsParams
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.analytics.linkContext
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.isSaved
import com.stripe.android.paymentsheet.state.LinkState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.TimeMark

@Singleton
internal class CheckoutAnalyticsPerformer @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val savedStateHandle: SavedStateHandle,
    private val stateHolder: CheckoutControllerStateHolder,
    private val linkAccountHolder: LinkAccountHolder,
    private val checkoutEventReporter: CheckoutEventReporter,
) {
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
            is ConfirmationHandler.Result.Succeeded -> checkoutEventReporter.succeeded(
                params = params,
                duration = duration,
            )
            is ConfirmationHandler.Result.Failed -> checkoutEventReporter.failed(
                params = params,
                error = result.cause,
                duration = duration,
            )
            is ConfirmationHandler.Result.Canceled -> checkoutEventReporter.canceled(
                params = params,
                duration = duration,
            )
        }
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
        confirmationStartedAt = checkoutEventReporter.started(params)
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
