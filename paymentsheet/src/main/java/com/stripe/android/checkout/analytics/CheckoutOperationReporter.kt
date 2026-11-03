package com.stripe.android.checkout.analytics

import android.content.Context
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
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
