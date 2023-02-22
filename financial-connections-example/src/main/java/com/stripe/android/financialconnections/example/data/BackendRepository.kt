package com.stripe.android.financialconnections.example.data

import com.stripe.android.financialconnections.example.data.model.CreateIntentResponse

class BackendRepository(
    settings: Settings
) {
    private val backendService: BackendApiService = BackendApiFactory(settings).create()

    suspend fun createLinkAccountSession(flow: String? = null, customerEmail: String? = null) =
        backendService.createLinkAccountSession(
            LinkAccountSessionBody(flow, customerEmail)
        )

    suspend fun createLinkAccountSessionForToken(flow: String? = null, customerEmail: String? = null) =
        backendService.createLinkAccountSessionForToken(
            LinkAccountSessionBody(flow, customerEmail)
        )

    suspend fun createPaymentIntent(
        country: String,
        flow: String? = null,
        customerId: String? = null,
        supportedPaymentMethods: String? = null,
        customerEmail: String? = null
    ): CreateIntentResponse = backendService.createPaymentIntent(
        PaymentIntentBody(
            flow = flow,
            country = country,
            customerId = customerId,
            customerEmail = customerEmail,
            supportedPaymentMethods = supportedPaymentMethods
        )
    )
}
