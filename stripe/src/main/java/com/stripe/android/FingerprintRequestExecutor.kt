package com.stripe.android

import com.stripe.android.model.parsers.FingerprintDataJsonParser
import java.util.Calendar
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface FingerprintRequestExecutor {
    suspend fun execute(
        request: FingerprintRequest
    ): FingerprintData?

    class Default(
        private val connectionFactory: ConnectionFactory = ConnectionFactory.Default(),
        private val workDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : FingerprintRequestExecutor {
        private val timestampSupplier = {
            Calendar.getInstance().timeInMillis
        }

        override suspend fun execute(
            request: FingerprintRequest
        ) = withContext(workDispatcher) {
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
