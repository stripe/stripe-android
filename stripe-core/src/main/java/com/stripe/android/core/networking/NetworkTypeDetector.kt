package com.stripe.android.core.networking

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.telephony.TelephonyManager
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class NetworkTypeDetector private constructor(
    private val connectivityManager: ConnectivityManager,
) {

    constructor(context: Context) : this(
        connectivityManager = context.applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager,
    )

    @Suppress("DEPRECATION")
    operator fun invoke(): String? {
        val networkInfo = connectivityManager.activeNetworkInfo

        if (networkInfo == null || !networkInfo.isConnected) {
            return null
        }

        val networkType = when (networkInfo.type) {
            ConnectivityManager.TYPE_WIFI -> NetworkType.WiFi
            ConnectivityManager.TYPE_MOBILE -> determineMobileNetworkType(networkInfo.subtype)
            else -> NetworkType.Unknown
        }

        return networkType.value
    }

    private fun determineMobileNetworkType(subtype: Int): NetworkType {
        return when (subtype) {
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN,
            TelephonyManager.NETWORK_TYPE_GSM -> {
                NetworkType.Mobile2G
            }
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> {
                NetworkType.Mobile3G
            }
            TelephonyManager.NETWORK_TYPE_LTE,
            TelephonyManager.NETWORK_TYPE_IWLAN,
            // This is for the non-exposed TelephonyManager.NETWORK_TYPE_LTE_CA
            19 -> {
                NetworkType.Mobile4G
            }
            TelephonyManager.NETWORK_TYPE_NR -> {
                NetworkType.Mobile5G
            }
            else -> {
                NetworkType.Unknown
            }
        }
    }

    private enum class NetworkType(val value: String) {
        WiFi("Wi-Fi"),
        Mobile2G("2G"),
        Mobile3G("3G"),
        Mobile4G("4G"),
        Mobile5G("5G"),
        Unknown("unknown"),
    }
}
