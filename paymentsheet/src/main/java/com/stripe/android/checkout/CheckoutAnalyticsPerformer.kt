@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.analytics.CheckoutConfirmationContext
import com.stripe.android.checkout.analytics.CheckoutConfirmationContext.InvocationSource
import com.stripe.android.checkout.analytics.CheckoutConfirmationEvent
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.DefaultAnalyticsMetadataFactory
import com.stripe.android.paymentsheet.state.LinkState
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
    private val stateHolder: CheckoutControllerStateHolder,
    private val linkAccountHolder: LinkAccountHolder,
    context: Context,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val durationProvider: DurationProvider,
    private val apiConfigurationProvider: Provider<ApiConfiguration.State>,
    @IOContext private val workContext: CoroutineContext,
) {
    private val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
        context = context,
        defaultProductUsageTokens = setOf("checkout_elements"),
        apiConfigurationProvider = apiConfigurationProvider,
    )

    fun onPaymentElementConfirmationStarted(paymentSelection: PaymentSelection) {
        saveConfirmation(InvocationSource.Merchant, paymentSelection)
    }

    fun onExpressCheckoutElementConfirmationStarted(paymentSelection: PaymentSelection) {
        saveConfirmation(InvocationSource.ExpressCheckoutElement, paymentSelection)
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
        val confirmationResponse = (result as? ConfirmationHandler.Result.Succeeded)
            ?.metadata?.get(CheckoutSessionResponseKey)
        val response = confirmationResponse ?: stateHolder.state?.checkoutSessionResponse
        val analyticsMetadata = DefaultAnalyticsMetadataFactory.create(response)
        val duration = durationProvider.end(DurationProvider.Key.CheckoutConfirmation)

        when (result) {
            is ConfirmationHandler.Result.Succeeded -> reportEvent(
                CheckoutConfirmationEvent.Succeeded(
                    confirmationContext = confirmationContext,
                    analyticsMetadata = analyticsMetadata,
                    duration = duration,
                )
            )
            is ConfirmationHandler.Result.Failed -> reportEvent(
                CheckoutConfirmationEvent.Failed(
                    confirmationContext = confirmationContext,
                    analyticsMetadata = analyticsMetadata,
                    duration = duration,
                    error = result.toConfirmationError() ?: PaymentSheetConfirmationError.Stripe(result.cause),
                )
            )
            is ConfirmationHandler.Result.Canceled -> reportEvent(
                CheckoutConfirmationEvent.Canceled(
                    confirmationContext = confirmationContext,
                    analyticsMetadata = analyticsMetadata,
                    duration = duration,
                )
            )
        }
    }

    private fun reportEvent(event: CheckoutConfirmationEvent) {
        val publishableKey = stateHolder.state?.paymentMethodMetadata?.apiConfiguration?.publishableKey
            ?: apiConfigurationProvider.get().publishableKey
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = event.params,
                    publishableKeyOverride = publishableKey,
                )
            )
        }
    }

    private fun saveConfirmation(
        invocationSource: InvocationSource,
        paymentSelection: PaymentSelection,
    ) {
        val state = stateHolder.state
        val metadata = when (invocationSource) {
            InvocationSource.Merchant -> state?.paymentMethodMetadata
            InvocationSource.ExpressCheckoutElement -> state?.expressCheckoutElementPaymentMethodMetadata
        }
        val confirmationContext = CheckoutConfirmationContext(
            paymentSelection = paymentSelection,
            invocationSource = invocationSource,
            activeLinkSession = linkAccountHolder.linkAccountInfo.value.account?.isVerified
                ?: (metadata?.linkState?.loginState == LinkState.LoginState.LoggedIn),
        )
        savedStateHandle[CONFIRMATION_CONTEXT_KEY] = confirmationContext
        durationProvider.start(DurationProvider.Key.CheckoutConfirmation)
        reportEvent(
            CheckoutConfirmationEvent.Started(
                confirmationContext = confirmationContext,
                analyticsMetadata = DefaultAnalyticsMetadataFactory.create(state?.checkoutSessionResponse),
            )
        )
    }

    private companion object {
        const val CONFIRMATION_CONTEXT_KEY = "CheckoutAnalyticsPerformer.confirmationContext"
    }
}
