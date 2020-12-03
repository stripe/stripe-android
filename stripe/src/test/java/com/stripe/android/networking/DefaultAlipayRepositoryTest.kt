package com.stripe.android.networking

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.AlipayAuthenticator
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.AlipayAuthResult
import com.stripe.android.model.PaymentIntentFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class DefaultAlipayRepositoryTest {
    private val stripeRepository: StripeRepository = mock()
    private val repository = DefaultAlipayRepository(stripeRepository)

    private val testDispatcher = TestCoroutineDispatcher()

    @Test
    fun `authenticate() should handle success`() = testDispatcher.runBlockingTest {
        val result = repository.authenticate(
            INTENT,
            createAuthenticator(AlipayAuthResult.RESULT_CODE_SUCCESS),
            REQUEST_OPTIONS
        )

        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
        verify(stripeRepository).retrieveObject(
            "https://hooks.stripe.com/adapter/alipay/redirect/complete/src_1HDEFWKlwPmebFhp6tcpln8T/src_client_secret_S6H9mVMKK6qxk9YxsUvbH55K",
            REQUEST_OPTIONS
        )
    }

    @Test
    fun `authenticate() should handle cancelation`() = testDispatcher.runBlockingTest {
        val result = repository.authenticate(
            INTENT,
            createAuthenticator(AlipayAuthResult.RESULT_CODE_CANCELLED),
            REQUEST_OPTIONS
        )
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.CANCELED)
    }

    @Test
    fun `authenticate() should handle failure`() = testDispatcher.runBlockingTest {
        val result = repository.authenticate(
            INTENT,
            createAuthenticator(AlipayAuthResult.RESULT_CODE_FAILED),
            REQUEST_OPTIONS
        )
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.FAILED)
    }

    @Test
    fun `authenticate() should handle unknown codes`() = testDispatcher.runBlockingTest {
        val result = repository.authenticate(
            INTENT,
            createAuthenticator("unknown"),
            REQUEST_OPTIONS
        )
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
    }

    @Test
    fun `authenticate() should handle missing results`() = testDispatcher.runBlockingTest {
        val result = repository.authenticate(
            INTENT,
            createAuthenticator(null),
            REQUEST_OPTIONS
        )
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
    }

    @Test
    fun `authenticate() should throw exception when alipay data missing`() = testDispatcher.runBlockingTest {
        val exception = assertFailsWith<RuntimeException> {
            repository.authenticate(
                PaymentIntentFixtures.PI_REQUIRES_REDIRECT,
                createAuthenticator(AlipayAuthResult.RESULT_CODE_SUCCESS),
                REQUEST_OPTIONS
            )
        }
        assertThat(exception.message)
            .isEqualTo("Unable to authenticate Payment Intent with Alipay SDK")
    }

    @Test
    fun `authenticate() should throw exception in test mode`() = testDispatcher.runBlockingTest {
        val exception = assertFailsWith<IllegalArgumentException> {
            repository.authenticate(
                PaymentIntentFixtures.ALIPAY_TEST_MODE,
                createAuthenticator(AlipayAuthResult.RESULT_CODE_SUCCESS),
                REQUEST_OPTIONS
            )
        }
        assertThat(exception.message)
            .isEqualTo(
                """
                Attempted to authenticate test mode PaymentIntent with the Alipay SDK.
                The Alipay SDK does not support test mode payments.
                """.trimIndent()
            )
    }

    private fun createAuthenticator(resultCode: String?) = object : AlipayAuthenticator {
        override fun onAuthenticationRequest(data: String): Map<String, String> {
            return resultCode?.let { mapOf("resultStatus" to it) }.orEmpty()
        }
    }

    private companion object {
        private val INTENT = PaymentIntentFixtures.ALIPAY_REQUIRES_ACTION
        private val REQUEST_OPTIONS = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    }
}
