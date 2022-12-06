package com.stripe.android.financialconnections.example.data

import com.stripe.android.financialconnections.example.data.model.CreateIntentResponse
import com.stripe.android.financialconnections.example.data.model.CreateLinkAccountSessionResponse
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
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

    @FormUrlEncoded
    @POST("create_payment_intent")
    suspend fun createPaymentIntent(
        @FieldMap params: MutableMap<String, String>
    ): CreateIntentResponse
}

data class LinkAccountSessionBody(
    val flow: String?
)
