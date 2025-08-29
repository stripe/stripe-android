package com.stripe.android.hcaptcha

import android.os.Handler
import androidx.fragment.app.FragmentActivity
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.hcaptcha.analytics.CaptchaEventsReporter
import com.stripe.hcaptcha.HCaptcha
import com.stripe.hcaptcha.HCaptchaError
import com.stripe.hcaptcha.HCaptchaException
import com.stripe.hcaptcha.HCaptchaTokenResponse
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaSize
import com.stripe.hcaptcha.task.OnFailureListener
import com.stripe.hcaptcha.task.OnSuccessListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

internal class DefaultHCaptchaServiceTest {

    @Test
    fun `performPassiveHCaptcha calls hCaptchaProvider`() = runTest {
        val testContext = createTestContext()
        testContext.setupSuccessfulHCaptcha("test-token")

        testContext.service.performPassiveHCaptcha(
            testContext.activity,
            siteKey = "test-site-key",
            rqData = null
        )

        testContext.hCaptchaProvider.awaitCall()
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
        verify(testContext.hCaptcha).setup(any(), configCaptor.capture())

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
        val siteKey = "test-site-key"
        testContext.setupSuccessfulHCaptcha(expectedToken)

        val result = testContext.service.performPassiveHCaptcha(
            testContext.activity,
            siteKey = siteKey,
            rqData = null
        )

        assertThat(result).isInstanceOf(HCaptchaService.Result.Success::class.java)
        assertThat((result as HCaptchaService.Result.Success).token).isEqualTo(expectedToken)

        // Verify analytics calls in order
        assertThat(testContext.captchaEventsReporter.awaitCall())
            .isEqualTo(FakeCaptchaEventsReporter.Call.Init(siteKey))
        assertThat(testContext.captchaEventsReporter.awaitCall())
            .isEqualTo(FakeCaptchaEventsReporter.Call.Execute(siteKey))
        assertThat(testContext.captchaEventsReporter.awaitCall())
            .isEqualTo(FakeCaptchaEventsReporter.Call.Success(siteKey))
        testContext.captchaEventsReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `performPassiveHCaptcha returns failure when HCaptcha fails`() = runTest {
        val testContext = createTestContext()
        val exception = HCaptchaException(HCaptchaError.NETWORK_ERROR)
        val siteKey = "test-site-key"
        testContext.setupFailedHCaptcha(exception)

        val result = testContext.service.performPassiveHCaptcha(
            testContext.activity,
            siteKey = siteKey,
            rqData = null
        )

        assertThat(result).isInstanceOf(HCaptchaService.Result.Failure::class.java)
        assertThat((result as HCaptchaService.Result.Failure).error).isEqualTo(exception)

        // Verify analytics calls in order
        assertThat(testContext.captchaEventsReporter.awaitCall())
            .isEqualTo(FakeCaptchaEventsReporter.Call.Init(siteKey))
        assertThat(testContext.captchaEventsReporter.awaitCall())
            .isEqualTo(FakeCaptchaEventsReporter.Call.Execute(siteKey))
        assertThat(testContext.captchaEventsReporter.awaitCall())
            .isEqualTo(FakeCaptchaEventsReporter.Call.Error(exception, siteKey))
        testContext.captchaEventsReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `performPassiveHCaptcha calls reset after successful completion`() = runTest {
        val testContext = createTestContext()
        testContext.setupSuccessfulHCaptcha("test-token")

        testContext.service.performPassiveHCaptcha(
            testContext.activity,
            siteKey = "test-site-key",
            rqData = null
        )

        verify(testContext.hCaptcha).reset()
    }

    @Test
    fun `performPassiveHCaptcha calls reset after failed completion`() = runTest {
        val testContext = createTestContext()
        val exception = HCaptchaException(HCaptchaError.NETWORK_ERROR)
        testContext.setupFailedHCaptcha(exception)

        testContext.service.performPassiveHCaptcha(
            testContext.activity,
            siteKey = "test-site-key",
            rqData = null
        )

        verify(testContext.hCaptcha).reset()
    }

    @Test
    fun `performPassiveHCaptcha calls reset on coroutine cancellation`() = runTest {
        val testContext = createTestContext()
        testContext.setupHangingHCaptcha()

        val job = launch {
            testContext.service.performPassiveHCaptcha(
                testContext.activity,
                siteKey = "test-site-key",
                rqData = null
            )
        }

        testContext.hCaptchaProvider.awaitCall()
        job.cancel()
        job.join()

        verify(testContext.hCaptcha, times(2)).reset()
    }

    @Test
    fun `performPassiveHCaptcha returns failure when startVerification throws exception`() = runTest {
        val testContext = createTestContext()
        val expectedException = RuntimeException("Test exception")
        val siteKey = "test-site-key"
        testContext.setupExceptionDuringSetup(expectedException)

        val result = testContext.service.performPassiveHCaptcha(
            testContext.activity,
            siteKey = siteKey,
            rqData = null
        )

        assertThat(result).isInstanceOf(HCaptchaService.Result.Failure::class.java)
        assertThat((result as HCaptchaService.Result.Failure).error).isEqualTo(expectedException)
        verify(testContext.hCaptcha).reset()

        // Verify analytics calls - setup failure happens before execute
        assertThat(testContext.captchaEventsReporter.awaitCall())
            .isEqualTo(FakeCaptchaEventsReporter.Call.Init(siteKey))
        assertThat(testContext.captchaEventsReporter.awaitCall())
            .isEqualTo(FakeCaptchaEventsReporter.Call.Error(expectedException, siteKey))
        testContext.captchaEventsReporter.ensureAllEventsConsumed()
    }

    private fun createTestContext(): TestContext {
        val activity = mock<FragmentActivity>()
        val hCaptcha = mock<HCaptcha>()
        val hCaptchaProvider = FakeHCaptchaProvider(hCaptcha)
        val captchaEventsReporter = FakeCaptchaEventsReporter()
        val service = DefaultHCaptchaService(hCaptchaProvider, captchaEventsReporter)

        return TestContext(
            activity = activity,
            hCaptcha = hCaptcha,
            captchaEventsReporter = captchaEventsReporter,
            hCaptchaProvider = hCaptchaProvider,
            service = service
        )
    }

    private fun TestContext.setupHCaptchaChaining() {
        whenever(hCaptcha.addOnSuccessListener(any())).doReturn(hCaptcha)
        whenever(hCaptcha.addOnFailureListener(any())).doReturn(hCaptcha)
        whenever(hCaptcha.setup(any(), any())).doReturn(hCaptcha)
    }

    private fun TestContext.setupSuccessfulHCaptcha(token: String = "test-token") {
        setupHCaptchaChaining()

        whenever(hCaptcha.verifyWithHCaptcha(any())).then {
            val successCaptor = argumentCaptor<OnSuccessListener<HCaptchaTokenResponse>>()
            verify(hCaptcha).addOnSuccessListener(successCaptor.capture())
            successCaptor.firstValue.onSuccess(createHCaptchaTokenResponse(token))
            hCaptcha
        }
    }

    private fun TestContext.setupFailedHCaptcha(exception: HCaptchaException) {
        setupHCaptchaChaining()

        whenever(hCaptcha.verifyWithHCaptcha(any())).then {
            val failureCaptor = argumentCaptor<OnFailureListener>()
            verify(hCaptcha).addOnFailureListener(failureCaptor.capture())
            failureCaptor.firstValue.onFailure(exception)
            hCaptcha
        }
    }

    private fun TestContext.setupHangingHCaptcha() {
        setupHCaptchaChaining()
        whenever(hCaptcha.verifyWithHCaptcha(any())).doReturn(hCaptcha)
    }

    private fun TestContext.setupExceptionDuringSetup(exception: Throwable) {
        whenever(hCaptcha.addOnSuccessListener(any())).doReturn(hCaptcha)
        whenever(hCaptcha.addOnFailureListener(any())).doReturn(hCaptcha)
        whenever(hCaptcha.setup(any(), any())).thenThrow(exception)
    }

    private fun createHCaptchaTokenResponse(token: String): HCaptchaTokenResponse {
        return HCaptchaTokenResponse(token, mock<Handler>())
    }

    private data class TestContext(
        val activity: FragmentActivity,
        val hCaptcha: HCaptcha,
        val captchaEventsReporter: FakeCaptchaEventsReporter,
        val hCaptchaProvider: FakeHCaptchaProvider,
        val service: DefaultHCaptchaService
    )

    private class FakeCaptchaEventsReporter : CaptchaEventsReporter {
        private val calls = Turbine<Call>()

        override fun init(siteKey: String) {
            calls.add(Call.Init(siteKey))
        }

        override fun execute(siteKey: String) {
            calls.add(Call.Execute(siteKey))
        }

        override fun success(siteKey: String) {
            calls.add(Call.Success(siteKey))
        }

        override fun error(error: Throwable?, siteKey: String) {
            calls.add(Call.Error(error, siteKey))
        }

        suspend fun awaitCall(): Call = calls.awaitItem()

        fun ensureAllEventsConsumed() {
            calls.ensureAllEventsConsumed()
        }

        sealed interface Call {
            val siteKey: String

            data class Init(override val siteKey: String) : Call
            data class Execute(override val siteKey: String) : Call
            data class Success(override val siteKey: String) : Call
            data class Error(val error: Throwable?, override val siteKey: String) : Call
        }
    }

    private class FakeHCaptchaProvider(
        private val hCaptcha: HCaptcha
    ) : HCaptchaProvider {
        private val calls = Turbine<Unit>()

        override fun get(): HCaptcha {
            calls.add(Unit)
            return hCaptcha
        }

        suspend fun awaitCall() {
            calls.awaitItem()
        }
    }
}
