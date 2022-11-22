package com.stripe.android.financialconnections.example.data

private const val BASE_URL = "https://night-discreet-femur.glitch.me/"

class BackendRepository(
    private val backendService: BackendApiService = BackendApiFactory(BASE_URL).create()
) {

    suspend fun createLinkAccountSession(flow: String? = null) =
        backendService.createLinkAccountSession(
            LinkAccountSessionBody(flow)
        )

    suspend fun createLinkAccountSessionForToken(flow: String? = null) =
        backendService.createLinkAccountSessionForToken(
            LinkAccountSessionBody(flow)
        )

}
