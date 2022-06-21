package com.stripe.android.stripecardscan.framework.api.dto

import androidx.annotation.RestrictTo
import com.stripe.android.stripecardscan.framework.util.AppDetails
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class AppInfo(
    @SerialName("app_package_name") val appPackageName: String?,
    @SerialName("sdk_version") val sdkVersion: String,
    @SerialName("build") val build: String,
    @SerialName("is_debug_build") val isDebugBuild: Boolean
) {
    companion object {
        internal fun fromAppDetails(appDetails: AppDetails): AppInfo = AppInfo(
            appPackageName = appDetails.appPackageName,
            sdkVersion = appDetails.sdkVersion,
            build = appDetails.sdkVersion,
            isDebugBuild = appDetails.isDebugBuild
        )
    }
}
