package com.stripe.android.stripe3ds2.init

import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.utils.Supplier
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DeviceParamNotAvailableFactoryTest {

    private val hardwareIdSupplier = Supplier { HardwareId("ad_id") }

    @Test
    fun create_withDefaults_shouldHaveTheCorrectNumberOfEntries() {
        assertThat(
            DeviceParamNotAvailableFactoryImpl(
                hardwareIdSupplier
            ).create()
        ).hasSize(140)
    }

    @Test
    fun marketOrRegionRestrictionParams_shouldBeEmpty() {
        assertThat(
            DeviceParamNotAvailableFactoryImpl(
                Build.VERSION_CODES.P,
                hardwareIdSupplier = hardwareIdSupplier
            ).marketOrRegionRestrictionParams
        ).hasSize(140)
    }

    @Test
    fun platformVersionParams_whenApiIs25_shouldHaveTheCorrectNumberOfEntries() {
        assertThat(
            DeviceParamNotAvailableFactoryImpl(
                Build.VERSION_CODES.N_MR1,
                hardwareIdSupplier = hardwareIdSupplier
            ).platformVersionParams
        ).hasSize(4)
    }

    @Test
    fun platformVersionParams_whenApiIs28_shouldHaveTheCorrectNumberOfEntries() {
        assertThat(
            DeviceParamNotAvailableFactoryImpl(
                Build.VERSION_CODES.P,
                hardwareIdSupplier = hardwareIdSupplier
            ).platformVersionParams
        ).hasSize(1)
    }

    @Test
    fun permissionParams_shouldHaveTheCorrectNumberOfEntries() {
        assertThat(
            DeviceParamNotAvailableFactoryImpl(
                Build.VERSION_CODES.P,
                hardwareIdSupplier = hardwareIdSupplier
            ).permissionParams
        ).hasSize(28)
    }

    @Test
    fun create_withoutHardwareId_shouldHaveTheCorrectNumberOfEntries() {
        assertThat(
            DeviceParamNotAvailableFactoryImpl { HardwareId("") }.create()
        ).hasSize(141)
    }
}
