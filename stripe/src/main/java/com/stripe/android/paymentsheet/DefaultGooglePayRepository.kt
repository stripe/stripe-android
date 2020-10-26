package com.stripe.android.paymentsheet

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class DefaultGooglePayRepository() : GooglePayRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val isReadyState = MutableStateFlow(null)

    override fun isReady(): Flow<Boolean?> = isReadyState
}
