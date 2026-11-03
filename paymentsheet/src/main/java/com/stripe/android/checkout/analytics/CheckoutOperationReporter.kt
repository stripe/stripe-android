package com.stripe.android.checkout.analytics

import android.content.Context
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.DefaultAnalyticsMetadataFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext
import kotlin.time.TimeSource

/**
 * Sends `checkout_elements` operation analytics. Each Dagger graph that runs Checkout operations
 * provides one, supplying the publishable key and configuration analytics it can see.
 */
internal class CheckoutOperationReporter(
    context: Context,
    apiConfigurationProvider: Provider<ApiConfiguration.State>,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val timeSource: TimeSource,
    private val workContext: CoroutineContext,
    private val publishableKey: () -> String,
    private val configurationParams: () -> Map<String, Any?>,
) {
    private val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
        context = context,
        defaultProductUsageTokens = setOf("checkout_elements"),
        apiConfigurationProvider = apiConfigurationProvider,
    )

    @OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)
    suspend fun <T> measureInit(
        clientSecret: String,
        configuration: CheckoutController.Configuration.State,
        block: suspend (InitOperation) -> Result<T>,
    ): Result<T> {
        val operation = InitOperation(clientSecret, configuration, timeSource, ::report)
        return operation.measure(startingResponse = null, response = { operation.response }) {
            block(operation)
        }
    }

    /**
     * Reports started before [block] and succeeded or failed after. [response] is the session the
     * operation starts from, or null before one is loaded. Every failure is reported, including
     * cancellation, so each started event has a matching outcome.
     */
    suspend fun <T> measure(
        operation: Operation,
        params: Map<String, Any?> = emptyMap(),
        response: CheckoutSessionResponse?,
        block: suspend (MeasuredOperation) -> Result<T>,
    ): Result<T> {
        val measuredOperation = MeasuredOperation(operation, params, response, timeSource, ::report)
        return measuredOperation.measure(startingResponse = response, response = { measuredOperation.response }) {
            block(measuredOperation)
        }
    }

    /** Reports an operation that completed without doing any work. */
    fun reportCompleted(
        operation: Operation,
        params: Map<String, Any?>,
        response: CheckoutSessionResponse,
    ) {
        MeasuredOperation(operation, params, response, timeSource, ::report).apply {
            started(response)
            succeeded(response)
        }
    }

    /** Reports an operation that is a single event rather than started and succeeded or failed. */
    fun reportEvent(
        operation: Operation,
        response: CheckoutSessionResponse?,
    ) {
        report(
            CheckoutAnalyticsEvent(
                operation = operation,
                outcome = null,
                analyticsMetadata = DefaultAnalyticsMetadataFactory.create(response),
                duration = null,
                error = null,
                operationParams = emptyMap(),
            )
        )
    }

    fun report(event: CheckoutAnalyticsEvent) {
        val publishableKey = publishableKey()
        val configurationParams = configurationParams()
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(
                    event = event,
                    // Operation params win so init reports the configuration it is attempting.
                    additionalParams = configurationParams + event.params,
                    publishableKeyOverride = publishableKey,
                )
            )
        }
    }
}
