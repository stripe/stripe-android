package com.stripe.android.core.networking

import android.app.Application
import androidx.annotation.RestrictTo
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.stripe.android.core.Logger
import com.stripe.android.core.utils.IsWorkManagerAvailable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface AnalyticsRequestV2Executor {
    fun enqueue(request: AnalyticsRequestV2)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultAnalyticsRequestV2Executor @Inject constructor(
    private val application: Application,
    private val networkClient: StripeNetworkClient,
    private val logger: Logger,
    private val isWorkManagerAvailable: IsWorkManagerAvailable,
    private val dispatcher: CoroutineDispatcher,
) : AnalyticsRequestV2Executor {

    override fun enqueue(request: AnalyticsRequestV2) {
        if (isWorkManagerAvailable()) {
            enqueueRequest(request)
        } else {
            executeRequest(request)
        }
    }

    private fun enqueueRequest(request: AnalyticsRequestV2) {
        val workManager = WorkManager.getInstance(application)
        val inputData = SendAnalyticsRequestV2Worker.createInputData(request)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<SendAnalyticsRequestV2Worker>()
            .addTag(SendAnalyticsRequestV2Worker.TAG)
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        workManager.enqueue(workRequest)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun executeRequest(request: AnalyticsRequestV2) {
        GlobalScope.launch(dispatcher) {
            runCatching {
                networkClient.executeRequest(request)
                logger.debug("EVENT: ${request.eventName}")
            }.onFailure {
                logger.error("Exception while making analytics request", it)
            }
        }
    }
}
