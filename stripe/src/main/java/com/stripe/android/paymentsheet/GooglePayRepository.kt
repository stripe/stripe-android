package com.stripe.android.paymentsheet

import kotlinx.coroutines.flow.Flow

interface GooglePayRepository {
    fun isReady(): Flow<Boolean?>
}
