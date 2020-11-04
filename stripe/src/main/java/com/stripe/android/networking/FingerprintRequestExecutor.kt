package com.stripe.android.networking

import com.stripe.android.FingerprintData
import com.stripe.android.model.parsers.FingerprintDataJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.coroutines.CoroutineContext

internal interface FingerprintRequestExecutor {
    suspend fun execute(
        request: FingerprintRequest
    ): FingerprintData?

    class Default(
        private val connectionFactory: ConnectionFactory = ConnectionFactory.Default(),
        private val workContext: CoroutineContext = Dispatchers.IO
    ) : FingerprintRequestExecutor {
        private val timestampSupplier = {
            Calendar.getInstance().timeInMillis
        }

        override suspend fun execute(
            request: FingerprintRequest
        ) = withContext(workContext) {
            // fingerprint request failures should be non-fatal
            runCatching {
                executeInternal(request)
            }.getOrNull()
        }

        private fun executeInternal(request: FingerprintRequest): FingerprintData? {
            connectionFactory.create(request).use { conn ->
                return runCatching {
                    conn.response.takeIf { it.isOk }?.let {
                        FingerprintDataJsonParser(timestampSupplier)
                            .parse(it.responseJson)
                    }
                }.getOrNull()
            }
        }
    }
}
