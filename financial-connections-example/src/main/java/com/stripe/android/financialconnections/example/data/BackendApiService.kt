package com.stripe.android.financialconnections.example.data

import androidx.annotation.Keep
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

@Keep
data class LinkAccountSessionBody(
    @SerializedName("flow")
    val flow: String? = null,
    @SerializedName("custom_pk")
    val publishableKey: String? = null,
    @SerializedName("custom_sk")
    val secretKey: String? = null,
    @SerializedName("customer_email")
    val customerEmail: String? = null,
    @SerializedName("test_environment")
    val testEnvironment: String? = null
)

@Keep
data class PaymentIntentBody(
    @SerializedName("flow")
    val flow: String? = null,
    @SerializedName("country")
    val country: String? = "US",
    @SerializedName("customer_id")
    val customerId: String? = null,
    @SerializedName("supported_payment_methods")
    val supportedPaymentMethods: String? = null,
    @SerializedName("custom_pk")
    val publishableKey: String? = null,
    @SerializedName("custom_sk")
    val secretKey: String? = null,
    @SerializedName("customer_email")
    val customerEmail: String? = null,
    @SerializedName("test_environment")
    val testEnvironment: String? = null
)
