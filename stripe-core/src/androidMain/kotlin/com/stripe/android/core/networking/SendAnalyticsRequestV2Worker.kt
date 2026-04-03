package com.stripe.android.core.networking

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.stripe.android.core.exception.InvalidRequestException

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
                if (error.shouldRetry && runAttemptCount < MaxAttempts - 1) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            },
        )
    }

    private suspend inline fun withRequest(block: (AnalyticsRequestV2) -> Result): Result {
        val id = inputData.getString(DataKey) ?: return Result.failure()
        val request = storage(applicationContext).retrieve(id) ?: return Result.failure()
        val workManagerRequest = request.withWorkManagerParams(runAttemptCount)

        val result = block(workManagerRequest)
        val canRemove = result != Result.retry()

        if (canRemove) {
            storage(applicationContext).delete(id)
        }

        return block(workManagerRequest)
    }

    companion object {

        const val TAG = "SendAnalyticsRequestV2Worker"

        var networkClient: StripeNetworkClient = DefaultStripeNetworkClient()
            private set

        var storage: (Context) -> AnalyticsRequestV2Storage =
            { RealAnalyticsRequestV2Storage(it.applicationContext as Application) }
            private set

        fun createInputData(id: String): Data {
            return workDataOf(DataKey to id)
        }

        @VisibleForTesting
        fun setNetworkClient(networkClient: StripeNetworkClient) {
            this.networkClient = networkClient
        }

        @VisibleForTesting
        fun setStorage(storage: AnalyticsRequestV2Storage) {
            this.storage = { storage }
        }
    }
}

private val Throwable.shouldRetry: Boolean
    get() = this !is InvalidRequestException
