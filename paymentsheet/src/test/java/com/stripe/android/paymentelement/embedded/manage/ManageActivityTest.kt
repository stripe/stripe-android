package com.stripe.android.paymentelement.embedded.manage

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ManageActivityTest {

    @Test
    fun `when launched without args should finish with cancelled result`() {
        ActivityScenario.launchActivityForResult(
            ManageActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            val result = ManageContract.parseResult(0, activityScenario.result.resultData)
            assertThat(result).isInstanceOf(ManageResult.Cancelled::class.java)
        }
    }
}
