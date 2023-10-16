package com.stripe.android.financialconnections.example.data

import com.stripe.android.financialconnections.example.BuildConfig
import com.stripe.android.financialconnections.example.data.model.CreateIntentResponse

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
        country: String,
        flow: String? = null,
        customerId: String? = null,
        supportedPaymentMethods: String? = null,
        keys: Pair<String, String>? = null,
        customerEmail: String? = null,
        testEnvironment: String = BuildConfig.TEST_ENVIRONMENT
    ): CreateIntentResponse = backendService.createPaymentIntent(
        PaymentIntentBody(
            flow = flow,
            country = country,
            customerId = customerId,
            supportedPaymentMethods = supportedPaymentMethods,
            publishableKey = keys?.first,
            secretKey = keys?.second,
            customerEmail = customerEmail,
            testEnvironment = testEnvironment
        )
    )
}
