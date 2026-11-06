package com.stripe.android.checkout

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiConfigurationPreview
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.utils.FakeDurationProvider
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@OptIn(ApiConfigurationPreview::class)
internal object TestCheckoutAnalyticsPerformerFactory {
    fun create(
        savedStateHandle: SavedStateHandle,
        stateHolder: CheckoutElementsControllerStateHolder,
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        workContext: CoroutineContext,
        linkAccountHolder: LinkAccountHolder = LinkAccountHolder(savedStateHandle),
        durationProvider: DurationProvider = FakeDurationProvider(),
        apiConfiguration: ApiConfiguration.State = PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG,
    ): CheckoutAnalyticsPerformer {
        val apiConfigurationProvider = Provider { apiConfiguration }
        return CheckoutAnalyticsPerformer(
            savedStateHandle = savedStateHandle,
            stateHolder = stateHolder,
            linkAccountHolder = linkAccountHolder,
            analyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext<Context>(),
                defaultProductUsageTokens = emptySet(),
                apiConfigurationProvider = apiConfigurationProvider,
            ),
            analyticsRequestExecutor = analyticsRequestExecutor,
            durationProvider = durationProvider,
            apiConfigurationProvider = apiConfigurationProvider,
            workContext = workContext,
        )
    }
}
