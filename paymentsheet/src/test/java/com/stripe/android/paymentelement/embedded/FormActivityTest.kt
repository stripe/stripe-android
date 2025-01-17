package com.stripe.android.paymentelement.embedded

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FormActivityTest {

    private lateinit var scenario: ActivityScenario<FormActivity>
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    @Test
    fun `when launched without args should finish with cancelled result`() {
        ActivityScenario.launchActivityForResult(
            FormActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            val result = FormContract().parseResult(0, activityScenario.result.resultData)
            assertThat(result).isInstanceOf(FormResult.Cancelled::class.java)
        }
    }
}
