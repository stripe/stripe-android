package com.stripe.android.financialconnections

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FinancialConnectionsSheetNativeActivityTest {

    @Test
    fun testActivityIsFinishedWhenNoArgsPassed() {
        ActivityScenario.launch(FinancialConnectionsSheetNativeActivity::class.java)
            .use { scenario ->
                assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            }
    }
}
