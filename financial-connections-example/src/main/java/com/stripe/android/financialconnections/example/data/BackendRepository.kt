package com.stripe.android.financialconnections.example.data

import com.stripe.android.financialconnections.example.BuildConfig
import com.stripe.android.financialconnections.example.data.model.CreateIntentResponse
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.MerchantsResponse
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

class BackendRepository(
    settings: Settings
) {
    private val backendService: BackendApiService = BackendApiFactory(settings).create()

    suspend fun createLinkAccountSession(
        linkAccountSessionBody: LinkAccountSessionBody = LinkAccountSessionBody(
            testEnvironment = BuildConfig.TEST_ENVIRONMENT
        )
    ) = backendService.createLinkAccountSession(linkAccountSessionBody)

    suspend fun createLinkAccountSessionForToken(
        linkAccountSessionBody: LinkAccountSessionBody = LinkAccountSessionBody(
            testEnvironment = BuildConfig.TEST_ENVIRONMENT
        )
    ) = backendService.createLinkAccountSessionForToken(
        linkAccountSessionBody
    )

    suspend fun createPaymentIntent(
        paymentIntentBody: PaymentIntentBody = PaymentIntentBody(
            testEnvironment = BuildConfig.TEST_ENVIRONMENT
        )
    ): CreateIntentResponse = backendService.createPaymentIntent(
        paymentIntentBody
    )

    suspend fun merchants(): MerchantsResponse = backendService.merchants()
}
