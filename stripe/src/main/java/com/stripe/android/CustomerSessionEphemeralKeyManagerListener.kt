package com.stripe.android

import java.util.concurrent.ThreadPoolExecutor

internal class CustomerSessionEphemeralKeyManagerListener(
    private val runnableFactory: CustomerSessionRunnableFactory,
    private val executor: ThreadPoolExecutor,
    private val listeners: MutableMap<String, CustomerSession.RetrievalListener?>,
    private val productUsage: CustomerSessionProductUsage
) : EphemeralKeyManager.KeyManagerListener {
    override fun onKeyUpdate(
        ephemeralKey: EphemeralKey,
        operation: EphemeralOperation
    ) {
        val runnable =
            runnableFactory.create(ephemeralKey, operation)
        runnable?.let {
            executor.execute(it)

            if (operation !is EphemeralOperation.RetrieveKey) {
                productUsage.reset()
            }
        }
    }

    override fun onKeyError(
        operationId: String,
        errorCode: Int,
        errorMessage: String
    ) {
        listeners.remove(operationId)?.onError(errorCode, errorMessage, null)
    }
}
