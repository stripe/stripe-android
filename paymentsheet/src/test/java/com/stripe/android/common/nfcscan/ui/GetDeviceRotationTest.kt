package com.stripe.android.common.nfcscan.ui

import android.content.Context
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplay

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class GetDeviceRotationTest(
    private val displayRotation: Int,
    private val expectedRotation: DeviceRotation,
) {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Config(sdk = [Build.VERSION_CODES.R])
    @Test
    fun `getDeviceRotation returns expected rotation for display rotation for Android R`() {
        val display = ShadowDisplay.getDefaultDisplay()
        shadowOf(display).setRotation(displayRotation)

        val displayContext = context.createDisplayContext(display)

        assertThat(displayContext.getDeviceRotation()).isEqualTo(expectedRotation)
    }

    @Suppress("DEPRECATION")
    @Config(sdk = [Build.VERSION_CODES.Q])
    @Test
    fun `getDeviceRotation returns expected rotation for display rotation for Android Q`() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        shadowOf(windowManager.defaultDisplay).setRotation(displayRotation)

        assertThat(context.getDeviceRotation()).isEqualTo(expectedRotation)
    }

    private companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "rotation={0}")
        fun parameters(): List<Array<Any>> = listOf(
            arrayOf(Surface.ROTATION_0, DeviceRotation.Portrait),
            arrayOf(Surface.ROTATION_90, DeviceRotation.LandscapeLeft),
            arrayOf(Surface.ROTATION_180, DeviceRotation.UpsideDown),
            arrayOf(Surface.ROTATION_270, DeviceRotation.LandscapeRight),
            arrayOf(99, DeviceRotation.Portrait),
        )
    }
}
