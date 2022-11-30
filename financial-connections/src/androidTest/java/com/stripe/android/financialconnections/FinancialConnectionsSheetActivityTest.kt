package com.stripe.android.financialconnections

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class FinancialConnectionsSheetActivityTest {

    @Test
    fun testActivityIsFinishedWhenNoArgsPassed() {
        ActivityScenario.launch(FinancialConnectionsSheetActivity::class.java)
            .use { scenario ->
                assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            }
    }
}
