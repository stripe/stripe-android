package com.stripe.android.stripe3ds2.init

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager.FEATURE_TELEPHONY_IMS
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.webkit.WebSettings
import androidx.core.os.LocaleListCompat
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Creates a Map populated with device identification data as defined in
 * "EMV® 3-D Secure SDK—Device Information".
 */
internal class DeviceDataFactoryImpl internal constructor(
    context: Context,
    private val appInfoRepository: AppInfoRepository,
    private val messageVersionRegistry: MessageVersionRegistry,
) : DeviceDataFactory {
    private val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    private val defaultUserAgent = WebSettings.getDefaultUserAgent(context)
    private val telephonyManager =
        (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
    private val secureFRPMode =
        Settings.Secure.getInt(context.contentResolver, Settings.Global.SECURE_FRP_MODE, 0)
    private val audioManager = (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
    private val packageManager = context.packageManager
    private val apiVersion = Build.VERSION.SDK_INT
    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
    private val dateTime = dateFormat.format(Calendar.getInstance().time)

    private val codeName = buildCodeName() ?: "UNKNOWN"
    private val osName = "Android " + codeName + " " + Build.VERSION.RELEASE + " API " + apiVersion
    private val timeZone = TimeZone.getDefault().rawOffset / MILLIS_IN_SECOND / SECONDS_IN_MINUTE

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    override suspend fun create(
        sdkReferenceNumber: String,
        sdkTransactionId: SdkTransactionId
    ): Map<String, Any?> {
        val map = hashMapOf(
            DeviceParam.PARAM_PLATFORM.toString() to "Android",
            DeviceParam.PARAM_DEVICE_MODEL.toString() to Build.MANUFACTURER + "||" + Build.MODEL,
            DeviceParam.PARAM_OS_NAME.toString() to osName,
            DeviceParam.PARAM_OS_VERSION.toString() to Build.VERSION.RELEASE,
            DeviceParam.PARAM_LOCALE.toString() to
                LocaleListCompat.create(Locale.getDefault()).toLanguageTags(),
            DeviceParam.PARAM_TIME_ZONE.toString() to timeZone.toString(),
            DeviceParam.PARAM_SCREEN_RESOLUTION.toString() to
                String.format(
                    Locale.ROOT,
                    "%sx%s",
                    displayMetrics.heightPixels,
                    displayMetrics.widthPixels
                ),
            DeviceParam.PARAM_SDK_APP_ID.toString() to appInfoRepository.get().sdkAppId,
            DeviceParam.PARAM_SDK_VERSION.toString() to messageVersionRegistry.current,
            DeviceParam.PARAM_SDK_REF_NUMBER.toString() to sdkReferenceNumber,
            DeviceParam.PARAM_DATE_TIME.toString() to dateTime,
            DeviceParam.PARAM_SDK_TRANS_ID.toString() to sdkTransactionId.toString(),
            DeviceParam.PARAM_WEB_VIEW_USER_AGENT.toString() to defaultUserAgent,
        )

        if (apiVersion >= Build.VERSION_CODES.P) {
            map[DeviceParam.PARAM_SIM_CARRIER_ID.toString()] =
                telephonyManager.simCarrierId.toString()
            map[DeviceParam.PARAM_SIM_CARRIER_ID_NAME.toString()] =
                telephonyManager.simCarrierIdName.toString()
        }

        if (apiVersion >= Build.VERSION_CODES.Q) {
            map[DeviceParam.PARAM_SIM_SPECIFIC_CARRIER_ID.toString()] =
                telephonyManager.simSpecificCarrierId.toString()
            map[DeviceParam.PARAM_SIM_SPECIFIC_CARRIER_ID_NAME.toString()] =
                telephonyManager.simSpecificCarrierIdName.toString()

            if (packageManager.hasSystemFeature(FEATURE_TELEPHONY_IMS)) {
                map[DeviceParam.PARAM_RTT_CALLING_MODE.toString()] =
                    telephonyManager.isRttSupported.toString()
            }
        }

        if (apiVersion >= Build.VERSION_CODES.R) {
            map[DeviceParam.PARAM_SUBSCRIPTION_ID.toString()] =
                telephonyManager.subscriptionId.toString()
            map[DeviceParam.PARAM_SECURE_FRP_MODE.toString()] =
                if (secureFRPMode == 1) "true" else "false"
        }

        if (apiVersion >= Build.VERSION_CODES.S) {
            map[DeviceParam.PARAM_HARDWARE_SKU.toString()] =
                Build.SKU
            map[DeviceParam.PARAM_SOC_MANUFACTURER.toString()] =
                Build.SOC_MANUFACTURER
            map[DeviceParam.PARAM_SOC_MODEL.toString()] =
                Build.SOC_MODEL
        }

        if (apiVersion >= Build.VERSION_CODES.TIRAMISU) {
            map[DeviceParam.PARAM_APPLY_RAMPING_RINGER.toString()] =
                audioManager.isRampingRingerEnabled.toString()
        }

        return map
    }

    private fun buildCodeName(): String? {
        val fields: Array<Field> = Build.VERSION_CODES::class.java.fields
        for (field in fields) {
            val fieldName: String = field.name
            var fieldValue = -1

            try {
                fieldValue = field.getInt(Any())
            } catch (_: IllegalArgumentException) {
            } catch (_: IllegalAccessException) {
            } catch (_: NullPointerException) {
            }

            if (fieldValue == Build.VERSION.SDK_INT) {
                return fieldName
            }
        }

        return null
    }

    companion object {
        private const val MILLIS_IN_SECOND = 1000
        private const val SECONDS_IN_MINUTE = 60
    }
}
