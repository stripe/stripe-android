package com.stripe.android.stripe3ds2.init

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures.SDK_TRANS_ID
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DeviceDataFactoryImplTest {
    @Test
    fun create_shouldHaveExpectedNumberOfEntries() = runTest {
        assertThat(createFactory().create("1", SDK_TRANS_ID))
            .hasSize(15)
    }

    @Test
    fun create_withEmptyHardwareId_shouldHaveExpectedNumberOfEntries() = runTest {
        assertThat(createFactory().create("1", SDK_TRANS_ID))
            .hasSize(15)
    }

    @Test
    fun create_includesTheCorrectValues() = runTest {
        val data = createFactory().create("1", SDK_TRANS_ID)

        // Only 8 allowed
        assertThat(data[DeviceParam.PARAM_PLATFORM.toString()])
            .isEqualTo("Android")
        assertThat(data[DeviceParam.PARAM_DEVICE_MODEL.toString()])
            .isEqualTo("unknown||robolectric")
        assertThat(data[DeviceParam.PARAM_OS_NAME.toString()])
            .isEqualTo("Android P 9 API 28")
        assertThat(data[DeviceParam.PARAM_OS_VERSION.toString()])
            .isEqualTo("9")
        assertThat(data[DeviceParam.PARAM_LOCALE.toString()])
            .isEqualTo("en-US")

        val timezone = data[DeviceParam.PARAM_TIME_ZONE.toString()] as String?
        assertThat(timezone)
            .isNotEmpty()

        assertThat(data[DeviceParam.PARAM_SCREEN_RESOLUTION.toString()])
            .isEqualTo("470x320")
    }

    private fun createFactory(): DeviceDataFactory {
        val context = ApplicationProvider.getApplicationContext<Context>()

        return DeviceDataFactoryImpl(context = context, FakeAppInfoRepository(), MessageVersionRegistry())
    }
}
