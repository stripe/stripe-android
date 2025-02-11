package com.stripe.android.financialconnections.webview

import org.json.JSONObject

internal class GetFinancialConnectionsSession(private val httpClient: HttpClient) {

    suspend operator fun invoke(
        publishableKey: String,
        clientSecret: String,
    ): JSONObject? {
        val url = "https://api.stripe.com/v1/link_account_sessions/session_receipt"
        val params = mapOf(
            "client_secret" to clientSecret,
        )
        return httpClient.makeGetRequest(
            headers = mapOf(
                "Authorization" to "Bearer $publishableKey",
            ), url = url, params = params
        )
    }
}