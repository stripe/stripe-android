package com.stripe.android.stripe3ds2.transaction

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeTransactionTimer(
    override val timeout: MutableStateFlow<Boolean> = MutableStateFlow(false),
    private val delayMillis: Long = 0L
) : TransactionTimer {

    constructor(
        timeoutVal: Boolean,
        delayMillis: Long = 0L
    ) : this(
        MutableStateFlow(timeoutVal),
        delayMillis
    )

    override suspend fun start() {
        delay(delayMillis)
    }
}
