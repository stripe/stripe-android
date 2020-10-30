package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.model.AlipayAuthResult
import com.stripe.android.model.PaymentIntentFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class AlipayAuthenticationTaskTest {
    private val intent = PaymentIntentFixtures.ALIPAY_REQUIRES_ACTION
    private val stripeRepository: StripeRepository = mock()
    private val requestOptions = ApiRequest.Options("public_key")
    private val callback: ApiResultCallback<AlipayAuthResult> = mock()

    private val testDispatcher = TestCoroutineDispatcher()

    @Test
    fun `AlipayAuthenticationTask should handle success`() = testDispatcher.runBlockingTest {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createAuthenticator("9000"),
            stripeRepository,
            requestOptions,
            testDispatcher,
            callback
        )
        val result = task.getResult()
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
        verify(stripeRepository).retrieveObject(
            "https://hooks.stripe.com/adapter/alipay/redirect/complete/src_1HDEFWKlwPmebFhp6tcpln8T/src_client_secret_S6H9mVMKK6qxk9YxsUvbH55K",
            requestOptions
        )
    }

    @Test
    fun `AlipayAuthenticationTask should handle cancelation`() = testDispatcher.runBlockingTest {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createAuthenticator("6001"),
            stripeRepository,
            requestOptions,
            testDispatcher,
            callback
        )
        val result = task.getResult()
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.CANCELED)
    }

    @Test
    fun `AlipayAuthenticationTask should handle failure`() = testDispatcher.runBlockingTest {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createAuthenticator("4000"),
            stripeRepository,
            requestOptions,
            testDispatcher,
            callback
        )
        val result = task.getResult()
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.FAILED)
    }

    @Test
    fun `AlipayAuthenticationTask should handle unknown codes`() = testDispatcher.runBlockingTest {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createAuthenticator("unknown"),
            stripeRepository,
            requestOptions,
            testDispatcher,
            callback
        )
        val result = task.getResult()
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
    }

    @Test
    fun `AlipayAuthenticationTask should handle missing results`() = testDispatcher.runBlockingTest {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createAuthenticator(null),
            stripeRepository,
            requestOptions,
            testDispatcher,
            callback
        )
        val result = task.getResult()
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
    }

    @Test
    fun `AlipayAuthenticationTask should throw exception when alipay data missing`() = testDispatcher.runBlockingTest {
        val task = StripePaymentController.AlipayAuthenticationTask(
            PaymentIntentFixtures.PI_REQUIRES_REDIRECT,
            createAuthenticator("9000"),
            stripeRepository,
            requestOptions,
            testDispatcher,
            callback
        )
        assertFailsWith<RuntimeException> {
            task.getResult()
        }
    }

    @Test
    fun `AlipayAuthenticationTask should throw exception in test mode`() = testDispatcher.runBlockingTest {
        val task = StripePaymentController.AlipayAuthenticationTask(
            PaymentIntentFixtures.ALIPAY_TEST_MODE,
            createAuthenticator("9000"),
            stripeRepository,
            requestOptions,
            testDispatcher,
            callback
        )
        assertFailsWith<IllegalArgumentException> {
            runBlocking { task.getResult() }
        }
    }

    private fun createAuthenticator(resultCode: String?) = object : AlipayAuthenticator {
        override fun onAuthenticationRequest(data: String): Map<String, String> {
            return resultCode?.let { mapOf("resultStatus" to it) }.orEmpty()
        }
    }
}
