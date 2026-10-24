package com.stripe.android.hcaptcha

import com.google.common.truth.Truth.assertThat
import com.stripe.android.hcaptcha.analytics.FakeCaptchaEventsReporter
import org.junit.Test

internal class HCaptchaModuleTest {
    @Test
    fun `provider creates a new reporting wrapper for each reporter`() {
        val firstReporter = FakeCaptchaEventsReporter()
        val secondReporter = FakeCaptchaEventsReporter()
        val firstService = HCaptchaModule.provideHCaptchaService(firstReporter)
        val secondService = HCaptchaModule.provideHCaptchaService(secondReporter)

        assertThat(firstService).isInstanceOf(ReportingHCaptchaService::class.java)
        assertThat(secondService).isInstanceOf(ReportingHCaptchaService::class.java)
        assertThat(secondService).isNotSameInstanceAs(firstService)
        firstReporter.ensureAllEventsConsumed()
        secondReporter.ensureAllEventsConsumed()
    }
}
