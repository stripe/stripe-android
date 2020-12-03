package com.stripe.android.view

import android.content.Context
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import kotlin.test.BeforeTest
import kotlin.test.Test

@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(RobolectricTestRunner::class)
class ViewWidthAnimatorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val activityScenarioFactory = ActivityScenarioFactory(context)

    private val view: View by lazy {
        activityScenarioFactory.createView { activity ->
            Button(activity).also { button ->
                button.layoutParams = ViewGroup.LayoutParams(START_WIDTH, 100)
            }
        }
    }

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }

    @Test
    fun animate_shouldSetViewWidthToEndWidth() {
        var isAnimationCompleted = false
        ViewWidthAnimator(view, 500L)
            .animate(START_WIDTH, END_WIDTH) {
                isAnimationCompleted = true
            }

        // complete pending animations
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertThat(isAnimationCompleted)
            .isTrue()
        assertThat(view.width)
            .isEqualTo(END_WIDTH)
    }

    private companion object {
        private const val START_WIDTH = 100
        private const val END_WIDTH = 200
    }
}
