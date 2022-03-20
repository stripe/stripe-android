package com.stripe.android.connections.analytics

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.stripe.android.connections.BuildConfig
import com.stripe.android.connections.di.PUBLISHABLE_KEY
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.RequestHeadersFactory
import com.stripe.android.core.version.StripeSdkVersion
import javax.inject.Inject
import javax.inject.Named

internal class ConnectionsAnalyticsRequestFactory @Inject constructor(
    application: Application,
    @Named(PUBLISHABLE_KEY) val publishableKeyProvider: String,
) {

    private val packageManager: PackageManager by lazy { application.packageManager }
    private val packageName: String by lazy { application.packageName.orEmpty() }
    private val packageInfo: PackageInfo? by lazy {
        runCatching { packageManager.getPackageInfo(packageName, 0) }.getOrNull()
    }

    internal fun createRequest(
        event: ConnectionsAnalyticsEvent
    ): AnalyticsRequest {
        return AnalyticsRequest(
            params = createParams(event),
            headers = RequestHeadersFactory.Analytics.create()
        )
    }

    private fun createParams(
        event: ConnectionsAnalyticsEvent
    ): Map<String, Any> {
        return createStandardParams(event).plus(createAppDataParams())
    }

    // TODO(carlosmuvi) share with PaymentAnalyticsRequestFactory.createStandardParams
    private fun createStandardParams(
        event: ConnectionsAnalyticsEvent
    ): Map<String, Any> {
        return mapOf(
            FIELD_ANALYTICS_UA to ANALYTICS_UA,
            FIELD_EVENT to event.eventCode.toString(),
            FIELD_PUBLISHABLE_KEY to publishableKeyProvider,
            FIELD_OS_NAME to Build.VERSION.CODENAME,
            FIELD_OS_RELEASE to Build.VERSION.RELEASE,
            FIELD_OS_VERSION to Build.VERSION.SDK_INT,
            FIELD_DEVICE_TYPE to DEVICE_TYPE,
            FIELD_BINDINGS_VERSION to StripeSdkVersion.VERSION_NAME,
            FIELD_IS_DEVELOPMENT to BuildConfig.DEBUG
        ).plus(event.additionalParams)
    }

    // TODO(carlosmuvi) share with PaymentAnalyticsRequestFactory.createAppDataParams
    private fun createAppDataParams(): Map<String, Any> {
        return when {
            packageInfo != null -> {
                mapOf(
                    FIELD_APP_NAME to getAppName(packageInfo, packageManager),
                    FIELD_APP_VERSION to packageInfo!!.versionCode
                )
            }
            else -> emptyMap()
        }
    }

    // TODO(carlosmuvi) share with PaymentAnalyticsRequestFactory.getAppName
    private fun getAppName(
        packageInfo: PackageInfo?,
        packageManager: PackageManager
    ): CharSequence {
        return packageInfo?.applicationInfo?.loadLabel(packageManager).takeUnless {
            it.isNullOrBlank()
        } ?: packageName
    }

    internal companion object {
        internal const val FIELD_ANALYTICS_UA = "analytics_ua"
        internal const val FIELD_APP_NAME = "app_name"
        internal const val FIELD_APP_VERSION = "app_version"
        internal const val FIELD_BINDINGS_VERSION = "bindings_version"
        internal const val FIELD_IS_DEVELOPMENT = "is_development"
        internal const val FIELD_DEVICE_TYPE = "device_type"
        internal const val FIELD_EVENT = "event"
        internal const val FIELD_OS_NAME = "os_name"
        internal const val FIELD_OS_RELEASE = "os_release"
        internal const val FIELD_OS_VERSION = "os_version"
        internal const val FIELD_PUBLISHABLE_KEY = "publishable_key"
        private const val ANALYTICS_PREFIX = "analytics"
        private const val ANALYTICS_NAME = "stripe_android"
        private const val ANALYTICS_VERSION = "1.0"

        private val DEVICE_TYPE: String = "${Build.MANUFACTURER}_${Build.BRAND}_${Build.MODEL}"

        internal const val ANALYTICS_UA = "$ANALYTICS_PREFIX.$ANALYTICS_NAME-$ANALYTICS_VERSION"
    }
}
