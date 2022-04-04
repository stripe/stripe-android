package com.stripe.android.connections.example.data

private const val BASE_URL = "https://carlosmuvi-stripe-backend.herokuapp.com/"

class BackendRepository(
    private val backendService: BackendApiService = BackendApiFactory(BASE_URL).create()
) {

    suspend fun createLinkAccountSession() = backendService.createLinkAccountSession()
}
