package com.stripe.android.paymentsheet

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface GooglePayRepository {
    fun isReady(): Flow<Boolean>

    class Disabled : GooglePayRepository {
        override fun isReady(): Flow<Boolean> = flowOf(false)
    }
}
