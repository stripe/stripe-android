package com.stripe.android

import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal interface FingerprintRequestExecutor {
    fun execute(
        request: FingerprintRequest,
        callback: (FingerprintData?) -> Unit
    )

    class Default(
        private val workScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        private val connectionFactory: ConnectionFactory = ConnectionFactory.Default()
    ) : FingerprintRequestExecutor {
        private val timestampSupplier = {
            Calendar.getInstance().timeInMillis
        }

        override fun execute(
            request: FingerprintRequest,
            callback: (FingerprintData?) -> Unit
        ) {
            workScope.launch {
                val fingerprintData = runCatching {
                    executeInternal(request)
                }.getOrNull()

                withContext(Dispatchers.Main) {
                    // fingerprint request failures should be non-fatal
                    callback(fingerprintData)
                }
            }
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
