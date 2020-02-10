package com.stripe.android

import java.util.concurrent.ThreadPoolExecutor

internal class CustomerSessionEphemeralKeyManagerListener(
    private val runnableFactory: CustomerSessionRunnableFactory,
    private val executor: ThreadPoolExecutor,
    private val listeners: MutableMap<String, CustomerSession.RetrievalListener?>
) : EphemeralKeyManager.KeyManagerListener {
    override fun onKeyUpdate(
        ephemeralKey: EphemeralKey,
        operation: EphemeralOperation
    ) {
        runnableFactory.create(ephemeralKey, operation)?.let {
            executor.execute(it)
        }
    }

    override fun onKeyError(
        operationId: String,
        errorCode: Int,
        errorMessage: String
    ) {
        listeners.remove(operationId)?.onError(
            errorCode, errorMessage, null
        )
    }
}
