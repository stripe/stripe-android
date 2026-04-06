package com.stripe.android.paymentsheet.example

import android.annotation.SuppressLint
import android.app.Application
import android.os.StrictMode
import com.stripe.android.core.networking.ConnectionFactory
import com.stripe.android.core.networking.StripeRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.time.Duration.Companion.seconds

class ExampleApplication : Application() {

    override fun onCreate() {
        val proxyIpAddress = BuildConfig.PROXY_IP_ADDRESS
        if (proxyIpAddress.isNotEmpty()) {
            ConnectionFactory.Default.connectionOpener = ProxyConnectionOpener(proxyIpAddress)
        }

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDeathOnNetwork()
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

@Suppress("EmptyFunctionBlock", "MagicNumber")
class ProxyConnectionOpener(
    private val ipAddress: String
) : ConnectionFactory.ConnectionOpener {
    override fun open(
        request: StripeRequest,
        callback: HttpClient.(request: StripeRequest) -> ConnectionFactory.StripeKtorConnection
    ): ConnectionFactory.StripeKtorConnection {
        val trustManager = trustAllTrustManager()
        val proxy = Proxy(
            Proxy.Type.HTTP,
            InetSocketAddress.createUnresolved(ipAddress, PROXY_PORT)
        )
        val client = HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT
                socketTimeoutMillis = READ_TIMEOUT
            }
            install(HttpCache)
            engine {
                config {
                    proxy(proxy)
                    sslSocketFactory(trustAllSocketFactory(trustManager), trustManager)
                }
            }
        }
        return callback(client, request)
    }

    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
    private fun trustAllSocketFactory(trustManager: X509TrustManager): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        return sslContext.socketFactory
    }

    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
    private fun trustAllTrustManager(): X509TrustManager {
        return object : X509TrustManager {
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
        }
    }

    private companion object {
        private const val PROXY_PORT = 8888
        private val CONNECT_TIMEOUT = 30.seconds.inWholeMilliseconds
        private val READ_TIMEOUT = 80.seconds.inWholeMilliseconds
    }
}
