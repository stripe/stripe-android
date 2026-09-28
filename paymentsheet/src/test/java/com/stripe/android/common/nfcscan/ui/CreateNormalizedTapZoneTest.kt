package com.stripe.android.common.nfcscan.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.tapzone.TapZone
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class CreateNormalizedTapZoneTest(
    private val rotation: DeviceRotation,
    private val tapZone: TapZone,
    private val expectedTapZone: TapZone,
) {
    @Test
    fun `createNormalizedTapZone maps tap zone coordinates for device rotation`() {
        assertThat(createNormalizedTapZone(rotation, tapZone)).isEqualTo(expectedTapZone)
    }

    private companion object {
        private val tapZone = TapZone(xBias = 0.25f, yBias = 0.75f)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): List<Array<Any>> = listOf(
            arrayOf(
                DeviceRotation.Portrait,
                tapZone,
                TapZone(xBias = 0.25f, yBias = 0.75f),
            ),
            arrayOf(
                DeviceRotation.UpsideDown,
                tapZone,
                TapZone(xBias = 0.75f, yBias = 0.25f),
            ),
            arrayOf(
                DeviceRotation.LandscapeLeft,
                tapZone,
                TapZone(xBias = 0.75f, yBias = 0.75f),
            ),
            arrayOf(
                DeviceRotation.LandscapeRight,
                tapZone,
                TapZone(xBias = 0.25f, yBias = 0.25f),
            ),
        )
    }
}
