package com.stripe.android.financialconnections.example.data

class BackendRepository(
    settings: Settings
) {
    private val backendService: BackendApiService = BackendApiFactory(settings).create()

    suspend fun createLinkAccountSession(flow: String? = null) =
        backendService.createLinkAccountSession(
            LinkAccountSessionBody(flow)
        )

    suspend fun createLinkAccountSessionForToken(flow: String? = null) =
        backendService.createLinkAccountSessionForToken(
            LinkAccountSessionBody(flow)
        )

}
