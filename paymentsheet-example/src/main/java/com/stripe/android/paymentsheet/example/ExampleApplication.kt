package com.stripe.android.paymentsheet.example

import android.annotation.SuppressLint
import android.app.Application
import android.os.StrictMode
import com.stripe.android.core.networking.ConnectionFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class ExampleApplication : Application() {

    override fun onCreate() {
        val proxyIpAddress = BuildConfig.PROXY_IP_ADDRESS
        if (proxyIpAddress.isNotEmpty()) {
            ConnectionFactory.Default.okHttpClient = buildProxyClient(proxyIpAddress)
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

    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
    private fun buildProxyClient(ipAddress: String): okhttp3.OkHttpClient {
        val socketAddress = InetSocketAddress.createUnresolved(ipAddress, 8888)
        val proxy = Proxy(Proxy.Type.HTTP, socketAddress)

        val trustAllManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf(trustAllManager), SecureRandom())

        return ConnectionFactory.Default.buildDefaultClient().newBuilder()
            .proxy(proxy)
            .sslSocketFactory(sslContext.socketFactory, trustAllManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    private companion object {
        private const val IS_PENALTY_DEATH_ENABLED = false
    }
}
