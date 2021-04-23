package com.stripe.example.paymentsheet

import com.stripe.example.service.CheckoutResponse
import kotlinx.coroutines.flow.Flow

internal interface Repository {
    suspend fun checkout(
        customer: String,
        currency: String,
        mode: String
    ): Flow<Result<CheckoutResponse>>
}
