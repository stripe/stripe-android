package com.stripe.android.common.nfcscan.ui

import android.content.Context
import android.os.Build
import android.view.Surface
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.createComposeCleanupRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplay

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
internal class RememberDeviceRotationTest(
    private val initialDisplayRotation: Int,
    private val initialExpectedRotation: DeviceRotation,
    private val updatedDisplayRotation: Int,
    private val updatedExpectedRotation: DeviceRotation,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    private val display = ShadowDisplay.getDefaultDisplay()
    private val context: Context = ApplicationProvider
        .getApplicationContext<Context>()
        .createDisplayContext(display)

    @Test
    fun `rememberDeviceRotation updates when display rotation changes`() {
        with(composeRule) {
            setDisplayRotation(initialDisplayRotation)

            val observedRotation = mutableStateOf<DeviceRotation?>(null)
            val observedView = mutableStateOf<View?>(null)

            setContent {
                CompositionLocalProvider(LocalContext provides context) {
                    observedView.value = LocalView.current
                    observedRotation.value = rememberDeviceRotation()
                }
            }

            waitForIdle()

            assertThat(observedRotation.value).isEqualTo(initialExpectedRotation)

            setDisplayRotation(updatedDisplayRotation)

            triggerComposeViewLayoutChange(observedView.value)

            waitForIdle()

            assertThat(observedRotation.value).isEqualTo(updatedExpectedRotation)
        }
    }

    private fun setDisplayRotation(rotation: Int) {
        return shadowOf(display).setRotation(rotation)
    }

    private fun ComposeContentTestRule.triggerComposeViewLayoutChange(
        view: View?
    ) {
        assertThat(view).isNotNull()

        with(requireNotNull(view)) {
            runOnUiThread {
                layout(
                    left,
                    top,
                    right + 1,
                    bottom,
                )
            }
        }
    }

    private companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{1}_to_{3}")
        fun parameters(): List<Array<Any>> = listOf(
            arrayOf(
                Surface.ROTATION_0,
                DeviceRotation.Portrait,
                Surface.ROTATION_90,
                DeviceRotation.LandscapeLeft,
            ),
            arrayOf(
                Surface.ROTATION_0,
                DeviceRotation.Portrait,
                Surface.ROTATION_270,
                DeviceRotation.LandscapeRight,
            ),
            arrayOf(
                Surface.ROTATION_90,
                DeviceRotation.LandscapeLeft,
                Surface.ROTATION_270,
                DeviceRotation.LandscapeRight,
            ),
            arrayOf(
                Surface.ROTATION_0,
                DeviceRotation.Portrait,
                Surface.ROTATION_180,
                DeviceRotation.UpsideDown,
            ),
        )
    }
}
