package com.stripe.android.utils

import com.stripe.android.paymentsheet.ConfirmCallback
import com.stripe.android.paymentsheet.DeferredIntentRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.channels.Channel

internal class FakeDeferredIntentRepository : DeferredIntentRepository {
    private val resultChannel = Channel<DeferredIntentRepository.Result>(1)

    fun emitResult(result: DeferredIntentRepository.Result) {
        resultChannel.trySend(result)
    }
    override suspend fun get(
        paymentSelection: PaymentSelection?,
        initializationMode: PaymentSheet.InitializationMode,
        confirmCallback: ConfirmCallback?
    ): DeferredIntentRepository.Result {
        return resultChannel.receive()
    }
}
