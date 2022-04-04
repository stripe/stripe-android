package com.stripe.android.connections.example.data

private const val BASE_URL = "https://desert-instinctive-eoraptor.glitch.me/"

class BackendRepository(
    private val backendService: BackendApiService = BackendApiFactory(BASE_URL).create()
) {

    suspend fun createLinkAccountSession() = backendService.createLinkAccountSession()
}
