package com.stripe.android

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.model.StripeIntent
import java.lang.RuntimeException
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlipayAuthenticationTest {
    private val intent = mock<StripeIntent>().also {
        whenever(it.nextActionData).thenReturn(
            StripeIntent.NextActionData.RedirectToUrl(
                Uri.parse("https://stripe.com/some/redirect/url"),
                returnUrl = "example://return_url",
                mobileData = StripeIntent.NextActionData.RedirectToUrl.MobileData.Alipay(
                    "alipay_data"
                )
            )
        )
    }
    private val callback: ApiResultCallback<Int> = mock()

    @Test
    fun `AlipayAuthenticationTask should handle success`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createHandler("9000"),
            callback
        )
        val result = runBlocking { task.getResult() }
        assertThat(result).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
    }

    @Test
    fun `AlipayAuthenticationTask should handle cancelation`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createHandler("6001"),
            callback
        )
        val result = runBlocking { task.getResult() }
        assertThat(result).isEqualTo(StripeIntentResult.Outcome.CANCELED)
    }

    @Test
    fun `AlipayAuthenticationTask should handle failure`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createHandler("4000"),
            callback
        )
        val result = runBlocking { task.getResult() }
        assertThat(result).isEqualTo(StripeIntentResult.Outcome.FAILED)
    }

    @Test
    fun `AlipayAuthenticationTask should handle unknown codes`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createHandler("unknown"),
            callback
        )
        val result = runBlocking { task.getResult() }
        assertThat(result).isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
    }

    @Test
    fun `AlipayAuthenticationTask should handle missing results`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            intent,
            createHandler(null),
            callback
        )
        val result = runBlocking { task.getResult() }
        assertThat(result).isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
    }

    @Test
    fun `AlipayAuthenticationTask should throw exception when alipay data missing`() {
        val task = StripePaymentController.AlipayAuthenticationTask(
            mock(),
            createHandler("9000"),
            callback
        )
        assertFailsWith<RuntimeException> {
            runBlocking { task.getResult() }
        }
    }

    private fun createHandler(resultCode: String?) = object : AlipayAuthenticationHandler {
        override fun authenticate(data: String): Map<String, String> {
            return resultCode?.let { mapOf("resultStatus" to it) }.orEmpty()
        }
    }
}
