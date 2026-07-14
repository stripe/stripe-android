package com.stripe.android.common.nfcscan.tapzone

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegate
import javax.inject.Inject

internal interface TapZoneResolver {
    fun get(): TapZone
}

internal class DefaultTapZoneResolver @Inject constructor(
    private val nfcHardwareDelegate: NfcHardwareDelegate,
    @DeviceManufacturer private val manufacturer: String,
    @DeviceModel private val model: String,
    @SdkVersion private val sdk: Int,
) : TapZoneResolver {
    override fun get(): TapZone {
        val manufacturer = manufacturer.trim().lowercase()
        val model = standardizeModelName(manufacturer, model)

        return deviceTapZoneMapping[manufacturer]?.get(model) ?: run {
            if (isAtLeastUpsideDownCake(sdk)) {
                getTapZoneFromAvailableAntennae()
            } else {
                DEFAULT
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun getTapZoneFromAvailableAntennae(): TapZone =
        nfcHardwareDelegate.antenna()?.let { info ->
            if (info.availableNfcAntennas.isEmpty()) {
                return DEFAULT
            } else if (info.deviceWidth == 0 || info.deviceHeight == 0) {
                return DEFAULT
            }

            val antenna = info.availableNfcAntennas.first()
            val xBias = antenna.locationX / info.deviceWidth.toFloat()
            val yBias = 1 - antenna.locationY / info.deviceHeight.toFloat()

            TapZone(xBias, yBias)
        } ?: run {
            DEFAULT
        }

    private fun standardizeModelName(manufacturer: String, model: String): String {
        var name = model.trim().lowercase()
        // Ignore chipset variants
        when (manufacturer) {
            "samsung" -> {
                name = name.take(n = 7)

                if (name.length >= SAMSUNG_REPLACEMENT_INDEX) {
                    // Samsung will sometimes have this character be '-', '_' or '5'
                    name = name.replaceRange(startIndex = 2, endIndex = SAMSUNG_REPLACEMENT_INDEX, replacement = "-")
                }
            }

            "oneplus" -> {
                if ("[a-z]\\d+".toRegex().matches(name)) {
                    // OnePlus 6 and earlier naming scheme is different from literally everything else
                    name = name.take(n = 4)
                } else if ("[a-z]{2}\\d+".toRegex().matches(name)) {
                    // OnePlus's normal naming scheme
                    name = name.take(n = 5)
                }
                // else OnePlus is using Oppo model naming schemes (starting with CPH+)
            }

            "xiaomi" -> {
                name = if (name.startsWith("m")) {
                    name.take(n = 5)
                } else {
                    name.take(n = 4)
                }
            }
        }
        return name
    }

    @ChecksSdkIntAtLeast(parameter = 0)
    private fun isAtLeastUpsideDownCake(sdk: Int): Boolean {
        return sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }

    private companion object {
        const val SAMSUNG_REPLACEMENT_INDEX = 3

        val DEFAULT = TapZone(xBias = 0.5f, yBias = 0.5f)
        val deviceTapZoneMapping = mapOf(
            // Samsung NFC locations are guesstimates based on https://www.samsung.com/hk_en/nfc-support/ or
            // teardown videos
            "samsung" to mapOf(
                // S22+
                "SM-S906".lowercase() to TapZone(0.5f, 0.5f),
                // S22
                "SM-S901".lowercase() to TapZone(0.5f, 0.5f),
                // S22 ultra
                "SM-S908".lowercase() to TapZone(0.5f, 0.5f),
                // S23
                "SM-S911".lowercase() to TapZone(0.5f, 0.5f),
                // S23+
                "SM-S916".lowercase() to TapZone(0.5f, 0.5f),
                // S23 ultra
                "SM-S918".lowercase() to TapZone(0.5f, 0.5f),
                // S24
                "SM-S921".lowercase() to TapZone(0.5f, 0.22f),
                // S24+
                "SM-S926".lowercase() to TapZone(0.5f, 0.22f),
                // S24 ultra
                "SM-S928".lowercase() to TapZone(0.5f, 0.22f),
                // S24 FE
                "SM-S721".lowercase() to TapZone(0.5f, 0.6f),
                // A14
                "SM-A145".lowercase() to TapZone(0.5f, 0.2f),
                // A14 5G
                "SM-A146".lowercase() to TapZone(0.5f, 0.2f),
                // A24 4G
                "SM-A245".lowercase() to TapZone(0.5f, 0.2f),
                // A25
                "SM-A256".lowercase() to TapZone(1f, 0f),
                // A33 5G
                "SM-A336".lowercase() to TapZone(1f, 0f),
                // A34
                "SM-A346".lowercase() to TapZone(1f, 0f),
                // A35
                "SM-A356".lowercase() to TapZone(0.5f, 0.2f),
                // A53 5G
                "SM-A536".lowercase() to TapZone(0.5f, 0.2f),
                // A54
                "SM-A546".lowercase() to TapZone(0.5f, 0.2f),
                // A55
                "SM-A556".lowercase() to TapZone(1f, 0f),
                // Tab Active5
                "SM-X300".lowercase() to TapZone(0.25f, 0f),
                "SM-X306".lowercase() to TapZone(0.25f, 0f),
                "SM-X308".lowercase() to TapZone(0.25f, 0f),
            ),
            "google" to mapOf(
                "Pixel 6".lowercase() to TapZone(0.5f, 0.4f),
                "Pixel 6 Pro".lowercase() to TapZone(0.5f, 0.35f),
                "Pixel 6a".lowercase() to TapZone(0.5f, 0.55f),
                "Pixel 7".lowercase() to TapZone(0.5f, 0.25f),
                "Pixel 7 Pro".lowercase() to TapZone(0.5f, 0.25f),
                "Pixel 7a".lowercase() to TapZone(0.5f, 0.35f),
                "Pixel 8".lowercase() to TapZone(0.5f, 0.3f),
                "Pixel 8 Pro".lowercase() to TapZone(0.5f, 0.3f),
                "Pixel 8a".lowercase() to TapZone(0.5f, 0.4f),
                "Pixel 9".lowercase() to TapZone(0.5f, 0.31f),
                "Pixel 9 Pro".lowercase() to TapZone(0.5f, 0.31f),
                "Pixel 9 Pro XL".lowercase() to TapZone(0.5f, 0.3f)
            ),
            // Mostly estimated from teardown or unboxing videos
            "oneplus" to mapOf(
                // 10R
                "CPH2423".lowercase() to TapZone(1f, 0f),
                // 10 Pro 5G
                "NE221".lowercase() to TapZone(0.5f, 0.2f),
                // 11
                "CPH2449".lowercase() to TapZone(0.5f, 0.2f),
                "CPH2447".lowercase() to TapZone(0.5f, 0.2f),
                "CPH2451".lowercase() to TapZone(0.5f, 0.2f),
                // 11R
                "CPH2487".lowercase() to TapZone(0.5f, 0.2f),
            ),
            /*
             * A quick note about OPPO:
             * - Model names that begin with "P" are omitted since they are targeted to Chinese markets
             * - OPPO does not print the NFC location in their user manuals or on the protective plastic,
             *   so the only way to figure out where it is is via teardown videos, which are mostly not in English.
             * - The accuracy of the below tap zones is therefore probably much lower than other OEMs
             *   with better documentation.
             */
            "oppo" to mapOf(
                // Find X5 Pro
                "CPH2305".lowercase() to TapZone(0.5f, 0.5f),
            ),
            "xiaomi" to mapOf(
                // Redmi Note 11
                "2201".lowercase() to TapZone(1f, 0f),
                // Redmi Note 12
                "2211".lowercase() to TapZone(1f, 0f),
            ),
        )
    }
}
