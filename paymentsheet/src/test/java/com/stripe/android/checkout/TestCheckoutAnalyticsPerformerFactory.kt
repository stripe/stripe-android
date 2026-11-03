@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.analytics.CheckoutOperationReporter
import com.stripe.android.checkout.analytics.toAnalyticsMap
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiConfigurationPreview
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.utils.FakeDurationProvider
import kotlin.coroutines.CoroutineContext
import kotlin.time.TimeSource

@OptIn(ApiConfigurationPreview::class)
internal object TestCheckoutAnalyticsPerformerFactory {
    fun create(
        confirmationHandler: ConfirmationHandler,
        savedStateHandle: SavedStateHandle,
        stateHolder: CheckoutControllerStateHolder,
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        workContext: CoroutineContext,
        linkAccountHolder: LinkAccountHolder = LinkAccountHolder(savedStateHandle),
        durationProvider: DurationProvider = FakeDurationProvider(),
        apiConfiguration: ApiConfiguration.State = PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG,
        timeSource: TimeSource = TimeSource.Monotonic,
        operationReporter: CheckoutOperationReporter = TestCheckoutOperationReporterFactory.create(
            analyticsRequestExecutor = analyticsRequestExecutor,
            workContext = workContext,
            publishableKey = {
                stateHolder.state?.paymentMethodMetadata?.apiConfiguration?.publishableKey
                    ?: apiConfiguration.publishableKey
            },
            configurationParams = { stateHolder.state?.configuration?.toAnalyticsMap().orEmpty() },
            apiConfiguration = apiConfiguration,
            timeSource = timeSource,
        ),
    ): CheckoutAnalyticsPerformer = CheckoutAnalyticsPerformer(
        confirmationHandler = confirmationHandler,
        savedStateHandle = savedStateHandle,
        stateHolder = stateHolder,
        linkAccountHolder = linkAccountHolder,
        durationProvider = durationProvider,
        operationReporter = operationReporter,
    )
}
