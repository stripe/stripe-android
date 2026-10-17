package com.stripe.android.financialconnections.lite

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FinancialConnectionsSheetLiteActivityTest {

    @Test
    fun `activity finishes gracefully when required args are missing`() = runTest {
        ActivityScenario.launchActivityForResult<FinancialConnectionsSheetLiteActivity>(
            Intent(
                ApplicationProvider.getApplicationContext(),
                FinancialConnectionsSheetLiteActivity::class.java
            )
        ).use { scenario ->
            // Activity should finish gracefully without crashing
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }
}
