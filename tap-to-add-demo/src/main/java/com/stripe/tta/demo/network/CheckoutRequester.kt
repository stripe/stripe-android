package com.stripe.tta.demo.network

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.tta.demo.network.model.CheckoutRequest
import com.stripe.tta.demo.network.model.CheckoutResponse
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class CheckoutRequester(
    private val applicationContext: Context,
    private val ioContext: CoroutineContext
) : BasePlaygroundRequester<CheckoutRequest, CheckoutResponse>(
    path = "checkout",
    ioContext = ioContext,
    requestSerializer = CheckoutRequest.serializer(),
    responseDeserializer = CheckoutResponse.serializer(),
) {
    override suspend fun fetch(request: CheckoutRequest): Result<CheckoutResponse> {
        return withContext(ioContext) {
            super.fetch(request).onSuccess { response ->
                PaymentConfiguration.init(
                    context = applicationContext,
                    publishableKey = response.publishableKey,
                )
            }
        }
    }
}
