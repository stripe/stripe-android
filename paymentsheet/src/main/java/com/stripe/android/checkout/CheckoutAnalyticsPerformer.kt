@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.analytics.CheckoutAnalyticsOperation
import com.stripe.android.checkout.analytics.CheckoutEventReporter
import com.stripe.android.checkout.analytics.CheckoutInvocationSource
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.analytics.linkContext
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.isSaved
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.reportPaymentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CheckoutAnalyticsPerformer @Inject constructor(
    private val eventReporter: EventReporter,
    private val savedStateHandle: SavedStateHandle,
    private val stateHolder: CheckoutControllerStateHolder,
    private val linkAccountHolder: LinkAccountHolder,
    private val checkoutEventReporter: CheckoutEventReporter,
) {
    private var confirmationSpan: CheckoutEventReporter.Span? = null

    fun onPaymentElementConfirmationStarted(paymentSelection: PaymentSelection) {
        saveConfirmation(IntegrationType.PaymentElement, paymentSelection)
    }

    fun onExpressCheckoutElementConfirmationStarted(paymentSelection: PaymentSelection) {
        saveConfirmation(IntegrationType.ExpressCheckoutElement, paymentSelection)
    }

    fun reportConfirmationResult(result: ConfirmationHandler.Result) {
        val selection = savedStateHandle.get<PaymentSelection>(PAYMENT_SELECTION_KEY) ?: return
        val span = currentSpan() ?: return
        val response = (result as? ConfirmationHandler.Result.Succeeded)?.metadata?.get(CheckoutSessionResponseKey)
        eventReporter.reportPaymentResult(result, selection)
        when (result) {
            is ConfirmationHandler.Result.Succeeded -> span.succeeded(
                response = stateHolder.state?.checkoutSessionResponse,
                requestId = response?.requestId,
                additionalParams = emptyMap(),
            )
            is ConfirmationHandler.Result.Failed -> span.failed(
                error = result.cause,
                response = stateHolder.state?.checkoutSessionResponse,
                additionalParams = emptyMap(),
            )
            is ConfirmationHandler.Result.Canceled -> span.canceled(
                response = stateHolder.state?.checkoutSessionResponse,
                additionalParams = emptyMap(),
            )
        }
        clearConfirmation()
    }

    fun reportConfirmationFailure(error: Throwable) {
        currentSpan()?.failed(error, stateHolder.state?.checkoutSessionResponse, emptyMap())
        clearConfirmation()
    }

    private fun currentSpan(): CheckoutEventReporter.Span? {
        val integrationType = savedStateHandle.get<IntegrationType>(INTEGRATION_TYPE_KEY) ?: return null
        val selection = savedStateHandle.get<PaymentSelection>(PAYMENT_SELECTION_KEY) ?: return null
        return confirmationSpan ?: checkoutEventReporter.resume(
            operation = CheckoutAnalyticsOperation.Confirm,
            response = stateHolder.state?.checkoutSessionResponse,
            additionalParams = confirmationParams(integrationType, selection),
        )
    }

    private fun saveConfirmation(
        integrationType: IntegrationType,
        paymentSelection: PaymentSelection,
    ) {
        savedStateHandle[INTEGRATION_TYPE_KEY] = integrationType
        savedStateHandle[PAYMENT_SELECTION_KEY] = paymentSelection
        savedStateHandle.remove<Bundle>(PARAMS_KEY)
        val params = confirmationParams(integrationType, paymentSelection)
        savedStateHandle[PARAMS_KEY] = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        confirmationSpan = checkoutEventReporter.start(
            operation = CheckoutAnalyticsOperation.Confirm,
            response = stateHolder.state?.checkoutSessionResponse,
            additionalParams = params,
        )
    }

    private fun confirmationParams(
        integrationType: IntegrationType,
        selection: PaymentSelection,
    ): Map<String, Any?> {
        val restored = savedStateHandle.get<Bundle>(PARAMS_KEY)
        val metadata = when (integrationType) {
            IntegrationType.PaymentElement -> stateHolder.state?.paymentMethodMetadata
            IntegrationType.ExpressCheckoutElement -> stateHolder.state?.expressCheckoutElementPaymentMethodMetadata
        }
        return restored?.keySet()?.associateWith { key ->
            @Suppress("DEPRECATION")
            restored.get(key)
        } ?: mapOf(
            "selected_lpm" to selection.code(),
            "is_saved_payment_method" to selection.isSaved,
            "link_context" to selection.linkContext(),
            "active_link_session" to (
                linkAccountHolder.linkAccountInfo.value.account?.isVerified
                    ?: (metadata?.linkState?.loginState == LinkState.LoginState.LoggedIn)
                ),
            "invocation_source" to when (integrationType) {
                IntegrationType.PaymentElement -> CheckoutInvocationSource.Merchant.value
                IntegrationType.ExpressCheckoutElement -> CheckoutInvocationSource.ExpressCheckoutElement.value
            },
        )
    }

    private fun clearConfirmation() {
        confirmationSpan = null
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
