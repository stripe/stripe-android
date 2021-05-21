package com.stripe.android.paymentsheet.example.repository

import com.stripe.android.paymentsheet.example.service.CheckoutResponse
import kotlinx.coroutines.flow.Flow

internal interface Repository {
    sealed class CheckoutCustomer(val value: String) {
        object New : CheckoutCustomer("new")
        object Returning : CheckoutCustomer("returning")
        data class WithId(val customerId: String) : CheckoutCustomer(customerId)
    }

    enum class CheckoutCurrency(val value: String) {
        USD("usd"),
        EUR("eur")
    }

    enum class CheckoutMode(val value: String) {
        Setup("setup"),
        Payment("payment")
    }

    suspend fun checkout(
        customer: CheckoutCustomer,
        currency: CheckoutCurrency,
        mode: CheckoutMode
    ): Flow<Result<CheckoutResponse>>
}
