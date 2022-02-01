package com.stripe.android.stripe3ds2.transaction

import androidx.annotation.VisibleForTesting
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import com.stripe.android.stripe3ds2.transactions.ErrorData
import com.stripe.android.stripe3ds2.transactions.ProtocolError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

interface TransactionTimer {
    suspend fun start()
    val timeout: Flow<Boolean>
}

internal class DefaultTransactionTimer(
    timeoutMinutes: Int,
    private val errorRequestExecutor: ErrorRequestExecutor,
    private val creqData: ChallengeRequestData,
    private val workContext: CoroutineContext
) : TransactionTimer {
    private val timeoutMillis = TimeUnit.MINUTES.toMillis(timeoutMinutes.toLong())

    private val mutableTimeoutFlow = MutableStateFlow(false)
    override val timeout: StateFlow<Boolean> = mutableTimeoutFlow

    /**
     * Called when challenge flow has been initiated.
     *
     * Should start the timer and associate the [TransactionTimer] with the given
     * SDK transaction id.
     */
    override suspend fun start() = withContext(workContext) {
        delay(timeoutMillis)

        withContext(Dispatchers.Main) {
            onTimeout()
        }
    }

    @VisibleForTesting
    internal fun onTimeout() {
        errorRequestExecutor.executeAsync(createTimeoutErrorData())
        mutableTimeoutFlow.value = true
    }

    private fun createTimeoutErrorData() = ErrorData(
        serverTransId = creqData.threeDsServerTransId,
        acsTransId = creqData.acsTransId,
        errorCode = ProtocolError.TransactionTimedout.code.toString(),
        errorComponent = ErrorData.ErrorComponent.ThreeDsSdk,
        errorDescription = ProtocolError.TransactionTimedout.description,
        errorDetail = "Timeout expiry reached for the transaction",
        messageVersion = creqData.messageVersion,
        sdkTransId = creqData.sdkTransId
    )
}
