package com.stripe.android.core.networking

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.stripe.android.core.Logger
import com.stripe.android.core.utils.IsWorkManagerAvailable
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface AnalyticsRequestV2Executor {
    suspend fun enqueue(request: AnalyticsRequestV2)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultAnalyticsRequestV2Executor @Inject constructor(
    private val context: Context,
    private val networkClient: StripeNetworkClient,
    private val logger: Logger,
    private val storage: AnalyticsRequestV2Storage,
    private val isWorkManagerAvailable: IsWorkManagerAvailable,
) : AnalyticsRequestV2Executor {

    override suspend fun enqueue(request: AnalyticsRequestV2) {
        val isEnqueued = isWorkManagerAvailable() && enqueueRequest(request)
        if (!isEnqueued) {
            executeRequest(request)
        }
    }

    private suspend fun enqueueRequest(request: AnalyticsRequestV2): Boolean {
        val workManager = WorkManager.getInstance(context)
        val id = storage.store(request)
        val inputData = SendAnalyticsRequestV2Worker.createInputData(id)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<SendAnalyticsRequestV2Worker>()
            .addTag(SendAnalyticsRequestV2Worker.TAG)
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        return runCatching { workManager.enqueue(workRequest).await() }.isSuccess
    }

    private suspend fun executeRequest(request: AnalyticsRequestV2) {
        runCatching {
            networkClient.executeRequest(request)
            logger.debug("EVENT: ${request.eventName}")
        }.onFailure {
            logger.error("Exception while making analytics request", it)
        }
    }
}
