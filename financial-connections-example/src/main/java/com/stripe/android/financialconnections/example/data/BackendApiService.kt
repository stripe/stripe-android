package com.stripe.android.financialconnections.example.data

import com.google.gson.annotations.SerializedName
import com.stripe.android.financialconnections.example.data.model.CreateIntentResponse
import com.stripe.android.financialconnections.example.data.model.CreateLinkAccountSessionResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface BackendApiService {
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
}

data class LinkAccountSessionBody(
    @SerializedName("flow")
    val flow: String?,
    @SerializedName("customer_email")
    val customerEmail: String?
)

data class PaymentIntentBody(
    @SerializedName("flow")
    val flow: String?,
    @SerializedName("country")
    val country: String?,
    @SerializedName("customer_id")
    val customerId: String?,
    @SerializedName("supported_payment_methods")
    val supportedPaymentMethods: String?,
    @SerializedName("customer_email")
    val customerEmail: String?
)
