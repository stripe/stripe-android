package com.stripe.android

import java.util.HashMap
import java.util.concurrent.ThreadPoolExecutor

internal class CustomerSessionEphemeralKeyManagerListener(
    private val runnableFactory: CustomerSessionRunnableFactory,
    private val executor: ThreadPoolExecutor,
    private val listeners: HashMap<String, CustomerSession.RetrievalListener>,
    private val productUsage: CustomerSessionProductUsage
) : EphemeralKeyManager.KeyManagerListener<CustomerEphemeralKey> {
    override fun onKeyUpdate(
        ephemeralKey: CustomerEphemeralKey,
        operationId: String,
        actionString: String?,
        arguments: Map<String, Any>?
    ) {
        val runnable =
            runnableFactory.create(ephemeralKey, operationId, actionString, arguments)
        runnable?.let {
            executor.execute(it)

            if (actionString != null) {
                productUsage.reset()
            }
        }
    }

    override fun onKeyError(
        operationId: String,
        httpCode: Int,
        errorMessage: String
    ) {
        listeners.remove(operationId)?.onError(httpCode, errorMessage, null)
    }
}
