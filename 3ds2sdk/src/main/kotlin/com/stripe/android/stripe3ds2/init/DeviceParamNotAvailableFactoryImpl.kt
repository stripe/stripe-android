package com.stripe.android.stripe3ds2.init

import android.os.Build
import androidx.annotation.VisibleForTesting
import com.stripe.android.stripe3ds2.utils.Supplier
import java.util.HashMap

/**
 * Creates a mapping of unavailable device params to reason codes, as defined in
 * "EMV® 3-D Secure SDK—Device Information - 2.8 Reasons for Device Parameters Unavailability"
 */
internal class DeviceParamNotAvailableFactoryImpl internal constructor(
    private val apiVersion: Int,
    private val hardwareIdSupplier: Supplier<HardwareId>
) : DeviceParamNotAvailableFactory {

    internal constructor(
        hardwareIdSupplier: Supplier<HardwareId>
    ) : this(
        Build.VERSION.SDK_INT,
        hardwareIdSupplier
    )

    override fun create(): Map<String, String> {
        return marketOrRegionRestrictionParams
            .plus(platformVersionParams)
            .plus(permissionParams)
    }

    @VisibleForTesting
    internal val marketOrRegionRestrictionParams: Map<String, String>
        get() {
            val params = HashMap<String, String>()

            val allowedParams = listOf(
                DeviceParam.PARAM_PLATFORM, DeviceParam.PARAM_DEVICE_MODEL,
                DeviceParam.PARAM_OS_NAME, DeviceParam.PARAM_OS_VERSION,
                DeviceParam.PARAM_LOCALE, DeviceParam.PARAM_TIME_ZONE,
                DeviceParam.PARAM_HARDWARE_ID, DeviceParam.PARAM_SCREEN_RESOLUTION
            )

            DeviceParam.values().forEach {
                if (!allowedParams.contains(it)) {
                    params[it.toString()] = Reason.MARKET_OR_REGION_RESTRICTION.toString()
                }
            }

            return params
        }

    @VisibleForTesting
    internal val platformVersionParams: Map<String, String>
        get() {
            val params = HashMap<String, String>()

            if (apiVersion < Build.VERSION_CODES.O) {
                params[DeviceParam.PARAM_TELE_IMEI_SV.toString()] =
                    Reason.PLATFORM_VERSION.toString()
                params[DeviceParam.PARAM_BUILD_SERIAL.toString()] =
                    Reason.PLATFORM_VERSION.toString()
                params[DeviceParam.PARAM_SECURE_INSTALL_NON_MARKET_APPS.toString()] =
                    Reason.PLATFORM_VERSION.toString()
            }

            if (apiVersion < Build.VERSION_CODES.M) {
                params[DeviceParam.PARAM_TELE_PHONE_COUNT.toString()] =
                    Reason.PLATFORM_VERSION.toString()
                params[DeviceParam.PARAM_TELE_IS_HEARING_AID_COMPATIBILITY_SUPPORTED.toString()] =
                    Reason.PLATFORM_VERSION.toString()
                params[DeviceParam.PARAM_TELE_IS_TTY_MODE_SUPPORTED.toString()] =
                    Reason.PLATFORM_VERSION.toString()
                params[DeviceParam.PARAM_TELE_IS_WORLD_PHONE.toString()] =
                    Reason.PLATFORM_VERSION.toString()
                params[DeviceParam.PARAM_BUILD_VERSION_PREVIEW_SDK_INT.toString()] =
                    Reason.PLATFORM_VERSION.toString()
                params[DeviceParam.PARAM_BUILD_VERSION_SDK_INT.toString()] =
                    Reason.PLATFORM_VERSION.toString()
                params[DeviceParam.PARAM_BUILD_VERSION_SECURITY_PATCH.toString()] =
                    Reason.PLATFORM_VERSION.toString()
                params[DeviceParam.PARAM_SYSTEM_DTMF_TONE_TYPE_WHEN_DIALING.toString()] =
                    Reason.PLATFORM_VERSION.toString()
                params[DeviceParam.PARAM_SYSTEM_VIBRATE_WHEN_RINGING.toString()] =
                    Reason.PLATFORM_VERSION.toString()
            }

            if (apiVersion > Build.VERSION_CODES.M) {
                params[DeviceParam.PARAM_SECURE_SYS_PROP_SETTING_VERSION.toString()] =
                    Reason.PLATFORM_VERSION.toString()
            }

            if (apiVersion < Build.VERSION_CODES.LOLLIPOP_MR1) {
                params[DeviceParam.PARAM_TELE_IS_VOICE_CAPABLE.toString()] =
                    Reason.PLATFORM_VERSION.toString()
            }

            return params
        }

    @VisibleForTesting
    internal val permissionParams: Map<String, String>
        get() {
            val params = HashMap<String, String>()

            params[DeviceParam.PARAM_WIFI_MAC.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_WIFI_BSSID.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_WIFI_SSID.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_WIFI_NETWORK_ID.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_WIFI_IS_5GHZ_BAND_SUPPORTED.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_WIFI_IS_DEVICE_TO_AP_RTT_SUPPORTED.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_WIFI_IS_ENHANCED_POWER_REPORTING_SUPPORTED.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_WIFI_IS_P2P_SUPPORTED.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_WIFI_IS_PREFERRED_NETWORK_OFFLOAD_SUPPORTED.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_WIFI_IS_SCAN_ALWAYS_AVAILABLE.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_WIFI_IS_TDLS_SUPPORTED.toString()] =
                Reason.PERMISSION.toString()

            params[DeviceParam.PARAM_LATITUDE.toString()] = Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_LONGITUDE.toString()] = Reason.PERMISSION.toString()

            if (!hardwareIdSupplier.get().isPresent) {
                params[DeviceParam.PARAM_HARDWARE_ID.toString()] = Reason.PLATFORM_VERSION.toString()
            }

            params[DeviceParam.PARAM_DEVICE_NAME.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_BLUETOOTH_ADDRESS.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_BLUETOOTH_BONDED_DEVICE.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_BLUETOOTH_IS_ENABLED.toString()] =
                Reason.PERMISSION.toString()

            params[DeviceParam.PARAM_TELE_DEVICE_ID.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_TELE_SUBSCRIBER_ID.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_TELE_IMEI_SV.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_TELE_GROUP_IDENTIFIER_L1.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_TELE_SIM_SERIAL_NUMBER.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_TELE_VOICE_MAIL_ALPHA_TAG.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_TELE_VOICE_MAIL_NUMBER.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_TELE_IS_TTY_MODE_SUPPORTED.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_TELE_IS_WORLD_PHONE.toString()] =
                Reason.PERMISSION.toString()
            params[DeviceParam.PARAM_BUILD_SERIAL.toString()] =
                Reason.PERMISSION.toString()

            params[DeviceParam.PARAM_SECURE_INSTALL_NON_MARKET_APPS.toString()] =
                Reason.PERMISSION.toString()

            return params
        }

    internal enum class Reason(private val code: String) {
        MARKET_OR_REGION_RESTRICTION("RE01"),
        PLATFORM_VERSION("RE02"),
        PERMISSION("RE03");

        override fun toString(): String {
            return code
        }
    }
}
