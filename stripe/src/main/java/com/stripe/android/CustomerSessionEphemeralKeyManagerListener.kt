package com.stripe.android

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class CustomerSessionEphemeralKeyManagerListener(
    private val runnableFactory: CustomerSessionRunnableFactory,
    private val workContext: CoroutineContext,
    private val listeners: MutableMap<String, CustomerSession.RetrievalListener?>
) : EphemeralKeyManager.KeyManagerListener {
    override fun onKeyUpdate(
        ephemeralKey: EphemeralKey,
        operation: EphemeralOperation
    ) {
        runnableFactory.create(ephemeralKey, operation)?.let {
            CoroutineScope(workContext).launch {
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
            errorCode,
            errorMessage,
            null
        )
    }
}
