package com.stripe.android.paymentmethodmessaging.element

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LearnMoreActivityTestTest {

    @Test
    fun `closes if launched without args`() {
        ActivityScenario.launch(
            LearnMoreActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

//    @Test
//    fun `loads url in webview`() {
//        val bundle = Bundle()
//        ActivityScenario.launch(
//            LearnMoreActivity::class.java,
//
//        ).use { activityScenario ->
//            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
//        }
//    }
}