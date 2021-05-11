package com.stripe.android.paymentsheet.example.repository

import com.stripe.android.paymentsheet.example.service.CheckoutResponse
import kotlinx.coroutines.flow.Flow

internal interface Repository {
    suspend fun checkout(
        customer: String,
        currency: String,
        mode: String
    ): Flow<Result<CheckoutResponse>>
}
