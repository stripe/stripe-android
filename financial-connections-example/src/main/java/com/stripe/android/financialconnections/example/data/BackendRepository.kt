package com.stripe.android.financialconnections.example.data

class BackendRepository(
    settings: Settings
) {

    private val backendService: BackendApiService = BackendApiFactory(settings).create()

    suspend fun createLinkAccountSession() =
        backendService.createLinkAccountSession()

    suspend fun createLinkAccountSessionForToken() =
        backendService.createLinkAccountSessionForToken()

}
