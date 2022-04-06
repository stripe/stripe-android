package com.stripe.android.connections.example.data

import com.stripe.android.connections.example.data.model.CreateAccountHolderResponse
import com.stripe.android.connections.example.data.model.CreateLinkAccountSessionBody
import com.stripe.android.connections.example.data.model.CreateLinkAccountSessionResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface BackendApiService {
    @POST("link_account_sessions")
    suspend fun createLinkAccountSession(
        @Body body: CreateLinkAccountSessionBody
    ): CreateLinkAccountSessionResponse

    @POST("accountholders")
    suspend fun createAccountHolder(): CreateAccountHolderResponse
}
