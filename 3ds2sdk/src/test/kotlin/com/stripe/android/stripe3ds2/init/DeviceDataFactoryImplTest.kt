package com.stripe.android.stripe3ds2.init

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DeviceDataFactoryImplTest {
    @Test
    fun create_shouldHaveExpectedNumberOfEntries() {
        assertThat(createFactory(HARDWARE_ID_VALUE).create())
            .hasSize(8)
    }

    @Test
    fun create_withEmptyHardwareId_shouldHaveExpectedNumberOfEntries() {
        assertThat(createFactory("").create())
            .hasSize(7)
    }

    @Test
    fun create_includesTheCorrectValues() {
        val data = createFactory(HARDWARE_ID_VALUE).create()

        // Only 8 allowed
        assertThat(data[DeviceParam.PARAM_PLATFORM.toString()])
            .isEqualTo("Android")
        assertThat(data[DeviceParam.PARAM_DEVICE_MODEL.toString()])
            .isEqualTo("robolectric")
        assertThat(data[DeviceParam.PARAM_OS_NAME.toString()])
            .isEqualTo("REL")
        assertThat(data[DeviceParam.PARAM_OS_VERSION.toString()])
            .isEqualTo("9")
        assertThat(data[DeviceParam.PARAM_LOCALE.toString()])
            .isEqualTo("en-US")

        val timezone = data[DeviceParam.PARAM_TIME_ZONE.toString()] as String?
        assertThat(timezone)
            .isNotEmpty()

        assertThat(data[DeviceParam.PARAM_HARDWARE_ID.toString()])
            .isEqualTo(HARDWARE_ID_VALUE)

        assertThat(data[DeviceParam.PARAM_SCREEN_RESOLUTION.toString()])
            .isEqualTo("470x320")
    }

    private fun createFactory(
        hardwareIdValue: String
    ): DeviceDataFactory {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return DeviceDataFactoryImpl(
            context = context
        ) { HardwareId(hardwareIdValue) }
    }

    private companion object {
        private val HARDWARE_ID_VALUE = UUID.randomUUID().toString()
    }
}
