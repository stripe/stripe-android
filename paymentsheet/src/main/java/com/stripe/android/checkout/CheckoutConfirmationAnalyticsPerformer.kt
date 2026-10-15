@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.analytics.CheckoutAnalyticsOperation
import com.stripe.android.checkout.analytics.CheckoutEventReporter
import com.stripe.android.checkout.analytics.CheckoutInvocationSource
import com.stripe.android.checkout.analytics.checkoutAnalyticsParams
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.analytics.linkContext
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.isSaved
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.LinkState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.TimeMark

@Singleton
internal class CheckoutConfirmationAnalyticsPerformer @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val stateHolder: CheckoutControllerStateHolder,
    private val linkAccountHolder: LinkAccountHolder,
    private val checkoutEventReporter: CheckoutEventReporter,
) {
    private var confirmationStartedAt: TimeMark? = null
    private var confirmationInitialParams: Map<String, Any?>? = null

    fun onPaymentElementConfirmationStarted(paymentSelection: PaymentSelection) {
        saveConfirmation(IntegrationType.PaymentElement, paymentSelection)
    }

    fun onExpressCheckoutElementConfirmationStarted(paymentSelection: PaymentSelection) {
        saveConfirmation(IntegrationType.ExpressCheckoutElement, paymentSelection)
    }

    fun reportConfirmationResult(result: ConfirmationHandler.Result) {
        val selection = savedStateHandle.get<PaymentSelection>(PAYMENT_SELECTION_KEY) ?: return
        val integrationType = savedStateHandle.get<IntegrationType>(INTEGRATION_TYPE_KEY) ?: return
        val initialParams = initialConfirmationParams(integrationType, selection)
        val response = stateHolder.state?.checkoutSessionResponse
        val additionalParams = terminalParams(initialParams, response)
        val duration = confirmationStartedAt?.elapsedNow()
        val confirmationResponse = (result as? ConfirmationHandler.Result.Succeeded)
            ?.metadata
            ?.get(CheckoutSessionResponseKey)

        when (result) {
            is ConfirmationHandler.Result.Succeeded -> checkoutEventReporter.succeeded(
                operation = CheckoutAnalyticsOperation.Confirm,
                response = response,
                requestId = confirmationResponse?.requestId,
                duration = duration,
                additionalParams = additionalParams,
            )
            is ConfirmationHandler.Result.Failed -> checkoutEventReporter.failed(
                operation = CheckoutAnalyticsOperation.Confirm,
                error = result.cause,
                response = response,
                requestId = null,
                duration = duration,
                additionalParams = additionalParams,
            )
            is ConfirmationHandler.Result.Canceled -> checkoutEventReporter.canceled(
                operation = CheckoutAnalyticsOperation.Confirm,
                response = response,
                duration = duration,
                additionalParams = additionalParams,
            )
        }
        clearConfirmation()
    }

    fun reportConfirmationFailure(error: Throwable) {
        val integrationType = savedStateHandle.get<IntegrationType>(INTEGRATION_TYPE_KEY)
        val selection = savedStateHandle.get<PaymentSelection>(PAYMENT_SELECTION_KEY)
        if (integrationType != null && selection != null) {
            val response = stateHolder.state?.checkoutSessionResponse
            checkoutEventReporter.failed(
                operation = CheckoutAnalyticsOperation.Confirm,
                error = error,
                response = response,
                requestId = null,
                duration = confirmationStartedAt?.elapsedNow(),
                additionalParams = terminalParams(
                    initialParams = initialConfirmationParams(integrationType, selection),
                    currentResponse = response,
                ),
            )
        }
        clearConfirmation()
    }

    private fun saveConfirmation(
        integrationType: IntegrationType,
        paymentSelection: PaymentSelection,
    ) {
        savedStateHandle[INTEGRATION_TYPE_KEY] = integrationType
        savedStateHandle[PAYMENT_SELECTION_KEY] = paymentSelection
        savedStateHandle.remove<Bundle>(PARAMS_KEY)
        val response = stateHolder.state?.checkoutSessionResponse
        val params = confirmationParams(integrationType, paymentSelection)
        val initialParams = initialParams(response, params)
        savedStateHandle[PARAMS_KEY] = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        confirmationInitialParams = initialParams
        confirmationStartedAt = checkoutEventReporter.started(
            operation = CheckoutAnalyticsOperation.Confirm,
            response = response,
            additionalParams = params,
        )
    }

    private fun initialConfirmationParams(
        integrationType: IntegrationType,
        selection: PaymentSelection,
    ): Map<String, Any?> {
        confirmationInitialParams?.let { return it }
        savedStateHandle.get<Bundle>(PARAMS_KEY)?.let { restored ->
            val restoredParams = restored.keySet().associateWith { key ->
                @Suppress("DEPRECATION")
                restored.get(key)
            }
            return initialParams(
                response = stateHolder.state?.checkoutSessionResponse,
                additionalParams = restoredParams,
            )
        }
        return initialParams(
            response = stateHolder.state?.checkoutSessionResponse,
            additionalParams = confirmationParams(integrationType, selection),
        )
    }

    private fun initialParams(
        response: CheckoutSessionResponse?,
        additionalParams: Map<String, Any?>,
    ): Map<String, Any?> = response?.checkoutAnalyticsParams().orEmpty() +
        additionalParams.filterValues { it != null }

    private fun terminalParams(
        initialParams: Map<String, Any?>,
        currentResponse: CheckoutSessionResponse?,
    ): Map<String, Any?> = initialParams.filterValues { it != null } +
        currentResponse?.checkoutAnalyticsParams().orEmpty()

    private fun confirmationParams(
        integrationType: IntegrationType,
        selection: PaymentSelection,
    ): Map<String, Any?> {
        val metadata = when (integrationType) {
            IntegrationType.PaymentElement -> stateHolder.state?.paymentMethodMetadata
            IntegrationType.ExpressCheckoutElement -> stateHolder.state?.expressCheckoutElementPaymentMethodMetadata
        }
        return mapOf(
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
        confirmationStartedAt = null
        confirmationInitialParams = null
        savedStateHandle.remove<IntegrationType>(INTEGRATION_TYPE_KEY)
        savedStateHandle.remove<PaymentSelection>(PAYMENT_SELECTION_KEY)
        savedStateHandle.remove<Bundle>(PARAMS_KEY)
    }

    private enum class IntegrationType {
        PaymentElement,
        ExpressCheckoutElement,
    }

    private companion object {
        // Keep saved-state keys stable across class renames.
        const val INTEGRATION_TYPE_KEY = "CheckoutAnalyticsPerformer.integrationType"
        const val PAYMENT_SELECTION_KEY = "CheckoutAnalyticsPerformer.paymentSelection"
        const val PARAMS_KEY = "CheckoutAnalyticsPerformer.params"
    }
}
