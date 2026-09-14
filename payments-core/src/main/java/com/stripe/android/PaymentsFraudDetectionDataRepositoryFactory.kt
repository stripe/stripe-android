@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.core.frauddetection.DefaultFraudDetectionDataRepository
import com.stripe.android.core.frauddetection.DefaultFraudDetectionDataRequestFactory
import com.stripe.android.core.frauddetection.DefaultFraudDetectionDataStore
import com.stripe.android.core.frauddetection.FraudDetectionErrorReporter
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.payments.core.analytics.ErrorReporter
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmOverloads
fun DefaultFraudDetectionDataRepository(
    context: Context,
    workContext: CoroutineContext = Dispatchers.IO,
): DefaultFraudDetectionDataRepository {
    val errorReporter = ErrorReporter.createFallbackInstance(context, emptySet())
    val applicationContext = context.applicationContext
    return DefaultFraudDetectionDataRepository(
        localStore = DefaultFraudDetectionDataStore(context, workContext),
        fraudDetectionDataRequestFactory = DefaultFraudDetectionDataRequestFactory(context),
        stripeNetworkClient = DefaultStripeNetworkClient(workContext = workContext),
        errorReporter = FraudDetectionErrorReporter { error ->
            errorReporter.reportFraudDetectionError(
                error = error,
                publishableKey = runCatching {
                    PaymentConfiguration.getInstance(applicationContext).publishableKey
                }.getOrNull(),
            )
        },
        workContext = workContext,
        fraudDetectionEnabledProvider = { Stripe.advancedFraudSignalsEnabled },
    )
}
