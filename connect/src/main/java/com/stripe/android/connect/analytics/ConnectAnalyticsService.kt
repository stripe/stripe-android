package com.stripe.android.connect.analytics

import android.app.Application
import android.content.Context
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.AnalyticsRequestV2
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
 * Service for logging [AnalyticsRequestV2] for the Connect SDK.
 */
internal class ConnectAnalyticsService(
    context: Context,
    private val isTestMode: Boolean,
) {
    internal var merchantId: String? = null

    private val application: Application = context.applicationContext as Application

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
        context = context,
        clientId = CLIENT_ID,
        origin = ORIGIN,
    )

    @OptIn(DelicateCoroutinesApi::class)
    fun track(event: ConnectAnalyticsEvent) {
        GlobalScope.launch(Dispatchers.IO) {
            val request = requestFactory.createRequest(
                eventName = event.eventName,
                additionalParams = event.params.orEmpty() + commonParams(),
                includeSDKParams = true,
            )
            requestExecutor.enqueue(request)
        }
    }

    private fun commonParams(): Map<String, Any?> {
        return mapOf(
            "livemode" to !isTestMode,
            "merchantId" to merchantId,
        ).filterNot { (_, v) -> v == null }
    }

    internal companion object {
        const val CLIENT_ID = "mobile_connect_sdk"
        const val ORIGIN = "stripe-connect-android"
    }
}
