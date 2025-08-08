package com.stripe.android.hcaptcha

import android.os.Handler
import androidx.fragment.app.FragmentActivity
import com.google.common.truth.Truth.assertThat
import com.stripe.hcaptcha.HCaptcha
import com.stripe.hcaptcha.HCaptchaError
import com.stripe.hcaptcha.HCaptchaException
import com.stripe.hcaptcha.HCaptchaTokenResponse
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaSize
import com.stripe.hcaptcha.task.OnFailureListener
import com.stripe.hcaptcha.task.OnSuccessListener
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

internal class DefaultHCaptchaServiceTest {

    @Test
    fun `performPassiveHCaptcha calls hCaptchaProvider with activity`() = runTest {
        val testContext = createTestContext()
        testContext.setupSuccessfulHCaptcha("test-token")

        testContext.service.performPassiveHCaptcha(
            testContext.activity,
            siteKey = "test-site-key",
            rqData = null
        )

        assertThat(testContext.hCaptchaProvider.getInvocations).containsExactly(testContext.activity)
    }

    @Test
    fun `performPassiveHCaptcha configures HCaptcha with correct settings`() = runTest {
        val testContext = createTestContext()
        testContext.setupSuccessfulHCaptcha()
        val testSiteKey = "test-site-key"
        val testRqData = "test-rq-data"

        testContext.service.performPassiveHCaptcha(
            testContext.activity,
            siteKey = testSiteKey,
            rqData = testRqData
        )

        val configCaptor = argumentCaptor<HCaptchaConfig>()
        verify(testContext.hCaptcha).setup(configCaptor.capture())

        with(configCaptor.firstValue) {
            assertThat(siteKey).isEqualTo(testSiteKey)
            assertThat(size).isEqualTo(HCaptchaSize.INVISIBLE)
            assertThat(rqdata).isEqualTo(testRqData)
            assertThat(loading).isFalse()
            assertThat(hideDialog).isTrue()
            assertThat(disableHardwareAcceleration).isTrue()
        }
    }

    @Test
    fun `performPassiveHCaptcha returns success when HCaptcha succeeds`() = runTest {
        val testContext = createTestContext()
        val expectedToken = "success-token"
        testContext.setupSuccessfulHCaptcha(expectedToken)

        val result = testContext.service.performPassiveHCaptcha(
            testContext.activity,
            siteKey = "test-site-key",
            rqData = null
        )

        assertThat(result).isInstanceOf(HCaptchaService.Result.Success::class.java)
        assertThat((result as HCaptchaService.Result.Success).token).isEqualTo(expectedToken)
    }

    @Test
    fun `performPassiveHCaptcha returns failure when HCaptcha fails`() = runTest {
        val testContext = createTestContext()
        val exception = HCaptchaException(HCaptchaError.NETWORK_ERROR)
        testContext.setupFailedHCaptcha(exception)

        val result = testContext.service.performPassiveHCaptcha(
            testContext.activity,
            siteKey = "test-site-key",
            rqData = null
        )

        assertThat(result).isInstanceOf(HCaptchaService.Result.Failure::class.java)
        assertThat((result as HCaptchaService.Result.Failure).error).isEqualTo(exception)
    }

    private fun createTestContext(): TestContext {
        val activity = mock<FragmentActivity>()
        val hCaptcha = mock<HCaptcha>()
        val hCaptchaProvider = FakeHCaptchaProvider(hCaptcha)
        val service = DefaultHCaptchaService(hCaptchaProvider)

        return TestContext(
            activity = activity,
            hCaptcha = hCaptcha,
            hCaptchaProvider = hCaptchaProvider,
            service = service
        )
    }

    private fun TestContext.setupHCaptchaChaining() {
        whenever(hCaptcha.addOnSuccessListener(any())).doReturn(hCaptcha)
        whenever(hCaptcha.addOnFailureListener(any())).doReturn(hCaptcha)
        whenever(hCaptcha.setup(any())).doReturn(hCaptcha)
    }

    private fun TestContext.setupSuccessfulHCaptcha(token: String = "test-token") {
        setupHCaptchaChaining()

        whenever(hCaptcha.verifyWithHCaptcha()).then {
            val successCaptor = argumentCaptor<OnSuccessListener<HCaptchaTokenResponse>>()
            verify(hCaptcha).addOnSuccessListener(successCaptor.capture())
            successCaptor.firstValue.onSuccess(createHCaptchaTokenResponse(token))
            hCaptcha
        }
    }

    private fun TestContext.setupFailedHCaptcha(exception: HCaptchaException) {
        setupHCaptchaChaining()

        whenever(hCaptcha.verifyWithHCaptcha()).then {
            val failureCaptor = argumentCaptor<OnFailureListener>()
            verify(hCaptcha).addOnFailureListener(failureCaptor.capture())
            failureCaptor.firstValue.onFailure(exception)
            hCaptcha
        }
    }

    private fun createHCaptchaTokenResponse(token: String): HCaptchaTokenResponse {
        return HCaptchaTokenResponse(token, mock<Handler>())
    }

    private data class TestContext(
        val activity: FragmentActivity,
        val hCaptcha: HCaptcha,
        val hCaptchaProvider: FakeHCaptchaProvider,
        val service: DefaultHCaptchaService
    )

    private class FakeHCaptchaProvider(
        private val hCaptcha: HCaptcha
    ) : HCaptchaProvider {
        val getInvocations = mutableListOf<FragmentActivity>()

        override fun get(activity: FragmentActivity): HCaptcha {
            getInvocations.add(activity)
            return hCaptcha
        }
    }
}
