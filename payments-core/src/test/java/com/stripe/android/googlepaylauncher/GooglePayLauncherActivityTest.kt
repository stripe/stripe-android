package com.stripe.android.googlepaylauncher

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class GooglePayLauncherActivityTest {
    private val contract = GooglePayLauncherContract()

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `start without args should finish with Error result`() {
        ActivityScenario.launchActivityForResult(
            GooglePayLauncherActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state)
                .isEqualTo(Lifecycle.State.DESTROYED)
            val result = parseResult(activityScenario) as GooglePayLauncher.Result.Failed
            assertThat(result.error.message)
                .isEqualTo(
                    "GooglePayLauncherActivity was started without arguments."
                )
        }
    }

    private fun parseResult(
        activityScenario: ActivityScenario<*>
    ): GooglePayLauncher.Result {
        return contract.parseResult(0, activityScenario.result.resultData)
    }
}
