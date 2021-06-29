package com.stripe.android.paymentsheet

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class FakeGooglePayRepository(
    internal var value: Boolean
) : GooglePayRepository {
    override fun isReady(): Flow<Boolean> = flowOf(value)
}
