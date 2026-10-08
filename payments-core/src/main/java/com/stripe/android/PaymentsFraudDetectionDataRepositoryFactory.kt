@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.core.frauddetection.DefaultFraudDetectionDataRepository
import com.stripe.android.core.frauddetection.DefaultFraudDetectionDataRequestFactory
import com.stripe.android.core.frauddetection.DefaultFraudDetectionDataStore
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.analytics.RealErrorReporter
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmOverloads
fun DefaultFraudDetectionDataRepository(
    context: Context,
    workContext: CoroutineContext = Dispatchers.IO,
): DefaultFraudDetectionDataRepository {
    return DefaultFraudDetectionDataRepository(
        localStore = DefaultFraudDetectionDataStore(context, workContext),
        fraudDetectionDataRequestFactory = DefaultFraudDetectionDataRequestFactory(context),
        stripeNetworkClient = DefaultStripeNetworkClient(workContext = workContext),
        errorReporter = { error, publishableKey ->
            RealErrorReporter(
                analyticsRequestExecutor = DefaultAnalyticsRequestExecutor(),
                analyticsRequestFactory = PaymentAnalyticsRequestFactory(context, publishableKey),
            ).reportFraudDetectionError(error, publishableKey)
        },
        workContext = workContext,
        fraudDetectionEnabledProvider = { Stripe.advancedFraudSignalsEnabled },
    )
}
