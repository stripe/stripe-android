package com.stripe.android.paymentsheet.benchmarkapp

import android.content.res.AssetManager
import com.stripe.android.core.networking.ConnectionFactory
import com.stripe.android.core.networking.StripeRequest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection

/** Intercepts SDK HTTP at the existing connection seam; no request reaches a live endpoint. */
internal class FixtureConnectionOpener(
    private val assets: AssetManager,
    private val captchaEnabled: Boolean,
) : ConnectionFactory.ConnectionOpener {
    override fun open(
        request: StripeRequest,
        callback: HttpURLConnection.(StripeRequest) -> Unit,
    ): HttpsURLConnection {
        val url = URL(request.url)
        val body = when {
            url.host == "api.stripe.com" && url.path == "/v1/elements/sessions" -> {
                val fixture = if (captchaEnabled) "elements-session-captcha.json" else "elements-session.json"
                assets.open(fixture).use { it.readBytes() }
            }
            url.host == "r.stripe.com" || url.host == "q.stripe.com" || url.host == "m.stripe.com" -> {
                "{}".toByteArray()
            }
            else -> error("Unexpected benchmark request: ${url.host}${url.path}")
        }
        return FixtureConnection(url, body).apply { callback(request) }
    }
}

private class FixtureConnection(url: URL, private val body: ByteArray) : HttpsURLConnection(url) {
    override fun connect() = Unit
    override fun disconnect() = Unit
    override fun usingProxy(): Boolean = false
    override fun getResponseCode(): Int = HTTP_OK
    override fun getInputStream(): InputStream = ByteArrayInputStream(body)
    override fun getOutputStream(): OutputStream = ByteArrayOutputStream()
    override fun getHeaderFields(): Map<String, List<String>> = mapOf(
        "Content-Type" to listOf("application/json"),
        "Request-Id" to listOf("req_benchmark"),
    )
    override fun getCipherSuite(): String = "benchmark"
    override fun getLocalCertificates(): Array<Certificate>? = null
    override fun getServerCertificates(): Array<Certificate> = emptyArray()
}
