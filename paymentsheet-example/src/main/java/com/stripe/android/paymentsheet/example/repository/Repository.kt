package com.stripe.android.paymentsheet.example.repository

import com.stripe.android.paymentsheet.example.service.CheckoutResponse

internal interface Repository {
    sealed class CheckoutCustomer(val value: String) {
        object Guest : CheckoutCustomer("guest")
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
        Payment("payment"),
        Payment_With_Setup("payment_with_setup")
    }

    suspend fun checkout(
        customer: CheckoutCustomer,
        currency: CheckoutCurrency,
        mode: CheckoutMode
    ): CheckoutResponse
}
