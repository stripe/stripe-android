package com.stripe.android.connect.analytics

import android.app.Application
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.AnalyticsRequestV2Factory
import com.stripe.android.core.networking.DefaultAnalyticsRequestV2Executor
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.RealAnalyticsRequestV2Storage
import com.stripe.android.core.utils.RealIsWorkManagerAvailable
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Analytics service configured for Connect SDK.
 * Consumers should prefer [ComponentAnalyticsService] instead as this service is very simple.
 */
internal interface ConnectAnalyticsService {
    fun track(eventName: String, params: Map<String, Any?>)
}

internal class DefaultConnectAnalyticsService(application: Application) : ConnectAnalyticsService {
    private val analyticsRequestStorage = RealAnalyticsRequestV2Storage(application)
    private val logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)
    private val networkClient = DefaultStripeNetworkClient()
    private val isWorkerAvailable = RealIsWorkManagerAvailable(
        isEnabledForMerchant = { true }
    )

    private val requestExecutor = DefaultAnalyticsRequestV2Executor(
        application = application,
        networkClient = networkClient,
        logger = logger,
        storage = analyticsRequestStorage,
        isWorkManagerAvailable = isWorkerAvailable,
    )

    private val requestFactory = AnalyticsRequestV2Factory(
        context = application,
        clientId = CLIENT_ID,
        origin = ORIGIN,
    )

    @OptIn(DelicateCoroutinesApi::class)
    override fun track(eventName: String, params: Map<String, Any?>) {
        GlobalScope.launch(Dispatchers.IO) {
            val request = requestFactory.createRequest(
                eventName = eventName,
                additionalParams = params,
                includeSDKParams = true,
            )
            requestExecutor.enqueue(request)
        }
    }

    internal companion object {
        const val CLIENT_ID = "mobile_connect_sdk"
        const val ORIGIN = "stripe-connect-android"
    }
}
