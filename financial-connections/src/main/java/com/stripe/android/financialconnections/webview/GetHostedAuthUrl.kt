package com.stripe.android.financialconnections.webview

import com.stripe.android.core.networking.RequestHeadersFactory.Companion.CHARSET
import com.stripe.android.core.networking.StripeRequest
import org.json.JSONObject

internal class GetHostedAuthUrl(private val httpClient: HttpClient) {

    suspend operator fun invoke(
        clientSecret: String,
        applicationId: String,
        publishableKey: String
    ): JSONObject? {
        val url = "https://api.stripe.com/v1/financial_connections/sessions/synchronize"
        val params = mapOf(
            "client_secret" to clientSecret,
            "key" to publishableKey,
            "_stripe_version" to "2022-11-15",
            "mobile[application_id]" to applicationId,
            "mobile[fullscreen]" to "false",
            "mobile[hide_close_button]" to "false",
        )
        val contentType = "${StripeRequest.MimeType.Form.code}; charset=$CHARSET"
        return httpClient.makePostRequest(url, params, contentType)
    }
}