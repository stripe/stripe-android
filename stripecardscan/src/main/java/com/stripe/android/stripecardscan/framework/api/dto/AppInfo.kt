package com.stripe.android.stripecardscan.framework.api.dto

import androidx.annotation.RestrictTo
import com.stripe.android.stripecardscan.framework.util.AppDetails
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AppInfo(
    @SerialName("app_package_name") val appPackageName: String?,
    @SerialName("application_id") val applicationId: String,
    @SerialName("library_package_name") val libraryPackageName: String,
    @SerialName("sdk_version") val sdkVersion: String,
    @SerialName("sdk_version_code") val sdkVersionCode: Int,
    @SerialName("sdk_flavor") val sdkFlavor: String,
    @SerialName("is_debug_build") val isDebugBuild: Boolean
) {
    companion object {
        internal fun fromAppDetails(appDetails: AppDetails): AppInfo = AppInfo(
            appPackageName = appDetails.appPackageName,
            applicationId = appDetails.applicationId,
            libraryPackageName = appDetails.libraryPackageName,
            sdkVersion = appDetails.sdkVersion,
            sdkVersionCode = appDetails.sdkVersionCode,
            sdkFlavor = appDetails.sdkFlavor,
            isDebugBuild = appDetails.isDebugBuild
        )
    }
}
