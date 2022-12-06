package com.stripe.android.payments

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.stripe.android.auth.PaymentBrowserAuthContract
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripeBrowserLauncherActivityTest {
    private val contract = PaymentBrowserAuthContract()

    @Test
    fun `start with no args should finish with no setResult`() {
        ActivityScenario.launchActivityForResult(
            StripeBrowserLauncherActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state)
                .isEqualTo(Lifecycle.State.DESTROYED)
            val result = parseResult(activityScenario)
            assertThat(result.clientSecret).isNull()
            assertThat(result.flowOutcome).isEqualTo(0)
            assertThat(result.exception).isNull()
            assertThat(result.canCancelSource).isFalse()
            assertThat(result.sourceId).isNull()
            assertThat(result.source).isNull()
            assertThat(result.source).isNull()
            assertThat(result.stripeAccountId).isNull()
        }
    }

    private fun parseResult(
        activityScenario: ActivityScenario<*>
    ): PaymentFlowResult.Unvalidated {
        return contract.parseResult(0, activityScenario.result.resultData)
    }
}
