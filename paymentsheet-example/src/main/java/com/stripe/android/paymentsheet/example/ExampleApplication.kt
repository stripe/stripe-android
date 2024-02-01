package com.stripe.android.paymentsheet.example

import android.annotation.SuppressLint
import android.app.Application
import android.os.StrictMode
import com.stripe.android.core.networking.ConnectionFactory
import com.stripe.android.core.networking.StripeRequest
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class ExampleApplication : Application() {

    override fun onCreate() {
        ConnectionFactory.Default.connectionOpener = ProxyConnectionOpener("192.168.1.96")

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .also {
                    if (IS_PENALTY_DEATH_ENABLED) {
                        it.penaltyDeath()
                    }
                }
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .also {
                    if (IS_PENALTY_DEATH_ENABLED) {
                        it.penaltyDeath()
                    }
                }
                .build()
        )

        super.onCreate()
    }

    private companion object {
        private const val IS_PENALTY_DEATH_ENABLED = false
    }
}

class ProxyConnectionOpener(
    private val ipAddress: String
) : ConnectionFactory.ConnectionOpener {
    override fun open(
        request: StripeRequest,
        callback: HttpURLConnection.(request: StripeRequest) -> Unit
    ): HttpURLConnection {
        val socketAddress = InetSocketAddress.createUnresolved(ipAddress, 8888)
        val proxy = Proxy(Proxy.Type.HTTP, socketAddress)

        return (URL(request.url).openConnection(proxy) as HttpURLConnection).apply {
            (this as? HttpsURLConnection)?.let { connection ->
                connection.sslSocketFactory = trustAllSocketFactory()
            }
            callback(request)
        }
    }

    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
    private fun trustAllSocketFactory(): SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?
                ) {
                }

                override fun checkServerTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?
                ) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return emptyArray()
                }
            })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        return sslContext.socketFactory
    }
}
