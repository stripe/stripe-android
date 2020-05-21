package com.stripe.android

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class CustomerSessionEphemeralKeyManagerListener(
    private val runnableFactory: CustomerSessionRunnableFactory,
    private val workDispatcher: CoroutineDispatcher,
    private val listeners: MutableMap<String, CustomerSession.RetrievalListener?>
) : EphemeralKeyManager.KeyManagerListener {
    override fun onKeyUpdate(
        ephemeralKey: EphemeralKey,
        operation: EphemeralOperation
    ) {
        runnableFactory.create(ephemeralKey, operation)?.let {
            CoroutineScope(workDispatcher).launch {
                it.run()
            }
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
