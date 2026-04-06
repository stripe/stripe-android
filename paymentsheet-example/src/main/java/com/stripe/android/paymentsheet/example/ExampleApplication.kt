package com.stripe.android.paymentsheet.example

import android.annotation.SuppressLint
import android.app.Application
import android.os.StrictMode
import com.stripe.android.core.networking.ConnectionFactory
import com.stripe.android.core.networking.HttpClientFactory
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class ExampleApplication : Application() {

    override fun onCreate() {
        val proxyIpAddress = BuildConfig.PROXY_IP_ADDRESS
        if (proxyIpAddress.isNotEmpty()) {
            ConnectionFactory.Default.httpClientFactory = ProxyHttpClientFactory(proxyIpAddress)
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
class ProxyHttpClientFactory(
    private val ipAddress: String
) : HttpClientFactory {
    override fun create(
        configure: HttpClientConfig<*>.() -> Unit
    ): HttpClient {
        val trustManager = trustAllTrustManager()
        val proxy = Proxy(
            Proxy.Type.HTTP,
            InetSocketAddress.createUnresolved(ipAddress, PROXY_PORT)
        )
        return HttpClient(OkHttp) {
            engine {
                config {
                    proxy(proxy)
                    sslSocketFactory(trustAllSocketFactory(trustManager), trustManager)
                }
            }
            configure()
        }
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
    }
}
