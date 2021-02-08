package com.stripe.android.paymentsheet

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

fun interface GooglePayRepository {
    fun isReady(): Flow<Boolean>

    object Disabled : GooglePayRepository {
        override fun isReady(): Flow<Boolean> = flowOf(false)
    }
}
