package com.stripe.android.financialconnections.example.data

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
}

data class LinkAccountSessionBody(
    val flow: String?
)
