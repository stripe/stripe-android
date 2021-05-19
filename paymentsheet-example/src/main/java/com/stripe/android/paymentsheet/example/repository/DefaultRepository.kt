package com.stripe.android.paymentsheet.example.repository

import com.stripe.android.paymentsheet.example.service.CheckoutBackendApi
import com.stripe.android.paymentsheet.example.service.CheckoutRequest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class DefaultRepository(
    private val checkoutBackendApi: CheckoutBackendApi,
    private val workContext: CoroutineContext
) : Repository {
    override suspend fun checkout(
        customer: Repository.CheckoutCustomer,
        currency: Repository.CheckoutCurrency,
        mode: Repository.CheckoutMode
    ) = withContext(workContext) {
        flowOf(
            kotlin.runCatching {
                checkoutBackendApi.checkout(CheckoutRequest(customer.value, currency.value, mode.value))
            }
        )
    }
}
