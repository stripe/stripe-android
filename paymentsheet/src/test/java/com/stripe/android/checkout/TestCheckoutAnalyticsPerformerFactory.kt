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
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.utils.FakeDurationProvider
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

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
    ): CheckoutAnalyticsPerformer = CheckoutAnalyticsPerformer(
        confirmationHandler = confirmationHandler,
        savedStateHandle = savedStateHandle,
        stateHolder = stateHolder,
        linkAccountHolder = linkAccountHolder,
        context = ApplicationProvider.getApplicationContext<Context>(),
        analyticsRequestExecutor = analyticsRequestExecutor,
        durationProvider = durationProvider,
        apiConfigurationProvider = Provider { apiConfiguration },
        workContext = workContext,
    )
}
