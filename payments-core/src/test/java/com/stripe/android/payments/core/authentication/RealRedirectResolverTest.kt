package com.stripe.android.payments.core.authentication

import com.google.common.truth.Truth.assertThat
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

private const val ResolvedUrl = "https://stripe.com/pay-me"

internal class RealRedirectResolverTest {

    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun `Resolves redirect if original URL returns corresponding status code`() = runTest {
        val resolver = buildResolver()
        val url = buildPmRedirectsUrl()

        networkRule.enqueue(
            path("/authorize/acct_1234567890"),
            method("GET"),
        ) { response ->
            response.setResponseCode(302)
            response.addHeader("Location", ResolvedUrl)
        }

        val result = resolver(url)
        assertThat(result).isEqualTo(ResolvedUrl)
    }

    @Test
    fun `Keeps original URL redirect if its status code does not indicate redirection`() = runTest {
        val resolver = buildResolver()
        val url = buildPmRedirectsUrl()

        networkRule.enqueue(
            path("/authorize/acct_1234567890"),
            method("GET"),
        ) { response ->
            response.setResponseCode(200)
        }

        val result = resolver(url)
        assertThat(result).isEqualTo(url)
    }

    @Test
    fun `Keeps original URL redirect if redirection attempt fails`() = runTest {
        val resolver = buildResolver()
        val url = buildPmRedirectsUrl()

        networkRule.enqueue(
            path("/authorize/acct_1234567890"),
            method("GET"),
        ) { response ->
            response.setResponseCode(500)
        }

        val result = resolver(url)
        assertThat(result).isEqualTo(url)
    }

    private fun buildResolver(): RedirectResolver {
        return RealRedirectResolver(
            configureSSL = {
                // We need to trust all because we're resolving both the localhost URL
                // as well as the redirect URL.
                sslSocketFactory = networkRule.clientSocketFactory(trustAll = true)
            }
        )
    }

    private fun buildPmRedirectsUrl(): String {
        return networkRule.baseUrl.newBuilder()
            .addPathSegment("authorize")
            .addPathSegment("acct_1234567890")
            .build()
            .toString()
    }
}
