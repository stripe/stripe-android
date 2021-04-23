package com.stripe.example.paymentsheet

import com.stripe.example.service.CheckoutBackendApi
import com.stripe.example.service.CheckoutRequest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class DefaultRepository(
    private val checkoutBackendApi: CheckoutBackendApi,
    private val workContext: CoroutineContext
) : Repository {
    override suspend fun checkout(
        customer: String,
        currency: String,
        mode: String
    ) = withContext(workContext) {
        flowOf(
            kotlin.runCatching {
                checkoutBackendApi.checkout(CheckoutRequest(customer, currency, mode))
            }
        )
    }
}
