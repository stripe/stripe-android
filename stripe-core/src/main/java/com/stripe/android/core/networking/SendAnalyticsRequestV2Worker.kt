package com.stripe.android.core.networking

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.stripe.android.core.exception.InvalidRequestException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DataKey = "data"
private const val MaxAttempts = 5

internal class SendAnalyticsRequestV2Worker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withRequest { request ->
        return runCatching {
            networkClient.executeRequest(request)
        }.fold(
            onSuccess = {
                Result.success()
            },
            onFailure = { error ->
                if (error.shouldRetry && runAttemptCount < MaxAttempts) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            },
        )
    }

    private inline fun withRequest(block: (AnalyticsRequestV2) -> Result): Result {
        val request = getRequest(inputData) ?: return Result.failure()
        val workManagerRequest = request.withWorkManagerParams(runAttemptCount)
        return block(workManagerRequest)
    }

    companion object {

        const val TAG = "SendAnalyticsRequestV2Worker"

        var networkClient: StripeNetworkClient = DefaultStripeNetworkClient()
            private set

        fun createInputData(request: AnalyticsRequestV2): Data {
            val encodedRequest = Json.encodeToString(request)
            return workDataOf(DataKey to encodedRequest)
        }

        private fun getRequest(data: Data): AnalyticsRequestV2? {
            val encodedRequest = data.getString(DataKey)
            return encodedRequest?.let {
                runCatching<AnalyticsRequestV2> {
                    Json.decodeFromString(it)
                }.getOrNull()
            }
        }

        @VisibleForTesting
        fun setNetworkClient(networkClient: StripeNetworkClient) {
            this.networkClient = networkClient
        }
    }
}

private val Throwable.shouldRetry: Boolean
    get() = this !is InvalidRequestException
