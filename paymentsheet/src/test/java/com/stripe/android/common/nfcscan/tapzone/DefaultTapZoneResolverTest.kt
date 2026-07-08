package com.stripe.android.common.nfcscan.tapzone

import android.nfc.AvailableNfcAntenna
import android.nfc.NfcAntennaInfo
import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.hardware.FakeNfcHardwareDelegate
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class DefaultTapZoneResolverTest {
    @Test
    fun `get returns mapped tap zone for known samsung device`() = runScenario(
        manufacturer = "samsung",
        model = "SM-S921B",
    ) {
        assertThat(resolver.get()).isEqualTo(TapZone(xBias = 0.5f, yBias = 0.22f))
    }

    @Test
    fun `get standardizes samsung model name with underscore variant`() = runScenario(
        manufacturer = "samsung",
        model = "SM_S906B",
    ) {
        assertThat(resolver.get()).isEqualTo(TapZone(xBias = 0.5f, yBias = 0.5f))
    }

    @Test
    fun `get returns mapped tap zone for known google pixel device`() = runScenario(
        manufacturer = "google",
        model = "Pixel 7",
    ) {
        assertThat(resolver.get()).isEqualTo(TapZone(xBias = 0.5f, yBias = 0.25f))
    }

    @Test
    fun `get returns mapped tap zone for known oneplus device`() = runScenario(
        manufacturer = "oneplus",
        model = "NE2215",
    ) {
        assertThat(resolver.get()).isEqualTo(TapZone(xBias = 0.5f, yBias = 0.2f))
    }

    @Test
    fun `get returns mapped tap zone for known xiaomi device`() = runScenario(
        manufacturer = "xiaomi",
        model = "2201117TG",
    ) {
        assertThat(resolver.get()).isEqualTo(TapZone(xBias = 1f, yBias = 0f))
    }

    @Test
    fun `get normalizes manufacturer casing and whitespace`() = runScenario(
        manufacturer = "  Samsung  ",
        model = "SM-S921B",
    ) {
        assertThat(resolver.get()).isEqualTo(TapZone(xBias = 0.5f, yBias = 0.22f))
    }

    @Test
    fun `get returns default tap zone for unknown device on pre-UPSIDE_DOWN_CAKE sdk`() = runScenario(
        manufacturer = "unknown",
        model = "unknown",
        sdk = Build.VERSION_CODES.TIRAMISU,
    ) {
        assertThat(resolver.get()).isEqualTo(TapZone(xBias = 0.5f, yBias = 0.5f))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun `get returns default tap zone when antenna info is unavailable`() = runScenario(
        manufacturer = "unknown",
        model = "unknown",
        sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        antennaInfo = null,
    ) {
        assertThat(resolver.get()).isEqualTo(TapZone(xBias = 0.5f, yBias = 0.5f))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun `get returns default tap zone when no antennas are available`() = runScenario(
        manufacturer = "unknown",
        model = "unknown",
        sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        antennaInfo = NfcAntennaInfo(
            1000,
            2000,
            false,
            emptyList(),
        ),
    ) {
        assertThat(resolver.get()).isEqualTo(TapZone(xBias = 0.5f, yBias = 0.5f))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun `get returns default tap zone when device dimensions are zero`() = runScenario(
        manufacturer = "unknown",
        model = "unknown",
        sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        antennaInfo = NfcAntennaInfo(
            0,
            2000,
            false,
            listOf(AvailableNfcAntenna(500, 400)),
        ),
    ) {
        assertThat(resolver.get()).isEqualTo(TapZone(xBias = 0.5f, yBias = 0.5f))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun `get computes tap zone from antenna location for unknown device`() = runScenario(
        manufacturer = "unknown",
        model = "unknown",
        sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        antennaInfo = NfcAntennaInfo(
            1000,
            2000,
            false,
            listOf(AvailableNfcAntenna(300, 400)),
        ),
    ) {
        assertThat(resolver.get()).isEqualTo(TapZone(xBias = 0.3f, yBias = 0.8f))
    }

    private fun runScenario(
        manufacturer: String,
        model: String,
        sdk: Int = Build.VERSION_CODES.TIRAMISU,
        antennaInfo: NfcAntennaInfo? = null,
        block: Scenario.() -> Unit,
    ) {
        val nfcHardwareDelegate = FakeNfcHardwareDelegate(antennaInfo = antennaInfo)
        val resolver = DefaultTapZoneResolver(
            nfcHardwareDelegate = nfcHardwareDelegate,
            manufacturer = manufacturer,
            model = model,
            sdk = sdk,
        )

        block(Scenario(resolver))
    }

    private class Scenario(
        val resolver: DefaultTapZoneResolver,
    )
}
