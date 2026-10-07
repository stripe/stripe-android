package com.stripe.android.core.networking

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection

@RunWith(TestParameterInjector::class)
internal class ConnectionFactoryTimeoutTest {
    @Test
    fun `connections use request timeouts when supplied and standard timeouts otherwise`(
        @TestParameter requestTimeouts: Boolean,
    ) {
        val requests = Turbine<StripeRequest>()
        val connection = FakeConnection()
        val original = ConnectionFactory.Default.connectionOpener
        ConnectionFactory.Default.connectionOpener = object : ConnectionFactory.ConnectionOpener {
            override fun open(
                request: StripeRequest,
                callback: HttpURLConnection.(StripeRequest) -> Unit,
            ): HttpsURLConnection {
                requests.add(request)
                connection.callback(request)
                return connection
            }
        }
        val request = object : StripeRequest() {
            override val method = Method.GET
            override val mimeType = MimeType.Form
            override val retryResponseCodes = emptyList<Int>()
            override val url = "https://api.stripe.com/v1/payment_pages/cs_test/poll"
            override val headers = emptyMap<String, String>()
            override val connectTimeoutMillis = 200.takeIf { requestTimeouts }
            override val readTimeoutMillis = 300.takeIf { requestTimeouts }
        }
        try {
            ConnectionFactory.Default.create(request)
            assertThat(requests.takeItem()).isSameInstanceAs(request)
            assertThat(connection.connectTimeout).isEqualTo(if (requestTimeouts) 200 else 30_000)
            assertThat(connection.readTimeout).isEqualTo(if (requestTimeouts) 300 else 80_000)
            requests.ensureAllEventsConsumed()
        } finally {
            ConnectionFactory.Default.connectionOpener = original
        }
    }

    private class FakeConnection : HttpsURLConnection(URL("https://api.stripe.com")) {
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun connect() = Unit
        override fun getCipherSuite() = "test"
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates(): Array<Certificate> = emptyArray()
    }
}
