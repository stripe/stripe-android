package com.stripe.android.hcaptcha.analytics

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.hcaptcha.analytics.FakeCaptchaEventsReporter.Call
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DeferringCaptchaEventsReporterTest {
    @Test
    fun `buffers callbacks in order without losing repeated events`() = runTest {
        val reporter = DeferringCaptchaEventsReporter()
        val realReporter = FakeCaptchaEventsReporter()
        val error = IllegalStateException("Failed")

        reporter.events.test {
            assertThat(awaitItem()).isEmpty()

            reporter.attachStart()
            assertThat(awaitItem()).hasSize(1)
            reporter.init("site_key")
            assertThat(awaitItem()).hasSize(2)
            reporter.execute("site_key")
            assertThat(awaitItem()).hasSize(3)
            reporter.execute("site_key")
            assertThat(awaitItem()).hasSize(4)
            reporter.success("site_key")
            assertThat(awaitItem()).hasSize(5)
            reporter.error(error, "site_key")
            assertThat(awaitItem()).hasSize(6)
            reporter.attachEnd("site_key", isReady = true)
            assertThat(awaitItem()).hasSize(7)

            reporter.reportTo(realReporter)
            assertThat(awaitItem()).isEmpty()
            assertThat(realReporter.calls.awaitItem()).isEqualTo(Call.AttachStart)
            assertThat(realReporter.calls.awaitItem()).isEqualTo(Call.Init("site_key"))
            assertThat(realReporter.calls.awaitItem()).isEqualTo(Call.Execute("site_key"))
            assertThat(realReporter.calls.awaitItem()).isEqualTo(Call.Execute("site_key"))
            assertThat(realReporter.calls.awaitItem()).isEqualTo(Call.Success("site_key"))
            assertThat(realReporter.calls.awaitItem()).isEqualTo(Call.Error(error, "site_key"))
            assertThat(realReporter.calls.awaitItem()).isEqualTo(Call.AttachEnd("site_key", isReady = true))
        }

        realReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `consumed events are not replayed to another reporter`() = runTest {
        val reporter = DeferringCaptchaEventsReporter()
        val firstReporter = FakeCaptchaEventsReporter()
        val secondReporter = FakeCaptchaEventsReporter()

        reporter.init("first_site_key")
        reporter.reportTo(firstReporter)
        reporter.error(null, "second_site_key")
        reporter.reportTo(secondReporter)
        reporter.reportTo(secondReporter)

        assertThat(firstReporter.calls.awaitItem()).isEqualTo(Call.Init("first_site_key"))
        assertThat(secondReporter.calls.awaitItem()).isEqualTo(Call.Error(null, "second_site_key"))
        firstReporter.ensureAllEventsConsumed()
        secondReporter.ensureAllEventsConsumed()
    }
}
