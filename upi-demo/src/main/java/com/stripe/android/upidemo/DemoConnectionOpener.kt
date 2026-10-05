package com.stripe.android.upidemo

import android.net.Uri
import com.stripe.android.core.networking.ConnectionFactory
import com.stripe.android.core.networking.StripeRequest
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection

/** Supplies fake HTTP responses to the real SDK repositories, parsers, and result processor. */
internal class DemoConnectionOpener : ConnectionFactory.ConnectionOpener {
    override fun open(
        request: StripeRequest,
        callback: HttpURLConnection.(StripeRequest) -> Unit,
    ): HttpsURLConnection {
        val response = responseFor(request)
        return DemoConnection(URL(request.url), response.first, response.second.toString()).apply {
            callback(request)
        }
    }

    private fun responseFor(request: StripeRequest): Pair<Int, JSONObject> {
        val uri = Uri.parse(request.url)
        val path = uri.path.orEmpty()
        return try {
            when {
                uri.host == "m.stripe.com" -> 200 to JSONObject()
                    .put("guid", "demo-guid").put("muid", "demo-muid").put("sid", "demo-sid")
                uri.host == "r.stripe.com" || uri.host == "q.stripe.com" -> 200 to JSONObject()
                path == "/v1/elements/sessions" -> {
                    val id = requireNotNull(uri.getQueryParameter("client_secret")).substringBefore("_secret_")
                    DemoStore.log("Loaded fake Elements Session with UPI Demo")
                    200 to DemoStore.elementsSession(id)
                }
                path.startsWith("/v1/payment_intents/") -> {
                    val id = uri.pathSegments[2]
                    when {
                        path.endsWith("/confirm") && request.method == StripeRequest.Method.POST ->
                            200 to DemoStore.confirm(id)
                        request.method == StripeRequest.Method.GET -> 200 to DemoStore.retrieve(id)
                        else -> error("Unsupported fake payment request: $path")
                    }
                }
                path == "/v1/payment_methods" && request.method == StripeRequest.Method.POST ->
                    200 to DemoStore.paymentMethod()
                else -> error("Unsupported demo request: $path")
            }
        } catch (error: Exception) {
            DemoStore.log(error.message ?: "Fake API error")
            400 to JSONObject().put("error", JSONObject()
                .put("type", "invalid_request_error")
                .put("message", error.message ?: "Fake API error"))
        }
    }
}

private class DemoConnection(url: URL, private val code: Int, private val body: String) : HttpsURLConnection(url) {
    override fun getResponseCode(): Int = code
    override fun getInputStream(): InputStream = ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))
    override fun getErrorStream(): InputStream = inputStream
    override fun getOutputStream(): OutputStream = ByteArrayOutputStream()
    override fun getHeaderFields(): Map<String, List<String>> = mapOf(
        "Content-Type" to listOf("application/json"),
        "Request-Id" to listOf("req_upidemo"),
    )
    override fun connect() = Unit
    override fun disconnect() = Unit
    override fun usingProxy(): Boolean = false
    override fun getCipherSuite(): String = "LOCAL_DEMO"
    override fun getLocalCertificates(): Array<Certificate>? = null
    override fun getServerCertificates(): Array<Certificate> = emptyArray()
}
