package com.stripe.android.common.nfcscan.security

import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IsDeviceSecureForNfcTest {
    @After
    fun tearDown() {
        FeatureFlags.disableNfcScanningSecurity.reset()
    }

    @Test
    fun `returns true when all security checks pass`() = runScenario {
        assertThat(isDeviceSecureForNfc.get()).isTrue()
    }

    @Test
    fun `returns true when property values are unavailable`() = runScenario(
        globalProperties = FakePlatformDeviceProperties(
            booleanValues = mapOf(
                DEVELOPMENT_SETTINGS_ENABLED to null,
                ADB_ENABLED to null,
                ADB_WIFI_ENABLED to null,
            ),
        ),
        osProperties = FakePlatformDeviceProperties(
            stringValues = mapOf(NFC_SNOOP_LOG_MODE to null),
            booleanValues = mapOf(NFC_VENDOR_DEBUG_ENABLED to null),
        ),
    ) {
        assertThat(isDeviceSecureForNfc.get()).isTrue()
    }

    @Test
    fun `returns false when developer mode is enabled`() = runScenario(
        globalProperties = FakePlatformDeviceProperties(
            booleanValues = mapOf(DEVELOPMENT_SETTINGS_ENABLED to true),
        ),
    ) {
        assertThat(isDeviceSecureForNfc.get()).isFalse()
    }

    @Test
    fun `returns false when USB debugging is enabled`() = runScenario(
        globalProperties = FakePlatformDeviceProperties(
            booleanValues = mapOf(ADB_ENABLED to true),
        ),
    ) {
        assertThat(isDeviceSecureForNfc.get()).isFalse()
    }

    @Test
    fun `returns false when Wi-Fi debugging is enabled`() = runScenario(
        globalProperties = FakePlatformDeviceProperties(
            booleanValues = mapOf(ADB_WIFI_ENABLED to true),
        ),
    ) {
        assertThat(isDeviceSecureForNfc.get()).isFalse()
    }

    @Test
    fun `returns false when NFC snoop log mode is full`() = runScenario(
        osProperties = FakePlatformDeviceProperties(
            stringValues = mapOf(NFC_SNOOP_LOG_MODE to NFC_SNOOP_LOG_MODE_FULL),
        ),
    ) {
        assertThat(isDeviceSecureForNfc.get()).isFalse()
    }

    @Test
    fun `returns false when NFC vendor debug is enabled`() = runScenario(
        osProperties = FakePlatformDeviceProperties(
            booleanValues = mapOf(NFC_VENDOR_DEBUG_ENABLED to true),
        ),
    ) {
        assertThat(isDeviceSecureForNfc.get()).isFalse()
    }

    @Test
    fun `returns true when security checks are disabled by feature flag`() = runScenario(
        globalProperties = FakePlatformDeviceProperties(
            booleanValues = mapOf(
                DEVELOPMENT_SETTINGS_ENABLED to true,
                ADB_ENABLED to true,
                ADB_WIFI_ENABLED to true,
            ),
        ),
        osProperties = FakePlatformDeviceProperties(
            stringValues = mapOf(NFC_SNOOP_LOG_MODE to NFC_SNOOP_LOG_MODE_FULL),
            booleanValues = mapOf(NFC_VENDOR_DEBUG_ENABLED to true),
        ),
    ) {
        FeatureFlags.disableNfcScanningSecurity.setEnabled(true)

        assertThat(isDeviceSecureForNfc.get()).isTrue()
    }

    private fun runScenario(
        globalProperties: FakePlatformDeviceProperties = FakePlatformDeviceProperties(),
        osProperties: FakePlatformDeviceProperties = FakePlatformDeviceProperties(),
        block: Scenario.() -> Unit,
    ) {
        val isDeviceSecureForNfc = DefaultIsDeviceSecureForNfc(
            osProperties = osProperties,
            globalProperties = globalProperties,
        )

        Scenario(isDeviceSecureForNfc = isDeviceSecureForNfc).block()
    }

    private data class Scenario(
        val isDeviceSecureForNfc: DefaultIsDeviceSecureForNfc,
    )

    private class FakePlatformDeviceProperties(
        private val stringValues: Map<String, String?> = emptyMap(),
        private val booleanValues: Map<String, Boolean?> = emptyMap(),
    ) : PlatformDeviceProperties {
        override fun getString(key: String): String? = stringValues[key]
        override fun getBoolean(key: String): Boolean? = booleanValues[key]
    }

    private companion object {
        const val DEVELOPMENT_SETTINGS_ENABLED = Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
        const val ADB_ENABLED = Settings.Global.ADB_ENABLED
        const val ADB_WIFI_ENABLED = "adb_wifi_enabled"
        const val NFC_SNOOP_LOG_MODE = "persist.nfc.snoop_log_mode"
        const val NFC_VENDOR_DEBUG_ENABLED = "persist.nfc.vendor_debug_enabled"
        const val NFC_SNOOP_LOG_MODE_FULL = "full"
    }
}
