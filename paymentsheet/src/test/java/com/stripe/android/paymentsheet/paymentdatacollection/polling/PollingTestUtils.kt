package com.stripe.android.paymentsheet.paymentdatacollection.polling

import com.stripe.android.model.StripeIntent
import com.stripe.android.polling.IntentStatusPoller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeIntentStatusPoller : IntentStatusPoller {

    private val _state = MutableStateFlow<StripeIntent.Status?>(null)
    override val state: StateFlow<StripeIntent.Status?> = _state

    private val forcePollChannel = Channel<StripeIntent.Status?>(capacity = 1)

    var isActive: Boolean = false
        private set

    override fun startPolling(scope: CoroutineScope) {
        isActive = true
    }

    override suspend fun forcePoll(): StripeIntent.Status? {
        return forcePollChannel.receive()
    }

    override fun stopPolling() {
        isActive = false
    }

    fun enqueueForcePollResult(status: StripeIntent.Status?) {
        forcePollChannel.trySend(status)
    }

    fun emitNextPollResult(status: StripeIntent.Status?) {
        _state.value = status
    }
}

internal class FakeTimeProvider(
    private val timeInMillis: Long = System.currentTimeMillis(),
) : TimeProvider {
    override fun currentTimeInMillis(): Long {
        return timeInMillis
    }
}
