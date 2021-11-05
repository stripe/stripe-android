package com.stripe.android.networking

import com.stripe.android.core.networking.ConnectionFactory
import com.stripe.android.core.networking.responseJson
import com.stripe.android.model.parsers.FraudDetectionDataJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.coroutines.CoroutineContext

internal interface FraudDetectionDataRequestExecutor {
    suspend fun execute(
        request: FraudDetectionDataRequest
    ): FraudDetectionData?
}

internal class DefaultFraudDetectionDataRequestExecutor(
    private val connectionFactory: ConnectionFactory = ConnectionFactory.Default,
    private val workContext: CoroutineContext = Dispatchers.IO
) : FraudDetectionDataRequestExecutor {
    private val timestampSupplier = {
        Calendar.getInstance().timeInMillis
    }

    override suspend fun execute(
        request: FraudDetectionDataRequest
    ) = withContext(workContext) {
        // fraud detection data request failures should be non-fatal
        runCatching {
            executeInternal(request)
        }.getOrNull()
    }

    private fun executeInternal(request: FraudDetectionDataRequest): FraudDetectionData? {
        connectionFactory.create(request).use { conn ->
            return runCatching {
                conn.response.takeIf { it.isOk }?.let {
                    FraudDetectionDataJsonParser(timestampSupplier)
                        .parse(it.responseJson())
                }
            }.getOrNull()
        }
    }
}
