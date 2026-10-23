package com.stripe.android.hcaptcha

import androidx.fragment.app.FragmentActivity
import com.google.common.truth.Truth.assertThat
import com.stripe.android.hcaptcha.analytics.FakeCaptchaEventsReporter
import com.stripe.android.hcaptcha.analytics.FakeCaptchaEventsReporter.Call
import com.stripe.android.hcaptcha.analytics.ObservableCaptchaEventsReporter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ReportingHCaptchaServiceTest {
    @Test
    fun `warmUp forwards arguments and reports all synchronous events`() = runScenario {
        delegate.warmUpResult = {
            observableReporter.init(SITE_KEY)
            observableReporter.execute(SITE_KEY)
            observableReporter.success(SITE_KEY)
        }

        service.warmUp(activity, SITE_KEY, RQ_DATA)

        assertThat(delegate.awaitWarmUpCall()).isEqualTo(
            FakeHCaptchaService.Call(activity, SITE_KEY, RQ_DATA, null)
        )
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.Init(SITE_KEY))
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.Execute(SITE_KEY))
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.Success(SITE_KEY))
    }

    @Test
    fun `performPassiveHCaptcha preserves the result and timeout and reports attach events`() = runScenario {
        val expectedResult = HCaptchaService.Result.Success("token")
        delegate.performPassiveHCaptchaResult = {
            observableReporter.attachStart()
            observableReporter.attachEnd(SITE_KEY, isReady = true)
            expectedResult
        }

        val result = service.performPassiveHCaptcha(activity, SITE_KEY, RQ_DATA, 30)

        assertThat(result).isSameInstanceAs(expectedResult)
        assertThat(delegate.awaitPerformPassiveHCaptchaCall()).isEqualTo(
            FakeHCaptchaService.Call(activity, SITE_KEY, RQ_DATA, 30)
        )
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.AttachStart)
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.AttachEnd(SITE_KEY, isReady = true))
    }

    @Test
    fun `performPassiveHCaptcha preserves failures and nullable arguments`() = runScenario {
        val error = IllegalStateException("Failed")
        val expectedResult = HCaptchaService.Result.Failure(error)
        delegate.performPassiveHCaptchaResult = {
            observableReporter.error(error, SITE_KEY)
            expectedResult
        }

        val result = service.performPassiveHCaptcha(activity, SITE_KEY, null, null)

        assertThat(result).isSameInstanceAs(expectedResult)
        assertThat(delegate.awaitPerformPassiveHCaptchaCall()).isEqualTo(
            FakeHCaptchaService.Call(activity, SITE_KEY, null, null)
        )
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.Error(error, SITE_KEY))
    }

    @Test
    fun `reports events while retrieval is still suspended`() = runScenario {
        val completion = CompletableDeferred<Unit>()
        delegate.warmUpResult = {
            observableReporter.init(SITE_KEY)
            completion.await()
            observableReporter.success(SITE_KEY)
        }

        val job = testScope.launch { service.warmUp(activity, SITE_KEY, null) }
        testScope.runCurrent()

        delegate.awaitWarmUpCall()
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.Init(SITE_KEY))
        assertThat(job.isCompleted).isFalse()

        completion.complete(Unit)
        job.join()
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.Success(SITE_KEY))
    }

    @Test
    fun `reports events immediately to preserve analytics timing`() = runScenario {
        delegate.warmUpResult = {
            observableReporter.init(SITE_KEY)
            assertThat(reporter.calls.expectMostRecentItem()).isEqualTo(Call.Init(SITE_KEY))
            observableReporter.success(SITE_KEY)
            assertThat(reporter.calls.expectMostRecentItem()).isEqualTo(Call.Success(SITE_KEY))
        }

        service.warmUp(activity, SITE_KEY, null)

        delegate.awaitWarmUpCall()
    }

    @Test
    fun `cancellation flushes final events and stops observing`() = runScenario {
        val error = IllegalStateException("Cancelled")
        delegate.warmUpResult = {
            try {
                awaitCancellation()
            } finally {
                observableReporter.error(error, SITE_KEY)
            }
        }

        val job = testScope.launch { service.warmUp(activity, SITE_KEY, null) }
        testScope.runCurrent()
        delegate.awaitWarmUpCall()
        job.cancelAndJoin()

        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.Error(error, SITE_KEY))
        observableReporter.init(SITE_KEY)
        testScope.runCurrent()
        reporter.calls.expectNoEvents()
    }

    @Test
    fun `throwing delegate flushes events and propagates the exception`() = runScenario {
        val error = IllegalStateException("Failed")
        delegate.warmUpResult = {
            observableReporter.error(error, SITE_KEY)
            throw error
        }

        val result = runCatching { service.warmUp(activity, SITE_KEY, null) }

        delegate.awaitWarmUpCall()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(result.exceptionOrNull()).hasMessageThat().isEqualTo(error.message)
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.Error(error, SITE_KEY))
    }

    @Test
    fun `wrappers use their own reporter when sharing a retrieval service`() = runScenario {
        val secondReporter = FakeCaptchaEventsReporter()
        val secondService = ReportingHCaptchaService(delegate, observableReporter, secondReporter)
        delegate.warmUpResult = { observableReporter.init(SITE_KEY) }

        service.warmUp(activity, SITE_KEY, null)
        delegate.awaitWarmUpCall()
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.Init(SITE_KEY))

        secondService.warmUp(activity, SITE_KEY, null)
        delegate.awaitWarmUpCall()
        assertThat(secondReporter.calls.awaitItem()).isEqualTo(Call.Init(SITE_KEY))
        secondReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `reobserving through the same wrapper does not report previously consumed events`() = runScenario {
        delegate.warmUpResult = {
            observableReporter.init(SITE_KEY)
            observableReporter.execute(SITE_KEY)
            observableReporter.success(SITE_KEY)
        }

        service.warmUp(activity, SITE_KEY, null)
        delegate.awaitWarmUpCall()
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.Init(SITE_KEY))
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.Execute(SITE_KEY))
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.Success(SITE_KEY))

        service.performPassiveHCaptcha(activity, SITE_KEY, null, null)
        delegate.awaitPerformPassiveHCaptchaCall()

        reporter.calls.expectNoEvents()
    }

    @Test
    fun `new wrapper only reports events buffered since the previous observation`() = runScenario {
        val secondReporter = FakeCaptchaEventsReporter()
        val secondService = ReportingHCaptchaService(delegate, observableReporter, secondReporter)
        delegate.warmUpResult = { observableReporter.init(SITE_KEY) }

        service.warmUp(activity, SITE_KEY, null)
        delegate.awaitWarmUpCall()
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.Init(SITE_KEY))

        observableReporter.execute(SITE_KEY)
        observableReporter.success(SITE_KEY)

        secondService.performPassiveHCaptcha(activity, SITE_KEY, null, null)
        delegate.awaitPerformPassiveHCaptchaCall()
        assertThat(secondReporter.calls.awaitItem()).isEqualTo(Call.Execute(SITE_KEY))
        assertThat(secondReporter.calls.awaitItem()).isEqualTo(Call.Success(SITE_KEY))

        secondService.performPassiveHCaptcha(activity, SITE_KEY, null, null)
        delegate.awaitPerformPassiveHCaptchaCall()
        secondReporter.calls.expectNoEvents()
        secondReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `overlapping warmUp and retrieval report each shared event once`() = runScenario {
        val completion = CompletableDeferred<Unit>()
        val secondService = ReportingHCaptchaService(delegate, observableReporter, reporter)
        delegate.warmUpResult = {
            observableReporter.init(SITE_KEY)
            completion.await()
            observableReporter.success(SITE_KEY)
        }
        delegate.performPassiveHCaptchaResult = {
            observableReporter.attachStart()
            observableReporter.attachEnd("second_site_key", isReady = false)
            HCaptchaService.Result.Success("token")
        }

        val warmUp = testScope.launch { service.warmUp(activity, SITE_KEY, null) }
        testScope.runCurrent()
        delegate.awaitWarmUpCall()
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.Init(SITE_KEY))

        secondService.performPassiveHCaptcha(activity, "second_site_key", null, null)
        delegate.awaitPerformPassiveHCaptchaCall()
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.AttachStart)
        assertThat(reporter.calls.awaitItem()).isEqualTo(
            Call.AttachEnd("second_site_key", isReady = false)
        )

        completion.complete(Unit)
        warmUp.join()
        assertThat(reporter.calls.awaitItem()).isEqualTo(Call.Success(SITE_KEY))
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val delegate = FakeHCaptchaService()
        val observableReporter = ObservableCaptchaEventsReporter()
        val reporter = FakeCaptchaEventsReporter()
        val service = ReportingHCaptchaService(delegate, observableReporter, reporter)

        Scenario(this, mock(), delegate, observableReporter, reporter, service).block()

        delegate.ensureAllEventsConsumed()
        reporter.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val testScope: TestScope,
        val activity: FragmentActivity,
        val delegate: FakeHCaptchaService,
        val observableReporter: ObservableCaptchaEventsReporter,
        val reporter: FakeCaptchaEventsReporter,
        val service: ReportingHCaptchaService,
    )

    private companion object {
        const val SITE_KEY = "site_key"
        const val RQ_DATA = "rq_data"
    }
}
