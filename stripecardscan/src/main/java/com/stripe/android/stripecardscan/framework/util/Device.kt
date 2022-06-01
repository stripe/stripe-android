package com.stripe.android.stripecardscan.framework.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import com.stripe.android.camera.framework.util.cacheFirstResult
import java.util.Locale

internal data class Device(
    val android_id: String?,
    val name: String,
    val bootCount: Int,
    val locale: String?,
    val carrier: String?,
    val networkOperator: String?,
    val phoneType: Int?,
    val phoneCount: Int,
    val osVersion: Int,
    val platform: String
) {
    companion object {
        private val getDeviceDetails = cacheFirstResult { context: Context? ->
            Device(
                android_id = getAndroidId(),
                name = getDeviceName(),
                bootCount = getDeviceBootCount(context),
                locale = getDeviceLocale(),
                carrier = getDeviceCarrier(context),
                networkOperator = getNetworkOperator(context),
                phoneType = getDevicePhoneType(context),
                phoneCount = getDevicePhoneCount(context),
                osVersion = getOsVersion(),
                platform = getPlatform()
            )
        }

        @JvmStatic
        fun fromContext(context: Context?) = getDeviceDetails(context?.applicationContext)
    }
}

/**
 * This was redacted for privacy reasons. Normally, this would be retrieved by this code:
 *
 * ```
 * Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
 * ```
 */
@SuppressLint("HardwareIds")
private fun getAndroidId() = "Redacted"

private fun getDeviceBootCount(context: Context?): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {
            context?.let {
                Settings.Global.getInt(it.contentResolver, Settings.Global.BOOT_COUNT)
            } ?: -1
        } catch (t: Throwable) {
            -1
        }
    } else {
        -1
    }

private fun getDeviceLocale(): String =
    "${Locale.getDefault().isO3Language}_${Locale.getDefault().isO3Country}"

private fun getDeviceCarrier(context: Context?) = try {
    (context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.networkOperatorName
} catch (t: Throwable) {
    null
}

private fun getDevicePhoneType(context: Context?) = try {
    (context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.phoneType
} catch (t: Throwable) {
    null
}

private fun getDevicePhoneCount(context: Context?) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                (context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)
                    ?.activeModemCount ?: -1
            } else {
                @Suppress("deprecation")
                (context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)
                    ?.phoneCount ?: -1
            }
        } catch (t: Throwable) {
            -1
        }
    } else {
        -1
    }

private fun getNetworkOperator(context: Context?) =
    (context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.networkOperator

internal fun getOsVersion() = Build.VERSION.SDK_INT

internal fun getPlatform() = "android"

/**
 * from https://stackoverflow.com/a/27836910/947883
 */
internal fun getDeviceName(): String {
    val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
    val model = Build.MODEL?.lowercase() ?: ""
    return if (model.startsWith(manufacturer)) {
        model
    } else {
        "$manufacturer $model"
    }
}
