package com.stripe.android.stripe3ds2.transaction

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class ImmediateTimeoutTransactionTimer : TransactionTimer {
    override suspend fun start() {
    }

    override val timeout: Flow<Boolean> = flowOf(true)
}
