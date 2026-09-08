@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.frauddetection.DefaultFraudDetectionDataRepository
import com.stripe.android.core.frauddetection.DefaultFraudDetectionDataRequestFactory
import com.stripe.android.core.frauddetection.DefaultFraudDetectionDataStore
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.utils.ApiConfigProviderFromPaymentConfig
import kotlinx.coroutines.Dispatchers
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun DefaultFraudDetectionDataRepository(
    context: Context,
    workContext: CoroutineContext,
): DefaultFraudDetectionDataRepository {
    return DefaultFraudDetectionDataRepository(
        context = context,
        apiConfigurationProvider = ApiConfigProviderFromPaymentConfig.get(context),
        workContext = workContext,
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmOverloads
fun DefaultFraudDetectionDataRepository(
    context: Context,
    apiConfigurationProvider: Provider<ApiConfiguration.State>,
    workContext: CoroutineContext = Dispatchers.IO,
): DefaultFraudDetectionDataRepository {
    return DefaultFraudDetectionDataRepository(
        localStore = DefaultFraudDetectionDataStore(context, workContext),
        fraudDetectionDataRequestFactory = DefaultFraudDetectionDataRequestFactory(context),
        stripeNetworkClient = DefaultStripeNetworkClient(workContext = workContext),
        errorReporter = ErrorReporter.createFallbackInstance(
            context,
            apiConfigurationProvider = apiConfigurationProvider,
            productUsage = emptySet(),
        ),
        workContext = workContext,
        fraudDetectionEnabledProvider = { Stripe.advancedFraudSignalsEnabled },
    )
}
