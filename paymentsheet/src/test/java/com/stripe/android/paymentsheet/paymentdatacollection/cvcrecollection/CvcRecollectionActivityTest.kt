package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CvcRecollectionActivityTest {

    @Test
    fun `activity finishes gracefully when required args are missing`() {
        ActivityScenario.launchActivityForResult<CvcRecollectionActivity>(
            Intent(
                ApplicationProvider.getApplicationContext(),
                CvcRecollectionActivity::class.java
            )
        ).use { scenario ->
            // Activity should finish gracefully without crashing
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }
}
