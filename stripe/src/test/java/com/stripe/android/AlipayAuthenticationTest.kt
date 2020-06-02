package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.model.PaymentIntentFixtures
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class AlipayAuthenticationTest {
    private val intent = PaymentIntentFixtures.ALIPAY_REQUIRES_ACTION
    private val callback: ApiResultCallback<Int> = mock()

    @Test
    fun `AlipayAuthenticationTask should handle success`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createAuthenticator("9000"),
            callback
        )
        val result = runBlocking { task.getResult() }
        assertThat(result).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
    }

    @Test
    fun `AlipayAuthenticationTask should handle cancelation`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createAuthenticator("6001"),
            callback
        )
        val result = runBlocking { task.getResult() }
        assertThat(result).isEqualTo(StripeIntentResult.Outcome.CANCELED)
    }

    @Test
    fun `AlipayAuthenticationTask should handle failure`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createAuthenticator("4000"),
            callback
        )
        val result = runBlocking { task.getResult() }
        assertThat(result).isEqualTo(StripeIntentResult.Outcome.FAILED)
    }

    @Test
    fun `AlipayAuthenticationTask should handle unknown codes`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createAuthenticator("unknown"),
            callback
        )
        val result = runBlocking { task.getResult() }
        assertThat(result).isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
    }

    @Test
    fun `AlipayAuthenticationTask should handle missing results`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createAuthenticator(null),
            callback
        )
        val result = runBlocking { task.getResult() }
        assertThat(result).isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
    }

    @Test
    fun `AlipayAuthenticationTask should throw exception when alipay data missing`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            mock(),
            createAuthenticator("9000"),
            callback
        )
        assertFailsWith<RuntimeException> {
            runBlocking { task.getResult() }
        }
    }

    private fun createAuthenticator(resultCode: String?) = object : AlipayAuthenticator {
        override fun onAuthenticationRequest(data: String): Map<String, String> {
            return resultCode?.let { mapOf("resultStatus" to it) }.orEmpty()
        }
    }
}
