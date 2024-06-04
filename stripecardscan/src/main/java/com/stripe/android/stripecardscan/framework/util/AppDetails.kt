package com.stripe.android.stripecardscan.framework.util

import android.content.Context
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.stripecardscan.BuildConfig

internal data class AppDetails(
    val appPackageName: String?,
    val applicationId: String,
    val libraryPackageName: String,
    val sdkVersion: String,
    val sdkVersionCode: Int,
    val sdkFlavor: String,
    val isDebugBuild: Boolean
) {
    companion object {
        @JvmStatic
        fun fromContext(context: Context?) = AppDetails(
            appPackageName = getAppPackageName(context),
            applicationId = getApplicationId(),
            libraryPackageName = getLibraryPackageName(),
            sdkVersion = getSdkVersion(),
            sdkVersionCode = getSdkVersionCode(),
            sdkFlavor = getSdkFlavor(),
            isDebugBuild = isDebugBuild()
        )
    }
}

internal fun getAppPackageName(context: Context?): String? =
    context?.applicationContext?.packageName

private fun getApplicationId(): String = "" // no longer available in later versions of gradle.

internal fun getLibraryPackageName(): String = BuildConfig.LIBRARY_PACKAGE_NAME

internal fun getSdkVersion(): String = StripeSdkVersion.VERSION_NAME

private fun getSdkVersionCode(): Int = -1 // no longer available in later versions of gradle.

internal fun getSdkFlavor(): String = BuildConfig.BUILD_TYPE

private fun isDebugBuild(): Boolean = BuildConfig.DEBUG
