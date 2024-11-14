package com.stripe.android.financialconnections.utils

import android.app.Application
import com.stripe.android.core.frauddetection.DefaultFraudDetectionDataRepository
import com.stripe.android.core.frauddetection.DefaultFraudDetectionDataRequestFactory
import com.stripe.android.core.frauddetection.DefaultFraudDetectionDataStore
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import kotlinx.coroutines.Dispatchers

internal fun DefaultFraudDetectionDataRepository(
    application: Application,
): DefaultFraudDetectionDataRepository {
    val workContext = Dispatchers.IO

    return DefaultFraudDetectionDataRepository(
        localStore = DefaultFraudDetectionDataStore(application, workContext),
        fraudDetectionDataRequestFactory = DefaultFraudDetectionDataRequestFactory(application),
        stripeNetworkClient = DefaultStripeNetworkClient(workContext = workContext),
        errorReporter = { /* No-op */ },
        workContext = workContext,
        fraudDetectionEnabledProvider = { true },
    )
}
