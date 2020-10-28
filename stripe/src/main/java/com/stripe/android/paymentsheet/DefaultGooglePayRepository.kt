package com.stripe.android.paymentsheet

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class DefaultGooglePayRepository : GooglePayRepository {
    private val isReadyState = MutableStateFlow(null)

    override fun isReady(): Flow<Boolean?> = isReadyState
}
