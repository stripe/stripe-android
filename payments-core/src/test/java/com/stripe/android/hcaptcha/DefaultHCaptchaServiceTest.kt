package com.stripe.android.hcaptcha

import android.os.Handler
import androidx.fragment.app.FragmentActivity
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.hcaptcha.DefaultHCaptchaServiceTest.FakeHCaptchaProvider.HCaptchaHandler
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
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class DefaultHCaptchaServiceTest {

    @Test
    fun `performPassiveHCaptcha calls hCaptchaProvider`() = runTest {
        TestContext.test {
            hCaptchaProvider.hCaptchaHandler = SetupSuccessfulHCaptcha()

            service.performPassiveHCaptcha(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )

            hCaptchaProvider.awaitCall()
        }
    }

    @Test
    fun `performPassiveHCaptcha configures HCaptcha with correct settings`() = runTest {
        TestContext.test {
            hCaptchaProvider.hCaptchaHandler = SetupSuccessfulHCaptcha()

            service.performPassiveHCaptcha(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = TEST_RQ_DATA
            )

            val configCaptor = argumentCaptor<HCaptchaConfig>()
            verify(hCaptchaProvider.awaitCall()).setup(any(), configCaptor.capture())
            with(configCaptor.firstValue) {
                assertThat(siteKey).isEqualTo(TEST_SITE_KEY)
                assertThat(size).isEqualTo(HCaptchaSize.INVISIBLE)
                assertThat(rqdata).isEqualTo(TEST_RQ_DATA)
                assertThat(loading).isFalse()
                assertThat(hideDialog).isTrue()
                assertThat(disableHardwareAcceleration).isTrue()
                assertThat(host).isEqualTo("stripecdn.com")
            }
        }
    }

    @Test
    fun `performPassiveHCaptcha returns success when HCaptcha succeeds`() = runTest {
        TestContext.test {
            hCaptchaProvider.hCaptchaHandler = SetupSuccessfulHCaptcha()

            val result = service.performPassiveHCaptcha(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )

            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Init(TEST_SITE_KEY))
            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Execute(TEST_SITE_KEY))
            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Success(TEST_SITE_KEY))
            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Attach(isReady = false, TEST_SITE_KEY))

            assertThat(result).isInstanceOf(HCaptchaService.Result.Success::class.java)
            assertThat((result as HCaptchaService.Result.Success).token).isEqualTo("token")

            captchaEventsReporter.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `performPassiveHCaptcha returns failure when HCaptcha fails`() = runTest {
        TestContext.test {
            val exception = HCaptchaException(HCaptchaError.NETWORK_ERROR)
            hCaptchaProvider.hCaptchaHandler = SetupFailedHCaptcha(exception)

            val result = service.performPassiveHCaptcha(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )

            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Init(TEST_SITE_KEY))
            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Execute(TEST_SITE_KEY))
            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Error(exception, TEST_SITE_KEY))
            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Attach(isReady = false, TEST_SITE_KEY))

            assertThat(result).isInstanceOf(HCaptchaService.Result.Failure::class.java)
            assertThat((result as HCaptchaService.Result.Failure).error).isEqualTo(exception)

            captchaEventsReporter.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `performPassiveHCaptcha calls reset after successful completion`() = runTest {
        TestContext.test {
            hCaptchaProvider.hCaptchaHandler = SetupSuccessfulHCaptcha()

            service.performPassiveHCaptcha(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )
            verify(hCaptchaProvider.awaitCall()).reset()
        }
    }

    @Test
    fun `performPassiveHCaptcha calls reset after failed completion`() = runTest {
        TestContext.test {
            val exception = HCaptchaException(HCaptchaError.NETWORK_ERROR)
            hCaptchaProvider.hCaptchaHandler = SetupFailedHCaptcha(exception)

            service.performPassiveHCaptcha(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )

            verify(hCaptchaProvider.awaitCall()).reset()
        }
    }

    @Test
    fun `performPassiveHCaptcha calls reset on coroutine cancellation`() = runTest {
        TestContext.test {
            hCaptchaProvider.hCaptchaHandler = SetupHangingHCaptcha

            val job = launch {
                service.performPassiveHCaptcha(
                    activity,
                    siteKey = TEST_SITE_KEY,
                    rqData = null
                )
            }

            val hCaptcha = hCaptchaProvider.awaitCall()
            job.cancel()
            job.join()

            verify(hCaptcha, times(2)).reset()
        }
    }

    @Test
    fun `performPassiveHCaptcha returns failure when startVerification throws exception`() = runTest {
        TestContext.test {
            val expectedException = RuntimeException("Test exception")
            hCaptchaProvider.hCaptchaHandler = SetupExceptionDuringSetup(expectedException)

            val result = service.performPassiveHCaptcha(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )

            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Init(TEST_SITE_KEY))
            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Error(expectedException, TEST_SITE_KEY))
            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Attach(isReady = false, TEST_SITE_KEY))

            assertThat(result).isInstanceOf(HCaptchaService.Result.Failure::class.java)
            assertThat((result as HCaptchaService.Result.Failure).error).isEqualTo(expectedException)
            verify(hCaptchaProvider.awaitCall(), atLeastOnce()).reset()

            captchaEventsReporter.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `performPassiveHCaptcha clears cache to Idle state after successful completion`() = runTest {
        TestContext.test {
            hCaptchaProvider.hCaptchaHandler = SetupSuccessfulHCaptcha()

            // First warmUp to populate cache with Success state
            service.warmUp(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )
            hCaptchaProvider.awaitCall()

            // Perform the passive hCaptcha which should clear cache
            service.performPassiveHCaptcha(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )

            // Verify a subsequent warmUp call goes through (would be skipped if cache wasn't cleared)
            hCaptchaProvider.hCaptchaHandler = SetupSuccessfulHCaptcha("new-token")
            service.warmUp(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )
            hCaptchaProvider.awaitCall() // This would not be called if cache wasn't cleared

            hCaptchaProvider.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `performPassiveHCaptcha clears cache to Idle state after failed completion`() = runTest {
        TestContext.test {
            val exception = HCaptchaException(HCaptchaError.NETWORK_ERROR)
            hCaptchaProvider.hCaptchaHandler = SetupFailedHCaptcha(exception)

            // First warmUp to populate cache with Failure state
            service.warmUp(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )
            hCaptchaProvider.awaitCall()

            // Perform the passive hCaptcha which should clear cache
            service.performPassiveHCaptcha(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )

            // Verify a subsequent warmUp call goes through (would be skipped if cache wasn't cleared)
            hCaptchaProvider.hCaptchaHandler = SetupSuccessfulHCaptcha("recovery-token")
            service.warmUp(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )
            hCaptchaProvider.awaitCall() // This would not be called if cache wasn't cleared

            hCaptchaProvider.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `performPassiveHCaptcha clears cache to Idle state after timeout exception`() = runTest {
        TestContext.test {
            // Setup hCaptcha that will hang (causing timeout)
            hCaptchaProvider.hCaptchaHandler = SetupHangingHCaptcha

            // Perform passive hCaptcha that should timeout and clear cache
            val result = service.performPassiveHCaptcha(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )

            assertThat(result).isInstanceOf(HCaptchaService.Result.Failure::class.java)

            // Verify a subsequent warmUp call goes through (would be skipped if cache wasn't cleared)
            hCaptchaProvider.hCaptchaHandler = SetupSuccessfulHCaptcha("recovery-token")
            service.warmUp(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )
            hCaptchaProvider.awaitCall() // This would not be called if cache wasn't cleared
        }
    }

    @Test
    fun `warmUp calls hCaptchaProvider and updates cache on success`() = runTest {
        TestContext.test {
            val expectedToken = "warm-up-token"
            hCaptchaProvider.hCaptchaHandler = SetupSuccessfulHCaptcha(expectedToken)

            service.warmUp(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = TEST_RQ_DATA
            )

            hCaptchaProvider.awaitCall()

            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Init(TEST_SITE_KEY))
            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Execute(TEST_SITE_KEY))
            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Success(TEST_SITE_KEY))

            val result = service.performPassiveHCaptcha(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = TEST_RQ_DATA
            )

            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Attach(isReady = true, TEST_SITE_KEY))

            assertThat(result).isInstanceOf(HCaptchaService.Result.Success::class.java)
            assertThat((result as HCaptchaService.Result.Success).token).isEqualTo(expectedToken)

            captchaEventsReporter.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `warmUp updates cache on failure`() = runTest {
        TestContext.test {
            val expectedException = HCaptchaException(HCaptchaError.NETWORK_ERROR)
            hCaptchaProvider.hCaptchaHandler = SetupFailedHCaptcha(expectedException)

            service.warmUp(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )

            hCaptchaProvider.awaitCall()

            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Init(TEST_SITE_KEY))
            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Execute(TEST_SITE_KEY))
            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Error(expectedException, TEST_SITE_KEY))

            val result = service.performPassiveHCaptcha(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )

            assertThat(captchaEventsReporter.awaitCall())
                .isEqualTo(FakeCaptchaEventsReporter.Call.Attach(isReady = true, TEST_SITE_KEY))

            assertThat(result).isInstanceOf(HCaptchaService.Result.Failure::class.java)
            assertThat((result as HCaptchaService.Result.Failure).error).isEqualTo(expectedException)
            captchaEventsReporter.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `warmUp skips execution when cache is in Loading state`() = runTest {
        TestContext.test {
            hCaptchaProvider.hCaptchaHandler = SetupHangingHCaptcha

            // Start first warmUp that will hang (Loading state)
            val firstWarmUpJob = launch {
                service.warmUp(
                    activity,
                    siteKey = TEST_SITE_KEY,
                    rqData = null
                )
            }

            val firstHCaptcha = hCaptchaProvider.awaitCall()

            service.warmUp(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )

            hCaptchaProvider.ensureAllEventsConsumed()

            verify(firstHCaptcha).verifyWithHCaptcha(any())

            firstWarmUpJob.cancel()
            firstWarmUpJob.join()
        }
    }

    @Test
    fun `warmUp skips execution when cache has Success state`() = runTest {
        TestContext.test {
            hCaptchaProvider.hCaptchaHandler = SetupSuccessfulHCaptcha()

            service.warmUp(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )

            hCaptchaProvider.awaitCall()

            service.warmUp(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )

            hCaptchaProvider.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `warmUp allows execution when cache is in Failure state`() = runTest {
        TestContext.test {
            val exception = HCaptchaException(HCaptchaError.NETWORK_ERROR)
            hCaptchaProvider.hCaptchaHandler = SetupFailedHCaptcha(exception)

            service.warmUp(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )

            hCaptchaProvider.awaitCall()

            hCaptchaProvider.hCaptchaHandler = SetupSuccessfulHCaptcha()

            service.warmUp(
                activity,
                siteKey = TEST_SITE_KEY,
                rqData = null
            )

            val secondHCaptcha = hCaptchaProvider.awaitCall()
            verify(secondHCaptcha).verifyWithHCaptcha(any())
        }
    }

    private object SetupHCaptchaChaining : HCaptchaHandler {
        override fun handle(hCaptcha: HCaptcha) {
            whenever(hCaptcha.addOnSuccessListener(any())).doReturn(hCaptcha)
            whenever(hCaptcha.addOnFailureListener(any())).doReturn(hCaptcha)
            whenever(hCaptcha.setup(any(), any())).doReturn(hCaptcha)
        }
    }

    private class SetupSuccessfulHCaptcha(private val token: String = "token") : HCaptchaHandler {
        override fun handle(hCaptcha: HCaptcha) {
            SetupHCaptchaChaining.handle(hCaptcha)

            whenever(hCaptcha.verifyWithHCaptcha(any())).then {
                val successCaptor = argumentCaptor<OnSuccessListener<HCaptchaTokenResponse>>()
                verify(hCaptcha).addOnSuccessListener(successCaptor.capture())
                successCaptor.firstValue.onSuccess(createHCaptchaTokenResponse(token))
                hCaptcha
            }
        }

        private fun createHCaptchaTokenResponse(token: String): HCaptchaTokenResponse {
            return HCaptchaTokenResponse(token, mock<Handler>())
        }
    }

    private object SetupHangingHCaptcha : HCaptchaHandler {
        override fun handle(hCaptcha: HCaptcha) {
            SetupHCaptchaChaining.handle(hCaptcha)

            whenever(hCaptcha.verifyWithHCaptcha(any())).doReturn(hCaptcha)
        }
    }

    private class SetupExceptionDuringSetup(private val exception: Throwable) : HCaptchaHandler {
        override fun handle(hCaptcha: HCaptcha) {
            whenever(hCaptcha.addOnSuccessListener(any())).doReturn(hCaptcha)
            whenever(hCaptcha.addOnFailureListener(any())).doReturn(hCaptcha)
            whenever(hCaptcha.setup(any(), any())).thenThrow(exception)
        }
    }

    private class SetupFailedHCaptcha(private val exception: HCaptchaException) : HCaptchaHandler {
        override fun handle(hCaptcha: HCaptcha) {
            SetupHCaptchaChaining.handle(hCaptcha)

            whenever(hCaptcha.verifyWithHCaptcha(any())).then {
                val failureCaptor = argumentCaptor<OnFailureListener>()
                verify(hCaptcha).addOnFailureListener(failureCaptor.capture())
                failureCaptor.firstValue.onFailure(exception)
                hCaptcha
            }
        }
    }

    private data class TestContext private constructor(
        val activity: FragmentActivity,
        val hCaptchaProvider: FakeHCaptchaProvider,
        val service: DefaultHCaptchaService,
        val captchaEventsReporter: FakeCaptchaEventsReporter
    ) {

        companion object {
            suspend fun test(
                activity: FragmentActivity = mock(),
                hCaptchaProvider: FakeHCaptchaProvider = FakeHCaptchaProvider(),
                captchaEventsReporter: FakeCaptchaEventsReporter = FakeCaptchaEventsReporter(),
                service: DefaultHCaptchaService = DefaultHCaptchaService(hCaptchaProvider, captchaEventsReporter),
                block: suspend TestContext.() -> Unit
            ) {
                TestContext(
                    activity = activity,
                    hCaptchaProvider = hCaptchaProvider,
                    service = service,
                    captchaEventsReporter = captchaEventsReporter

                ).apply { block(this) }
            }
        }
    }

    private class FakeHCaptchaProvider : HCaptchaProvider {
        var hCaptchaHandler: HCaptchaHandler = HCaptchaHandler {}
        private val calls = Turbine<HCaptcha>()

        override fun get(): HCaptcha {
            val hCaptcha = mock<HCaptcha>()
            hCaptchaHandler.handle(hCaptcha)
            calls.add(hCaptcha)
            return hCaptcha
        }

        suspend fun awaitCall(): HCaptcha {
            return calls.awaitItem()
        }

        fun ensureAllEventsConsumed() = calls.ensureAllEventsConsumed()

        fun interface HCaptchaHandler {
            fun handle(hCaptcha: HCaptcha)
        }
    }

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

        override fun attach(siteKey: String, isReady: Boolean) {
            calls.add(Call.Attach(isReady, siteKey))
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
            data class Attach(val isReady: Boolean, override val siteKey: String) : Call
        }
    }

    companion object {
        private const val TEST_SITE_KEY = "test-site-key"
        private const val TEST_RQ_DATA = "test-rq-data"
    }
}
