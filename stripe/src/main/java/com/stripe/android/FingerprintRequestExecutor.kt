package com.stripe.android

import com.stripe.android.model.parsers.FingerprintDataJsonParser
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal interface FingerprintRequestExecutor {
    fun execute(
        request: FingerprintRequest
    ): Flow<FingerprintData?>

    class Default(
        private val connectionFactory: ConnectionFactory = ConnectionFactory.Default()
    ) : FingerprintRequestExecutor {
        private val timestampSupplier = {
            Calendar.getInstance().timeInMillis
        }

        override fun execute(
            request: FingerprintRequest
        ) = flow<FingerprintData?> {
            emit(
                // fingerprint request failures should be non-fatal
                runCatching {
                    executeInternal(request)
                }.getOrNull()
            )
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
