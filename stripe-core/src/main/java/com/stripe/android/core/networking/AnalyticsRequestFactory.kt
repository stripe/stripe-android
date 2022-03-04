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
        return mapOf(AnalyticsFields.EVENT to this.toString())
    }

    private fun standardParams(): Map<String, Any> = mapOf(
        AnalyticsFields.ANALYTICS_UA to ANALYTICS_UA,
        AnalyticsFields.PUBLISHABLE_KEY to runCatching {
            publishableKeyProvider.get()
        }.getOrDefault(ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY),
        AnalyticsFields.OS_NAME to Build.VERSION.CODENAME,
        AnalyticsFields.OS_RELEASE to Build.VERSION.RELEASE,
        AnalyticsFields.OS_VERSION to Build.VERSION.SDK_INT,
        AnalyticsFields.DEVICE_TYPE to DEVICE_TYPE,
        AnalyticsFields.BINDINGS_VERSION to StripeSdkVersion.VERSION_NAME,
        AnalyticsFields.IS_DEVELOPMENT to BuildConfig.DEBUG
    )

    internal fun appDataParams(): Map<String, Any> {
        return when {
            packageManager != null && packageInfo != null -> {
                mapOf(
                    AnalyticsFields.APP_NAME to getAppName(packageInfo, packageManager),
                    AnalyticsFields.APP_VERSION to packageInfo.versionCode
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
     * value that will be sent as [AnalyticsFields.EVENT] param.
     */
    val eventName: String
}
