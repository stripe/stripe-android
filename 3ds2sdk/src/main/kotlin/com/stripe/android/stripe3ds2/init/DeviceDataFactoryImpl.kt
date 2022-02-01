package com.stripe.android.stripe3ds2.init

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import androidx.core.os.LocaleListCompat
import com.stripe.android.stripe3ds2.utils.Supplier
import java.util.Locale
import java.util.TimeZone

/**
 * Creates a Map populated with device identification data as defined in
 * "EMV® 3-D Secure SDK—Device Information".
 */
internal class DeviceDataFactoryImpl internal constructor(
    context: Context,
    private val hardwareIdSupplier: Supplier<HardwareId>
) : DeviceDataFactory {
    private val displayMetrics: DisplayMetrics = context.resources.displayMetrics

    override fun create(): Map<String, Any?> {
        val hardwareId = hardwareIdSupplier.get().value
        return mapOf(
            DeviceParam.PARAM_PLATFORM.toString() to "Android",
            DeviceParam.PARAM_DEVICE_MODEL.toString() to Build.MODEL,
            DeviceParam.PARAM_OS_NAME.toString() to Build.VERSION.CODENAME,
            DeviceParam.PARAM_OS_VERSION.toString() to Build.VERSION.RELEASE,
            DeviceParam.PARAM_LOCALE.toString() to
                LocaleListCompat.create(Locale.getDefault()).toLanguageTags(),
            DeviceParam.PARAM_TIME_ZONE.toString() to TimeZone.getDefault().displayName,
            DeviceParam.PARAM_SCREEN_RESOLUTION.toString() to
                String.format(
                    Locale.ROOT, "%sx%s",
                    displayMetrics.heightPixels, displayMetrics.widthPixels
                )
        ).plus(
            if (hardwareId.isNotEmpty()) {
                mapOf(DeviceParam.PARAM_HARDWARE_ID.toString() to hardwareId)
            } else {
                emptyMap()
            }
        )
    }
}
