package com.stripe.android.core.networking

import android.content.Context
import android.os.Build
import androidx.annotation.RestrictTo
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.core.version.StripeSdkVersion

/**
 * Factory to generate [AnalyticsRequestV2], can be optionally configured to add the following
 * standard SDK specific parameters.
 *   os_version - Android version
 *   sdk_platform - always "android"
 *   sdk_version - current SDK version
 *   device_type - MANUFACTURER, brand and model
 *   app_name - the host application name
 *   app_version - the host app version
 *   plugin_type - whether SDK is integrated natively or through other wrappers(e.g react native)
 *   platform_info - information about current platform
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AnalyticsRequestV2Factory(
    context: Context,
    private val clientId: String,
    private val origin: String,
    private val pluginType: String = PLUGIN_NATIVE
) {
    private val appContext = context.applicationContext

    /**
     * Creates an [AnalyticsRequestV2] with parameters.
     *
     * @param includeSDKParams - whether to include default SDK params.
     */
    fun createRequestR(
        eventName: String,
        additionalParams: Map<String, Any> = mapOf(),
        includeSDKParams: Boolean = true
    ) = AnalyticsRequestV2(
        eventName = eventName,
        clientId = clientId,
        origin = origin,
        params = if (includeSDKParams) {
            additionalParams + sdkParams()
        } else {
            additionalParams
        }
    )

    // Common SDK related parameters, need dedicated ingestion logic on server side.
    private fun sdkParams() = mapOf(
        AnalyticsFields.OS_VERSION to Build.VERSION.SDK_INT,
        PARAM_SDK_PLATFORM to "android",
        PARAM_SDK_VERSION to StripeSdkVersion.VERSION_NAME,
        AnalyticsFields.DEVICE_TYPE to "${Build.MANUFACTURER}_${Build.BRAND}_${Build.MODEL}",
        AnalyticsFields.APP_NAME to getAppName(),
        AnalyticsFields.APP_VERSION to appContext.packageInfo?.versionCode,
        PARAM_PLUGIN_TYPE to pluginType,
        PARAM_PLATFORM_INFO to mapOf(
            PARAM_PACKAGE_NAME to appContext.packageName
        )
    )

    private fun getAppName(): CharSequence {
        return appContext.packageInfo?.applicationInfo?.loadLabel(appContext.packageManager)
            .takeUnless {
                it.isNullOrBlank()
            } ?: appContext.packageName
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        const val PLUGIN_NATIVE = "native"
        const val PLUGIN_REACT_NATIVE = "react-native"

        internal const val PARAM_SDK_PLATFORM = "sdk_platform"
        internal const val PARAM_SDK_VERSION = "sdk_version"
        internal const val PARAM_PLUGIN_TYPE = "plugin_type"
        internal const val PARAM_PLATFORM_INFO = "platform_info"
        internal const val PARAM_PACKAGE_NAME = "package_name"
    }
}
