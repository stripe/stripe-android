package com.stripe.android.networktesting

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class NetworkRuleTest {
    @Test
    fun `default rule accepts requests without betas`() = runScenario {
        assertThat(request(version = "2020-03-02")).isEqualTo(200)
        networkRule.validate()
    }

    @Test
    fun `beta validation accepts the integration beta`() = runScenario(validateBetas = true) {
        assertThat(request(version = "2020-03-02;integration_beta=v1")).isEqualTo(200)
        networkRule.validate()
    }

    @Test
    fun `beta validation rejects requests missing the integration beta`() = runScenario(validateBetas = true) {
        assertThat(request(version = "2020-03-02")).isEqualTo(500)
        assertThrows(IllegalStateException::class.java) { networkRule.validate() }
    }

    @Test
    fun `disabling beta validation still validates credentials`() = runScenario {
        assertThat(request(version = "2020-03-02", publishableKey = "pk_test_wrong")).isEqualTo(500)
        assertThrows(IllegalStateException::class.java) { networkRule.validate() }
    }

    @Test
    fun `custom authorization skips beta validation`() = runScenario(
        validateBetas = true,
        applyDefaultAuthorization = false,
    ) {
        assertThat(request(version = "2020-03-02", publishableKey = "pk_test_custom")).isEqualTo(200)
        networkRule.validate()
    }

    private fun runScenario(
        validateBetas: Boolean = false,
        applyDefaultAuthorization: Boolean = true,
        block: Scenario.() -> Unit,
    ) {
        val networkRule = NetworkRule(validateBetas = validateBetas)
        networkRule.enqueue(
            RequestMatchers.path("/v1/payment_intents/pi_example"),
            applyDefaultAuthorization = applyDefaultAuthorization,
        ) { response -> response.setBody("{}") }
        Scenario(networkRule).block()
    }

    private class Scenario(val networkRule: NetworkRule) {
        fun request(
            version: String,
            publishableKey: String = TestApiKeys.PUBLISHABLE,
        ): Int {
            val connection = URL("${networkRule.baseUrl}v1/payment_intents/pi_example")
                .openConnection() as HttpsURLConnection
            return try {
                connection.sslSocketFactory = networkRule.clientSocketFactory()
                connection.setRequestProperty("original-host", "api.stripe.com")
                connection.setRequestProperty("Authorization", "Bearer $publishableKey")
                connection.setRequestProperty("Stripe-Account", TestApiKeys.ACCOUNT)
                connection.setRequestProperty("Stripe-Version", version)
                connection.responseCode
            } finally {
                connection.disconnect()
            }
        }
    }
}
