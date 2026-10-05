package com.stripe.android.financialconnections.example.data

import com.stripe.android.financialconnections.example.BuildConfig
import com.stripe.android.financialconnections.example.data.model.CreateAccountHolderBody
import com.stripe.android.financialconnections.example.data.model.CreateConsentBody
import com.stripe.android.financialconnections.example.data.model.CreateIntentResponse
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.MerchantsResponse
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

internal class BackendRepository(
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

    suspend fun createAccountHolder(body: CreateAccountHolderBody) = backendService.createAccountHolder(body)

    suspend fun createConsent(body: CreateConsentBody) = backendService.createConsent(body)

    suspend fun createSetupIntent(paymentIntentBody: PaymentIntentBody) =
        backendService.createSetupIntent(paymentIntentBody)

    suspend fun merchants(): MerchantsResponse = backendService.merchants()
}
