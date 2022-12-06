package com.stripe.android.financialconnections.example.data

private const val BASE_URL = "https://little-protective-queen.glitch.me/"

class BackendRepository(
    private val backendService: BackendApiService = BackendApiFactory(BASE_URL).create()
) {

    suspend fun createLinkAccountSession() =
        backendService.createLinkAccountSession()

    suspend fun createLinkAccountSessionForToken() =
        backendService.createLinkAccountSessionForToken()

}
