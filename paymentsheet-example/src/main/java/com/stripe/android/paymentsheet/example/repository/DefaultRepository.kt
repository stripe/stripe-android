package com.stripe.android.paymentsheet.example.repository

import com.stripe.android.paymentsheet.example.service.CheckoutBackendApi
import com.stripe.android.paymentsheet.example.service.CheckoutRequest

internal class DefaultRepository(
    private val checkoutBackendApi: CheckoutBackendApi
) : Repository {
    override suspend fun checkout(
        customer: Repository.CheckoutCustomer,
        currency: Repository.CheckoutCurrency,
        mode: Repository.CheckoutMode,
        setShippingAddress: Boolean
    ) = checkoutBackendApi.checkout(CheckoutRequest(customer.value, currency.value, mode.value, setShippingAddress))
}
