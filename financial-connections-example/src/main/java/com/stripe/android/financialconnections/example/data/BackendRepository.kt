package com.stripe.android.financialconnections.example.data

import com.stripe.android.financialconnections.example.data.model.CreateIntentResponse
import okhttp3.ResponseBody

class BackendRepository(
    settings: Settings
) {
    private val backendService: BackendApiService = BackendApiFactory(settings).create()

    suspend fun createLinkAccountSession(flow: String? = null) =
        backendService.createLinkAccountSession(
            LinkAccountSessionBody(flow)
        )

    suspend fun createLinkAccountSessionForToken(flow: String? = null) =
        backendService.createLinkAccountSessionForToken(
            LinkAccountSessionBody(flow)
        )

    suspend fun createPaymentIntent(
        country: String,
        customerId: String? = null,
        supportedPaymentMethods: String? = null
    ): CreateIntentResponse {
        return backendService.createPaymentIntent(
            mapOf("country" to country)
                .plus(
                    customerId?.let {
                        mapOf("customer_id" to it)
                    }.orEmpty()
                ).plus(
                    supportedPaymentMethods?.let {
                        mapOf("supported_payment_methods" to it)
                    }.orEmpty()
                ).toMutableMap()
        )
    }

}
