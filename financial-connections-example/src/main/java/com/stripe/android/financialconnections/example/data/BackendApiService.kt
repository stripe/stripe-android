package com.stripe.android.financialconnections.example.data

import com.stripe.android.financialconnections.example.data.model.CreateIntentResponse
import com.stripe.android.financialconnections.example.data.model.CreateAccountHolderBody
import com.stripe.android.financialconnections.example.data.model.CreateAccountHolderResponse
import com.stripe.android.financialconnections.example.data.model.CreateLinkAccountSessionResponse
import com.stripe.android.financialconnections.example.data.model.CreateConsentBody
import com.stripe.android.financialconnections.example.data.model.CreateSetupIntentResponse
import com.stripe.android.financialconnections.example.data.model.IssuedConsent
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.MerchantsResponse
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

internal interface BackendApiService {
    @POST("create_accountholder")
    suspend fun createAccountHolder(
        @Body body: CreateAccountHolderBody
    ): CreateAccountHolderResponse

    @POST("create_consent")
    suspend fun createConsent(
        @Body body: CreateConsentBody
    ): IssuedConsent

    @POST("create_link_account_session")
    suspend fun createLinkAccountSession(
        @Body body: LinkAccountSessionBody
    ): CreateLinkAccountSessionResponse

    @POST("create_las_for_token")
    suspend fun createLinkAccountSessionForToken(
        @Body linkAccountSessionBody: LinkAccountSessionBody
    ): CreateLinkAccountSessionResponse

    @POST("create_payment_intent")
    suspend fun createPaymentIntent(
        @Body params: PaymentIntentBody
    ): CreateIntentResponse

    @POST("create_setup_intent")
    suspend fun createSetupIntent(
        @Body params: PaymentIntentBody
    ): CreateSetupIntentResponse

    @GET("merchants")
    suspend fun merchants(): MerchantsResponse
}
