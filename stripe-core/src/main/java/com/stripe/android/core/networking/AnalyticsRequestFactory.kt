package com.stripe.android.core.networking

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RestrictTo
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.version.StripeSdkVersion
import javax.inject.Provider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class AnalyticsRequestFactory(
    private val packageManager: PackageManager?,
    private val packageInfo: PackageInfo?,
    private val packageName: String,
    private val publishableKeyProvider: Provider<String>,
    internal val defaultProductUsageTokens: Set<String> = emptySet()
) {

    /**
     * Builds an Analytics request for the given [AnalyticsEvent],
     * including common params + event-specific params defined in [AnalyticsEvent.params]
     *
     * @param additionalParams any extra parameters that should be sent with this event.
     * Ensure this common parameters are not already included in [standardParams].
     *
     */
    fun createRequest(
        event: AnalyticsEvent,
        additionalParams: Map<String, Any>
    ): AnalyticsRequest {
        return AnalyticsRequest(
            params = createParams(event) + additionalParams,
            headers = RequestHeadersFactory.Analytics.create()
        )
    }

    /**
     * Backwards compatible for events not inheriting [AnalyticsEvent].
     *
     * @see createRequest
     */
    @Deprecated("use {@link #createRequest(AnalyticsEvent, Map<String, Any>)}")
    fun createRequest(
        event: String,
        additionalParams: Map<String, Any>
    ): AnalyticsRequest {
        return createRequest(
            event = object : AnalyticsEvent {
                override val eventName: String = event
            },
            additionalParams = additionalParams
        )
    }

    private fun createParams(
        event: AnalyticsEvent
    ): Map<String, Any> {
        return standardParams() + appDataParams() + event.params()
    }

    private fun AnalyticsEvent.params(): Map<String, String> {
        return mapOf(FIELD_EVENT to this.toString())
    }

    private fun standardParams(): Map<String, Any> = mapOf(
        FIELD_ANALYTICS_UA to ANALYTICS_UA,
        FIELD_PUBLISHABLE_KEY to runCatching {
            publishableKeyProvider.get()
        }.getOrDefault(ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY),
        FIELD_OS_NAME to Build.VERSION.CODENAME,
        FIELD_OS_RELEASE to Build.VERSION.RELEASE,
        FIELD_OS_VERSION to Build.VERSION.SDK_INT,
        FIELD_DEVICE_TYPE to DEVICE_TYPE,
        FIELD_BINDINGS_VERSION to StripeSdkVersion.VERSION_NAME,
        FIELD_IS_DEVELOPMENT to BuildConfig.DEBUG
    )

    internal fun appDataParams(): Map<String, Any> {
        return when {
            packageManager != null && packageInfo != null -> {
                mapOf(
                    FIELD_APP_NAME to getAppName(packageInfo, packageManager),
                    FIELD_APP_VERSION to packageInfo.versionCode
                )
            }
            else -> emptyMap()
        }
    }

    private fun getAppName(
        packageInfo: PackageInfo?,
        packageManager: PackageManager
    ): CharSequence {
        return packageInfo?.applicationInfo?.loadLabel(packageManager).takeUnless {
            it.isNullOrBlank()
        } ?: packageName
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        const val FIELD_ANALYTICS_UA = "analytics_ua"
        const val FIELD_APP_NAME = "app_name"
        const val FIELD_APP_VERSION = "app_version"
        const val FIELD_BINDINGS_VERSION = "bindings_version"
        const val FIELD_IS_DEVELOPMENT = "is_development"
        const val FIELD_DEVICE_TYPE = "device_type"
        const val FIELD_EVENT = "event"
        const val FIELD_OS_NAME = "os_name"
        const val FIELD_OS_RELEASE = "os_release"
        const val FIELD_OS_VERSION = "os_version"
        const val FIELD_PUBLISHABLE_KEY = "publishable_key"
        private const val ANALYTICS_PREFIX = "analytics"
        private const val ANALYTICS_NAME = "stripe_android"
        private const val ANALYTICS_VERSION = "1.0"

        private val DEVICE_TYPE: String = "${Build.MANUFACTURER}_${Build.BRAND}_${Build.MODEL}"

        const val ANALYTICS_UA = "$ANALYTICS_PREFIX.$ANALYTICS_NAME-$ANALYTICS_VERSION"
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AnalyticsEvent {
    /**
     * value that will be sent as [FIELD_EVENT] param.
     */
    val eventName: String
}
