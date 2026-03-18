package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CheckoutSessionLoaderTest {

    @get:Rule
    val networkRule = NetworkRule()

    private val repository = CheckoutSessionRepository(
        stripeNetworkClient = DefaultStripeNetworkClient(),
        publishableKeyProvider = { "pk_test_123" },
        stripeAccountIdProvider = { null },
    )

    @Test
    fun `load extracts session ID and returns response on success`() = runTest {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/cs_test_abc123/init"),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val loader = CheckoutSessionLoader(
            repository = repository,
        )

        val result = loader.load("cs_test_abc123_secret_xyz")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().id)
            .isEqualTo("cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh")
    }

    @Test
    fun `load returns failure when repository fails`() = runTest {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/cs_test_abc123/init"),
        ) { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"fail"}}""")
        }

        val loader = CheckoutSessionLoader(
            repository = repository,
        )

        val result = loader.load("cs_test_abc123_secret_xyz")

        assertThat(result.isFailure).isTrue()
    }
}
