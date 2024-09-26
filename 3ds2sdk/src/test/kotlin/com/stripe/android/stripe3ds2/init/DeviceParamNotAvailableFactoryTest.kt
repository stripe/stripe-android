package com.stripe.android.stripe3ds2.init

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DeviceParamNotAvailableFactoryTest {

    @Test
    fun create_withDefaults_shouldHaveTheCorrectNumberOfEntries() {
        assertThat(
            DeviceParamNotAvailableFactoryImpl().create()
        ).hasSize(155)
    }

    @Test
    fun marketOrRegionRestrictionParams_shouldBeEmpty() {
        assertThat(
            DeviceParamNotAvailableFactoryImpl(
                Build.VERSION_CODES.P
            ).marketOrRegionRestrictionParams
        ).hasSize(145)
    }

    @Test
    fun platformVersionParams_whenApiIs25_shouldHaveTheCorrectNumberOfEntries() {
        assertThat(
            DeviceParamNotAvailableFactoryImpl(
                Build.VERSION_CODES.N_MR1
            ).platformVersionParams
        ).hasSize(17)
    }

    @Test
    fun platformVersionParams_whenApiIs28_shouldHaveTheCorrectNumberOfEntries() {
        assertThat(
            DeviceParamNotAvailableFactoryImpl(
                Build.VERSION_CODES.P
            ).platformVersionParams
        ).hasSize(11)
    }

    @Test
    fun permissionParams_shouldHaveTheCorrectNumberOfEntries() {
        assertThat(
            DeviceParamNotAvailableFactoryImpl(
                Build.VERSION_CODES.P
            ).permissionParams
        ).hasSize(33)
    }

    @Test
    fun create_withoutHardwareId_shouldHaveTheCorrectNumberOfEntries() {
        assertThat(
            DeviceParamNotAvailableFactoryImpl(Build.VERSION.SDK_INT).create()
        ).hasSize(155)
    }
}
