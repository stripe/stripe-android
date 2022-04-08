package com.stripe.android.connections.example.data

import com.stripe.android.connections.example.data.model.CreateLinkAccountSessionResponse
import retrofit2.http.POST

interface BackendApiService {
    @POST("create_link_account_session")
    suspend fun createLinkAccountSession(): CreateLinkAccountSessionResponse

    @POST("create_las_for_token")
    suspend fun createLinkAccountSessionForToken(): CreateLinkAccountSessionResponse

}
