package com.stripe.android

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import java.util.Calendar
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal interface FingerprintRequestExecutor {
    fun execute(
        request: FingerprintRequest
    ): LiveData<FingerprintData?>

    class Default(
        private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
        private val connectionFactory: ConnectionFactory = ConnectionFactory.Default()
    ) : FingerprintRequestExecutor {
        private val timestampSupplier = {
            Calendar.getInstance().timeInMillis
        }

        override fun execute(
            request: FingerprintRequest
        ) = liveData<FingerprintData?>(dispatcher) {
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
                        FingerprintData(
                            guid = it.body,
                            timestamp = timestampSupplier()
                        )
                    }
                }.getOrNull()
            }
        }
    }
}
