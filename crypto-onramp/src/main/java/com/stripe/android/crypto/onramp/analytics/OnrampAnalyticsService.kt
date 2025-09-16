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

internal interface OnrampAnalyticsService {
    val elementsSessionId: String

    fun track(event: OnrampAnalyticsEvent)

    interface Factory {
        fun create(elementsSessionId: String): OnrampAnalyticsService
    }
}

internal class OnrampAnalyticsServiceImpl @AssistedInject constructor(
    context: Context,
    @Assisted override val elementsSessionId: String,
    private val requestExecutor: AnalyticsRequestV2Executor,
    @IOContext private val workContext: CoroutineContext,
) : OnrampAnalyticsService {

    private val requestFactory = AnalyticsRequestV2Factory(
        context,
        clientId = CLIENT_ID,
        origin = ORIGIN,
    )

    override fun track(event: OnrampAnalyticsEvent) {
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
    interface Factory : OnrampAnalyticsService.Factory {
        override fun create(elementsSessionId: String): OnrampAnalyticsServiceImpl
    }

    companion object {
        const val CLIENT_ID = "mobile-onramp-sdk"
        const val ORIGIN = "stripe-onramp-android"
    }
}
