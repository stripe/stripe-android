package com.stripe.android.core.networking

import android.net.http.HttpResponseCache
import com.google.common.truth.Truth.assertThat
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.method
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import java.net.CacheRequest
import java.net.CacheResponse
import java.net.ResponseCache
import java.net.URI
import java.net.URLConnection
import kotlin.test.Test

internal class NetworkCachingTest {
    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun `request that should cache creates a connection that uses cache`() = runTest {
        networkRule.enqueue(method("GET")) { response ->
            response.setBody("response1")
        }
        networkRule.enqueue(method("GET")) { response ->
            response.setBody("response2")
        }
        networkRule.enqueue(method("GET")) { response ->
            response.setBody("response3")
        }

        var getCacheCount = 0
        var putCacheCount = 0
        HttpResponseCache.setDefault(object : ResponseCache() {
            override fun get(
                uri: URI?,
                rqstMethod: String?,
                rqstHeaders: MutableMap<String, MutableList<String>>?
            ): CacheResponse? {
                getCacheCount++
                return null
            }

            override fun put(uri: URI?, conn: URLConnection?): CacheRequest? {
                putCacheCount++
                return null
            }
        })

        val executor = DefaultStripeNetworkClient(
            workContext = testScheduler,
            connectionFactory = ConnectionFactory.Default
        )

        val url = ApiRequest.API_HOST

        val requestWithCache = FakeStripeRequest(
            shouldCache = true,
            method = StripeRequest.Method.GET,
            url = url
        )

        val requestWithoutCache = FakeStripeRequest(
            shouldCache = false,
            method = StripeRequest.Method.GET,
            url = url
        )

        executor.executeRequest(requestWithCache)
        assertThat(getCacheCount).isEqualTo(1)
        assertThat(putCacheCount).isEqualTo(1)

        executor.executeRequest(requestWithCache)
        assertThat(getCacheCount).isEqualTo(2)
        assertThat(putCacheCount).isEqualTo(2)

        executor.executeRequest(requestWithoutCache)
        assertThat(getCacheCount).isEqualTo(2)
        assertThat(putCacheCount).isEqualTo(2)
    }
}
