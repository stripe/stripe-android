package com.stripe.android.common.nfcscan.security

import android.provider.Settings
import com.stripe.android.core.utils.FeatureFlags
import javax.inject.Inject

internal fun interface IsDeviceSecureForNfc {
    fun get(): Boolean
}

/**
 * Default implementation of [IsDeviceSecureForNfc] that checks if the user's device is secure for NFC scanning
 *
 * In addition to checking if the developer option master toggle is enabled, this also checks if various other toggles
 * are enabled, including toggles that could:
 * - Allow debugging over USB or Wi-Fi
 * - Log sensitive NFC data directly to logcat
 */
internal class DefaultIsDeviceSecureForNfc @Inject constructor(
    @OsSettingsDevicePropertiesKey private val osProperties: PlatformDeviceProperties,
    @GlobalSettingsDevicePropertiesKey private val globalProperties: PlatformDeviceProperties,
) : IsDeviceSecureForNfc {
    override fun get(): Boolean {
        // Override security checks if flag is enabled
        if (FeatureFlags.disableNfcScanningSecurity.isEnabled) {
            return true
        }

        // Check for developer mode and debugging toggles
        val isDeveloperModeDisabled = globalProperties.getBoolean(TOGGLE_DEVELOPER_MODE) != true
        val isUsbDebuggingDisabled = globalProperties.getBoolean(USB_DEBUGGING) != true
        val isWifiDebuggingDisabled = globalProperties.getBoolean(WIFI_DEBUGGING) != true

        // Check for verbose NFC logging toggles, which could expose sensitive NFC message data
        val isNotSnoopLogModeFull = osProperties.getString(NFC_SNOOP_LOG_MODE) != NFC_SNOOP_LOG_MODE_FULL
        val vendorDebugDisabled = osProperties.getBoolean(NFC_VENDOR_DEBUG_ENABLED) != true

        return isDeveloperModeDisabled &&
            isUsbDebuggingDisabled &&
            isWifiDebuggingDisabled &&
            isNotSnoopLogModeFull &&
            vendorDebugDisabled
    }

    private companion object {
        const val TOGGLE_DEVELOPER_MODE = Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
        const val USB_DEBUGGING = Settings.Global.ADB_ENABLED
        const val WIFI_DEBUGGING = "adb_wifi_enabled"
        const val NFC_SNOOP_LOG_MODE = "persist.nfc.snoop_log_mode"
        const val NFC_VENDOR_DEBUG_ENABLED = "persist.nfc.vendor_debug_enabled"
        const val NFC_SNOOP_LOG_MODE_FULL = "full"
    }
}
