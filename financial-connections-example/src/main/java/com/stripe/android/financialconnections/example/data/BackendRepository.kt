package com.stripe.android.financialconnections.example.data

private const val BASE_URL = "https://stripe-mobile-connections-example.glitch.me/"

class BackendRepository(
    private val backendService: BackendApiService = BackendApiFactory(BASE_URL).create()
) {

    suspend fun createLinkAccountSession() = backendService.createLinkAccountSession()
}
