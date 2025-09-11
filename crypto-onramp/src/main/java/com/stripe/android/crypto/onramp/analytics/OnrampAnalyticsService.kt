package com.stripe.android.crypto.onramp.analytics

import android.content.Context
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestV2Executor
import com.stripe.android.core.networking.AnalyticsRequestV2Factory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

internal class OnrampAnalyticsService @AssistedInject constructor(
    context: Context,
    private val requestExecutor: AnalyticsRequestV2Executor,
    @IOContext private val workContext: CoroutineContext,
    @Assisted val elementsSessionId: String,
) {

    private val requestFactory = AnalyticsRequestV2Factory(
        context,
        clientId = CLIENT_ID,
        origin = ORIGIN,
    )

    fun track(event: OnrampAnalyticsEvent) {
        CoroutineScope(workContext).launch {
            val request = requestFactory.createRequest(
                eventName = event.eventName,
                additionalParams = buildMap {
                    put("elements_session_id", elementsSessionId)
                    event.params?.let { putAll(it) }
                },
                includeSDKParams = true,
            )
            requestExecutor.enqueue(request)
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(elementsSessionId: String): OnrampAnalyticsService
    }

    companion object {
        const val CLIENT_ID = "mobile-onramp-sdk"
        const val ORIGIN = "stripe-onramp-android"
    }
}
