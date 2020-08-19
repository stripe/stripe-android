package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.model.AlipayAuthResult
import com.stripe.android.model.PaymentIntentFixtures
import java.lang.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlipayAuthenticationTaskTest {
    private val intent = PaymentIntentFixtures.ALIPAY_REQUIRES_ACTION
    private val stripeRepository: StripeRepository = mock()
    private val requestOptions = ApiRequest.Options("public_key")
    private val callback: ApiResultCallback<AlipayAuthResult> = mock()

    @Test
    fun `AlipayAuthenticationTask should handle success`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createAuthenticator("9000"),
            stripeRepository,
            requestOptions,
            callback
        )
        val result = runBlocking { task.getResult() }
        assertThat(result.outcome)
            .isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
        verify(stripeRepository).retrieveObject(
            "https://hooks.stripe.com/adapter/alipay/redirect/complete/src_1HDEFWKlwPmebFhp6tcpln8T/src_client_secret_S6H9mVMKK6qxk9YxsUvbH55K",
            requestOptions
        )
    }

    @Test
    fun `AlipayAuthenticationTask should handle cancelation`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createAuthenticator("6001"),
            stripeRepository,
            requestOptions,
            callback
        )
        val result = runBlocking { task.getResult() }
        assertThat(result?.outcome)
            .isEqualTo(StripeIntentResult.Outcome.CANCELED)
    }

    @Test
    fun `AlipayAuthenticationTask should handle failure`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createAuthenticator("4000"),
            stripeRepository,
            requestOptions,
            callback
        )
        val result = runBlocking { task.getResult() }
        assertThat(result?.outcome)
            .isEqualTo(StripeIntentResult.Outcome.FAILED)
    }

    @Test
    fun `AlipayAuthenticationTask should handle unknown codes`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createAuthenticator("unknown"),
            stripeRepository,
            requestOptions,
            callback
        )
        val result = runBlocking { task.getResult() }
        assertThat(result?.outcome)
            .isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
    }

    @Test
    fun `AlipayAuthenticationTask should handle missing results`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createAuthenticator(null),
            stripeRepository,
            requestOptions,
            callback
        )
        val result = runBlocking { task.getResult() }
        assertThat(result?.outcome)
            .isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
    }

    @Test
    fun `AlipayAuthenticationTask should throw exception when alipay data missing`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            PaymentIntentFixtures.PI_REQUIRES_REDIRECT,
            createAuthenticator("9000"),
            stripeRepository,
            requestOptions,
            callback
        )
        assertFailsWith<RuntimeException> {
            runBlocking { task.getResult() }
        }
    }

    @Test
    fun `AlipayAuthenticationTask should throw exception in test mode`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            PaymentIntentFixtures.ALIPAY_TEST_MODE,
            createAuthenticator("9000"),
            stripeRepository,
            requestOptions,
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
